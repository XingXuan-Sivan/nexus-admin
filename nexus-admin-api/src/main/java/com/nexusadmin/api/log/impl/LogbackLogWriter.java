package com.nexusadmin.api.log.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.domain.log.LogEntry;
import com.nexusadmin.api.domain.log.LogLevel;
import com.nexusadmin.api.domain.log.LogType;
import com.nexusadmin.api.log.LogStorage;
import com.nexusadmin.api.log.LogWriter;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 默认日志写入器，桥接 Logback 与平台结构化日志体系。
 *
 * <p>双重角色：
 * <ul>
 *   <li><strong>Logback Appender</strong>：在 {@link #start()} 时附加到 Root Logger，
 *       自动拦截所有 SLF4J 日志，转换为 LogEntry 后写入 LogStorage。</li>
 *   <li><strong>LogWriter 实现</strong>：实现 LogWriter 的写入契约，
 *       供插件或代码显式写入结构化日志。</li>
 * </ul>
 *
 * <p>通过 MDC 自动注入上下文信息（traceId、userId、tenantId、channelId），
 * 并通过 MDC 键 {@code logType} 动态路由日志类型，默认使用 LogType.SYSTEM。</p>
 */
public class LogbackLogWriter extends AppenderBase<ILoggingEvent> implements LogWriter {

    private static final String MDC_LOG_TYPE = "logType";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_TENANT_ID = "tenantId";
    private static final String MDC_CHANNEL_ID = "channelId";

    private final LogStorage logStorage;

    public LogbackLogWriter(LogStorage logStorage) {
        this.logStorage = logStorage;
        setName("NexusAdminLogWriter");
    }

    // ═══════════════ Logback Appender 生命周期 ═══════════════

    @Override
    public void start() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(this);
        super.start();
    }

    @Override
    public void stop() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAppender(this);
        super.stop();
    }

    // ═══════════════ Logback Appender（隐式 SLF4J 路径） ═══════════════

    @Override
    protected void append(ILoggingEvent event) {
        LogType type = resolveType(MDC.get(MDC_LOG_TYPE));
        LogEntry entry = LogEntry.builder()
                .type(type)
                .level(convertLevel(event.getLevel()))
                .message(event.getFormattedMessage())
                .traceId(MDC.get(MDC_TRACE_ID))
                .userId(MDC.get(MDC_USER_ID))
                .tenantId(MDC.get(MDC_TENANT_ID))
                .channelId(MDC.get(MDC_CHANNEL_ID))
                .attribute("logger", event.getLoggerName())
                .attribute("thread", event.getThreadName())
                .build();
        logStorage.store(entry);
    }

    // ═══════════════ LogWriter 实现（显式结构化日志路径） ═══════════════

    @Override
    public void write(LogEntry entry, InvocationContext context) {
        LogEntry enriched = LogEntry.builder()
                .id(entry.id())
                .timestamp(entry.timestamp())
                .type(entry.type() != null ? entry.type() : LogType.SYSTEM)
                .level(entry.level() != null ? entry.level() : LogLevel.INFO)
                .message(entry.message())
                .tenantId(coalesce(entry.tenantId(), context != null ? context.tenantId() : null))
                .userId(coalesce(entry.userId(), context != null ? context.userId() : null))
                .traceId(coalesce(entry.traceId(), context != null ? context.traceId() : null))
                .sessionId(coalesce(entry.sessionId(), context != null ? context.sessionId() : null))
                .channelId(coalesce(entry.channelId(), context != null ? context.channelId() : null))
                .attributes(entry.attributes())
                .build();
        logStorage.store(enriched);
    }

    // ═══════════════ 工具方法 ═══════════════

    /**
     * 从 MDC 解析日志类型，未设置时默认 SYSTEM。
     */
    private static LogType resolveType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return LogType.SYSTEM;
        }
        return LogType.fromString(typeStr);
    }

    /**
     * 转换 Logback Level 为平台 LogLevel。
     */
    private static LogLevel convertLevel(Level level) {
        return switch (level.toInt()) {
            case Level.TRACE_INT -> LogLevel.TRACE;
            case Level.DEBUG_INT -> LogLevel.DEBUG;
            case Level.INFO_INT -> LogLevel.INFO;
            case Level.WARN_INT -> LogLevel.WARN;
            case Level.ERROR_INT -> LogLevel.ERROR;
            default -> LogLevel.INFO;
        };
    }

    /**
     * 返回第一个非空值，两者均为空时返回 null。
     */
    private static String coalesce(String first, String second) {
        return first != null ? first : second;
    }
}
