package com.nexusadmin.core.config.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件配置源，从 plugins-data/config 目录读取配置。
 * <p>支持 platform.yml、disabled.yml 和 {pluginId}.yml 文件。</p>
 */
public class FileConfigSource implements ConfigSource {

    private static final Logger log = LoggerFactory.getLogger(FileConfigSource.class);

    /**
     * 配置文件目录。
     */
    private final Path configDir;

    /**
     * 配置缓存，避免频繁读取文件。
     */
    private final Map<String, Map<String, Object>> configCache = new ConcurrentHashMap<>();

    /**
     * YAML 解析器。
     */
    private final Yaml yaml = new Yaml();

    /**
     * 构造文件配置源。
     *
     * @param configDir 配置文件目录
     */
    public FileConfigSource(Path configDir) {
        this.configDir = configDir;
        ensureConfigDirExists();
    }

    @Override
    public Optional<String> get(String scope, String key) {
        Map<String, Object> config = loadConfig(scope);
        Object value = getNestedValue(config, key);
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public String name() {
        return "File";
    }

    /**
     * 获取指定作用域的完整配置映射。
     *
     * @param scope 配置作用域
     * @return 配置映射
     */
    public Map<String, Object> getConfigMap(String scope) {
        return Collections.unmodifiableMap(loadConfig(scope));
    }

    /**
     * 清除指定作用域的缓存。
     *
     * @param scope 配置作用域
     */
    public void invalidateCache(String scope) {
        configCache.remove(scope);
        log.debug("已清除配置缓存: {}", scope);
    }

    /**
     * 清除所有缓存。
     */
    public void invalidateAllCache() {
        configCache.clear();
        log.debug("已清除所有配置缓存");
    }

    /**
     * 加载指定作用域的配置。
     *
     * @param scope 配置作用域
     * @return 配置映射
     */
    private Map<String, Object> loadConfig(String scope) {
        return configCache.computeIfAbsent(scope, this::loadFromFile);
    }

    /**
     * 从文件加载配置。
     *
     * @param scope 配置作用域
     * @return 配置映射
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadFromFile(String scope) {
        Path configFile = resolveConfigFile(scope);

        if (!Files.exists(configFile)) {
            return new ConcurrentHashMap<>();
        }

        try (InputStream is = Files.newInputStream(configFile)) {
            Map<String, Object> loaded = yaml.loadAs(is, Map.class);
            if (loaded == null) {
                return new ConcurrentHashMap<>();
            }
            log.debug("已加载配置文件: {}", configFile);
            return new ConcurrentHashMap<>(loaded);
        } catch (IOException e) {
            log.warn("加载配置文件失败: {}", configFile, e);
            return new ConcurrentHashMap<>();
        }
    }

    /**
     * 解析配置文件路径。
     *
     * @param scope 配置作用域
     * @return 配置文件路径
     */
    private Path resolveConfigFile(String scope) {
        String fileName;
        if ("platform".equals(scope)) {
            fileName = "platform.yml";
        } else if (scope.startsWith("platform.")) {
            // platform 子作用域配置，如 platform.disabled
            fileName = scope.substring("platform.".length()) + ".yml";
        } else {
            // 插件私有配置，scope 即为 pluginId
            fileName = scope + ".yml";
        }
        return configDir.resolve(fileName);
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

    /**
     * 确保配置目录存在。
     */
    private void ensureConfigDirExists() {
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            throw new RuntimeException("创建配置目录失败: " + configDir, e);
        }
    }

    /**
     * 获取配置目录路径。
     *
     * @return 配置目录路径
     */
    public Path getConfigDir() {
        return configDir;
    }
}
