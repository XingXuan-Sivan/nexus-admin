package com.nexusadmin.core.config;

import com.nexusadmin.core.config.event.ConfigListener;
import com.nexusadmin.core.config.schema.ConfigSchema;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 配置管理器接口，配置中心的统一入口。
 * <p>提供配置读写、Schema 管理、UI Schema 生成等能力。</p>
 */
public interface ConfigManager {

    /**
     * 获取字符串类型的配置值。
     *
     * @param scope 配置作用域，如 "platform" 或 "order-plugin"
     * @param key   配置键名
     * @return 配置值，如果不存在则返回空 Optional
     */
    Optional<String> get(String scope, String key);

    /**
     * 获取指定类型的配置值。
     * <p>支持基本类型及其包装类型、String、Enum 等。</p>
     *
     * @param scope 配置作用域
     * @param key   配置键名
     * @param type  目标类型
     * @param <T>   类型参数
     * @return 配置值，如果不存在或转换失败则返回空 Optional
     */
    <T> Optional<T> get(String scope, String key, Class<T> type);

    /**
     * 设置配置值。
     * <p>配置将被持久化，并触发 ConfigChangedEvent 事件。</p>
     *
     * @param scope 配置作用域
     * @param key   配置键名
     * @param value 配置值
     */
    void set(String scope, String key, String value);

    /**
     * 批量设置配置值。
     *
     * @param scope 配置作用域
     * @param key   配置键名前缀
     * @param value 配置值对象
     */
    void set(String scope, String key, Object value);

    /**
     * 检查指定配置是否存在。
     *
     * @param scope 配置作用域
     * @param key   配置键名
     * @return 如果配置存在返回 true
     */
    boolean exists(String scope, String key);

    /**
     * 删除指定配置。
     *
     * @param scope 配置作用域
     * @param key   配置键名
     */
    void remove(String scope, String key);

    /**
     * 获取指定配置域的 Schema。
     *
     * @param schemaId 配置域 ID
     * @return Schema，如果不存在则返回空 Optional
     */
    Optional<ConfigSchema> getSchema(String schemaId);

    /**
     * 构建指定配置域的 UI Schema。
     *
     * @param schemaId 配置域 ID
     * @return UI Schema 映射
     */
    Map<String, Object> buildUISchema(String schemaId);

    /**
     * 获取平台配置的 Schema。
     *
     * @return 平台 Schema，如果不存在则返回空 Optional
     */
    default Optional<ConfigSchema> getPlatformSchema() {
        return getSchema("platform");
    }

    /**
     * 构建平台配置的 UI Schema。
     *
     * @return 平台 UI Schema 映射
     */
    default Map<String, Object> buildPlatformUISchema() {
        return buildUISchema("platform");
    }

    /**
     * 注册插件配置。
     * <p>加载插件的 schema 和默认配置。</p>
     *
     * @param pluginId     插件ID
     * @param classLoader  插件类加载器
     */
    void registerPlugin(String pluginId, ClassLoader classLoader);

    /**
     * 注销插件配置。
     *
     * @param pluginId 插件ID
     */
    void unregisterPlugin(String pluginId);

    /**
     * 检查插件是否被禁用。
     *
     * @param pluginId 插件ID
     * @return 如果插件被禁用返回 true
     */
    boolean isPluginDisabled(String pluginId);

    /**
     * 设置插件禁用状态。
     *
     * @param pluginId 插件ID
     * @param disabled 是否禁用
     */
    void setPluginDisabled(String pluginId, boolean disabled);

    /**
     * 添加配置变更监听器。
     *
     * @param listener 配置监听器
     */
    void addListener(ConfigListener listener);

    /**
     * 移除配置变更监听器。
     *
     * @param listener 配置监听器
     */
    void removeListener(ConfigListener listener);

    /**
     * 获取所有已注册的配置域 ID。
     *
     * @return 配置域 ID 集合，不为空
     */
    default Set<String> getRegisteredSchemaIds() {
        return Collections.emptySet();
    }
}
