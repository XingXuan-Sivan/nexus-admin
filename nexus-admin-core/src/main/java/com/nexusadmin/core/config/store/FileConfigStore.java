package com.nexusadmin.core.config.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件配置存储实现，将配置持久化到 YAML 文件。
 * <p>配置文件存储在 plugins-data/config/ 目录下。</p>
 * <p>支持 platform.yml、disabled.yml 和 {pluginId}.yml 文件。</p>
 */
public class FileConfigStore implements ConfigStore {

    private static final Logger log = LoggerFactory.getLogger(FileConfigStore.class);

    /**
     * 配置文件目录。
     */
    private final Path configDir;

    /**
     * 配置缓存，避免频繁读取文件。
     */
    private final Map<String, Map<String, Object>> configCache = new ConcurrentHashMap<>();

    /**
     * YAML 序列化器。
     */
    private final Yaml yaml;

    /**
     * 构造文件配置存储。
     *
     * @param configDir 配置文件目录
     */
    public FileConfigStore(Path configDir) {
        this.configDir = configDir;
        this.yaml = createYaml();
        ensureConfigDirExists();
    }

    @Override
    public Optional<String> get(String scope, String key) {
        Map<String, Object> config = loadConfig(scope);
        Object value = getNestedValue(config, key);
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }

    @Override
    public Map<String, Object> getScope(String scope) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(loadConfig(scope)));
    }

    @Override
    public void set(String scope, String key, Object value) {
        Path configFile = resolveConfigFile(scope);

        try {
            Map<String, Object> config = loadExistingConfig(configFile);
            setNestedValue(config, key, value);
            writeConfig(configFile, config);

            // 更新缓存
            configCache.put(scope, config);

            log.debug("配置已写入: {}.{} = {}", scope, key, value);
        } catch (IOException e) {
            throw new RuntimeException("写入配置失败: " + scope + "." + key, e);
        }
    }

    @Override
    public void setAll(String scope, Map<String, Object> config) {
        Path configFile = resolveConfigFile(scope);

        try {
            writeConfig(configFile, config);
            configCache.put(scope, new ConcurrentHashMap<>(config));
            log.debug("配置已批量写入: {} ({} 项)", scope, config.size());
        } catch (IOException e) {
            throw new RuntimeException("批量写入配置失败: " + scope, e);
        }
    }

    @Override
    public void remove(String scope, String key) {
        Path configFile = resolveConfigFile(scope);

        if (!Files.exists(configFile)) {
            return;
        }

        try {
            Map<String, Object> config = loadExistingConfig(configFile);
            removeNestedValue(config, key);
            writeConfig(configFile, config);
            configCache.put(scope, config);
            log.debug("配置已删除: {}.{}", scope, key);
        } catch (IOException e) {
            throw new RuntimeException("删除配置失败: " + scope + "." + key, e);
        }
    }

    @Override
    public boolean exists(String scope, String key) {
        return get(scope, key).isPresent();
    }

    @Override
    public void invalidateCache(String scope) {
        configCache.remove(scope);
        log.trace("已清除配置缓存: {}", scope);
    }

    @Override
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
        return configCache.computeIfAbsent(scope, s -> {
            Path configFile = resolveConfigFile(s);
            return loadExistingConfig(configFile);
        });
    }

    /**
     * 加载现有配置。
     *
     * @param configFile 配置文件路径
     * @return 配置映射
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadExistingConfig(Path configFile) {
        if (!Files.exists(configFile)) {
            return new ConcurrentHashMap<>();
        }

        try (var is = Files.newInputStream(configFile)) {
            Map<String, Object> loaded = yaml.loadAs(is, Map.class);
            if (loaded == null) {
                return new ConcurrentHashMap<>();
            }
            // 过滤掉 null 键和 null 值，避免 ConcurrentHashMap 抛出空指针异常
            Map<String, Object> filtered = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : loaded.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
            return new ConcurrentHashMap<>(filtered);
        } catch (IOException e) {
            log.warn("加载配置文件失败: {}", configFile, e);
            return new ConcurrentHashMap<>();
        }
    }

    /**
     * 写入配置到文件。
     *
     * @param configFile 配置文件路径
     * @param config     配置映射
     */
    private void writeConfig(Path configFile, Map<String, Object> config) throws IOException {
        Files.createDirectories(configFile.getParent());

        try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            yaml.dump(config, writer);
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
     * 设置嵌套配置值。
     *
     * @param config 配置映射
     * @param key    点分隔的键名
     * @param value  配置值
     */
    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> config, String key, Object value) {
        String[] parts = key.split("\\.");
        Map<String, Object> current = config;

        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) {
                next = new LinkedHashMap<String, Object>();
                current.put(parts[i], next);
            }
            current = (Map<String, Object>) next;
        }

        current.put(parts[parts.length - 1], value);
    }

    /**
     * 移除嵌套配置值。
     *
     * @param config 配置映射
     * @param key    点分隔的键名
     */
    @SuppressWarnings("unchecked")
    private void removeNestedValue(Map<String, Object> config, String key) {
        String[] parts = key.split("\\.");
        Map<String, Object> current = config;

        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) {
                return;
            }
            current = (Map<String, Object>) next;
        }

        current.remove(parts[parts.length - 1]);
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
     * 创建 YAML 序列化器。
     *
     * @return YAML 实例
     */
    private Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        return new Yaml(options);
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

