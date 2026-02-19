package com.nexusadmin.api.extension.log;

import com.nexusadmin.api.context.CoreContext;
import com.nexusadmin.api.domain.log.LogEntry;
import com.nexusadmin.core.extension.ExtensionPoint;

import java.time.Duration;
import java.time.Instant;

/**
 * 日志保留策略扩展点，用于根据日志内容与默认保留时间决定是否保留以及过期时间。
 */
public interface LogRetentionPolicy extends ExtensionPoint {

    /**
     * 决定某条日志的保留策略。
     *
     * @param request 保留决策请求
     * @param context 平台上下文
     * @return 保留决策结果
     */
    RetentionDecision decide(LogRetentionRequest request, CoreContext context);

    /**
     * 日志保留决策请求。
     *
     * @param entry          日志条目
     * @param defaultRetention 默认保留时间
     */
    record LogRetentionRequest(LogEntry entry, Duration defaultRetention) {
    }

    /**
     * 日志保留决策结果。
     *
     * @param retain   是否保留
     * @param expireAt 过期时间
     * @param reason   决策原因
     */
    record RetentionDecision(boolean retain, Instant expireAt, String reason) {
    }
}
