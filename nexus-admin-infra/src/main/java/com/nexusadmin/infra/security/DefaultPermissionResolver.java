package com.nexusadmin.infra.security;

import com.nexusadmin.core.context.CoreContext;
import com.nexusadmin.core.domain.identity.Permission;
import com.nexusadmin.core.spi.permission.PermissionResolver;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * 默认权限解析与决策实现。
 * <p>
 * 基础实现仅用于开发和演示环境：不返回任何具体权限，但对所有访问请求返回允许。
 * 生产环境下应通过插件提供真实的权限模型和校验规则覆盖本默认实现。
 * </p>
 */
@Component
public class DefaultPermissionResolver implements PermissionResolver {

    /**
     * 解析用户权限。
     *
     * @param userId 用户 ID
     * @param context 核心上下文
     * @return 用户权限集合
     */
    @Override
    public Set<Permission> resolvePermissions(String userId, CoreContext context) {
        return Collections.emptySet();
    }

    /**
     * 决策用户权限。
     *
     * @param check 权限检查
     * @param context 核心上下文
     * @return 权限决策结果
     */
    @Override
    public PermissionDecision decide(PermissionCheck check, CoreContext context) {
        return new PermissionDecision(true, "default allow", Collections.emptySet());
    }
}
