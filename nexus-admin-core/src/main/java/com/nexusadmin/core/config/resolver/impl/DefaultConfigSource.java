package com.nexusadmin.core.config.resolver.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusadmin.core.config.ConfigScopeIds;
import com.nexusadmin.core.config.resolver.ConfigSource;
import com.nexusadmin.core.config.schema.ConfigSchema;
import com.nexusadmin.core.config.schema.SchemaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认配置源，从插件的 META-INF/config.yml 读取默认配置。
 * <p>优先级最低，作为配置的默认值。</p>
 */
public class DefaultConfigSource implements ConfigSource {

    private static final Logger log = LoggerFactory.getLogger(DefaultConfigSource.class);

    /**
     * 默认配置文件路径。
     */
    private static final String DEFAULT_CONFIG_PATH = "META-INF/config.yml";

    /**
     * 配置缓存，key 为 pluginId。
     */
    private final Map<String, Map<String, Object>> configCache = new ConcurrentHashMap<>();

    /**
     * YAML 解析器。
     */
    private final Yaml yaml = new Yaml();

    /**
     * 类加载器提供者，用于加载插件资源。
     */
    private final ClassLoaderProvider classLoaderProvider;
    private final SchemaRegistry schemaRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 类加载器提供者接口。
     */
    @FunctionalInterface
    public interface ClassLoaderProvider {
        /**
         * 获取指定插件的类加载器。
         *
         * @param pluginId 插件ID
         * @return 类加载器，如果插件不存在返回 null
         */
        ClassLoader getClassLoader(String pluginId);
    }

    /**
     * 构造默认配置源。
     *
     * @param classLoaderProvider 类加载器提供者
     */
    public DefaultConfigSource(ClassLoaderProvider classLoaderProvider) {
        this(classLoaderProvider, null);
    }

    public DefaultConfigSource(ClassLoaderProvider classLoaderProvider,
                               SchemaRegistry schemaRegistry) {
        this.classLoaderProvider = classLoaderProvider;
        this.schemaRegistry = schemaRegistry;
    }

    @Override
    public Optional<String> get(String scope, String key) {
        return getObject(scope, key).map(Object::toString);
    }

    @Override
    public Optional<Object> getObject(String scope, String key) {
        Map<String, Object> config = loadConfig(scope);
        Object value = getNestedValue(config, key);
        return Optional.ofNullable(value);
    }

    @Override
    public int priority() {
        return 30;
    }

    @Override
    public String name() {
        return "Default";
    }

    @Override
    public String sourceType() {
        return "default";
    }

    /**
     * 获取指定插件的完整默认配置。
     *
     * @param pluginId 插件ID
     * @return 配置映射
     */
    public Map<String, Object> getConfigMap(String pluginId) {
        return Collections.unmodifiableMap(loadConfig(pluginId));
    }

    /** 注册插件类加载器并清除相应默认配置缓存。 */
    public void registerClassLoader(String pluginId, ClassLoader classLoader) {
        if (classLoaderProvider instanceof MutableClassLoaderProvider mutable) {
            mutable.register(pluginId, classLoader);
            invalidateCache(pluginId);
        }
    }

    /** 注销插件类加载器。 */
    public void unregisterClassLoader(String pluginId) {
        if (classLoaderProvider instanceof MutableClassLoaderProvider mutable) {
            mutable.unregister(pluginId);
            invalidateCache(pluginId);
        }
    }

    /** 可由运行时共享的可变类加载器注册表。 */
    public static final class MutableClassLoaderProvider implements ClassLoaderProvider {
        private final Map<String, ClassLoader> classLoaders = new ConcurrentHashMap<>();

        @Override
        public ClassLoader getClassLoader(String pluginId) {
            return classLoaders.get(pluginId);
        }

        public void register(String pluginId, ClassLoader classLoader) {
            if (classLoader != null) {
                classLoaders.put(pluginId, classLoader);
            }
        }

        public void unregister(String pluginId) {
            classLoaders.remove(pluginId);
        }
    }

    /**
     * 清除指定插件的缓存。
     *
     * @param pluginId 插件ID
     */
    public void invalidateCache(String pluginId) {
        configCache.remove(pluginId);
        log.debug("已清除默认配置缓存: {}", pluginId);
    }

    /**
     * 加载指定插件的默认配置。
     *
     * @param pluginId 插件ID
     * @return 配置映射
     */
    private Map<String, Object> loadConfig(String pluginId) {
        return configCache.computeIfAbsent(pluginId, this::loadFromPlugin);
    }

    /**
     * 从插件加载默认配置。
     *
     * @param pluginId 插件ID
     * @return 配置映射
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadFromPlugin(String pluginId) {
        Map<String, Object> defaults = schemaDefaults(pluginId);
        if (ConfigScopeIds.isPlatform(pluginId)) {
            return new ConcurrentHashMap<>(defaults);
        }
        ClassLoader classLoader = classLoaderProvider.getClassLoader(pluginId);
        if (classLoader == null) {
            return new ConcurrentHashMap<>(defaults);
        }

        URL resource = classLoader.getResource(DEFAULT_CONFIG_PATH);
        if (resource == null) {
            return new ConcurrentHashMap<>(defaults);
        }

        try (InputStream is = resource.openStream()) {
            Map<String, Object> loaded = yaml.loadAs(is, Map.class);
            if (loaded == null) {
                return new ConcurrentHashMap<>(defaults);
            }
            log.debug("已加载插件默认配置: {} from {}", pluginId, resource);
            return new ConcurrentHashMap<>(deepMerge(defaults, stringKeyMap(loaded)));
        } catch (IOException e) {
            log.warn("加载插件默认配置失败: {}", pluginId, e);
            return new ConcurrentHashMap<>(defaults);
        }
    }

    private Map<String, Object> schemaDefaults(String pluginId) {
        if (schemaRegistry == null) {
            return new LinkedHashMap<>();
        }
        return schemaRegistry.get(pluginId)
                .map(ConfigSchema::document)
                .map(document -> objectDefaults(document, document, new HashSet<>(), 0))
                .orElseGet(LinkedHashMap::new);
    }

    private Map<String, Object> objectDefaults(JsonNode root,
                                               JsonNode definition,
                                               Set<String> referenceStack,
                                               int depth) {
        if (definition == null || depth > 64) {
            return new LinkedHashMap<>();
        }
        JsonNode node = definition;
        JsonNode reference = node.path("$ref");
        String referenceText = reference.isTextual() ? reference.asText() : null;
        if (referenceText != null && referenceText.startsWith("#")
                && referenceStack.add(referenceText)) {
            node = root.at(referenceText.substring(1));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        JsonNode properties = node.path("properties");
        if (properties.isObject()) {
            properties.fields().forEachRemaining(entry -> {
                Object value = defaultValue(root, entry.getValue(),
                        new HashSet<>(referenceStack), depth + 1);
                if (value != null) {
                    result.put(entry.getKey(), value);
                }
            });
        }
        return result;
    }

    private Object defaultValue(JsonNode root,
                                JsonNode definition,
                                Set<String> referenceStack,
                                int depth) {
        if (definition == null || depth > 64) {
            return null;
        }
        JsonNode node = definition;
        JsonNode reference = node.path("$ref");
        if (reference.isTextual() && reference.asText().startsWith("#")
                && referenceStack.add(reference.asText())) {
            node = root.at(reference.asText().substring(1));
        }
        if (node.has("default")) {
            return objectMapper.convertValue(node.get("default"), Object.class);
        }
        Map<String, Object> nested = objectDefaults(root, node, referenceStack, depth + 1);
        return nested.isEmpty() ? null : nested;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepMerge(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> result = new LinkedHashMap<>(base);
        override.forEach((key, value) -> {
            Object existing = result.get(key);
            if (existing instanceof Map<?, ?> existingMap && value instanceof Map<?, ?> valueMap) {
                result.put(key, deepMerge((Map<String, Object>) existingMap,
                        (Map<String, Object>) valueMap));
            } else {
                result.put(key, value);
            }
        });
        return result;
    }

    private Map<String, Object> stringKeyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key),
                value instanceof Map<?, ?> nested ? stringKeyMap(nested) : value));
        return result;
    }

    /**
     * 获取嵌套配置值。
     *
     * @param config 配置映射
     * @param key    点分隔的键名
     * @return 配置值
     */
    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> config, String key) {
        String[] parts = key.split("\\.");
        Object current = config;

        for (String part : parts) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(part);
            if (current == null) {
                return null;
            }
        }

        return current;
    }
}
