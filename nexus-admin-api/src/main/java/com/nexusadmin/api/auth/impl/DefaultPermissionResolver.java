package com.nexusadmin.api.auth.impl;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.domain.identity.Permission;
import com.nexusadmin.api.auth.PermissionResolver;
import com.nexusadmin.api.service.IdentityService;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 默认权限解析器，委托 IdentityService 查询用户拥有的权限。
 *
 * <p>注册到 ExtensionRegistry（priority=25，兜底级）。
 * admin 用户拥有所有权限。插件可通过注册更高优先级的 PermissionResolver 覆盖。</p>
 */
public class DefaultPermissionResolver implements PermissionResolver {

    private final IdentityService identityService;

    public DefaultPermissionResolver(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public Set<Permission> resolvePermissions(String userId, InvocationContext context) {
        Set<String> permCodes = identityService.getUserPermissionCodes(userId);
        return identityService.listPermissions().stream()
                .filter(p -> permCodes.contains(p.code()))
                .collect(Collectors.toSet());
    }

    @Override
    public PermissionDecision decide(PermissionCheck check, InvocationContext context) {
        Set<String> userPermCodes = identityService.getUserPermissionCodes(check.userId());
        String requiredCode = check.resource() + "." + check.action();
        boolean allowed = userPermCodes.contains(requiredCode)
                || userPermCodes.contains(check.resource() + ".*")
                || userPermCodes.contains("*");
        return new PermissionDecision(allowed,
                allowed ? "权限校验通过" : "缺少权限: " + requiredCode,
                userPermCodes);
    }
}
