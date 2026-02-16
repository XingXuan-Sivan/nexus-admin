package com.nexusadmin.api.domain.org;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 职位领域对象，表示组织架构中的职位。
 *
 * @param id           职位唯一标识
 * @param name         职位名称
 * @param departmentId 所属部门 ID
 * @param attributes   自定义扩展属性
 */
public record Position(String id,
                       String name,
                       String departmentId,
                       Map<String, String> attributes) {
    public Position {
        attributes = attributes == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }
}
