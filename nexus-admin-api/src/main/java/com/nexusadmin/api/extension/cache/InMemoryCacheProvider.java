package com.nexusadmin.api.extension.cache;

import com.nexusadmin.api.context.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 ConcurrentHashMap 的内存缓存默认实现。
 *
 * <p>作为 CacheProvider ExtensionPoint 的兜底实现，无外部依赖。</p>
 */
public class InMemoryCacheProvider implements CacheProvider {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCacheProvider.class);

    private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<CacheValue> get(CacheKey key, InvocationContext context) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        // 检查是否过期
        if (entry.isExpired()) {
            cache.remove(key);
            log.debug("缓存已过期: {}", key);
            return Optional.empty();
        }
        return Optional.of(entry.value);
    }

    @Override
    public void put(CacheKey key, CacheValue value, InvocationContext context) {
        cache.put(key, new CacheEntry(value));
        log.debug("已写入缓存: {}", key);
    }

    @Override
    public void evict(CacheKey key, InvocationContext context) {
        cache.remove(key);
        log.debug("已清除缓存: {}", key);
    }

    /**
     * 缓存条目，记录写入时间戳用于过期判断。
     */
    private static class CacheEntry {
        final CacheValue value;
        final long writeTimeMillis;

        CacheEntry(CacheValue value) {
            this.value = value;
            this.writeTimeMillis = System.currentTimeMillis();
        }

        boolean isExpired() {
            if (value.ttlSeconds() <= 0) {
                return false;
            }
            long expireTime = writeTimeMillis + value.ttlSeconds() * 1000;
            return System.currentTimeMillis() > expireTime;
        }
    }
}
