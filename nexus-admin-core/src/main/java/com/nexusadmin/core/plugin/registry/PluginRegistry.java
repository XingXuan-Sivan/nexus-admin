package com.nexusadmin.core.plugin.registry;

import com.nexusadmin.core.plugin.loader.PluginWrapper;

import java.util.Collection;

/**
 * 插件注册中心接口，负责管理已安装插件的存储和检索。
 * <p>将插件状态存储从 PluginManager 中抽离，实现状态管理外置。</p>
 */
public interface PluginRegistry {

    /**
     * 注册插件到注册中心。
     *
     * @param plugin 已加载的插件包装对象，不能为空
     * @throws IllegalArgumentException 如果 plugin 为 null
     * @throws IllegalStateException    如果插件ID已存在
     */
    void register(PluginWrapper plugin);

    /**
     * 从注册中心注销插件。
     *
     * @param pluginId 插件唯一标识，不能为空
     * @throws IllegalArgumentException 如果 pluginId 为 null 或空
     */
    void unregister(String pluginId);

    /**
     * 根据插件ID获取已注册的插件。
     *
     * @param pluginId 插件唯一标识
     * @return 插件包装对象，不存在则返回 null
     */
    PluginWrapper get(String pluginId);

    /**
     * 获取所有已注册的插件列表。
     *
     * @return 插件包装对象集合，不会返回 null
     */
    Collection<PluginWrapper> list();

    /**
     * 检查指定插件ID是否已注册。
     *
     * @param pluginId 插件唯一标识
     * @return 如果已注册返回 true，否则返回 false
     */
    boolean contains(String pluginId);

    /**
     * 清空注册中心，移除所有已注册插件。
     * <p><strong>注意：</strong>此方法通常用于测试或系统关闭场景。</p>
     */
    void clear();
}
