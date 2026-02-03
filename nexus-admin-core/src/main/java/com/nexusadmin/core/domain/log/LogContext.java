package com.nexusadmin.core.domain.log;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 日志上下文对象，承载日志记录时的上下文信息，包括用户、租户、链路等。
 *
 * @param userId      当前用户 ID
 * @param tenantId    当前租户 ID
 * @param traceId     链路追踪 ID
 * @param aiSessionId AI 会话 ID
 * @param attributes  自定义扩展属性
 */
public record LogContext(String userId,
                         String tenantId,
                         String traceId,
                         String aiSessionId,
                         Map<String, String> attributes) {
    public LogContext {
        attributes = attributes == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }
}
