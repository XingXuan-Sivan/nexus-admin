package com.nexusadmin.api.domain.view;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 平台信息视图，用于展示平台基本信息。
 *
 * @param name        平台名称
 * @param version     平台版本
 * @param description 平台描述
 * @param buildInfo   构建信息
 * @param attributes  扩展属性
 */
public record PlatformInfoView(String name,
                               String version,
                               String description,
                               Map<String, String> buildInfo,
                               Map<String, String> attributes) {

    /**
     * 创建平台信息视图。
     */
    public PlatformInfoView {
        name = name != null ? name : "";
        version = version != null ? version : "";
        description = description != null ? description : "";
        buildInfo = buildInfo == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(buildInfo));
        attributes = attributes == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }
}
