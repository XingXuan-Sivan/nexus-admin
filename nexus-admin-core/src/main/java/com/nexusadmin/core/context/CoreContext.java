package com.nexusadmin.core.context;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 平台调用上下文，承载租户、用户、链路追踪、AI 会话等通用元数据。
 * <p>该类为不可变对象，通过 {@link CoreContext.Builder} 构建，线程安全。</p>
 */
public class CoreContext {
    /**
     * 当前请求所属租户 ID。
     */
    private final String tenantId;
    /**
     * 当前请求所属用户 ID。
     */
    private final String userId;
    /**
     * 当前请求的链路追踪 ID。
     */
    private final String traceId;
    /**
     * AI 会话 ID，用于串联同一 AI 会话内的多次调用。
     */
    private final String aiSessionId;
    /**
     * 上下文创建时间戳。
     */
    private final Instant timestamp;
    /**
     * 自定义属性集合，用于扩展额外的上下文信息。
     */
    private final Map<String, String> attributes;

    /**
     * 构造上下文对象，通常由 Builder 调用。
     *
     * @param builder 构建器
     */
    private CoreContext(Builder builder) {
        this.tenantId = builder.tenantId;
        this.userId = builder.userId;
        this.traceId = builder.traceId;
        this.aiSessionId = builder.aiSessionId;
        this.timestamp = builder.timestamp == null ? Instant.now() : builder.timestamp;
        this.attributes = Collections.unmodifiableMap(new HashMap<>(builder.attributes));
    }

    public String tenantId() {
        return tenantId;
    }

    public String userId() {
        return userId;
    }

    public String traceId() {
        return traceId;
    }

    public String aiSessionId() {
        return aiSessionId;
    }

    public Instant timestamp() {
        return timestamp;
    }

    /**
     * 返回不可变的属性 Map。
     *
     * @return 属性集合
     */
    public Map<String, String> attributes() {
        return attributes;
    }

    /**
     * 创建一个新的 Builder 实例。
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String tenantId;
        private String userId;
        private String traceId;
        private String aiSessionId;
        private Instant timestamp;
        private final Map<String, String> attributes = new HashMap<>();

        /**
         * 设置租户 ID。
         *
         * @param tenantId 租户 ID
         * @return 当前 Builder
         */
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * 设置用户 ID。
         *
         * @param userId 用户 ID
         * @return 当前 Builder
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 设置链路追踪 ID。
         *
         * @param traceId 链路追踪 ID
         * @return 当前 Builder
         */
        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        /**
         * 设置 AI 会话 ID。
         *
         * @param aiSessionId AI 会话 ID
         * @return 当前 Builder
         */
        public Builder aiSessionId(String aiSessionId) {
            this.aiSessionId = aiSessionId;
            return this;
        }

        /**
         * 设置时间戳。
         *
         * @param timestamp 时间戳
         * @return 当前 Builder
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * 添加单个属性键值对。
         *
         * @param key   属性键，不可为空
         * @param value 属性值，可为空
         * @return 当前 Builder
         */
        public Builder attribute(String key, String value) {
            Objects.requireNonNull(key, "attribute key");
            if (value != null) {
                attributes.put(key, value);
            }
            return this;
        }

        /**
         * 批量添加属性集合。
         *
         * @param attrs 属性 Map
         * @return 当前 Builder
         */
        public Builder attributes(Map<String, String> attrs) {
            if (attrs != null) {
                attributes.putAll(attrs);
            }
            return this;
        }

        /**
         * 构建不可变的 CoreContext 实例。
         *
         * @return CoreContext
         */
        public CoreContext build() {
            return new CoreContext(this);
        }
    }
}
