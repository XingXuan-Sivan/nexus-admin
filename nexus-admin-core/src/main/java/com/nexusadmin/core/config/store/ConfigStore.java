package com.nexusadmin.core.config.store;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
     * 以 scope 为事务边界替换完整持久化配置，并执行乐观并发检查。
     *
     * @param scope            配置域
     * @param config           新的持久化配置
     * @param expectedRevision 客户端读取到的 revision
     * @return 写入后的 revision
     */
    default String replaceScope(String scope,
                                Map<String, Object> config,
                                String expectedRevision) {
        throw new UnsupportedOperationException("当前配置存储不支持 revision 原子写入");
    }

    /** 获取配置域当前 revision。 */
    default String getRevision(String scope) {
        return "unsupported";
    }

    /** 列出存储中已存在的配置域。 */
    default Set<String> listScopes() {
        return Set.of();
    }

    /** 当前存储是否支持受控原始文档。 */
    default boolean supportsDocuments() {
        return false;
    }

    /** 读取配置域对应的受控文档。 */
    default Optional<StoredConfigDocument> readDocument(String scope) {
        return Optional.empty();
    }

    /** 将 YAML 或 JSON 文档解析为类型化配置树，不产生写入。 */
    default Map<String, Object> parseDocument(String format, String content) {
        throw new UnsupportedOperationException("当前配置存储不支持原始文档");
    }

    /**
     * 校验并原子保存受控文档。
     *
     * @return 写入后的 revision
     */
    default String replaceDocument(String scope,
                                   String format,
                                   String content,
                                   String expectedRevision) {
        throw new UnsupportedOperationException("当前配置存储不支持原始文档");
    }

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
