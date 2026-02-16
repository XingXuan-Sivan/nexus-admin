package com.nexusadmin.api.domain.identity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 角色领域对象，表示平台中的权限角色，关联一组权限码。
 *
 * @param id              角色唯一标识
 * @param name            角色名称
 * @param permissionCodes 所关联的权限码集合
 * @param attributes      自定义扩展属性
 */
public record Role(String id,
                   String name,
                   Set<String> permissionCodes,
                   Map<String, String> attributes) {
    public Role {
        permissionCodes = permissionCodes == null ? Set.of() : Set.copyOf(permissionCodes);
        attributes = attributes == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }
}
