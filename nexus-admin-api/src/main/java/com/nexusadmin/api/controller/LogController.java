package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.domain.log.LogEntry;
import com.nexusadmin.api.domain.log.LogLevel;
import com.nexusadmin.api.domain.log.LogType;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.domain.result.PageResult;
import com.nexusadmin.api.service.LogService;
import com.nexusadmin.api.service.LogService.LogRetentionConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 日志查看控制器。
 *
 * <p>提供日志查询、保留策略管理与清理 API。</p>
 */
@RestController
@RequestMapping("/admin/v1/logs")
@Tag(name = "日志管理")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    /**
     * 按条件分页查询日志。
     *
     * @param type    日志类型（可选）
     * @param level   日志级别（可选）
     * @param keyword 关键字（可选）
     * @param from    开始时间（可选）
     * @param to      结束时间（可选）
     * @param page    当前页码
     * @param size    每页数量
     * @return 分页日志条目
     */
    @GetMapping
    @RequirePermission("system.view")
    @Operation(summary = "查询日志")
    public PageResult<LogEntry> query(@RequestParam(required = false) LogType type,
                                       @RequestParam(required = false) LogLevel level,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Instant from,
                                       @RequestParam(required = false) Instant to,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return logService.query(type, level, keyword, from, to, page, size);
    }

    /**
     * 获取日志保留策略。
     */
    @GetMapping("/retention")
    @RequirePermission("system.view")
    @Operation(summary = "获取日志保留策略")
    public DataResult<LogRetentionConfig> getRetentionConfig() {
        return DataResult.success(logService.getRetentionConfig());
    }

    /**
     * 更新日志保留策略。
     */
    @PutMapping("/retention")
    @RequirePermission("config.manage")
    @Operation(summary = "更新日志保留策略")
    public DataResult<LogRetentionConfig> updateRetentionConfig(@RequestBody LogRetentionConfig config) {
        logService.updateRetentionConfig(config);
        return DataResult.success(logService.getRetentionConfig());
    }

    /**
     * 手动触发日志清理。
     */
    @PostMapping("/cleanup")
    @RequirePermission("config.manage")
    @Operation(summary = "清理过期日志")
    public DataResult<Long> cleanup() {
        long count = logService.cleanupExpired();
        return DataResult.success(count);
    }
}
