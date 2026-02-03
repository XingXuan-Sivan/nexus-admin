package com.nexusadmin.core.domain.identity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 用户领域对象，承载用户身份、角色关联、部门关联等信息。
 *
 * @param id            用户唯一标识
 * @param username      用户名，用于登录
 * @param displayName   显示名称
 * @param roleIds       所关联的角色 ID 集合
 * @param departmentIds 所关联的部门 ID 集合
 * @param attributes    自定义扩展属性
 */
public record User(String id,
                   String username,
                   String displayName,
                   Set<String> roleIds,
                   Set<String> departmentIds,
                   Map<String, String> attributes) {
    public User {
        roleIds = roleIds == null ? Set.of() : Set.copyOf(roleIds);
        departmentIds = departmentIds == null ? Set.of() : Set.copyOf(departmentIds);
        attributes = attributes == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }
}
