package com.nexusadmin.core.config.schema.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusadmin.core.config.schema.ConfigProperty;
import com.nexusadmin.core.config.schema.ConfigSchema;
import com.nexusadmin.core.config.schema.SchemaProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 平台配置 Schema 提供者。
 *
 * <p>平台域与插件域统一使用 JSON Schema Draft 2020-12，管理 API 因而可以原样返回
 * 嵌套结构、条件规则和 {@code x-ui-*} 扩展，不再经过有损的 YAML 属性模型转换。</p>
 */
public final class PlatformSchemaProvider implements SchemaProvider {

    private static final Logger log = LoggerFactory.getLogger(PlatformSchemaProvider.class);
    private static final Map<String, String> RESOURCES = Map.of(
            "platform", "META-INF/schema/platform.json",
            "platform.disabled", "META-INF/schema/platform-disabled.json"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Optional<ConfigSchema> load(String schemaId, ClassLoader classLoader) {
        String resource = RESOURCES.get(schemaId);
        if (resource == null || classLoader == null) {
            return Optional.empty();
        }
        try (InputStream input = classLoader.getResourceAsStream(resource)) {
            if (input == null) {
                return Optional.empty();
            }
            JsonNode document = objectMapper.readTree(input);
            Map<String, ConfigProperty> properties = extractTopLevelProperties(document);
            return Optional.of(ConfigSchema.fromDocument(schemaId, document, properties));
        } catch (IOException e) {
            log.warn("加载平台配置 Schema 失败: {}", schemaId, e);
            return Optional.empty();
        }
    }

    @Override
    public String name() {
        return "PlatformJsonSchemaProvider";
    }

    @Override
    public int priority() {
        return 40;
    }

    private Map<String, ConfigProperty> extractTopLevelProperties(JsonNode document) {
        Map<String, ConfigProperty> result = new LinkedHashMap<>();
        JsonNode properties = document.path("properties");
        if (!properties.isObject()) {
            return result;
        }
        List<String> required = new ArrayList<>();
        document.path("required").forEach(node -> required.add(node.asText()));
        Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode definition = field.getValue();
            List<String> enumValues = new ArrayList<>();
            definition.path("enum").forEach(value -> enumValues.add(value.asText()));
            result.put(field.getKey(), ConfigProperty.builder()
                    .key(field.getKey())
                    .type(definition.path("type").asText("string"))
                    .title(definition.path("title").asText(field.getKey()))
                    .description(definition.path("description").asText(""))
                    .defaultValue(definition.has("default")
                            ? objectMapper.convertValue(definition.get("default"), Object.class) : null)
                    .enumValues(enumValues.isEmpty() ? null : enumValues)
                    .minimum(definition.path("minimum").isNumber()
                            ? definition.path("minimum").numberValue() : null)
                    .maximum(definition.path("maximum").isNumber()
                            ? definition.path("maximum").numberValue() : null)
                    .required(required.contains(field.getKey()))
                    .build());
        }
        return result;
    }
}
