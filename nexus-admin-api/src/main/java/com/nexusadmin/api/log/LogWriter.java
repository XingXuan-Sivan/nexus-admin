package com.nexusadmin.api.log;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.domain.log.LogEntry;
import com.nexusadmin.core.extension.ExtensionPoint;

/**
 * 日志写入扩展点，用于将规范化的日志条目输出到具体日志存储或管道。
 */
public interface LogWriter extends ExtensionPoint {

    /**
     * 写入一条日志到存储。
     *
     * @param entry   日志条目
     * @param context 调用上下文
     */
    void write(LogEntry entry, InvocationContext context);
}
