package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.identity.User;
import com.nexusadmin.api.domain.result.PageResult;
import com.nexusadmin.api.exception.ExtensionNotImplementedException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户管理服务（骨架实现）。
 *
 * <p>提供用户的增删改查能力，当前为骨架实现，实际功能由拓展点驱动。</p>
 * <p>支持通过声明同类型 Bean 覆盖，插件可提供完整的用户管理实现。</p>
 */
@Service
public class UserService {

    /**
     * 获取用户列表（分页）。
     *
     * @param page 当前页码，从 1 开始
     * @param size 每页数量
     * @return 分页用户视图
     */
    public PageResult<User> list(int page, int size) {
        throw new ExtensionNotImplementedException("用户管理由拓展点实现");
    }

    /**
     * 获取用户详情。
     *
     * @param id 用户唯一标识
     * @return 用户视图，不存在则返回空
     */
    public Optional<User> get(String id) {
        throw new ExtensionNotImplementedException("用户管理由拓展点实现");
    }

    /**
     * 创建用户。
     *
     * @param user 用户视图（不含 id）
     * @return 创建后的用户视图
     */
    public User create(User user) {
        throw new ExtensionNotImplementedException("用户管理由拓展点实现");
    }

    /**
     * 更新用户。
     *
     * @param id   用户唯一标识
     * @param user 用户视图（更新字段）
     * @return 更新后的用户视图
     */
    public User update(String id, User user) {
        throw new ExtensionNotImplementedException("用户管理由拓展点实现");
    }

    /**
     * 删除用户。
     *
     * @param id 用户唯一标识
     */
    public void delete(String id) {
        throw new ExtensionNotImplementedException("用户管理由拓展点实现");
    }
}
