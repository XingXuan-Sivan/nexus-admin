package com.nexusadmin.api.management;

/**
 * 插件摘要视图，用于列表展示。
 *
 * @param pluginId    插件标识
 * @param version     插件版本
 * @param name        插件名称
 * @param description 插件描述
 * @param state       插件状态
 * @param provider    提供者信息
 */
public record PluginView(String pluginId,
                         String version,
                         String name,
                         String description,
                         PluginStateView state,
                         String provider) {

    /**
     * 创建插件摘要视图。
     */
    public PluginView {
        // 确保非空字段
        pluginId = pluginId != null ? pluginId : "";
        version = version != null ? version : "";
        name = name != null ? name : "";
        description = description != null ? description : "";
        state = state != null ? state : PluginStateView.DISCOVERED;
        provider = provider != null ? provider : "";
    }
}
