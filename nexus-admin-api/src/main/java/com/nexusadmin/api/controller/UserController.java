package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.domain.identity.User;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.domain.result.PageResult;
import com.nexusadmin.api.domain.result.Result;
import com.nexusadmin.api.service.UserService;
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
 * 用户管理控制器。
 * <p>
 * 提供用户的增删改查 API。
 */
@RestController
@RequestMapping("/admin/v1/users")
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;

    /**
     * 构造用户管理控制器。
     *
     * @param userService 用户管理服务
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取用户列表（分页）。
     *
     * @param page 当前页码，从 1 开始
     * @param size 每页数量
     * @return 分页用户视图
     */
    @GetMapping
    @RequirePermission("users.view")
    @Operation(summary = "获取用户列表")
    public PageResult<User> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return userService.list(page, size);
    }

    /**
     * 获取用户详情。
     *
     * @param id 用户唯一标识
     * @return 用户视图
     */
    @GetMapping("/{id}")
    @RequirePermission("users.view")
    @Operation(summary = "获取用户详情")
    public DataResult<User> get(@PathVariable("id") String id) {
        return DataResult.success(userService.get(id).orElse(null));
    }

    /**
     * 创建用户。
     *
     * @param user 用户视图
     * @return 创建后的用户视图
     */
    @PostMapping
    @RequirePermission("users.manage")
    @Operation(summary = "创建用户")
    public DataResult<User> create(@RequestBody User user) {
        return DataResult.success(userService.create(user));
    }

    /**
     * 更新用户。
     *
     * @param id   用户唯一标识
     * @param user 用户视图
     * @return 更新后的用户视图
     */
    @PutMapping("/{id}")
    @RequirePermission("users.manage")
    @Operation(summary = "更新用户")
    public DataResult<User> update(@PathVariable("id") String id,
                                       @RequestBody User user) {
        return DataResult.success(userService.update(id, user));
    }

    /**
     * 删除用户。
     *
     * @param id 用户唯一标识
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @RequirePermission("users.manage")
    @Operation(summary = "删除用户")
    public Result delete(@PathVariable("id") String id) {
        userService.delete(id);
        return Result.success();
    }
}
