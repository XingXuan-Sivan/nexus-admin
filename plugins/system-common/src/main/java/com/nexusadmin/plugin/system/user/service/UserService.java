package com.nexusadmin.plugin.system.user.service;

import com.nexusadmin.core.domain.identity.User;

import java.util.List;

/**
 * 用户领域服务接口，定义用户管理的核心操作。
 */
public interface UserService {

    /**
     * 查询用户列表。
     */
    List<User> listUsers();

    /**
     * 按 ID 查询单个用户。
     */
    User getUser(String userId);

    /**
     * 创建用户。
     */
    void createUser(User user);

    /**
     * 更新用户信息。
     */
    void updateUser(User user);

    /**
     * 删除用户。
     */
    void deleteUser(String userId);
}
