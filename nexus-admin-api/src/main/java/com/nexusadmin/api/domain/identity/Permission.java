package com.nexusadmin.api.domain.identity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 权限领域对象，表示对特定资源的特定操作许可。
 *
 * @param code        权限编码，唯一标识权限
 * @param name        权限名称
 * @param description 权限描述
 * @param resource    资源标识，表示权限作用的资源类型
 * @param action      操作标识，表示允许的操作类型
 * @param attributes  自定义扩展属性
 */
public record Permission(String code,
                         String name,
                         String description,
                         String resource,
                         String action,
                         Map<String, String> attributes) {
    public Permission {
        attributes = attributes == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }
}
