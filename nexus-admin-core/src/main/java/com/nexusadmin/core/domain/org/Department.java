package com.nexusadmin.core.domain.org;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 部门领域对象，表示组织架构中的部门单元。
 *
 * @param id         部门唯一标识
 * @param name       部门名称
 * @param parentId   父部门 ID，用于构建部门树
 * @param attributes 自定义扩展属性
 */
public record Department(String id,
                         String name,
                         String parentId,
                         Map<String, String> attributes) {
    public Department {
        attributes = attributes == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }
}
