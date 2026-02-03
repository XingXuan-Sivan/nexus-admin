package com.nexusadmin.core.spi.log;

import com.nexusadmin.core.context.CoreContext;
import com.nexusadmin.core.domain.log.LogEntry;

import java.time.Duration;
import java.time.Instant;

/**
 * 日志保留策略 SPI，用于根据日志内容与默认保留时间决定是否保留以及过期时间。
 */
public interface LogRetentionPolicy {
    /**
     * 决定某条日志的保留策略。
     *
     * @param request 保留决策请求
     * @param context 平台上下文
     * @return 保留决策结果
     */
    RetentionDecision decide(LogRetentionRequest request, CoreContext context);

    record LogRetentionRequest(LogEntry entry, Duration defaultRetention) {
    }

    record RetentionDecision(boolean retain, Instant expireAt, String reason) {
    }
}
