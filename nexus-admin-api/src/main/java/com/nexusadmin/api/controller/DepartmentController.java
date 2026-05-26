package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.domain.org.Department;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.domain.result.PageResult;
import com.nexusadmin.api.domain.result.Result;
import com.nexusadmin.api.service.DepartmentService;
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

import java.util.List;

/**
 * 部门管理控制器。
 *
 * <p>提供部门的增删改查 API。</p>
 */
@RestController
@RequestMapping("/admin/v1/departments")
@Tag(name = "部门管理")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * 获取部门列表（分页）。
     */
    @GetMapping
    @RequirePermission("users.view")
    @Operation(summary = "获取部门列表")
    public PageResult<Department> list(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return departmentService.list(page, size);
    }

    /**
     * 获取部门详情。
     */
    @GetMapping("/{id}")
    @RequirePermission("users.view")
    @Operation(summary = "获取部门详情")
    public DataResult<Department> get(@PathVariable("id") String id) {
        return DataResult.success(departmentService.get(id).orElse(null));
    }

    /**
     * 创建部门。
     */
    @PostMapping
    @RequirePermission("users.manage")
    @Operation(summary = "创建部门")
    public DataResult<Department> create(@RequestBody Department dept) {
        return DataResult.success(departmentService.create(dept));
    }

    /**
     * 更新部门。
     */
    @PutMapping("/{id}")
    @RequirePermission("users.manage")
    @Operation(summary = "更新部门")
    public DataResult<Department> update(@PathVariable("id") String id,
                                          @RequestBody Department dept) {
        return DataResult.success(departmentService.update(id, dept));
    }

    /**
     * 删除部门。
     */
    @DeleteMapping("/{id}")
    @RequirePermission("users.manage")
    @Operation(summary = "删除部门")
    public Result delete(@PathVariable("id") String id) {
        departmentService.delete(id);
        return Result.success();
    }

    /**
     * 获取子部门列表。
     */
    @GetMapping("/{parentId}/children")
    @RequirePermission("users.view")
    @Operation(summary = "获取子部门列表")
    public DataResult<List<Department>> getChildren(@PathVariable("parentId") String parentId) {
        return DataResult.success(departmentService.getChildren(parentId));
    }
}
