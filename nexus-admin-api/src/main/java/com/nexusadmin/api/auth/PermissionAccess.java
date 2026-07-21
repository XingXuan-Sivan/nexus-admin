package com.nexusadmin.api.auth;

import com.nexusadmin.api.auth.PermissionResolver.PermissionCheck;
import com.nexusadmin.api.auth.PermissionResolver.PermissionDecision;
import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.util.HttpAuthUtils;
import com.nexusadmin.core.extension.ExtensionConsumer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前 HTTP 请求的权限判定入口。
 *
 * <p>接口拦截与返回给前端的能力元数据必须使用同一套判定逻辑，避免出现
 * “接口不可访问但 capability 为 true”或反向不一致。判定结果仅在当前请求内缓存。</p>
 */
public final class PermissionAccess {

    private static final String CACHE_ATTRIBUTE_PREFIX =
            PermissionAccess.class.getName() + ".decision.";

    private final ExtensionConsumer<PermissionResolver> resolverConsumer;

    public PermissionAccess(ExtensionConsumer<PermissionResolver> resolverConsumer) {
        this.resolverConsumer = resolverConsumer;
    }

    /**
     * 判断当前请求用户是否拥有权限。非 HTTP 调用或未认证时返回 false。
     *
     * @param permission 权限标识
     * @return 是否允许
     */
    public boolean currentUserHas(String permission) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return false;
        }
        return check(attributes.getRequest(), permission).allowed();
    }

    /**
     * 对指定 HTTP 请求执行权限判定。
     *
     * @param request    HTTP 请求
     * @param permission 权限标识，{@code *} 表示仅要求认证
     * @return 判定结果
     */
    public AccessDecision check(HttpServletRequest request, String permission) {
        Object cached = request.getAttribute(CACHE_ATTRIBUTE_PREFIX + permission);
        if (cached instanceof AccessDecision decision) {
            return decision;
        }

        AccessDecision decision = decide(request, permission);
        request.setAttribute(CACHE_ATTRIBUTE_PREFIX + permission, decision);
        return decision;
    }

    private AccessDecision decide(HttpServletRequest request, String permission) {
        String userId = HttpAuthUtils.getSessionUser(request);
        if (userId == null) {
            return AccessDecision.deny("未认证，请先登录");
        }
        if ("*".equals(permission)) {
            return AccessDecision.allow();
        }

        PermissionResolver resolver = resolverConsumer.get().orElse(null);
        if (resolver == null) {
            return AccessDecision.deny("未找到 PermissionResolver 扩展点实现");
        }

        int separator = permission.lastIndexOf('.');
        String resource = separator > 0 ? permission.substring(0, separator) : permission;
        String action = separator > 0 ? permission.substring(separator + 1) : "*";
        InvocationContext context = InvocationContext.builder()
                .userId(userId)
                .channelId("HTTP")
                .attribute("clientIp", request.getRemoteAddr())
                .attribute("userAgent", request.getHeader("User-Agent"))
                .build();
        PermissionDecision decision = resolver.decide(
                new PermissionCheck(userId, resource, action, null), context);
        return new AccessDecision(decision.allowed(), decision.reason());
    }

    /** 权限判定结果。 */
    public record AccessDecision(boolean allowed, String reason) {

        private static AccessDecision allow() {
            return new AccessDecision(true, "权限校验通过");
        }

        private static AccessDecision deny(String reason) {
            return new AccessDecision(false, reason);
        }
    }
}
