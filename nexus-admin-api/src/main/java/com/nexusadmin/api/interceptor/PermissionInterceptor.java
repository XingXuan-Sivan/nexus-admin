package com.nexusadmin.api.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.extension.permission.PermissionResolver;
import com.nexusadmin.api.extension.permission.PermissionResolver.PermissionCheck;
import com.nexusadmin.api.extension.permission.PermissionResolver.PermissionDecision;
import com.nexusadmin.api.domain.result.ProblemDetail;
import com.nexusadmin.api.domain.result.StatusCodes;
import com.nexusadmin.core.extension.ExtensionConsumer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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

    private static final String SESSION_ATTR_USER = "authenticatedUser";

    private static final String TYPE_BASE = "https://nexusadmin.io/probs/";

    private final ExtensionConsumer<PermissionResolver> resolverConsumer;
    private final ObjectMapper objectMapper;

    /**
     * 构造权限检查拦截器。
     *
     * @param resolverConsumer 权限解析器扩展点消费者
     * @param objectMapper     JSON 序列化器
     */
    public PermissionInterceptor(ExtensionConsumer<PermissionResolver> resolverConsumer,
                                 ObjectMapper objectMapper) {
        this.resolverConsumer = resolverConsumer;
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
        String userId = getAuthenticatedUser(request);
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

        // 委托 PermissionResolver 校验具体权限
        PermissionResolver resolver = resolverConsumer.get().orElse(null);
        if (resolver == null) {
            log.warn("未找到 PermissionResolver 扩展点实现，默认放行");
            return true;
        }

        InvocationContext context = buildContext(request, userId);
        String[] parts = permission.split("\\.", 2);
        String resource = parts.length > 1 ? parts[0] : permission;
        String action = parts.length > 1 ? parts[1] : "*";

        PermissionCheck check = new PermissionCheck(userId, resource, action, null);
        PermissionDecision decision = resolver.decide(check, context);

        if (!decision.allowed()) {
            log.warn("权限校验失败，用户: {}，所需权限: {}，原因: {}", userId, permission, decision.reason());
            writeForbiddenResponse(response, "权限不足: " + permission);
            return false;
        }

        return true;
    }

    /**
     * 从 Session 中获取已认证用户标识。
     *
     * @param request HTTP 请求
     * @return 用户标识，未认证返回 null
     */
    private String getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object user = session.getAttribute(SESSION_ATTR_USER);
        return user instanceof String s ? s : null;
    }

    /**
     * 构建调用上下文。
     *
     * @param request HTTP 请求
     * @param userId  用户标识
     * @return 调用上下文
     */
    private InvocationContext buildContext(HttpServletRequest request, String userId) {
        return InvocationContext.builder()
                .userId(userId)
                .channelId("HTTP")
                .attribute("clientIp", request.getRemoteAddr())
                .attribute("userAgent", request.getHeader("User-Agent"))
                .build();
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
