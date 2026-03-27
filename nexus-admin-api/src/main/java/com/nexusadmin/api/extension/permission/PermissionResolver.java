package com.nexusadmin.api.extension.permission;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.domain.identity.Permission;
import com.nexusadmin.core.extension.ExtensionPoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 权限解析与决策扩展点，用于解析用户拥有的权限并对访问请求做出授权决策。
 */
public interface PermissionResolver extends ExtensionPoint {

    /**
     * 解析用户拥有的所有权限。
     *
     * @param userId  用户 ID
     * @param context 调用上下文
     * @return 用户权限集合
     */
    Set<Permission> resolvePermissions(String userId, InvocationContext context);

    /**
     * 决定某次访问请求是否允许。
     *
     * @param check   权限检查请求
     * @param context 调用上下文
     * @return 授权决策结果
     */
    PermissionDecision decide(PermissionCheck check, InvocationContext context);

    /**
     * 权限检查请求。
     *
     * @param userId      用户 ID
     * @param resource    资源
     * @param action      操作
     * @param attributes  属性
     */
    record PermissionCheck(String userId,
                           String resource,
                           String action,
                           Map<String, String> attributes) {
        public PermissionCheck {
            attributes = attributes == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(attributes));
        }
    }

    /**
     * 授权决策结果。
     *
     * @param allowed         是否允许访问
     * @param reason          决策原因
     * @param permissionCodes 权限码集合
     */
    record PermissionDecision(boolean allowed,
                              String reason,
                              Set<String> permissionCodes) {
        public PermissionDecision {
            permissionCodes = permissionCodes == null ? Set.of() : Set.copyOf(permissionCodes);
        }
    }
}
