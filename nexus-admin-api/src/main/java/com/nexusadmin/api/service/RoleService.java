package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.identity.Role;
import com.nexusadmin.api.domain.result.PageResult;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 角色管理服务，委托 {@link IdentityService} 实现。
 *
 * <p>作为单一实体的 Service 适配层，将角色相关的 CRUD 请求转发到统一身份管理接口。
 * 支持通过声明同类型 Bean 覆盖。</p>
 */
@Service
public class RoleService {

    private final IdentityService identityService;

    public RoleService(IdentityService identityService) {
        this.identityService = identityService;
    }

    public PageResult<Role> list(int page, int size) {
        return identityService.listRoles(page, size);
    }

    public Optional<Role> get(String id) {
        return identityService.getRole(id);
    }

    public Role create(Role role) {
        return identityService.createRole(role);
    }

    public Role update(String id, Role role) {
        return identityService.updateRole(id, role);
    }

    public void delete(String id) {
        identityService.deleteRole(id);
    }
}
