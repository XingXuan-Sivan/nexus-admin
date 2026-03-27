package com.nexusadmin.app.config;

import com.nexusadmin.api.management.ConfigAdminFacade;
import com.nexusadmin.core.config.ConfigManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 配置管理门面实现类。
 */
public class ConfigAdminFacadeImpl implements ConfigAdminFacade {

    private final ConfigManager configManager;

    /**
     * 构造配置管理门面。
     *
     * @param configManager 配置管理器
     */
    public ConfigAdminFacadeImpl(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public Map<String, Object> getPlatformUISchema() {
        if (configManager == null) {
            return Map.of();
        }
        return configManager.buildPlatformUISchema();
    }

    @Override
    public Map<String, String> getPlatformConfigValues() {
        if (configManager == null) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        configManager.getPlatformSchema().ifPresent(schema -> {
            schema.properties().keySet().forEach(key -> {
                configManager.get("platform", key).ifPresent(v -> result.put(key, v));
            });
        });
        return result;
    }

    @Override
    public void updatePlatformConfig(Map<String, String> values) {
        if (configManager != null && values != null) {
            values.forEach((k, v) -> configManager.set("platform", k, v));
        }
    }

    @Override
    public Optional<Map<String, Object>> getPluginUISchema(String pluginId) {
        if (configManager == null || pluginId == null) {
            return Optional.empty();
        }
        Map<String, Object> schema = configManager.buildUISchema("plugin:" + pluginId);
        return Optional.ofNullable(schema).filter(s -> !s.isEmpty());
    }

    @Override
    public Map<String, String> getPluginConfigValues(String pluginId) {
        if (configManager == null || pluginId == null) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        String scope = "plugin:" + pluginId;
        configManager.getSchema(scope).ifPresent(schema -> {
            schema.properties().keySet().forEach(key -> {
                configManager.get(scope, key).ifPresent(v -> result.put(key, v));
            });
        });
        return result;
    }

    @Override
    public void updatePluginConfig(String pluginId, Map<String, String> values) {
        if (configManager != null && pluginId != null && values != null) {
            String scope = "plugin:" + pluginId;
            values.forEach((k, v) -> configManager.set(scope, k, v));
        }
    }

    @Override
    public List<String> getDisabledPlugins() {
        if (configManager == null) {
            return List.of();
        }
        // 禁用列表由配置中心管理，这里简化处理
        return new ArrayList<>();
    }

    @Override
    public void setPluginDisabled(String pluginId, boolean disabled) {
        if (configManager != null && pluginId != null) {
            configManager.setPluginDisabled(pluginId, disabled);
        }
    }
}
