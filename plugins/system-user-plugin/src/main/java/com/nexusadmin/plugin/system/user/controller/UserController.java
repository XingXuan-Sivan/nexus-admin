package com.nexusadmin.plugin.system.user.controller;

import com.nexusadmin.api.domain.identity.User;
import com.nexusadmin.api.result.DataResult;
import com.nexusadmin.plugin.system.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理控制器。
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    /**
     * 构造用户控制器。
     *
     * @param userService 用户服务
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 查询用户列表。
     *
     * @return 用户列表
     */
    @GetMapping("/list")
    public DataResult<List<User>> listUsers() {
        return DataResult.success(userService.listUsers());
    }

    /**
     * 按 ID 查询单个用户。
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    public DataResult<User> getUser(String userId) {
        return DataResult.success(userService.getUser(userId));
    }

    /**
     * 创建用户。
     *
     * @param user 用户信息
     */
    public void createUser(User user) {
        userService.createUser(user);
    }

    /**
     * 更新用户信息。
     *
     * @param user 用户信息
     */
    public void updateUser(User user) {
        userService.updateUser(user);
    }

    /**
     * 删除用户。
     *
     * @param userId 用户 ID
     */
    public void deleteUser(String userId) {
        userService.deleteUser(userId);
    }
}
