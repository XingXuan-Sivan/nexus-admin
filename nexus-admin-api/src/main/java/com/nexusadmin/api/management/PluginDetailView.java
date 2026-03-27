package com.nexusadmin.api.management;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 插件详情视图，用于展示插件的完整信息。
 *
 * @param pluginId      插件标识
 * @param version       插件版本
 * @param name          插件名称
 * @param description   插件描述
 * @param state         插件状态
 * @param provider      提供者信息
 * @param mainClass     主类名
 * @param dependencies  依赖插件标识集合
 * @param extensions    扩展点信息列表
 * @param loadedAt      加载时间
 * @param startedAt     启动时间
 * @param attributes    扩展属性
 */
public record PluginDetailView(String pluginId,
                               String version,
                               String name,
                               String description,
                               PluginStateView state,
                               String provider,
                               String mainClass,
                               Set<String> dependencies,
                               List<ExtensionView> extensions,
                               Instant loadedAt,
                               Instant startedAt,
                               Map<String, String> attributes) {

    /**
     * 创建插件详情视图。
     */
    public PluginDetailView {
        pluginId = pluginId != null ? pluginId : "";
        version = version != null ? version : "";
        name = name != null ? name : "";
        description = description != null ? description : "";
        state = state != null ? state : PluginStateView.DISCOVERED;
        provider = provider != null ? provider : "";
        mainClass = mainClass != null ? mainClass : "";
        dependencies = dependencies != null ? Set.copyOf(dependencies) : Set.of();
        extensions = extensions != null ? List.copyOf(extensions) : List.of();
        attributes = attributes == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    /**
     * 扩展点视图，描述插件注册的扩展点信息。
     *
     * @param extensionPoint 扩展点接口名
     * @param className      实现类名
     * @param priority       优先级
     */
    public record ExtensionView(String extensionPoint,
                                String className,
                                int priority) {
        public ExtensionView {
            extensionPoint = extensionPoint != null ? extensionPoint : "";
            className = className != null ? className : "";
        }
    }
}
