package com.nexusadmin.plugin.system.user.controller;

import com.nexusadmin.api.domain.identity.User;
import com.nexusadmin.plugin.system.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理控制器层占位类。
 * <p>
 * 目前仅定义方法签名和调用服务层的结构，后续可根据实际 Web 框架（如 Spring MVC）添加注解与实现。
 * </p>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 查询用户列表。
     */
    @GetMapping("/list")
    public List<User> listUsers() {
        System.out.println("UserController 初始化>>>>>>>>>>>>>>>>>>>>>>>>>>");
        return userService.listUsers();
    }

    /**
     * 按 ID 查询单个用户。
     */
    public User getUser(String userId) {
        return userService.getUser(userId);
    }

    /**
     * 创建用户。
     */
    public void createUser(User user) {
        userService.createUser(user);
    }

    /**
     * 更新用户信息。
     */
    public void updateUser(User user) {
        userService.updateUser(user);
    }

    /**
     * 删除用户。
     */
    public void deleteUser(String userId) {
        userService.deleteUser(userId);
    }
}
