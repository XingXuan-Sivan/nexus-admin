package com.nexusadmin.api.extension.web;

import java.util.List;

/**
 * 插件 Web 端点注册表。
 * <p>
 * 负责追踪每个插件注册的所有 Web 映射，以支持精确卸载。
 */
public interface PluginWebRegistry {

    /**
     * 添加一个映射关系到注册表。
     *
     * @param pluginId    插件唯一标识
     * @param mappingInfo 映射信息对象
     */
    void add(String pluginId, Object mappingInfo);

    /**
     * 获取指定插件的所有映射关系。
     *
     * @param pluginId 插件唯一标识
     * @return 映射信息列表，不存在则返回空列表
     */
    List<Object> get(String pluginId);

    /**
     * 移除指定插件的所有映射关系。
     *
     * @param pluginId 插件唯一标识
     */
    void remove(String pluginId);

    /**
     * 检查指定插件是否有已注册的端点。
     *
     * @param pluginId 插件唯一标识
     * @return 如果存在已注册端点返回 true
     */
    boolean hasMappings(String pluginId);

    /**
     * 获取所有已注册端点的插件数量。
     *
     * @return 插件数量
     */
    int size();

    /**
     * 清空所有注册信息。
     */
    void clear();
}
