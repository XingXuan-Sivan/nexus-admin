package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.identity.User;
import com.nexusadmin.api.domain.result.PageResult;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户管理服务，委托 {@link IdentityService} 实现。
 *
 * <p>作为单一实体的 Service 适配层，将用户相关的 CRUD 请求转发到统一身份管理接口。
 * 支持通过声明同类型 Bean 覆盖。</p>
 */
@Service
public class UserService {

    private final IdentityService identityService;

    public UserService(IdentityService identityService) {
        this.identityService = identityService;
    }

    public PageResult<User> list(int page, int size) {
        return identityService.listUsers(page, size);
    }

    public Optional<User> get(String id) {
        return identityService.getUser(id);
    }

    public User create(User user) {
        return identityService.createUser(user);
    }

    public User update(String id, User user) {
        return identityService.updateUser(id, user);
    }

    public void delete(String id) {
        identityService.deleteUser(id);
    }
}
