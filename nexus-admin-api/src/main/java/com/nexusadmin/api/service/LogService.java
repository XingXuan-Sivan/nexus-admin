package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.log.LogEntry;
import com.nexusadmin.api.domain.log.LogLevel;
import com.nexusadmin.api.domain.result.PageResult;

import java.time.Duration;
import java.time.Instant;

/**
 * 日志管理接口，提供日志查询、清理与保留策略管理能力。
 *
 * <p>与 LogWriter ExtensionPoint 职责分离：
 * LogWriter 负责"写入"（运行时能力，可多实现），
 * LogService 负责"查询与管理"（管理面板功能，单一实现）。</p>
 *
 * <p>查询接口使用 String category + String typeName 参数，
 * 天然兼容插件通过 {@code LogType.of()} 创建的自定义类型。</p>
 */
public interface LogService {

    /**
     * 按条件分页查询日志。
     *
     * @param category 主分类（可选，为空表示不限。如 "system"、"audit"）
     * @param typeName 子类型名称（可选，为空表示不限。如 "login"、"api-request"）
     * @param level    日志级别（可选）
     * @param keyword  关键字（可选）
     * @param from     起始时间（可选）
     * @param to       截止时间（可选）
     * @param page     页码（从 1 开始）
     * @param size     每页数量
     * @return 分页日志条目
     */
    PageResult<LogEntry> query(String category, String typeName, LogLevel level,
                               String keyword, Instant from, Instant to, int page, int size);

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
