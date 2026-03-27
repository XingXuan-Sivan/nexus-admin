package com.nexusadmin.api.management;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 配置管理门面接口，提供配置的查询与修改能力。
 *
 * <p>该接口封装配置中心操作，为管理面板提供稳定的控制平面 API。</p>
 */
public interface ConfigAdminFacade {

    // ==================== 平台配置 ====================

    /**
     * 获取平台配置的 UI Schema。
     *
     * <p>UI Schema 描述配置项的元数据，供前端动态渲染配置表单。</p>
     *
     * @return UI Schema 映射，不为空
     */
    Map<String, Object> getPlatformUISchema();

    /**
     * 获取平台配置值。
     *
     * @return 配置键值映射，不为空
     */
    Map<String, String> getPlatformConfigValues();

    /**
     * 更新平台配置值。
     *
     * <p>配置变更后会触发事件通知相关监听器。</p>
     *
     * @param values 配置键值映射
     */
    void updatePlatformConfig(Map<String, String> values);

    // ==================== 插件配置 ====================

    /**
     * 获取插件配置的 UI Schema。
     *
     * @param pluginId 插件标识
     * @return UI Schema 映射，插件不存在 Schema 时返回空
     */
    Optional<Map<String, Object>> getPluginUISchema(String pluginId);

    /**
     * 获取插件配置值。
     *
     * @param pluginId 插件标识
     * @return 配置键值映射，不为空
     */
    Map<String, String> getPluginConfigValues(String pluginId);

    /**
     * 更新插件配置值。
     *
     * <p>配置变更后会触发事件通知相关监听器。</p>
     *
     * @param pluginId 插件标识
     * @param values   配置键值映射
     */
    void updatePluginConfig(String pluginId, Map<String, String> values);

    // ==================== 禁用列表管理 ====================

    /**
     * 获取禁用的插件标识列表。
     *
     * @return 禁用插件标识列表，不为空
     */
    List<String> getDisabledPlugins();

    /**
     * 设置插件禁用状态。
     *
     * @param pluginId 插件标识
     * @param disabled 是否禁用
     */
    void setPluginDisabled(String pluginId, boolean disabled);
}
