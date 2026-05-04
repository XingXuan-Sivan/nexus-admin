package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.domain.identity.Role;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.domain.result.PageResult;
import com.nexusadmin.api.domain.result.Result;
import com.nexusadmin.api.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色管理控制器。
 * <p>
 * 提供角色的增删改查 API。
 */
@RestController
@RequestMapping("/admin/v1/roles")
@Tag(name = "角色管理")
public class RoleController {

    private final RoleService roleService;

    /**
     * 构造角色管理控制器。
     *
     * @param roleService 角色管理服务
     */
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 获取角色列表（分页）。
     *
     * @param page 当前页码，从 1 开始
     * @param size 每页数量
     * @return 分页角色视图
     */
    @GetMapping
    @RequirePermission("roles.view")
    @Operation(summary = "获取角色列表")
    public PageResult<Role> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return roleService.list(page, size);
    }

    /**
     * 获取角色详情。
     *
     * @param id 角色唯一标识
     * @return 角色视图
     */
    @GetMapping("/{id}")
    @RequirePermission("roles.view")
    @Operation(summary = "获取角色详情")
    public DataResult<Role> get(@PathVariable("id") String id) {
        return DataResult.success(roleService.get(id).orElse(null));
    }

    /**
     * 创建角色。
     *
     * @param role 角色视图
     * @return 创建后的角色视图
     */
    @PostMapping
    @RequirePermission("roles.manage")
    @Operation(summary = "创建角色")
    public DataResult<Role> create(@RequestBody Role role) {
        return DataResult.success(roleService.create(role));
    }

    /**
     * 更新角色。
     *
     * @param id   角色唯一标识
     * @param role 角色视图
     * @return 更新后的角色视图
     */
    @PutMapping("/{id}")
    @RequirePermission("roles.manage")
    @Operation(summary = "更新角色")
    public DataResult<Role> update(@PathVariable("id") String id,
                                       @RequestBody Role role) {
        return DataResult.success(roleService.update(id, role));
    }

    /**
     * 删除角色。
     *
     * @param id 角色唯一标识
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @RequirePermission("roles.manage")
    @Operation(summary = "删除角色")
    public Result delete(@PathVariable("id") String id) {
        roleService.delete(id);
        return Result.success();
    }
}
