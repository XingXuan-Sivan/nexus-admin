package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.domain.identity.Permission;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 权限管理控制器。
 * <p>
 * 提供权限列表和权限树的查询 API。
 */
@RestController
@RequestMapping("/admin/v1/permissions")
@Tag(name = "权限管理")
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 构造权限管理控制器。
     *
     * @param permissionService 权限查询服务
     */
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 获取权限列表。
     *
     * @return 权限视图列表
     */
    @GetMapping
    @RequirePermission("permissions.view")
    @Operation(summary = "获取权限列表")
    public DataResult<List<Permission>> list() {
        return DataResult.success(permissionService.list());
    }

    /**
     * 获取按模块分组的权限树。
     *
     * @return 模块-权限映射
     */
    @GetMapping("/tree")
    @RequirePermission("permissions.view")
    @Operation(summary = "获取权限树")
    public DataResult<Map<String, List<Permission>>> tree() {
        return DataResult.success(permissionService.tree());
    }
}
