package com.nexusadmin.core.plugin;

import com.nexusadmin.core.plugin.loader.PluginMetadata;
import com.nexusadmin.core.plugin.loader.PluginWrapper;

import java.util.Collection;
import java.util.List;

/**
 * 插件管理器接口，定义插件生命周期的对外 API。
 * <p>提供完整的插件管理能力，包括发现、解析、加载、初始化、启动、停止、卸载、禁用、启用、升级等操作。</p>
 */
public interface PluginManager {

    /**
     * 启动插件系统。
     * <p>执行完整的生命周期流程：发现、解析、加载、初始化、自动启动。</p>
     */
    void bootstrap();

    // ===== 启动阶段（有序）=====

    /**
     * 扫描插件目录，生成候选插件元数据。
     *
     * @return 候选插件元数据列表
     */
    List<PluginMetadata> discover();

    /**
     * 执行依赖校验和拓扑排序。
     *
     * @param discoveredPlugins 已发现的插件元数据列表
     * @return 解析后的插件元数据列表
     */
    List<PluginMetadata> resolve(List<PluginMetadata> discoveredPlugins);

    /**
     * 加载已解析的插件。
     * <p>执行类加载、注册等操作，将插件置为 LOADED 状态。</p>
     *
     * @param resolvedPlugins 已解析的插件元数据列表
     * @return 插件包装对象列表
     */
    List<PluginWrapper> load(List<PluginMetadata> resolvedPlugins);

    // ===== 生命周期驱动 =====

    /**
     * 初始化指定插件。
     * <p>调用插件的 onInitialize 方法，将插件从 LOADED 状态迁移到 INITIALIZED 状态。</p>
     *
     * @param pluginId 插件唯一标识
     */
    void initialize(String pluginId);

    /**
     * 启动指定插件。
     * <p>仅允许处于 INITIALIZED 或 STOPPED 状态的插件启动。</p>
     *
     * @param pluginId 插件唯一标识
     */
    void start(String pluginId);

    /**
     * 停止指定插件。
     * <p>仅允许处于 ACTIVE 状态的插件停止。</p>
     *
     * @param pluginId 插件唯一标识
     */
    void stop(String pluginId);

    /**
     * 卸载指定插件。
     * <p>会先尝试停止运行中的插件，然后执行清理和资源释放，将插件置为 UNLOADED 状态。</p>
     *
     * @param pluginId 插件唯一标识
     */
    void unload(String pluginId);

    /**
     * 物理删除指定插件。
     * <p>删除插件文件或目录，并从注册中心移除。</p>
     *
     * @param pluginId 插件唯一标识
     */
    void delete(String pluginId);

    // ===== 启用/禁用 =====

    /**
     * 启用指定插件。
     * <p>将插件从 DISABLED 状态恢复到 ACTIVE 状态。</p>
     *
     * @param pluginId 插件唯一标识
     */
    void enable(String pluginId);

    /**
     * 禁用指定插件。
     * <p>将插件从 ACTIVE 状态迁移到 DISABLED 状态。</p>
     *
     * @param pluginId 插件唯一标识
     */
    void disable(String pluginId);

    /**
     * 检查指定插件是否已启用。
     *
     * @param pluginId 插件唯一标识
     * @return 如果插件已启用返回 true
     */
    boolean isEnabled(String pluginId);

    // ===== 升级 =====

    /**
     * 冷升级指定插件。
     * <p>停止旧版本，卸载，加载新版本，启动。</p>
     *
     * @param pluginId   插件唯一标识
     * @param newVersion 新版本元数据
     */
    void upgradeCold(String pluginId, PluginMetadata newVersion);

    /**
     * 热升级指定插件。
     * <p>双实例运行，无缝切换。</p>
     *
     * @param pluginId   插件唯一标识
     * @param newVersion 新版本元数据
     */
    void upgradeHot(String pluginId, PluginMetadata newVersion);

    // ===== 查询 =====

    /**
     * 获取指定插件的当前状态。
     *
     * @param pluginId 插件唯一标识
     * @return 插件状态
     */
    PluginState getState(String pluginId);

    /**
     * 获取指定插件信息。
     *
     * @param pluginId 插件唯一标识
     * @return 插件包装对象，不存在则返回 null
     */
    PluginWrapper get(String pluginId);

    /**
     * 列出所有已加载的插件。
     *
     * @return 插件包装对象集合，不会返回 null
     */
    Collection<PluginWrapper> list();

    /**
     * 按状态列出插件。
     *
     * @param state 插件状态
     * @return 指定状态的插件集合
     */
    Collection<PluginWrapper> listByState(PluginState state);

    /**
     * 检查指定插件是否处于活跃状态。
     *
     * @param pluginId 插件唯一标识
     * @return 如果插件处于 ACTIVE 状态返回 true
     */
    boolean isActive(String pluginId);

    // ===== 批量操作（有序）=====

    /**
     * 批量启动插件。
     *
     * @param plugins 要启动的插件列表
     */
    void startAll(List<PluginWrapper> plugins);

    /**
     * 批量停止插件。
     *
     * @param plugins 要停止的插件列表
     */
    void stopAll(List<PluginWrapper> plugins);

    /**
     * 重新加载失败的插件。
     *
     * @param plugins 要重新加载的插件列表
     */
    void reloadFailed(List<PluginWrapper> plugins);
}
