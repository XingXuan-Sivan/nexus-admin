package com.nexusadmin.infra.logging.adapter;

import com.nexusadmin.core.domain.log.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于 SLF4J 的简单日志桥接实现，将平台日志输出到本地日志系统。
 */
public class Slf4jLogBridge {
    private static final Logger log = LoggerFactory.getLogger(Slf4jLogBridge.class);

    /**
     * 记录一条平台日志到 SLF4J。
     *
     * @param entry 规范化日志条目
     */
    public void record(LogEntry entry) {
        if (entry == null) {
            return;
        }
        log.info("[{}] {}", entry.type(), entry.message());
    }
}
