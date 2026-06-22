package com.nexusadmin.api.service.impl;

import com.nexusadmin.api.domain.log.LogEntry;
import com.nexusadmin.api.domain.log.LogLevel;
import com.nexusadmin.api.domain.result.PageResult;
import com.nexusadmin.api.log.LogStorage;
import com.nexusadmin.api.service.LogService;

import java.time.Duration;
import java.time.Instant;

/**
 * 日志管理的默认实现，委托 LogStorage 完成查询与清理。
 *
 * <p>保留策略由本类管理，存储细节完全委托 LogStorage。</p>
 */
public class DefaultLogService implements LogService {

    private final LogStorage logStorage;
    private volatile LogRetentionConfig retentionConfig;

    public DefaultLogService(LogStorage logStorage) {
        this.logStorage = logStorage;
        this.retentionConfig = new LogRetentionConfig(
                Duration.ofDays(30), Duration.ofDays(90), Duration.ofDays(180), true);
    }

    @Override
    public PageResult<LogEntry> query(String category, String typeName, LogLevel level,
                                       String keyword, Instant from, Instant to,
                                       int page, int size) {
        return logStorage.query(category, typeName, level, keyword, from, to, page, size);
    }

    @Override
    public LogRetentionConfig getRetentionConfig() {
        return retentionConfig;
    }

    @Override
    public void updateRetentionConfig(LogRetentionConfig config) {
        this.retentionConfig = config;
    }

    @Override
    public long cleanupExpired() {
        Instant now = Instant.now();
        Duration retention = retentionConfig.defaultRetention();
        if (retention == null) {
            return 0;
        }
        return logStorage.removeBefore(now.minus(retention));
    }
}
