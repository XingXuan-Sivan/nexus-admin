package com.nexusadmin.core.domain.log;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 日志条目领域对象，记录平台内的各类日志事件。
 *
 * @param id         日志唯一标识，若为 null 则自动生成 UUID
 * @param timestamp  日志生成时间，若为 null 则自动使用当前时间
 * @param type       日志类型（审计、AI、业务等）
 * @param level      日志级别（INFO、WARN、ERROR 等）
 * @param message    日志消息内容
 * @param context    日志上下文，包含租户、用户、链路等信息
 * @param attributes 自定义扩展属性
 */
public record LogEntry(String id,
                       Instant timestamp,
                       LogType type,
                       LogLevel level,
                       String message,
                       LogContext context,
                       Map<String, String> attributes) {
    public LogEntry {
        id = id == null ? UUID.randomUUID().toString() : id;
        timestamp = timestamp == null ? Instant.now() : timestamp;
        attributes = attributes == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }
}
