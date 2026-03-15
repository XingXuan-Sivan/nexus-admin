package com.nexusadmin.core.config.binder;

import com.nexusadmin.core.config.schema.ConfigProperty;
import com.nexusadmin.core.config.schema.ConfigSchema;
import com.nexusadmin.core.config.schema.SchemaRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 配置 UI 构建器，将 ConfigSchema 转换为 UI Schema。
 * <p>生成的 UI Schema 可用于前端动态渲染配置表单。</p>
 */
public class ConfigUIBuilder {

    /**
     * Schema 注册中心。
     */
    private final SchemaRegistry schemaRegistry;

    /**
     * 构造 UI 构建器。
     *
     * @param schemaRegistry Schema 注册中心
     */
    public ConfigUIBuilder(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = Objects.requireNonNull(schemaRegistry, "Schema 注册中心不能为空");
    }

    /**
     * 为指定配置域构建 UI Schema。
     *
     * @param schemaId 配置域 ID
     * @return UI Schema 映射
     */
    public Map<String, Object> build(String schemaId) {
        ConfigSchema schema = schemaRegistry.getOrEmpty(schemaId);
        return buildFromSchema(schema);
    }

    /**
     * 从 Schema 构建 UI Schema。
     *
     * @param schema 配置 Schema
     * @return UI Schema 映射
     */
    public Map<String, Object> buildFromSchema(ConfigSchema schema) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pluginId", schema.pluginId());

        List<Map<String, Object>> fields = new ArrayList<>();
        for (ConfigProperty property : schema.properties().values()) {
            fields.add(convertProperty(property));
        }
        result.put("fields", fields);

        return result;
    }

    /**
     * 为所有配置域构建 UI Schema。
     *
     * @return 配置域 ID 到 UI Schema 的映射
     */
    public Map<String, Map<String, Object>> buildAll() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (String schemaId : schemaRegistry.getRegisteredSchemaIds()) {
            result.put(schemaId, build(schemaId));
        }
        return result;
    }

    /**
     * 转换配置属性为 UI 字段定义。
     *
     * @param property 配置属性
     * @return UI 字段定义
     */
    private Map<String, Object> convertProperty(ConfigProperty property) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("key", property.key());
        field.put("type", mapTypeToUI(property.type()));
        field.put("title", property.title());

        if (!property.description().isEmpty()) {
            field.put("description", property.description());
        }

        if (property.defaultValue() != null) {
            field.put("default", property.defaultValue());
        }

        if (property.isRequired()) {
            field.put("required", true);
        }

        if (property.hasEnumValues()) {
            field.put("enum", property.enumValues());
        }

        if (property.hasRange()) {
            Map<String, Object> validation = new LinkedHashMap<>();
            if (property.minimum() != null) {
                validation.put("minimum", property.minimum());
            }
            if (property.maximum() != null) {
                validation.put("maximum", property.maximum());
            }
            if (!validation.isEmpty()) {
                field.put("validation", validation);
            }
        }

        return field;
    }

    /**
     * 将 Schema 类型映射为 UI 类型。
     *
     * @param schemaType Schema 类型
     * @return UI 类型
     */
    private String mapTypeToUI(String schemaType) {
        return switch (schemaType.toLowerCase()) {
            case "string" -> "text";
            case "integer", "number" -> "number";
            case "boolean" -> "switch";
            case "array" -> "array";
            case "object" -> "object";
            case "enum" -> "select";
            case "password" -> "password";
            case "textarea", "text-area" -> "textarea";
            default -> "text";
        };
    }

    /**
     * 构建平台配置的 UI Schema。
     *
     * @param platformSchema 平台配置 Schema
     * @return UI Schema 映射
     */
    public Map<String, Object> buildPlatformSchema(ConfigSchema platformSchema) {
        return buildFromSchema(platformSchema);
    }
}
