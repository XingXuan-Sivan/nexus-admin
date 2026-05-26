package com.nexusadmin.api.extension.storage;

import com.nexusadmin.api.context.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于本地文件系统的存储默认实现，支持 namespace/key 二级目录。
 *
 * <p>作为 StorageProvider ExtensionPoint 的兜底实现，无外部依赖。</p>
 */
public class LocalStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageProvider.class);

    private final Path baseDir;
    private final Map<StorageKey, byte[]> memoryFallback = new ConcurrentHashMap<>();

    /**
     * 构造本地存储提供者。
     *
     * @param baseDir 存储根目录
     */
    public LocalStorageProvider(String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            log.warn("无法创建存储根目录: {}，将使用内存回退", this.baseDir, e);
        }
    }

    /**
     * 使用默认目录构造。
     */
    public LocalStorageProvider() {
        this("data/storage");
    }

    @Override
    public void save(StorageObject object, InvocationContext context) {
        Path filePath = resolvePath(object.key());
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, object.payload());
            log.debug("已保存存储对象: {}", object.key());
        } catch (IOException e) {
            log.warn("文件写入失败，使用内存回退: {}", object.key(), e);
            memoryFallback.put(object.key(), object.payload());
        }
    }

    @Override
    public Optional<StorageObject> load(StorageKey key, InvocationContext context) {
        Path filePath = resolvePath(key);
        try {
            if (Files.exists(filePath)) {
                byte[] payload = Files.readAllBytes(filePath);
                String contentType = Files.probeContentType(filePath);
                return Optional.of(new StorageObject(key, payload,
                        contentType != null ? contentType : "application/octet-stream",
                        Map.of("source", "local")));
            }
        } catch (IOException e) {
            log.warn("文件读取失败: {}", key, e);
        }
        // 内存回退
        byte[] bytes = memoryFallback.get(key);
        if (bytes != null) {
            return Optional.of(new StorageObject(key, bytes, "application/octet-stream",
                    Map.of("source", "memory-fallback")));
        }
        return Optional.empty();
    }

    @Override
    public void delete(StorageKey key, InvocationContext context) {
        Path filePath = resolvePath(key);
        try {
            Files.deleteIfExists(filePath);
            log.debug("已删除存储对象: {}", key);
        } catch (IOException e) {
            log.warn("文件删除失败: {}", key, e);
        }
        memoryFallback.remove(key);
    }

    /**
     * 根据存储键解析文件路径。
     *
     * @param key 存储键
     * @return 文件路径
     */
    private Path resolvePath(StorageKey key) {
        return baseDir.resolve(key.namespace()).resolve(key.key());
    }
}
