package com.nexusadmin.core.config.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusadmin.core.config.ConfigScopeIds;
import com.nexusadmin.core.exception.ConfigRevisionConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于 YAML 文件的配置存储。
 *
 * <p>所有路径均由经过白名单校验的 scope 映射，调用方无法传入文件路径。写入以 scope
 * 为锁粒度，先写同目录临时文件并强制落盘，再进行原子替换；乐观锁 revision 在同一临界区
 * 内校验，避免检查与写入之间的竞态。</p>
 */
public final class FileConfigStore implements ConfigStore {

    private static final Logger log = LoggerFactory.getLogger(FileConfigStore.class);
    private static final int MAX_DOCUMENT_BYTES = 512 * 1024;
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final Path configDir;
    private final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Yaml yaml;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FileConfigStore(Path configDir) {
        this.configDir = configDir.toAbsolutePath().normalize();
        this.yaml = createYaml();
        ensureConfigDirExists();
    }

    @Override
    public Optional<String> get(String scope, String key) {
        Object value = nestedValue(loadConfig(scope), key);
        return value == null ? Optional.empty() : Optional.of(value.toString());
    }

    @Override
    public Map<String, Object> getScope(String scope) {
        return deepCopyMap(loadConfig(scope));
    }

    @Override
    public void set(String scope, String key, Object value) {
        ReentrantLock lock = lock(scope);
        lock.lock();
        try {
            Map<String, Object> config = loadFromFile(resolveConfigFile(scope));
            setNestedValue(config, key, value);
            writeLocked(scope, config, null);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setAll(String scope, Map<String, Object> config) {
        replaceScope(scope, config, null);
    }

    @Override
    public String replaceScope(String scope,
                               Map<String, Object> config,
                               String expectedRevision) {
        ReentrantLock lock = lock(scope);
        lock.lock();
        try {
            return writeLocked(scope, config == null ? Map.of() : config, expectedRevision);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(String scope, String key) {
        ReentrantLock lock = lock(scope);
        lock.lock();
        try {
            Map<String, Object> config = loadFromFile(resolveConfigFile(scope));
            removeNestedValue(config, key);
            writeLocked(scope, config, null);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean exists(String scope, String key) {
        return nestedValue(loadConfig(scope), key) != null;
    }

    @Override
    public String getRevision(String scope) {
        Path file = resolveConfigFile(scope);
        try {
            return revision(Files.exists(file) ? Files.readAllBytes(file) : new byte[0]);
        } catch (IOException e) {
            throw new IllegalStateException("读取配置 revision 失败: " + scope, e);
        }
    }

    @Override
    public Set<String> listScopes() {
        Set<String> scopes = new TreeSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(configDir, "*.yml")) {
            for (Path path : stream) {
                String name = path.getFileName().toString();
                String scope = name.substring(0, name.length() - ".yml".length());
                if (isValidScope(scope)) {
                    scopes.add(scope);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("列出配置域失败", e);
        }
        return Set.copyOf(scopes);
    }

    @Override
    public boolean supportsDocuments() {
        return true;
    }

    @Override
    public Optional<StoredConfigDocument> readDocument(String scope) {
        Path file = resolveConfigFile(scope);
        try {
            byte[] bytes = Files.exists(file) ? Files.readAllBytes(file) : new byte[0];
            if (bytes.length > MAX_DOCUMENT_BYTES) {
                throw new IllegalArgumentException("配置文档超过 " + MAX_DOCUMENT_BYTES + " 字节限制");
            }
            return Optional.of(new StoredConfigDocument(
                    scope,
                    file.getFileName().toString(),
                    detectFormat(bytes),
                    new String(bytes, StandardCharsets.UTF_8),
                    revision(bytes),
                    MAX_DOCUMENT_BYTES
            ));
        } catch (IOException e) {
            throw new IllegalStateException("读取配置文档失败: " + scope, e);
        }
    }

    @Override
    public Map<String, Object> parseDocument(String format, String content) {
        String normalizedFormat = format == null ? "" : format.trim().toLowerCase();
        if (!"yaml".equals(normalizedFormat) && !"json".equals(normalizedFormat)) {
            throw new IllegalArgumentException("配置文档格式仅支持 yaml 或 json");
        }
        byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_DOCUMENT_BYTES) {
            throw new IllegalArgumentException("配置文档超过 " + MAX_DOCUMENT_BYTES + " 字节限制");
        }
        if (content == null || content.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Object value = "json".equals(normalizedFormat)
                    ? objectMapper.readValue(content, MAP_TYPE)
                    : yaml.load(content);
            if (!(value instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("配置文档根节点必须是对象");
            }
            return stringKeyMap(map);
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON 配置文档语法无效");
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("YAML 配置文档语法无效");
        }
    }

    @Override
    public String replaceDocument(String scope,
                                  String format,
                                  String content,
                                  String expectedRevision) {
        Map<String, Object> parsed = parseDocument(format, content);
        String normalizedFormat = format.trim().toLowerCase();
        if ("json".equals(normalizedFormat)) {
            // 存储文件扩展名保持 .yml；JSON 是 YAML 的合法子集，可原样持久化。
            normalizedFormat = "json";
        }
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        ReentrantLock lock = lock(scope);
        lock.lock();
        try {
            Path target = resolveConfigFile(scope);
            byte[] currentBytes = Files.exists(target) ? Files.readAllBytes(target) : new byte[0];
            String currentRevision = revision(currentBytes);
            String normalizedExpected = normalizeRevision(expectedRevision);
            if (normalizedExpected != null && !normalizedExpected.equals(currentRevision)) {
                throw new ConfigRevisionConflictException(normalizedExpected, currentRevision);
            }
            writeAtomically(target, bytes);
            cache.put(scope, deepCopyMap(parsed));
            String newRevision = revision(bytes);
            log.debug("配置文档已原子写入: scope={}, format={}, revision={}",
                    scope, normalizedFormat, newRevision);
            return newRevision;
        } catch (ConfigRevisionConflictException e) {
            throw e;
        } catch (IOException e) {
            throw new IllegalStateException("写入配置文档失败: " + scope, e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void invalidateCache(String scope) {
        validateScope(scope);
        cache.remove(scope);
    }

    @Override
    public void invalidateAllCache() {
        cache.clear();
    }

    public Path getConfigDir() {
        return configDir;
    }

    private String writeLocked(String scope,
                               Map<String, Object> config,
                               String expectedRevision) {
        Path target = resolveConfigFile(scope);
        try {
            byte[] currentBytes = Files.exists(target) ? Files.readAllBytes(target) : new byte[0];
            String currentRevision = revision(currentBytes);
            String normalizedExpected = normalizeRevision(expectedRevision);
            if (normalizedExpected != null && !normalizedExpected.equals(currentRevision)) {
                throw new ConfigRevisionConflictException(normalizedExpected, currentRevision);
            }

            Map<String, Object> stableConfig = deepCopyMap(config);
            byte[] newBytes = yaml.dump(stableConfig).getBytes(StandardCharsets.UTF_8);
            if (newBytes.length > MAX_DOCUMENT_BYTES) {
                throw new IllegalArgumentException(
                        "配置文档超过 " + MAX_DOCUMENT_BYTES + " 字节限制");
            }
            writeAtomically(target, newBytes);
            cache.put(scope, deepCopyMap(stableConfig));
            String newRevision = revision(newBytes);
            log.debug("配置域已原子写入: scope={}, revision={}", scope, newRevision);
            return newRevision;
        } catch (ConfigRevisionConflictException e) {
            throw e;
        } catch (IOException e) {
            throw new IllegalStateException("写入配置失败: " + scope, e);
        }
    }

    private void writeAtomically(Path target, byte[] bytes) throws IOException {
        Files.createDirectories(configDir);
        Path temp = Files.createTempFile(configDir, "." + target.getFileName(), ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temp,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.write(ByteBuffer.wrap(bytes));
                channel.force(true);
            }
            if (Files.exists(target)) {
                Path backup = target.resolveSibling(target.getFileName() + ".bak");
                Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temp, target,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                throw new IOException("当前文件系统不支持配置文件原子替换", e);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private Map<String, Object> loadConfig(String scope) {
        validateScope(scope);
        return cache.computeIfAbsent(scope, key -> loadFromFile(resolveConfigFile(key)));
    }

    private Map<String, Object> loadFromFile(Path file) {
        if (!Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length > MAX_DOCUMENT_BYTES) {
                throw new IllegalArgumentException("配置文档超过 " + MAX_DOCUMENT_BYTES + " 字节限制");
            }
            if (bytes.length == 0) {
                return new LinkedHashMap<>();
            }
            Object loaded = yaml.load(new String(bytes, StandardCharsets.UTF_8));
            if (loaded == null) {
                return new LinkedHashMap<>();
            }
            if (!(loaded instanceof Map<?, ?> map)) {
                throw new IllegalStateException("配置文件根节点必须是对象: " + file.getFileName());
            }
            return stringKeyMap(map);
        } catch (IOException e) {
            throw new IllegalStateException("读取配置文件失败: " + file.getFileName(), e);
        } catch (RuntimeException e) {
            // Parser exceptions can embed a source line. Do not retain them as a cause because
            // the global exception logger would then expose configuration values.
            throw new IllegalStateException("解析配置文件失败: " + file.getFileName());
        }
    }

    private Path resolveConfigFile(String scope) {
        validateScope(scope);
        String fileName = scope + ".yml";
        Path target = configDir.resolve(fileName).normalize();
        if (!target.startsWith(configDir)) {
            throw new IllegalArgumentException("配置域映射超出配置目录");
        }
        return target;
    }

    private ReentrantLock lock(String scope) {
        validateScope(scope);
        return locks.computeIfAbsent(scope, ignored -> new ReentrantLock());
    }

    private void validateScope(String scope) {
        ConfigScopeIds.requireValid(scope);
    }

    private boolean isValidScope(String scope) {
        return ConfigScopeIds.isValid(scope);
    }

    private String normalizeRevision(String revision) {
        if (revision == null || revision.isBlank()) {
            return null;
        }
        String value = revision.trim();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String revision(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder("cfg-");
            for (int i = 0; i < 12; i++) {
                result.append(String.format("%02x", digest[i]));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("运行环境缺少 SHA-256", e);
        }
    }

    private String detectFormat(byte[] bytes) {
        String content = new String(bytes, StandardCharsets.UTF_8).stripLeading();
        return content.startsWith("{") ? "json" : "yaml";
    }

    @SuppressWarnings("unchecked")
    private Object nestedValue(Map<String, Object> config, String key) {
        Object current = config;
        for (String part : key.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> config, String key, Object value) {
        String[] parts = key.split("\\.");
        Map<String, Object> current = config;
        for (int index = 0; index < parts.length - 1; index++) {
            Object child = current.get(parts[index]);
            if (!(child instanceof Map<?, ?>)) {
                child = new LinkedHashMap<String, Object>();
                current.put(parts[index], child);
            }
            current = (Map<String, Object>) child;
        }
        current.put(parts[parts.length - 1], deepCopy(value));
    }

    @SuppressWarnings("unchecked")
    private void removeNestedValue(Map<String, Object> config, String key) {
        String[] parts = key.split("\\.");
        Map<String, Object> current = config;
        for (int index = 0; index < parts.length - 1; index++) {
            Object child = current.get(parts[index]);
            if (!(child instanceof Map<?, ?>)) {
                return;
            }
            current = (Map<String, Object>) child;
        }
        current.remove(parts[parts.length - 1]);
    }

    private Map<String, Object> stringKeyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key == null) {
                throw new IllegalArgumentException("配置对象不允许 null 键");
            }
            result.put(key.toString(), deepCopy(value));
        });
        return result;
    }

    private Map<String, Object> deepCopyMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, deepCopy(value)));
        return result;
    }

    private Object deepCopy(Object value) {
        if (value instanceof Map<?, ?> map) {
            return stringKeyMap(map);
        }
        if (value instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>();
            collection.forEach(item -> result.add(deepCopy(item)));
            return result;
        }
        return value;
    }

    private Yaml createYaml() {
        LoaderOptions loader = new LoaderOptions();
        loader.setAllowDuplicateKeys(false);
        loader.setMaxAliasesForCollections(50);
        loader.setCodePointLimit(MAX_DOCUMENT_BYTES);
        DumperOptions dumper = new DumperOptions();
        dumper.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumper.setPrettyFlow(true);
        dumper.setIndent(2);
        return new Yaml(new SafeConstructor(loader), new Representer(dumper), dumper, loader);
    }

    private void ensureConfigDirExists() {
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            throw new IllegalStateException("创建配置目录失败: " + configDir, e);
        }
    }
}
