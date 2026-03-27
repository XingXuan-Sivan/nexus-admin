package com.nexusadmin.api.context;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 平台调用上下文，承载一次调用的身份、追踪、渠道等通用元数据。
 *
 * <p>该类为不可变对象，通过 {@link Builder} 构建，线程安全。</p>
 *
 * <p><strong>职责边界：</strong></p>
 * <ul>
 *   <li>承载调用元数据（租户、用户、追踪、渠道等），不包含业务实体</li>
 *   <li>作为 SPI 接口的环境参数，为插件提供上下文信息</li>
 *   <li>不持有框架对象、数据源、会话等重资源</li>
 * </ul>
 *
 * <p><strong>字段说明：</strong></p>
 * <ul>
 *   <li>{@code tenantId} - 租户标识，用于多租户隔离</li>
 *   <li>{@code userId} - 用户标识，支持匿名或系统任务为空</li>
 *   <li>{@code traceId} - 链路追踪标识，用于日志关联与分布式追踪</li>
 *   <li>{@code channelId} - 调用渠道标识（HTTP、CLI、SCHEDULER、AI_AGENT 等）</li>
 *   <li>{@code sessionId} - 会话标识，用于 AI 会话等长对话场景串联</li>
 *   <li>{@code timestamp} - 上下文创建时间</li>
 *   <li>{@code attributes} - 扩展属性，承载不固定或场景特定的信息</li>
 * </ul>
 */
public final class InvocationContext {

    /**
     * 当前调用所属租户标识。
     */
    private final String tenantId;

    /**
     * 当前调用所属用户标识。
     */
    private final String userId;

    /**
     * 当前调用的链路追踪标识。
     */
    private final String traceId;

    /**
     * 调用渠道标识，表示调用的来源渠道。
     *
     * <p>常见取值：HTTP、CLI、SCHEDULER、AI_AGENT、EVENT 等。</p>
     */
    private final String channelId;

    /**
     * 会话标识，用于串联同一会话内的多次调用。
     *
     * <p>主要用于 AI 会话、长时间交互等场景。</p>
     */
    private final String sessionId;

    /**
     * 上下文创建时间戳。
     */
    private final Instant timestamp;

    /**
     * 自定义扩展属性集合。
     *
     * <p>用于承载不固定或场景特定的上下文信息，如 IP、UserAgent、Locale 等。</p>
     */
    private final Map<String, String> attributes;

    /**
     * 私有构造方法，由 Builder 调用。
     *
     * @param builder 构建器
     */
    private InvocationContext(Builder builder) {
        this.tenantId = builder.tenantId;
        this.userId = builder.userId;
        this.traceId = builder.traceId;
        this.channelId = builder.channelId;
        this.sessionId = builder.sessionId;
        this.timestamp = builder.timestamp == null ? Instant.now() : builder.timestamp;
        this.attributes = Collections.unmodifiableMap(new HashMap<>(builder.attributes));
    }

    /**
     * 获取租户标识。
     *
     * @return 租户标识，可能为空
     */
    public String tenantId() {
        return tenantId;
    }

    /**
     * 获取用户标识。
     *
     * @return 用户标识，可能为空（匿名或系统任务）
     */
    public String userId() {
        return userId;
    }

    /**
     * 获取链路追踪标识。
     *
     * @return 追踪标识，可能为空
     */
    public String traceId() {
        return traceId;
    }

    /**
     * 获取调用渠道标识。
     *
     * @return 渠道标识，可能为空
     */
    public String channelId() {
        return channelId;
    }

    /**
     * 获取会话标识。
     *
     * @return 会话标识，可能为空
     */
    public String sessionId() {
        return sessionId;
    }

    /**
     * 获取上下文创建时间。
     *
     * @return 创建时间，不为空
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * 获取扩展属性集合。
     *
     * <p>返回的 Map 为不可变视图。</p>
     *
     * @return 扩展属性集合，不为空
     */
    public Map<String, String> attributes() {
        return attributes;
    }

    /**
     * 从扩展属性中获取指定键的值。
     *
     * @param key 属性键
     * @return 属性值，不存在则返回空
     */
    public String attribute(String key) {
        return attributes.get(key);
    }

    /**
     * 创建新的构建器实例。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 基于当前上下文创建新的构建器，便于复制修改。
     *
     * @return 包含当前值的构建器
     */
    public Builder toBuilder() {
        return new Builder()
                .tenantId(tenantId)
                .userId(userId)
                .traceId(traceId)
                .channelId(channelId)
                .sessionId(sessionId)
                .timestamp(timestamp)
                .attributes(attributes);
    }

    /**
     * 调用上下文构建器。
     */
    public static final class Builder {

        private String tenantId;
        private String userId;
        private String traceId;
        private String channelId;
        private String sessionId;
        private Instant timestamp;
        private final Map<String, String> attributes = new HashMap<>();

        private Builder() {
        }

        /**
         * 设置租户标识。
         *
         * @param tenantId 租户标识
         * @return 当前构建器
         */
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * 设置用户标识。
         *
         * @param userId 用户标识
         * @return 当前构建器
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 设置链路追踪标识。
         *
         * @param traceId 追踪标识
         * @return 当前构建器
         */
        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        /**
         * 设置调用渠道标识。
         *
         * @param channelId 渠道标识
         * @return 当前构建器
         */
        public Builder channelId(String channelId) {
            this.channelId = channelId;
            return this;
        }

        /**
         * 设置会话标识。
         *
         * @param sessionId 会话标识
         * @return 当前构建器
         */
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * 设置创建时间。
         *
         * @param timestamp 时间戳
         * @return 当前构建器
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * 添加单个扩展属性。
         *
         * @param key   属性键，不可为空
         * @param value 属性值，为空时忽略
         * @return 当前构建器
         */
        public Builder attribute(String key, String value) {
            Objects.requireNonNull(key, "属性键不可为空");
            if (value != null) {
                attributes.put(key, value);
            }
            return this;
        }

        /**
         * 批量添加扩展属性。
         *
         * @param attrs 属性集合
         * @return 当前构建器
         */
        public Builder attributes(Map<String, String> attrs) {
            if (attrs != null) {
                attributes.putAll(attrs);
            }
            return this;
        }

        /**
         * 构建不可变的调用上下文实例。
         *
         * @return 调用上下文
         */
        public InvocationContext build() {
            return new InvocationContext(this);
        }
    }
}
