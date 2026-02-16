package com.nexusadmin.api.spi.cache;

import com.nexusadmin.api.context.CoreContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 缓存 SPI，用于统一访问和管理不同底层实现的缓存数据。
 */
public interface CacheProvider {
    /**
     * 从缓存中读取数据。
     *
     * @param key     缓存键
     * @param context 平台上下文
     * @return 缓存值，可为空
     */
    Optional<CacheValue> get(CacheKey key, CoreContext context);

    /**
     * 向缓存写入数据。
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param context 平台上下文
     */
    void put(CacheKey key, CacheValue value, CoreContext context);

    /**
     * 从缓存中删除数据。
     *
     * @param key     缓存键
     * @param context 平台上下文
     */
    void evict(CacheKey key, CoreContext context);

    /**
     * 缓存键。
     *
     * @param namespace 缓存命名空间
     * @param key       缓存键
     */
    record CacheKey(String namespace, String key) {
    }

    /**
     * 缓存值。
     *
     * @param payload   缓存数据
     * @param ttlSeconds 过期时间（秒）
     * @param metadata  元数据
     */
    record CacheValue(byte[] payload,
                      long ttlSeconds,
                      Map<String, String> metadata) {
        public CacheValue {
            metadata = metadata == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(metadata));
        }
    }
}
