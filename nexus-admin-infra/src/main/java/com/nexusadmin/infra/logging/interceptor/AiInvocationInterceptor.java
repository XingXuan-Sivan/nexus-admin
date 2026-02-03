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
 * AI 调用日志拦截器，用于记录 AI 能力的调用情况（动作与耗时）。
 */
@Component
public class AiInvocationInterceptor {
    private final CoreRuntime coreRuntime;

    /**
     * 构造拦截器。
     *
     * @param coreRuntime 核心运行时，用于写入日志
     */
    public AiInvocationInterceptor(CoreRuntime coreRuntime) {
        this.coreRuntime = coreRuntime;
    }

    /**
     * 记录一次 AI 调用信息。
     *
     * @param action     调用动作标识
     * @param durationMs 调用耗时（毫秒）
     * @param context    平台上下文，可为空
     */
    public void recordInvocation(String action, long durationMs, CoreContext context) {
        CoreContext safeContext = context == null ? CoreContext.builder().build() : context;
        Map<String, String> attributes = new HashMap<>();
        attributes.put("action", action);
        attributes.put("durationMs", String.valueOf(durationMs));
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
                LogType.AI,
                LogLevel.INFO,
                "ai invocation",
                logContext,
                attributes
        );
        coreRuntime.writeLog(entry, safeContext);
    }
}
