package com.nexusadmin.core.config.resolver;

import com.nexusadmin.core.config.resolver.impl.FileConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 配置解析器，负责按优先级从多个配置源解析配置值。
 * <p>优先级顺序：Env > File > Default</p>
 */
public class ConfigResolver {

    private static final Logger log = LoggerFactory.getLogger(ConfigResolver.class);

    /**
     * 配置源列表，按优先级排序。
     */
    private final List<ConfigSource> sources = new CopyOnWriteArrayList<>();

    /**
     * 构造配置解析器。
     */
    public ConfigResolver() {
    }

    /**
     * 构造配置解析器并添加配置源。
     *
     * @param sources 配置源列表
     */
    public ConfigResolver(List<ConfigSource> sources) {
        sources.forEach(this::addSource);
    }

    /**
     * 添加配置源。
     * <p>配置源将按优先级自动排序。</p>
     *
     * @param source 配置源
     */
    public void addSource(ConfigSource source) {
        Objects.requireNonNull(source, "配置源不能为空");
        sources.add(source);
        sources.sort(Comparator.comparingInt(ConfigSource::priority));
        log.debug("已添加配置源: {} (优先级: {})", source.name(), source.priority());
    }

    /**
     * 移除配置源。
     *
     * @param source 配置源
     */
    public void removeSource(ConfigSource source) {
        sources.remove(source);
        log.debug("已移除配置源: {}", source.name());
    }

    /**
     * 解析配置值。
     * <p>按优先级遍历所有配置源，返回第一个找到的值。</p>
     *
     * @param scope 配置作用域
     * @param key   配置键名
     * @return 配置值，如果不存在则返回空 Optional
     */
    public Optional<String> resolve(String scope, String key) {
        Objects.requireNonNull(scope, "作用域不能为空");
        Objects.requireNonNull(key, "键名不能为空");

        for (ConfigSource source : sources) {
            try {
                Optional<String> value = source.get(scope, key);
                if (value.isPresent()) {
                    log.trace("配置已解析 [{}]: {}.{} = {}",
                            source.name(), scope, key, value.get());
                    return value;
                }
            } catch (Exception e) {
                log.warn("配置源解析失败 [{}]: {}.{}",
                        source.name(), scope, key, e);
            }
        }

        log.trace("配置未找到: {}.{}", scope, key);
        return Optional.empty();
    }

    /**
     * 获取所有配置源。
     *
     * @return 配置源列表
     */
    public List<ConfigSource> getSources() {
        return new ArrayList<>(sources);
    }

    /**
     * 清空所有配置源。
     */
    public void clearSources() {
        sources.clear();
        log.debug("已清空所有配置源");
    }

    /**
     * 使指定作用域的缓存失效。
     * <p>通知所有支持缓存的配置源清除缓存。</p>
     *
     * @param scope 配置作用域
     */
    public void invalidateCache(String scope) {
        for (ConfigSource source : sources) {
            if (source instanceof FileConfigSource fileSource) {
                fileSource.invalidateCache(scope);
            }
        }
        log.trace("已使配置缓存失效: {}", scope);
    }
}
