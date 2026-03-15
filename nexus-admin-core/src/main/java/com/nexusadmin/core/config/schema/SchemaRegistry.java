package com.nexusadmin.core.config.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schema 注册中心，管理所有配置域的 Schema。
 * <p>支持插件 Schema 和平台 Schema，线程安全，支持并发访问。</p>
 */
public class SchemaRegistry {

    private static final Logger log = LoggerFactory.getLogger(SchemaRegistry.class);

    /**
     * Schema 存储映射，key 为 schemaId（配置域 ID）。
     */
    private final Map<String, ConfigSchema> schemas = new ConcurrentHashMap<>();

    /**
     * 注册 Schema。
     *
     * @param schemaId 配置域 ID，例如插件ID或平台ID
     * @param schema   配置 Schema
     */
    public void register(String schemaId, ConfigSchema schema) {
        Objects.requireNonNull(schemaId, "配置域 ID 不能为空");
        Objects.requireNonNull(schema, "Schema 不能为空");

        schemas.put(schemaId, schema);
        log.debug("已注册 Schema: {} (属性数: {})", schemaId, schema.propertyCount());
    }

    /**
     * 获取指定配置域的 Schema。
     *
     * @param schemaId 配置域 ID
     * @return Schema，如果不存在返回空 Optional
     */
    public Optional<ConfigSchema> get(String schemaId) {
        return Optional.ofNullable(schemas.get(schemaId));
    }

    /**
     * 获取指定配置域的 Schema，如果不存在返回空 Schema。
     *
     * @param schemaId 配置域 ID
     * @return Schema，不会返回 null
     */
    public ConfigSchema getOrEmpty(String schemaId) {
        return get(schemaId).orElse(ConfigSchema.empty(schemaId));
    }

    /**
     * 检查是否包含指定配置域的 Schema。
     *
     * @param schemaId 配置域 ID
     * @return 如果包含返回 true
     */
    public boolean contains(String schemaId) {
        return schemas.containsKey(schemaId);
    }

    /**
     * 注销指定配置域的 Schema。
     *
     * @param schemaId 配置域 ID
     */
    public void unregister(String schemaId) {
        schemas.remove(schemaId);
        log.debug("已注销 Schema: {}", schemaId);
    }

    /**
     * 获取所有已注册的配置域 ID。
     *
     * @return 配置域 ID 集合
     */
    public Set<String> getRegisteredSchemaIds() {
        return Collections.unmodifiableSet(schemas.keySet());
    }

    /**
     * 获取所有已注册的 Schema。
     *
     * @return Schema 映射
     */
    public Map<String, ConfigSchema> getAllSchemas() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(schemas));
    }

    /**
     * 获取已注册的 Schema 数量。
     *
     * @return Schema 数量
     */
    public int size() {
        return schemas.size();
    }

    /**
     * 检查是否为空。
     *
     * @return 如果没有注册任何 Schema 返回 true
     */
    public boolean isEmpty() {
        return schemas.isEmpty();
    }

    /**
     * 清空所有注册的 Schema。
     */
    public void clear() {
        schemas.clear();
        log.debug("已清空所有 Schema");
    }
}
