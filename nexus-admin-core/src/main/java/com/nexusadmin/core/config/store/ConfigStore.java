package com.nexusadmin.core.config.store;

import java.util.Map;
import java.util.Optional;

/**
 * 配置存储 SPI 接口，定义配置持久化的标准操作。
 * <p>支持多种实现：文件存储、数据库存储、远程存储等。</p>
 */
public interface ConfigStore {

    /**
     * 获取指定作用域和键的配置值。
     *
     * @param scope 配置作用域
     * @param key   配置键名（支持点分隔的嵌套键）
     * @return 配置值，如果不存在则返回空 Optional
     */
    Optional<String> get(String scope, String key);

    /**
     * 获取指定作用域的完整配置映射。
     *
     * @param scope 配置作用域
     * @return 配置映射，如果不存在则返回空 Map
     */
    Map<String, Object> getScope(String scope);

    /**
     * 设置配置值。
     *
     * @param scope 配置作用域
     * @param key   配置键名（支持点分隔的嵌套键）
     * @param value 配置值
     */
    void set(String scope, String key, Object value);

    /**
     * 批量设置配置值。
     *
     * @param scope  配置作用域
     * @param config 配置映射
     */
    void setAll(String scope, Map<String, Object> config);

    /**
     * 删除指定配置。
     *
     * @param scope 配置作用域
     * @param key   配置键名
     */
    void remove(String scope, String key);

    /**
     * 检查指定配置是否存在。
     *
     * @param scope 配置作用域
     * @param key   配置键名
     * @return 如果配置存在返回 true
     */
    boolean exists(String scope, String key);

    /**
     * 清除指定作用域的缓存（如果实现支持缓存）。
     *
     * @param scope 配置作用域
     */
    void invalidateCache(String scope);

    /**
     * 清除所有缓存。
     */
    void invalidateAllCache();
}
