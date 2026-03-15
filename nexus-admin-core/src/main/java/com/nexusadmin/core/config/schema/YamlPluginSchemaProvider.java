package com.nexusadmin.core.config.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * YAML 插件 Schema 提供者，从 META-INF/schema.yml 加载配置 Schema。
 * <p>Schema 文件是可选的，不存在时返回空 Optional。</p>
 */
public class YamlPluginSchemaProvider implements SchemaProvider {

    private static final Logger log = LoggerFactory.getLogger(YamlPluginSchemaProvider.class);

    /**
     * Schema 文件路径。
     */
    private static final String SCHEMA_PATH = "META-INF/schema.yml";

    /**
     * YAML 解析器。
     */
    private final Yaml yaml = new Yaml();

    @Override
    public Optional<ConfigSchema> load(String schemaId, ClassLoader classLoader) {
        if (classLoader == null) {
            return Optional.empty();
        }

        URL resource = classLoader.getResource(SCHEMA_PATH);
        if (resource == null) {
            log.trace("插件 {} 未找到 Schema 文件", schemaId);
            return Optional.empty();
        }

        try (InputStream is = resource.openStream()) {
            ConfigSchema schema = parseSchema(is, schemaId);
            if (!schema.isEmpty()) {
                log.debug("已加载插件 Schema: {} (属性数: {})", schemaId, schema.propertyCount());
            }
            return Optional.of(schema);
        } catch (IOException e) {
            log.warn("加载 Schema 失败: {} from {}", schemaId, resource, e);
            return Optional.empty();
        }
    }

    @Override
    public String name() {
        return "YamlSchemaProvider";
    }

    /**
     * 解析 Schema YAML。
     *
     * @param is       输入流
     * @param schemaId 配置域 ID
     * @return 配置 Schema
     */
    @SuppressWarnings("unchecked")
    private ConfigSchema parseSchema(InputStream is, String schemaId) {
        Map<String, Object> yamlData = yaml.loadAs(is, Map.class);
        if (yamlData == null) {
            return ConfigSchema.empty(schemaId);
        }

        // 验证 schemaId 一致性
        Object yamlPluginId = yamlData.get("pluginId");
        if (yamlPluginId != null && !schemaId.equals(yamlPluginId)) {
            log.warn("Schema 中的 pluginId ({}) 与实际配置域 ID ({}) 不一致", yamlPluginId, schemaId);
        }

        // 解析 properties
        Map<String, ConfigProperty> properties = new LinkedHashMap<>();
        Object propsObj = yamlData.get("properties");

        if (propsObj instanceof Map) {
            Map<String, Object> propsMap = (Map<String, Object>) propsObj;
            for (Map.Entry<String, Object> entry : propsMap.entrySet()) {
                String key = entry.getKey();
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> propDef = (Map<String, Object>) entry.getValue();
                    ConfigProperty property = parseProperty(key, propDef);
                    properties.put(key, property);
                }
            }
        }

        return new ConfigSchema(schemaId, properties);
    }

    /**
     * 解析单个属性定义。
     *
     * @param key     属性键名
     * @param propDef 属性定义映射
     * @return 配置属性
     */
    @SuppressWarnings("unchecked")
    private ConfigProperty parseProperty(String key, Map<String, Object> propDef) {
        ConfigProperty.Builder builder = ConfigProperty.builder()
                .key(key)
                .type(getString(propDef, "type", "string"))
                .title(getString(propDef, "title", key))
                .description(getString(propDef, "description", ""))
                .defaultValue(propDef.get("default"))
                .required(getBoolean(propDef, "required", false));

        // 解析枚举值
        Object enumObj = propDef.get("enum");
        if (enumObj instanceof List) {
            List<String> enumValues = new ArrayList<>();
            for (Object item : (List<?>) enumObj) {
                enumValues.add(item.toString());
            }
            builder.enumValues(enumValues);
        }

        // 解析数值范围
        Object minObj = propDef.get("minimum");
        if (minObj instanceof Number) {
            builder.minimum((Number) minObj);
        }

        Object maxObj = propDef.get("maximum");
        if (maxObj instanceof Number) {
            builder.maximum((Number) maxObj);
        }

        return builder.build();
    }

    /**
     * 获取字符串值。
     */
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 获取布尔值。
     */
    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}
