package com.nexusadmin.api.log;

import com.nexusadmin.api.domain.log.LogEntry;
import com.nexusadmin.api.domain.log.LogLevel;
import com.nexusadmin.api.domain.result.PageResult;

import java.time.Instant;

/**
 * 日志存储抽象，定义日志条目的持久化与查询契约。
 *
 * <p>与 LogWriter 职责分离：LogWriter 负责"日志写到哪"，
 * LogStorage 负责"日志怎么存、怎么查"。</p>
 *
 * <p>默认实现为 InMemoryLogStorage（基于 ConcurrentLinkedDeque 的内存存储），
 * 可通过声明同名 Spring Bean 替换为 ES、数据库等实现。</p>
 *
 * <p>category 和 typeName 均为 String 类型，不对日志类型做枚举校验，
 * 天然兼容插件通过 {@code LogType.of()} 创建的自定义类型。</p>
 */
public interface LogStorage {

    /**
     * 存储一条日志条目。
     *
     * @param entry 日志条目，不可为空
     */
    void store(LogEntry entry);

    /**
     * 按条件分页查询日志。
     *
     * @param category 主分类（可选，为空表示不限。如 "system"、"audit"）
     * @param typeName 子类型名称（可选，为空表示不限。如 "login"、"api-request"）
     * @param level    日志级别（可选，为空表示不限）
     * @param keyword  关键字（可选，为空表示不限，匹配 message 字段）
     * @param from     起始时间（可选）
     * @param to       截止时间（可选）
     * @param page     页码（从 1 开始）
     * @param size     每页数量
     * @return 分页日志条目
     */
    PageResult<LogEntry> query(String category, String typeName, LogLevel level,
                               String keyword, Instant from, Instant to, int page, int size);

    /**
     * 删除早于指定时间的所有日志条目。
     *
     * @param timestamp 截止时间
     * @return 删除的条目数量
     */
    long removeBefore(Instant timestamp);

    /**
     * 获取当前存储的日志条目总数。
     *
     * @return 日志条目总数
     */
    long count();
}
