package com.nexusadmin.core.config.schema.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SpecVersion;
import com.nexusadmin.core.config.schema.ConfigProperty;
import com.nexusadmin.core.config.schema.ConfigSchema;
import com.nexusadmin.core.config.schema.SchemaProvider;
import com.nexusadmin.core.config.schema.SchemaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

/**
 * JSON Schema 插件配置提供者，从 META-INF/schema.json 加载配置 Schema。
 * <p>schema.json 采用 JSON Schema Draft 2020-12 格式。</p>
 */
public class JsonPluginSchemaProvider implements SchemaProvider {

    private static final Logger log = LoggerFactory.getLogger(JsonPluginSchemaProvider.class);

    /**
     * JSON Schema 文件路径。
     */
    private static final String SCHEMA_JSON_PATH = "META-INF/schema.json";

    /**
     * Jackson ObjectMapper 实例。
     */
    private final ObjectMapper objectMapper;

    /**
     * Schema 验证器。
     */
    private final SchemaValidator validator;
    private final JsonSchema metaSchema;

    /**
     * 构造 JSON Schema 插件配置提供者。
     */
    public JsonPluginSchemaProvider() {
        this.objectMapper = new ObjectMapper();
        this.validator = new JsonSchemaValidator(objectMapper);
        this.metaSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(SchemaLocation.of(
                        "https://json-schema.org/draft/2020-12/schema"));
    }

    @Override
    public Optional<ConfigSchema> load(String schemaId, ClassLoader classLoader) {
        if (classLoader == null) {
            return Optional.empty();
        }

        return loadJsonSchema(schemaId, classLoader);
    }

    @Override
    public String name() {
        return "JsonSchemaProvider";
    }

    @Override
    public int priority() {
        return 50;
    }

    /**
     * 加载 JSON Schema 格式的配置 Schema。
     *
     * @param schemaId    配置域 ID
     * @param classLoader 类加载器
     * @return ConfigSchema，如果不存在则返回空 Optional
     */
    private Optional<ConfigSchema> loadJsonSchema(String schemaId, ClassLoader classLoader) {
        URL resource = classLoader.getResource(SCHEMA_JSON_PATH);
        if (resource == null) {
            log.trace("插件 {} 未找到 schema.json 文件", schemaId);
            return Optional.empty();
        }

        try (InputStream is = resource.openStream()) {
            JsonNode schemaNode = objectMapper.readTree(is);

            // 转换 JSON Schema 为 ConfigSchema
            validateSchemaDocument(schemaNode);
            ConfigSchema schema = convertJsonSchemaToConfigSchema(schemaId, schemaNode);
            if (!schema.isEmpty()) {
                log.debug("已加载插件 JSON Schema: {} (属性数: {})", schemaId, schema.propertyCount());
            }
            return Optional.of(schema);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("加载 JSON Schema 失败: {} from {}", schemaId, resource, e);
            throw new IllegalArgumentException("插件配置 Schema 无效: " + schemaId, e);
        }
    }

    /**
     * 将 JSON Schema 节点转换为 ConfigSchema。
     * <p>提取顶层 properties 中的属性定义，映射为 ConfigProperty。</p>
     *
     * @param schemaId   配置域 ID
     * @param schemaNode JSON Schema 节点
     * @return ConfigSchema
     */
    @SuppressWarnings("unchecked")
    private ConfigSchema convertJsonSchemaToConfigSchema(String schemaId, JsonNode schemaNode) {
        Map<String, ConfigProperty> properties = new LinkedHashMap<>();

        JsonNode propsNode = schemaNode.get("properties");
        if (propsNode == null || !propsNode.isObject()) {
            return ConfigSchema.fromDocument(schemaId, schemaNode, Map.of());
        }

        // 收集 required 字段列表
        List<String> requiredFields = new ArrayList<>();
        JsonNode requiredNode = schemaNode.get("required");
        if (requiredNode != null && requiredNode.isArray()) {
            for (JsonNode req : requiredNode) {
                if (req.isTextual()) {
                    requiredFields.add(req.asText());
                }
            }
        }

        // 逐属性解析
        for (Map.Entry<String, JsonNode> entry : asIterable(propsNode.fields())) {
            String key = entry.getKey();
            JsonNode propNode = entry.getValue();
            ConfigProperty property = parseJsonProperty(key, propNode, requiredFields.contains(key));
            properties.put(key, property);
        }

        return ConfigSchema.fromDocument(schemaId, schemaNode, properties);
    }

    private void validateSchemaDocument(JsonNode schemaNode) {
        if (schemaNode == null || !schemaNode.isObject()) {
            throw new IllegalArgumentException("Schema 根节点必须是对象");
        }
        JsonNode dialect = schemaNode.get("$schema");
        if (dialect == null || !dialect.isTextual()
                || !"https://json-schema.org/draft/2020-12/schema".equals(dialect.asText())) {
            throw new IllegalArgumentException("仅支持 JSON Schema Draft 2020-12");
        }
        Set<com.networknt.schema.ValidationMessage> metaErrors = metaSchema.validate(schemaNode);
        if (!metaErrors.isEmpty()) {
            throw new IllegalArgumentException("Schema 未通过 Draft 2020-12 meta-schema 校验");
        }
        validateReferences(schemaNode, 0);
        validateSensitiveDefaults(schemaNode, schemaNode, false, new HashSet<>(), 0);
    }

    private void validateReferences(JsonNode node, int depth) {
        if (depth > 64) {
            throw new IllegalArgumentException("Schema 嵌套层级超过限制");
        }
        if (node.isObject()) {
            JsonNode reference = node.get("$ref");
            if (reference != null && (!reference.isTextual() || !reference.asText().startsWith("#"))) {
                throw new IllegalArgumentException("Schema 仅允许本地 $ref");
            }
            node.elements().forEachRemaining(child -> validateReferences(child, depth + 1));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> validateReferences(child, depth + 1));
        }
    }

    private void validateSensitiveDefaults(JsonNode root,
                                           JsonNode node,
                                           boolean inheritedSensitive,
                                           Set<String> visitedReferences,
                                           int depth) {
        if (node == null || depth > 64) {
            return;
        }
        if (node.isObject()) {
            boolean sensitive = inheritedSensitive
                    || node.path("writeOnly").asBoolean(false)
                    || node.path("x-ui-options").path("sensitive").asBoolean(false);
            if (sensitive && (node.has("default") || node.has("const")
                    || node.has("enum") || node.has("examples"))) {
                throw new IllegalArgumentException("敏感字段不得声明 default/const/enum/examples");
            }
            JsonNode reference = node.path("$ref");
            if (sensitive && reference.isTextual() && reference.asText().startsWith("#")
                    && visitedReferences.add(reference.asText())) {
                validateSensitiveDefaults(root, root.at(reference.asText().substring(1)),
                        true, visitedReferences, depth + 1);
            }
            node.elements().forEachRemaining(child -> validateSensitiveDefaults(
                    root, child, sensitive, visitedReferences, depth + 1));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> validateSensitiveDefaults(
                    root, child, inheritedSensitive, visitedReferences, depth + 1));
        }
    }

    /**
     * 解析 JSON Schema 中的单个属性定义。
     *
     * @param key      属性键名
     * @param propNode 属性 JSON 节点
     * @param required 是否必填
     * @return 配置属性
     */
    private ConfigProperty parseJsonProperty(String key, JsonNode propNode, boolean required) {
        ConfigProperty.Builder builder = ConfigProperty.builder()
                .key(key)
                .type(mapJsonType(propNode))
                .title(getTextOrDefault(propNode, "title", key))
                .description(getTextOrDefault(propNode, "description", ""))
                .defaultValue(parseDefault(propNode))
                .required(required);

        // 解析枚举值
        JsonNode enumNode = propNode.get("enum");
        if (enumNode != null && enumNode.isArray()) {
            List<String> enumValues = new ArrayList<>();
            for (JsonNode enumItem : enumNode) {
                enumValues.add(enumItem.asText());
            }
            builder.enumValues(enumValues);
        }

        // 解析数值范围
        JsonNode minNode = propNode.get("minimum");
        if (minNode != null && minNode.isNumber()) {
            builder.minimum(minNode.numberValue());
        }

        JsonNode maxNode = propNode.get("maximum");
        if (maxNode != null && maxNode.isNumber()) {
            builder.maximum(maxNode.numberValue());
        }

        return builder.build();
    }

    /**
     * 将 JSON Schema type 映射为 ConfigProperty type。
     *
     * @param propNode 属性节点
     * @return 类型字符串
     */
    private String mapJsonType(JsonNode propNode) {
        JsonNode typeNode = propNode.get("type");
        if (typeNode == null || !typeNode.isTextual()) {
            return "string";
        }
        String jsonType = typeNode.asText();
        return switch (jsonType) {
            case "integer" -> "integer";
            case "number" -> "number";
            case "boolean" -> "boolean";
            case "array" -> "array";
            case "object" -> "object";
            default -> "string";
        };
    }

    /**
     * 获取文本字段值或默认值。
     *
     * @param node         JSON 节点
     * @param fieldName    字段名
     * @param defaultValue 默认值
     * @return 字段值
     */
    private String getTextOrDefault(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.get(fieldName);
        return (field != null && field.isTextual()) ? field.asText() : defaultValue;
    }

    /**
     * 解析 default 字段值。
     *
     * @param propNode 属性节点
     * @return 默认值对象
     */
    private Object parseDefault(JsonNode propNode) {
        JsonNode defaultNode = propNode.get("default");
        if (defaultNode == null) {
            return null;
        }
        if (defaultNode.isTextual()) return defaultNode.asText();
        if (defaultNode.isBoolean()) return defaultNode.asBoolean();
        if (defaultNode.isInt()) return defaultNode.asInt();
        if (defaultNode.isLong()) return defaultNode.asLong();
        if (defaultNode.isDouble()) return defaultNode.asDouble();
        return defaultNode.asText();
    }

    /**
     * 将 Iterator<Map.Entry> 包装为 Iterable，用于 for-each 循环。
     *
     * @param iterator 迭代器
     * @param <T>      元素类型
     * @return Iterable
     */
    private <T> Iterable<T> asIterable(java.util.Iterator<T> iterator) {
        return () -> iterator;
    }

    /**
     * 获取 Schema 验证器。
     *
     * @return 验证器实例
     */
    public SchemaValidator getValidator() {
        return validator;
    }
}
