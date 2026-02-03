package com.nexusadmin.infra.logging.interceptor;

import com.nexusadmin.core.context.CoreContext;
import com.nexusadmin.core.domain.log.LogContext;
import com.nexusadmin.core.domain.log.LogEntry;
import com.nexusadmin.core.domain.log.LogLevel;
import com.nexusadmin.core.domain.log.LogType;
import com.nexusadmin.core.service.CoreRuntime;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 访问日志拦截器，记录每次请求的基本信息并通过 CoreRuntime 写入技术日志。
 */
@Component
public class HttpAccessInterceptor implements HandlerInterceptor {
    private static final String START_TIME_ATTR = "platform.infra.httpStartTime";

    private final CoreRuntime coreRuntime;

    /**
     * 构造拦截器。
     *
     * @param coreRuntime 核心运行时，用于写入日志
     */
    public HttpAccessInterceptor(CoreRuntime coreRuntime) {
        this.coreRuntime = coreRuntime;
    }

    /**
     * 请求预处理阶段，记录开始时间用于后续计算耗时。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return 是否继续处理请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    /**
     * 请求完成后回调，组装访问日志并写入核心日志体系。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @param ex       异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        long start = getStartTime(request);
        long cost = start == 0L ? 0L : Math.max(0L, System.currentTimeMillis() - start);
        CoreContext context = CoreContext.builder()
                .tenantId(request.getHeader("X-Tenant-Id"))
                .userId(request.getHeader("X-User-Id"))
                .traceId(request.getHeader("X-Trace-Id"))
                .attribute("userAgent", request.getHeader("User-Agent"))
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put("method", request.getMethod());
        attributes.put("path", request.getRequestURI());
        attributes.put("status", String.valueOf(response.getStatus()));
        attributes.put("durationMs", String.valueOf(cost));

        LogContext logContext = new LogContext(
                context.userId(),
                context.tenantId(),
                context.traceId(),
                context.aiSessionId(),
                context.attributes()
        );
        LogEntry entry = new LogEntry(
                null,
                null,
                LogType.TECH,
                LogLevel.INFO,
                "HTTP " + request.getMethod() + " " + request.getRequestURI(),
                logContext,
                attributes
        );
        coreRuntime.writeLog(entry, context);
    }

    /**
     * 从请求属性中安全获取开始时间。
     * 
     * @param request HTTP 请求
     */
    private long getStartTime(HttpServletRequest request) {
        Object value = request.getAttribute(START_TIME_ATTR);
        if (value instanceof Long time) {
            return time;
        }
        return 0L;
    }
}
