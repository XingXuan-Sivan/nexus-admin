package com.nexusadmin.core.config.resolver.impl;

import com.nexusadmin.core.config.resolver.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
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
        this.classLoaderProvider = classLoaderProvider;
    }

    @Override
    public Optional<String> get(String scope, String key) {
        // 只处理插件私有配置，scope 即为 pluginId
        if (scope.startsWith("platform")) {
            return Optional.empty();
        }

        String pluginId = scope;
        Map<String, Object> config = loadConfig(pluginId);
        Object value = getNestedValue(config, key);
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }

    @Override
    public int priority() {
        return 30;
    }

    @Override
    public String name() {
        return "Default";
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
        ClassLoader classLoader = classLoaderProvider.getClassLoader(pluginId);
        if (classLoader == null) {
            return new ConcurrentHashMap<>();
        }

        URL resource = classLoader.getResource(DEFAULT_CONFIG_PATH);
        if (resource == null) {
            return new ConcurrentHashMap<>();
        }

        try (InputStream is = resource.openStream()) {
            Map<String, Object> loaded = yaml.loadAs(is, Map.class);
            if (loaded == null) {
                return new ConcurrentHashMap<>();
            }
            log.debug("已加载插件默认配置: {} from {}", pluginId, resource);
            return new ConcurrentHashMap<>(loaded);
        } catch (IOException e) {
            log.warn("加载插件默认配置失败: {}", pluginId, e);
            return new ConcurrentHashMap<>();
        }
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
