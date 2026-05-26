package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.log.LogEntry;
import com.nexusadmin.api.domain.log.LogLevel;
import com.nexusadmin.api.domain.log.LogType;
import com.nexusadmin.api.domain.result.PageResult;

import java.time.Duration;
import java.time.Instant;

/**
 * 日志管理接口，提供日志查询、清理与保留策略管理能力。
 *
 * <p>与 LogWriter ExtensionPoint 职责分离：
 * LogWriter 负责"写入"（运行时能力，可多实现），
 * LogService 负责"查询与管理"（管理面板功能，单一实现）。</p>
 */
public interface LogService {

    /** 按条件分页查询日志 */
    PageResult<LogEntry> query(LogType type, LogLevel level, String keyword,
                               Instant from, Instant to, int page, int size);

    /** 获取日志保留策略 */
    LogRetentionConfig getRetentionConfig();

    /** 更新日志保留策略 */
    void updateRetentionConfig(LogRetentionConfig config);

    /** 手动触发日志清理 */
    long cleanupExpired();

    /** 日志保留配置 */
    record LogRetentionConfig(Duration defaultRetention, Duration auditRetention,
                               Duration errorRetention, boolean autoCleanup) {}
}
