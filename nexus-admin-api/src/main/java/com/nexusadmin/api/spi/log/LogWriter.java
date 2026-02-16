package com.nexusadmin.api.spi.log;

import com.nexusadmin.api.context.CoreContext;
import com.nexusadmin.api.domain.log.LogEntry;

/**
 * 日志写入 SPI，用于将规范化的日志条目输出到具体日志存储或管道。
 */
public interface LogWriter {
    /**
     * 写入一条日志到存储。
     *
     * @param entry   日志条目
     * @param context 平台上下文
     */
    void write(LogEntry entry, CoreContext context);
}
