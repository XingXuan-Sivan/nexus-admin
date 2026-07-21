package com.nexusadmin.core.config.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 插件ID。
     */
    private final String pluginId;

    /**
     * 配置属性映射，key 为属性键名。
     */
    private final Map<String, ConfigProperty> properties;

    /** 未经压平的 JSON Schema 文档。 */
    private final JsonNode document;

    /**
     * 构造配置 Schema。
     *
     * @param pluginId   插件ID
     * @param properties 配置属性映射
     */
    public ConfigSchema(String pluginId, Map<String, ConfigProperty> properties) {
        this(pluginId, properties, buildDocument(pluginId, properties));
    }

    /**
     * 构造保留原始 JSON Schema 文档的配置 Schema。
     *
     * @param pluginId   配置域标识
     * @param properties 供核心旧式绑定器使用的顶层属性摘要
     * @param document   Draft 2020-12 Schema 文档
     */
    public ConfigSchema(String pluginId,
                        Map<String, ConfigProperty> properties,
                        JsonNode document) {
        this.pluginId = Objects.requireNonNull(pluginId, "插件ID不能为空");
        this.properties = properties != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(properties))
                : Collections.emptyMap();
        this.document = Objects.requireNonNull(document, "Schema 文档不能为空").deepCopy();
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
     * 获取未经压平的 JSON Schema 文档副本。
     *
     * @return JSON Schema 文档
     */
    public JsonNode document() {
        return document.deepCopy();
    }

    /**
     * 获取 Schema 方言。
     *
     * @return 方言 URI；未声明时使用 Draft 2020-12
     */
    public String dialect() {
        JsonNode dialect = document.get("$schema");
        return dialect != null && dialect.isTextual()
                ? dialect.asText()
                : "https://json-schema.org/draft/2020-12/schema";
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
     * 从原始 JSON Schema 创建配置定义。
     *
     * @param pluginId  配置域标识
     * @param document  原始 Schema 文档
     * @param properties 顶层属性摘要
     * @return 配置 Schema
     */
    public static ConfigSchema fromDocument(String pluginId,
                                            JsonNode document,
                                            Map<String, ConfigProperty> properties) {
        return new ConfigSchema(pluginId, properties, document);
    }

    private static JsonNode buildDocument(String pluginId, Map<String, ConfigProperty> properties) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        root.put("$id", "urn:nexus-admin:config:" + pluginId);
        root.put("type", "object");
        ObjectNode propertyNodes = root.putObject("properties");
        if (properties != null) {
            for (ConfigProperty property : properties.values()) {
                ObjectNode node = propertyNodes.putObject(property.key());
                node.put("type", "enum".equals(property.type()) ? "string" : property.type());
                node.put("title", property.title());
                if (!property.description().isEmpty()) {
                    node.put("description", property.description());
                }
                if (property.defaultValue() != null) {
                    node.set("default", OBJECT_MAPPER.valueToTree(property.defaultValue()));
                }
                if (property.enumValues() != null) {
                    node.set("enum", OBJECT_MAPPER.valueToTree(property.enumValues()));
                }
                if (property.minimum() != null) {
                    node.put("minimum", property.minimum().doubleValue());
                }
                if (property.maximum() != null) {
                    node.put("maximum", property.maximum().doubleValue());
                }
            }
        }
        return root;
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
