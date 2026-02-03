package com.nexusadmin.infra.logging.interceptor;

import com.nexusadmin.core.context.CoreContext;
import com.nexusadmin.core.domain.log.LogContext;
import com.nexusadmin.core.domain.log.LogEntry;
import com.nexusadmin.core.domain.log.LogLevel;
import com.nexusadmin.core.domain.log.LogType;
import com.nexusadmin.core.service.CoreRuntime;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 鉴权决策日志拦截器，用于记录每次鉴权结果及原因。
 */
@Component
public class AuthDecisionInterceptor {
    private final CoreRuntime coreRuntime;

    /**
     * 构造拦截器。
     *
     * @param coreRuntime 核心运行时，用于写入日志
     */
    public AuthDecisionInterceptor(CoreRuntime coreRuntime) {
        this.coreRuntime = coreRuntime;
    }

    /**
     * 记录一次鉴权决策信息。
     *
     * @param subject 主体标识（如用户或角色）
     * @param allowed 是否允许访问
     * @param reason  鉴权结果原因说明
     * @param context 平台上下文，可为空
     */
    public void recordDecision(String subject, boolean allowed, String reason, CoreContext context) {
        CoreContext safeContext = context == null ? CoreContext.builder().build() : context;
        Map<String, String> attributes = new HashMap<>();
        attributes.put("subject", subject);
        attributes.put("allowed", String.valueOf(allowed));
        if (reason != null) {
            attributes.put("reason", reason);
        }
        LogContext logContext = new LogContext(
                safeContext.userId(),
                safeContext.tenantId(),
                safeContext.traceId(),
                safeContext.aiSessionId(),
                safeContext.attributes()
        );
        LogEntry entry = new LogEntry(
                null,
                null,
                LogType.AUDIT,
                LogLevel.INFO,
                "auth decision",
                logContext,
                attributes
        );
        coreRuntime.writeLog(entry, safeContext);
    }
}
