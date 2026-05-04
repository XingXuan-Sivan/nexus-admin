package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.identity.Role;
import com.nexusadmin.api.domain.result.PageResult;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 角色管理服务（骨架实现）。
 *
 * <p>提供角色的增删改查能力，当前为骨架实现，实际功能由拓展点驱动。</p>
 * <p>支持通过声明同类型 Bean 覆盖，插件可提供完整的角色管理实现。</p>
 */
@Service
public class RoleService {

    /**
     * 获取角色列表（分页）。
     *
     * @param page 当前页码，从 1 开始
     * @param size 每页数量
     * @return 分页角色视图
     */
    public PageResult<Role> list(int page, int size) {
        throw new UnsupportedOperationException("角色管理由拓展点实现");
    }

    /**
     * 获取角色详情。
     *
     * @param id 角色唯一标识
     * @return 角色视图，不存在则返回空
     */
    public Optional<Role> get(String id) {
        throw new UnsupportedOperationException("角色管理由拓展点实现");
    }

    /**
     * 创建角色。
     *
     * @param role 角色视图（不含 id）
     * @return 创建后的角色视图
     */
    public Role create(Role role) {
        throw new UnsupportedOperationException("角色管理由拓展点实现");
    }

    /**
     * 更新角色。
     *
     * @param id   角色唯一标识
     * @param role 角色视图（更新字段）
     * @return 更新后的角色视图
     */
    public Role update(String id, Role role) {
        throw new UnsupportedOperationException("角色管理由拓展点实现");
    }

    /**
     * 删除角色。
     *
     * @param id 角色唯一标识
     */
    public void delete(String id) {
        throw new UnsupportedOperationException("角色管理由拓展点实现");
    }
}
