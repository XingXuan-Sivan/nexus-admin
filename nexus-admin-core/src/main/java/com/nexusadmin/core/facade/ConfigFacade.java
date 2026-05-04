package com.nexusadmin.core.facade;

import com.nexusadmin.core.config.ConfigManager;
import com.nexusadmin.core.config.event.ConfigListener;
import com.nexusadmin.core.config.schema.ConfigSchema;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 配置管理门面。
 * <p>聚合 {@link ConfigManager}，提供统一的配置读写、Schema 管理与监听器操作入口。</p>
 */
public class ConfigFacade {

    private final ConfigManager configManager;

    /**
     * 构造配置管理门面。
     *
     * @param configManager 配置管理器实例，不能为空
     */
    public ConfigFacade(ConfigManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "配置管理器不能为空");
    }

    /**
     * 获取字符串类型的配置值。
     *
     * @param scope 配置作用域
     * @param key   配置键名
     * @return 配置值，如果不存在则返回空 Optional
     */
    public Optional<String> get(String scope, String key) {
        return configManager.get(scope, key);
    }

    /**
     * 获取指定类型的配置值。
     *
     * @param scope 配置作用域
     * @param key   配置键名
     * @param type  目标类型
     * @param <T>   类型参数
     * @return 配置值，如果不存在或转换失败则返回空 Optional
     */
    public <T> Optional<T> get(String scope, String key, Class<T> type) {
        return configManager.get(scope, key, type);
    }

    /**
     * 设置字符串类型的配置值。
     *
     * @param scope 配置作用域
     * @param key   配置键名
     * @param value 配置值
     */
    public void set(String scope, String key, String value) {
        configManager.set(scope, key, value);
    }

    /**
     * 批量设置配置值。
     *
     * @param scope 配置作用域
     * @param key   配置键名前缀
     * @param value 配置值对象
     */
    public void set(String scope, String key, Object value) {
        configManager.set(scope, key, value);
    }

    /**
     * 检查指定配置是否存在。
     *
     * @param scope 配置作用域
     * @param key   配置键名
     * @return 如果配置存在返回 true
     */
    public boolean exists(String scope, String key) {
        return configManager.exists(scope, key);
    }

    /**
     * 删除指定配置。
     *
     * @param scope 配置作用域
     * @param key   配置键名
     */
    public void remove(String scope, String key) {
        configManager.remove(scope, key);
    }

    /**
     * 获取指定配置域的 Schema。
     *
     * @param schemaId 配置域 ID
     * @return Schema，如果不存在则返回空 Optional
     */
    public Optional<ConfigSchema> getSchema(String schemaId) {
        return configManager.getSchema(schemaId);
    }

    /**
     * 构建指定配置域的 UI Schema。
     *
     * @param schemaId 配置域 ID
     * @return UI Schema 映射
     */
    public Map<String, Object> buildUISchema(String schemaId) {
        return configManager.buildUISchema(schemaId);
    }

    /**
     * 获取平台配置的 Schema。
     *
     * @return 平台 Schema，如果不存在则返回空 Optional
     */
    public Optional<ConfigSchema> getPlatformSchema() {
        return configManager.getPlatformSchema();
    }

    /**
     * 构建平台配置的 UI Schema。
     *
     * @return 平台 UI Schema 映射
     */
    public Map<String, Object> buildPlatformUISchema() {
        return configManager.buildPlatformUISchema();
    }

    /**
     * 注册插件配置。
     *
     * @param pluginId    插件ID
     * @param classLoader 插件类加载器
     */
    public void registerPlugin(String pluginId, ClassLoader classLoader) {
        configManager.registerPlugin(pluginId, classLoader);
    }

    /**
     * 注销插件配置。
     *
     * @param pluginId 插件ID
     */
    public void unregisterPlugin(String pluginId) {
        configManager.unregisterPlugin(pluginId);
    }

    /**
     * 检查插件是否被禁用。
     *
     * @param pluginId 插件ID
     * @return 如果插件被禁用返回 true
     */
    public boolean isPluginDisabled(String pluginId) {
        return configManager.isPluginDisabled(pluginId);
    }

    /**
     * 设置插件禁用状态。
     *
     * @param pluginId 插件ID
     * @param disabled 是否禁用
     */
    public void setPluginDisabled(String pluginId, boolean disabled) {
        configManager.setPluginDisabled(pluginId, disabled);
    }

    /**
     * 添加配置变更监听器。
     *
     * @param listener 配置监听器
     */
    public void addListener(ConfigListener listener) {
        configManager.addListener(listener);
    }

    /**
     * 移除配置变更监听器。
     *
     * @param listener 配置监听器
     */
    public void removeListener(ConfigListener listener) {
        configManager.removeListener(listener);
    }

    /**
     * 获取底层配置管理器实例。
     *
     * @return 配置管理器
     */
    public ConfigManager configManager() {
        return configManager;
    }
}
