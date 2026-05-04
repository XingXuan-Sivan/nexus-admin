package com.nexusadmin.core.config.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 配置 Schema，描述插件的配置结构。
 * <p>对应从 META-INF/schema.json 解析的配置定义。</p>
 */
public final class ConfigSchema {

    /**
     * 插件ID。
     */
    private final String pluginId;

    /**
     * 配置属性映射，key 为属性键名。
     */
    private final Map<String, ConfigProperty> properties;

    /**
     * 构造配置 Schema。
     *
     * @param pluginId   插件ID
     * @param properties 配置属性映射
     */
    public ConfigSchema(String pluginId, Map<String, ConfigProperty> properties) {
        this.pluginId = Objects.requireNonNull(pluginId, "插件ID不能为空");
        this.properties = properties != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(properties))
                : Collections.emptyMap();
    }

    /**
     * 获取插件ID。
     *
     * @return 插件ID
     */
    public String pluginId() {
        return pluginId;
    }

    /**
     * 获取所有配置属性。
     *
     * @return 属性映射
     */
    public Map<String, ConfigProperty> properties() {
        return properties;
    }

    /**
     * 获取指定属性定义。
     *
     * @param key 属性键名
     * @return 属性定义
     */
    public Optional<ConfigProperty> getProperty(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    /**
     * 检查是否包含指定属性。
     *
     * @param key 属性键名
     * @return 如果包含返回 true
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /**
     * 获取属性数量。
     *
     * @return 属性数量
     */
    public int propertyCount() {
        return properties.size();
    }

    /**
     * 检查是否为空 Schema（无属性定义）。
     *
     * @return 如果没有属性返回 true
     */
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("ConfigSchema[pluginId=%s, properties=%d]", pluginId, properties.size());
    }

    /**
     * 创建空 Schema。
     *
     * @param pluginId 插件ID
     * @return 空 Schema
     */
    public static ConfigSchema empty(String pluginId) {
        return new ConfigSchema(pluginId, Collections.emptyMap());
    }

    /**
     * 构建器模式创建 ConfigSchema。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * ConfigSchema 构建器。
     */
    public static class Builder {
        private String pluginId;
        private final Map<String, ConfigProperty> properties = new LinkedHashMap<>();

        public Builder pluginId(String pluginId) {
            this.pluginId = pluginId;
            return this;
        }

        public Builder addProperty(ConfigProperty property) {
            this.properties.put(property.key(), property);
            return this;
        }

        public Builder addProperty(String key, ConfigProperty property) {
            this.properties.put(key, property);
            return this;
        }

        public Builder properties(Map<String, ConfigProperty> properties) {
            this.properties.putAll(properties);
            return this;
        }

        public ConfigSchema build() {
            return new ConfigSchema(pluginId, properties);
        }
    }
}
