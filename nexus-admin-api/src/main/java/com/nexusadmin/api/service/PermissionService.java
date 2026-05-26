package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.identity.Permission;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限查询服务，委托 {@link IdentityService} 实现。
 *
 * <p>作为权限实体的 Service 适配层，将权限相关的查询请求转发到统一身份管理接口。
 * 支持通过声明同类型 Bean 覆盖。</p>
 */
@Service
public class PermissionService {

    private final IdentityService identityService;

    public PermissionService(IdentityService identityService) {
        this.identityService = identityService;
    }

    public List<Permission> list() {
        return identityService.listPermissions();
    }

    public Map<String, List<Permission>> tree() {
        return identityService.listPermissions().stream()
                .collect(Collectors.groupingBy(Permission::resource));
    }
}
