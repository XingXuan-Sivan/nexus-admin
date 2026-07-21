package com.nexusadmin.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusadmin.api.domain.result.ProblemDetail;
import com.nexusadmin.api.domain.result.StatusCodes;
import com.nexusadmin.api.util.HttpAuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 权限检查拦截器，基于 {@link RequirePermission} 注解对管理面板 API 进行权限校验。
 *
 * <p>校验逻辑：</p>
 * <ul>
 *   <li>无 @RequirePermission 注解 → 直接放行</li>
 *   <li>注解值为 "*" → 仅检查是否已认证</li>
 *   <li>注解值为具体权限标识 → 委托 PermissionResolver 拓展点校验</li>
 * </ul>
 */
public class PermissionInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PermissionInterceptor.class);

    private static final String TYPE_BASE = "https://nexusadmin.io/probs/";

    private final PermissionAccess permissionAccess;
    private final ObjectMapper objectMapper;

    /**
     * 构造权限检查拦截器。
     *
     * @param resolverConsumer 权限解析器扩展点消费者
     * @param objectMapper     JSON 序列化器
     */
    public PermissionInterceptor(PermissionAccess permissionAccess,
                                 ObjectMapper objectMapper) {
        this.permissionAccess = permissionAccess;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 仅处理 Controller 方法
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 获取 @RequirePermission 注解（优先方法级，其次类级）
        RequirePermission methodAnnotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        RequirePermission classAnnotation = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        RequirePermission annotation = methodAnnotation != null ? methodAnnotation : classAnnotation;

        // 无注解 → 直接放行
        if (annotation == null) {
            return true;
        }

        // 获取当前认证用户
        String userId = HttpAuthUtils.getSessionUser(request);
        if (userId == null) {
            log.warn("未认证用户尝试访问受保护端点: {}", request.getRequestURI());
            writeForbiddenResponse(response, "未认证，请先登录");
            return false;
        }

        String permission = annotation.value();

        // 值为 "*" → 仅检查是否已认证
        if ("*".equals(permission)) {
            return true;
        }

        PermissionAccess.AccessDecision decision = permissionAccess.check(request, permission);
        if (!decision.allowed()) {
            log.warn("权限校验失败，用户: {}，所需权限: {}，原因: {}", userId, permission, decision.reason());
            writeForbiddenResponse(response, "权限不足: " + permission);
            return false;
        }

        return true;
    }

    /**
     * 写入 403 Forbidden 响应（RFC 7807 ProblemDetail 格式）。
     *
     * @param response HTTP 响应
     * @param detail   详细描述
     */
    private void writeForbiddenResponse(HttpServletResponse response, String detail) throws Exception {
        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "auth/access-denied")
                .title("无访问权限")
                .status(HttpStatus.FORBIDDEN.value())
                .detail(detail)
                .errorCode(StatusCodes.PERMISSION_DENIED.code())
                .build();

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
