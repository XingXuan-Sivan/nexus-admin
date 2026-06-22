package com.nexusadmin.api.log.impl;

import com.nexusadmin.api.domain.log.LogEntry;
import com.nexusadmin.api.domain.log.LogLevel;
import com.nexusadmin.api.domain.result.PageResult;
import com.nexusadmin.api.log.LogStorage;

import java.time.Instant;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * 日志存储默认内存实现，基于 ConcurrentLinkedDeque 存储最近日志。
 *
 * <p>支持按 category、typeName、级别、关键字、时间范围进行分页查询。
 * category 和 typeName 均为可选的字符串参数，天然兼容插件自定义类型。</p>
 */
public class InMemoryLogStorage implements LogStorage {

    private static final int MAX_ENTRIES = 10000;

    private final Deque<LogEntry> entries = new ConcurrentLinkedDeque<>();

    @Override
    public void store(LogEntry entry) {
        entries.addFirst(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.pollLast();
        }
    }

    @Override
    public PageResult<LogEntry> query(String category, String typeName, LogLevel level,
                                       String keyword, Instant from, Instant to,
                                       int page, int size) {
        List<LogEntry> filtered = entries.stream()
                .filter(e -> category == null || category.isEmpty()
                        || (e.type() != null && e.type().isCategory(category)))
                .filter(e -> typeName == null || typeName.isEmpty()
                        || (e.type() != null && e.type().name().equals(typeName)))
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
    public long removeBefore(Instant timestamp) {
        long count = entries.stream()
                .filter(e -> e.timestamp().isBefore(timestamp))
                .count();
        entries.removeIf(e -> e.timestamp().isBefore(timestamp));
        return count;
    }

    @Override
    public long count() {
        return entries.size();
    }
}
