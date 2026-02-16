package com.nexusadmin.plugin.system.user.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nexusadmin.api.domain.identity.User;
import com.nexusadmin.plugin.system.user.mapper.UserMapper;
import com.nexusadmin.plugin.system.user.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户领域服务默认实现，占位用于后续编写具体业务逻辑。
 */
@Service
public class UserServiceImpl implements UserService {

//    private final UserMapper userMapper;

//    public UserServiceImpl(UserMapper userMapper) {
//        this.userMapper = userMapper;
//    }

    @Override
    public List<User> listUsers() {
        // TODO 实现用户列表查询逻辑
        throw new UnsupportedOperationException("listUsers not implemented yet");
    }

    @Override
    public User getUser(String userId) {
        // TODO 实现用户详情查询逻辑
        throw new UnsupportedOperationException("getUser not implemented yet");
    }

    @Override
    public void createUser(User user) {
        // TODO 实现用户创建逻辑
        throw new UnsupportedOperationException("createUser not implemented yet");
    }

    @Override
    public void updateUser(User user) {
        // TODO 实现用户更新逻辑
        throw new UnsupportedOperationException("updateUser not implemented yet");
    }

    @Override
    public void deleteUser(String userId) {
        // TODO 实现用户删除逻辑
        throw new UnsupportedOperationException("deleteUser not implemented yet");
    }
}
