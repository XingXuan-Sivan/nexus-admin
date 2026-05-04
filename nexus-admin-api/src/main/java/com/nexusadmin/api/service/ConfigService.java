package com.nexusadmin.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 配置管理服务。
 *
 * <p>提供配置域的查询与修改能力，包括平台配置、插件配置、Schema 管理、禁用列表管理。</p>
 * <p>支持通过声明同类型 Bean 覆盖，便于插件提供定制实现。</p>
 */
@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    private final com.nexusadmin.core.facade.ConfigFacade configFacade;

    /**
     * 构造配置管理服务。
     *
     * @param configFacade 核心配置管理门面
     */
    public ConfigService(com.nexusadmin.core.facade.ConfigFacade configFacade) {
        this.configFacade = configFacade;
    }

    /**
     * 获取平台配置的 UI Schema。
     *
     * @return UI Schema 映射，不为空
     */
    public Map<String, Object> getPlatformUISchema() {
        return configFacade.buildPlatformUISchema();
    }

    /**
     * 获取平台配置值。
     *
     * @return 配置键值映射，不为空
     */
    public Map<String, String> getPlatformConfigValues() {
        Map<String, String> result = new HashMap<>();
        configFacade.getPlatformSchema().ifPresent(schema ->
                schema.properties().keySet().forEach(key ->
                        configFacade.get("platform", key).ifPresent(v -> result.put(key, v))
                )
        );
        return result;
    }

    /**
     * 更新平台配置值。
     *
     * @param values 配置键值映射
     */
    public void updatePlatformConfig(Map<String, String> values) {
        if (values != null) {
            values.forEach((k, v) -> configFacade.set("platform", k, v));
        }
    }

    /**
     * 获取插件配置的 UI Schema。
     *
     * @param pluginId 插件标识
     * @return UI Schema 映射，插件不存在 Schema 时返回空
     */
    public Optional<Map<String, Object>> getPluginUISchema(String pluginId) {
        if (pluginId == null) {
            return Optional.empty();
        }
        Map<String, Object> schema = configFacade.buildUISchema("plugin:" + pluginId);
        return Optional.ofNullable(schema).filter(s -> !s.isEmpty());
    }

    /**
     * 获取插件配置值。
     *
     * @param pluginId 插件标识
     * @return 配置键值映射，不为空
     */
    public Map<String, String> getPluginConfigValues(String pluginId) {
        if (pluginId == null) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        String scope = "plugin:" + pluginId;
        configFacade.getSchema(scope).ifPresent(schema ->
                schema.properties().keySet().forEach(key ->
                        configFacade.get(scope, key).ifPresent(v -> result.put(key, v))
                )
        );
        return result;
    }

    /**
     * 更新插件配置值。
     *
     * @param pluginId 插件标识
     * @param values   配置键值映射
     */
    public void updatePluginConfig(String pluginId, Map<String, String> values) {
        if (pluginId != null && values != null) {
            String scope = "plugin:" + pluginId;
            values.forEach((k, v) -> configFacade.set(scope, k, v));
        }
    }

    /**
     * 获取禁用的插件标识列表。
     *
     * @return 禁用插件标识列表，不为空
     */
    public List<String> getDisabledPlugins() {
        return new ArrayList<>();
    }

    /**
     * 设置插件禁用状态。
     *
     * @param pluginId 插件标识
     * @param disabled 是否禁用
     */
    public void setPluginDisabled(String pluginId, boolean disabled) {
        if (pluginId != null) {
            configFacade.setPluginDisabled(pluginId, disabled);
        }
    }

    /**
     * 获取所有配置域标识列表。
     *
     * @return 配置域标识列表，不为空
     */
    public List<String> getScopes() {
        throw new UnsupportedOperationException("获取配置域列表尚未实现");
    }

    /**
     * 获取指定配置域的 JSON Schema。
     *
     * @param scope 配置域标识
     * @return JSON Schema 映射，配置域不存在时返回空
     */
    public Optional<Map<String, Object>> getSchema(String scope) {
        if (scope == null) {
            return Optional.empty();
        }
        return configFacade.getSchema(scope)
                .map(schema -> configFacade.buildUISchema(scope));
    }

    /**
     * 获取指定配置域的全部配置值。
     *
     * @param scope 配置域标识
     * @return 配置键值映射，不为空
     */
    public Map<String, String> getConfig(String scope) {
        if (scope == null) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        configFacade.getSchema(scope).ifPresent(schema ->
                schema.properties().keySet().forEach(key ->
                        configFacade.get(scope, key).ifPresent(v -> result.put(key, v))
                )
        );
        return result;
    }

    /**
     * 更新指定配置域的配置值。
     *
     * @param scope  配置域标识
     * @param values 配置键值映射
     */
    public void updateConfig(String scope, Map<String, String> values) {
        if (scope != null && values != null) {
            values.forEach((k, v) -> configFacade.set(scope, k, v));
        }
    }

    /**
     * 重置指定配置域为默认值。
     *
     * @param scope 配置域标识
     */
    public void resetConfig(String scope) {
        throw new UnsupportedOperationException("重置配置尚未实现");
    }
}
