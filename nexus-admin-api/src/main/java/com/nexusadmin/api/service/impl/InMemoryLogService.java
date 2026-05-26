package com.nexusadmin.api.service.impl;

import com.nexusadmin.api.domain.log.LogEntry;
import com.nexusadmin.api.domain.log.LogLevel;
import com.nexusadmin.api.domain.log.LogType;
import com.nexusadmin.api.domain.result.PageResult;
import com.nexusadmin.api.service.LogService;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * 日志管理的默认内存实现，基于 ConcurrentLinkedDeque 存储最近日志。
 */
public class InMemoryLogService implements LogService {

    private static final int MAX_ENTRIES = 10000;

    private final Deque<LogEntry> entries = new ConcurrentLinkedDeque<>();
    private volatile LogRetentionConfig retentionConfig = new LogRetentionConfig(
            Duration.ofDays(30), Duration.ofDays(90), Duration.ofDays(180), true);

    @Override
    public PageResult<LogEntry> query(LogType type, LogLevel level, String keyword,
                                      Instant from, Instant to, int page, int size) {
        List<LogEntry> filtered = entries.stream()
                .filter(e -> type == null || e.type() == type)
                .filter(e -> level == null || e.level() == level)
                .filter(e -> keyword == null || keyword.isEmpty()
                        || (e.message() != null && e.message().contains(keyword)))
                .filter(e -> from == null || !e.timestamp().isBefore(from))
                .filter(e -> to == null || !e.timestamp().isAfter(to))
                .sorted(Comparator.comparing(LogEntry::timestamp).reversed())
                .collect(Collectors.toList());

        int total = filtered.size();
        int fromIdx = Math.min((page - 1) * size, total);
        int toIdx = Math.min(fromIdx + size, total);
        return PageResult.of(total, page, size, filtered.subList(fromIdx, toIdx));
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
        long count = entries.stream()
                .filter(e -> e.timestamp().isBefore(now.minus(retention)))
                .count();
        entries.removeIf(e -> e.timestamp().isBefore(now.minus(retention)));
        return count;
    }

    /**
     * 写入日志条目（供 LogWriter 等组件调用）。
     */
    public void append(LogEntry entry) {
        entries.addFirst(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.pollLast();
        }
    }
}
