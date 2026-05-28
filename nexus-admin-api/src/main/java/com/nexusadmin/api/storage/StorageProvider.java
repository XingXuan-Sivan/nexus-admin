package com.nexusadmin.api.storage;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.core.extension.ExtensionPoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 存储扩展点，用于统一对底层对象存储或文件存储的读写删除操作。
 */
public interface StorageProvider extends ExtensionPoint {

    /**
     * 保存对象到存储。
     *
     * @param object  存储对象
     * @param context 调用上下文
     */
    void save(StorageObject object, InvocationContext context);

    /**
     * 从存储加载对象。
     *
     * @param key     存储键
     * @param context 调用上下文
     * @return 存储对象，可为空
     */
    Optional<StorageObject> load(StorageKey key, InvocationContext context);

    /**
     * 从存储删除对象。
     *
     * @param key     存储键
     * @param context 调用上下文
     */
    void delete(StorageKey key, InvocationContext context);

    /**
     * 存储键。
     *
     * @param namespace 存储命名空间
     * @param key       存储键
     */
    record StorageKey(String namespace, String key) {
    }

    /**
     * 存储对象。
     *
     * @param key         存储键
     * @param payload     存储数据
     * @param contentType 内容类型
     * @param metadata    元数据
     */
    record StorageObject(StorageKey key,
                         byte[] payload,
                         String contentType,
                         Map<String, String> metadata) {
        public StorageObject {
            metadata = metadata == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(metadata));
        }
    }
}
