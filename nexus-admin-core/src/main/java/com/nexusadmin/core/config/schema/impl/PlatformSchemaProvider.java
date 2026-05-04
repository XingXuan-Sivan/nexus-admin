package com.nexusadmin.core.config.schema.impl;

import com.nexusadmin.core.config.schema.ConfigProperty;
import com.nexusadmin.core.config.schema.ConfigSchema;
import com.nexusadmin.core.config.schema.SchemaProvider;
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
import java.util.Objects;
import java.util.Optional;

/**
 * 平台 Schema 提供者，负责加载并合并平台及其子作用域的 Schema。
 * <p>从 META-INF/schema/platform*.yml 加载平台配置 Schema，
 * 并将所有子作用域合并为一个统一的 platform Schema。</p>
 */
public class PlatformSchemaProvider implements SchemaProvider {

    private static final Logger log = LoggerFactory.getLogger(PlatformSchemaProvider.class);

    /**
     * 平台基础 Schema 文件路径。
     */
    private static final String PLATFORM_SCHEMA_PATH = "META-INF/schema/platform.yml";

    /**
     * 平台禁用配置 Schema 文件路径。
     */
    private static final String PLATFORM_DISABLED_SCHEMA_PATH = "META-INF/schema/platform-disabled.yml";

    /**
     * YAML 解析器。
     */
    private final Yaml yaml = new Yaml();

    @Override
    public Optional<ConfigSchema> load(String schemaId, ClassLoader classLoader) {
        if (!"platform".equals(schemaId)) {
            return Optional.empty();
        }
        if (classLoader == null) {
            return Optional.empty();
        }

        // 加载所有平台子作用域 Schema
        List<ConfigSchema> subSchemas = new ArrayList<>();

        loadSingleSchema(classLoader, PLATFORM_SCHEMA_PATH).ifPresent(subSchemas::add);
        loadSingleSchema(classLoader, PLATFORM_DISABLED_SCHEMA_PATH).ifPresent(subSchemas::add);

        if (subSchemas.isEmpty()) {
            log.debug("未找到平台 Schema 配置");
            return Optional.empty();
        }

        // 合并为一个统一的 platform Schema
        ConfigSchema mergedSchema = mergePlatformSchemas(subSchemas);
        log.debug("已加载并合并平台 Schema (子域数: {}, 总属性数: {})",
                subSchemas.size(), mergedSchema.propertyCount());

        return Optional.of(mergedSchema);
    }

    @Override
    public String name() {
        return "PlatformYamlSchemaProvider";
    }

    /**
     * 从指定路径加载单个 Schema。
     *
     * @param classLoader 类加载器
     * @param path        资源路径
     * @return Schema，如果不存在则返回空 Optional
     */
    private Optional<ConfigSchema> loadSingleSchema(ClassLoader classLoader, String path) {
        URL resource = classLoader.getResource(path);
        if (resource == null) {
            return Optional.empty();
        }

        try (InputStream is = resource.openStream()) {
            Map<String, Object> yamlData = yaml.loadAs(is, Map.class);
            if (yamlData == null) {
                return Optional.empty();
            }

            String subSchemaId = Objects.toString(yamlData.get("pluginId"), null);
            Map<String, ConfigProperty> properties = parseProperties(yamlData);

            return Optional.of(new ConfigSchema(subSchemaId, properties));
        } catch (IOException e) {
            log.warn("加载平台 Schema 失败: {}", path, e);
            return Optional.empty();
        }
    }

    /**
     * 合并多个平台子作用域 Schema 为一个统一的 platform Schema。
     * <p>子作用域的属性键名会添加前缀以区分来源，
     * 例如 platform.disabled 作用域的 disabled-plugins 属性会变为 disabled.disabled-plugins。</p>
     *
     * @param subSchemas 子作用域 Schema 列表
     * @return 合并后的 platform Schema
     */
    private ConfigSchema mergePlatformSchemas(List<ConfigSchema> subSchemas) {
        ConfigSchema.Builder builder = ConfigSchema.builder().pluginId("platform");
        Map<String, ConfigProperty> mergedProperties = new LinkedHashMap<>();

        for (ConfigSchema subSchema : subSchemas) {
            String subId = subSchema.pluginId();
            boolean isRootPlatform = "platform".equals(subId);

            for (ConfigProperty prop : subSchema.properties().values()) {
                String originalKey = prop.key();
                String mergedKey = isRootPlatform
                        ? originalKey
                        : extractSubScopePrefix(subId) + "." + originalKey;

                if (mergedProperties.containsKey(mergedKey)) {
                    log.warn("平台 Schema 属性键冲突: {}，后续定义将覆盖", mergedKey);
                }

                ConfigProperty mergedProp = new ConfigProperty(
                        mergedKey,
                        prop.type(),
                        prop.title(),
                        prop.description(),
                        prop.defaultValue(),
                        prop.enumValues(),
                        prop.minimum(),
                        prop.maximum(),
                        prop.isRequired()
                );
                mergedProperties.put(mergedKey, mergedProp);
            }
        }

        return new ConfigSchema("platform", mergedProperties);
    }

    /**
     * 从子作用域 ID 提取前缀。
     * <p>例如 "platform.disabled" 提取为 "disabled"。</p>
     *
     * @param subScopeId 子作用域 ID
     * @return 前缀
     */
    private String extractSubScopePrefix(String subScopeId) {
        if (subScopeId == null || !subScopeId.startsWith("platform.")) {
            return subScopeId;
        }
        return subScopeId.substring("platform.".length());
    }

    /**
     * 解析 YAML 中的 properties 定义。
     *
     * @param yamlData YAML 数据映射
     * @return 属性映射
     */
    @SuppressWarnings("unchecked")
    private Map<String, ConfigProperty> parseProperties(Map<String, Object> yamlData) {
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

        return properties;
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
