package com.nexusadmin.api.domain.log;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 日志条目领域对象，记录平台内的各类日志事件。
 *
 * @param id         日志唯一标识，若为空则自动生成 UUID
 * @param timestamp  日志生成时间，若为空则自动使用当前时间
 * @param type       日志类型（审计、AI、业务等）
 * @param level      日志级别（INFO、WARN、ERROR 等）
 * @param message    日志消息内容
 * @param tenantId   租户标识
 * @param userId     用户标识
 * @param traceId    链路追踪标识
 * @param sessionId  会话标识
 * @param channelId  调用渠道标识
 * @param attributes 自定义扩展属性
 */
public record LogEntry(String id,
                       Instant timestamp,
                       LogType type,
                       LogLevel level,
                       String message,
                       String tenantId,
                       String userId,
                       String traceId,
                       String sessionId,
                       String channelId,
                       Map<String, String> attributes) {

    /**
     * 创建日志条目，自动生成 ID 和时间戳。
     */
    public LogEntry {
        id = id == null ? UUID.randomUUID().toString() : id;
        timestamp = timestamp == null ? Instant.now() : timestamp;
        attributes = attributes == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    /**
     * 从调用上下文创建日志条目构建器。
     *
     * @param context 调用上下文
     * @return 构建器
     */
    public static Builder fromContext(com.nexusadmin.api.context.InvocationContext context) {
        return new Builder()
                .tenantId(context.tenantId())
                .userId(context.userId())
                .traceId(context.traceId())
                .sessionId(context.sessionId())
                .channelId(context.channelId());
    }

    /**
     * 创建日志条目构建器。
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 日志条目构建器。
     */
    public static final class Builder {

        private String id;
        private Instant timestamp;
        private LogType type;
        private LogLevel level;
        private String message;
        private String tenantId;
        private String userId;
        private String traceId;
        private String sessionId;
        private String channelId;
        private final Map<String, String> attributes = new HashMap<>();

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder type(LogType type) {
            this.type = type;
            return this;
        }

        public Builder level(LogLevel level) {
            this.level = level;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder channelId(String channelId) {
            this.channelId = channelId;
            return this;
        }

        public Builder attribute(String key, String value) {
            if (key != null && value != null) {
                attributes.put(key, value);
            }
            return this;
        }

        public Builder attributes(Map<String, String> attrs) {
            if (attrs != null) {
                attributes.putAll(attrs);
            }
            return this;
        }

        public LogEntry build() {
            return new LogEntry(id, timestamp, type, level, message,
                    tenantId, userId, traceId, sessionId, channelId, attributes);
        }
    }
}
