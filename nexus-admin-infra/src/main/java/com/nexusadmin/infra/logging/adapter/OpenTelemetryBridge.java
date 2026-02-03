package com.nexusadmin.infra.logging.adapter;

import com.nexusadmin.core.domain.log.LogEntry;

/**
 * OpenTelemetry 日志桥接预留实现，用于未来将平台日志投递到 OpenTelemetry 体系。
 */
public class OpenTelemetryBridge {
    /**
     * 记录一条日志到 OpenTelemetry。
     * 当前实现为空，作为未来集成占位。
     *
     * @param entry 规范化日志条目
     */
    public void record(LogEntry entry) {
        // reserved for OpenTelemetry integration
    }
}
