package com.nexusadmin.api.facade;

import com.nexusadmin.api.exception.PluginOperationException;

import java.util.List;
import java.util.Optional;

/**
 * 插件管理门面接口，提供插件生命周期的控制与查询能力。
 *
 * <p>该接口封装插件管理操作，为管理面板提供稳定的控制平面 API。</p>
 */
public interface PluginFacade {

    /**
     * 获取所有插件的摘要列表。
     *
     * @return 插件摘要列表，不为空
     */
    List<PluginView> listAll();

    /**
     * 按状态筛选插件。
     *
     * @param state 插件状态
     * @return 符合条件的插件列表，不为空
     */
    List<PluginView> listByState(PluginStateView state);

    /**
     * 获取插件详情。
     *
     * @param pluginId 插件标识
     * @return 插件详情，不存在则返回空
     */
    Optional<PluginDetailView> getDetail(String pluginId);

    /**
     * 启动插件。
     *
     * <p>将插件从 INITIALIZED 或 STOPPED 状态迁移到 ACTIVE 状态。</p>
     *
     * @param pluginId 插件标识
     * @throws PluginOperationException 启动失败时抛出
     */
    void start(String pluginId);

    /**
     * 停止插件。
     *
     * <p>将插件从 ACTIVE 状态迁移到 STOPPED 状态。</p>
     *
     * @param pluginId 插件标识
     * @throws PluginOperationException 停止失败时抛出
     */
    void stop(String pluginId);

    /**
     * 启用插件。
     *
     * <p>将插件从 DISABLED 状态恢复，并持久化启用状态。</p>
     *
     * @param pluginId 插件标识
     * @throws PluginOperationException 启用失败时抛出
     */
    void enable(String pluginId);

    /**
     * 禁用插件。
     *
     * <p>将插件设为 DISABLED 状态，并持久化禁用状态。</p>
     *
     * @param pluginId 插件标识
     * @throws PluginOperationException 禁用失败时抛出
     */
    void disable(String pluginId);

    /**
     * 卸载插件。
     *
     * <p>停止并卸载插件，释放资源，从注册中心移除。</p>
     *
     * @param pluginId 插件标识
     * @throws PluginOperationException 卸载失败时抛出
     */
    void unload(String pluginId);

    /**
     * 检查插件是否已启用。
     *
     * @param pluginId 插件标识
     * @return 启用状态
     */
    boolean isEnabled(String pluginId);

    /**
     * 检查插件是否处于活跃状态。
     *
     * @param pluginId 插件标识
     * @return 活跃状态
     */
    boolean isActive(String pluginId);
}
