package com.nexusadmin.api.log.factory;

import com.nexusadmin.api.domain.log.LogEntry;
import com.nexusadmin.api.domain.log.LogLevel;
import com.nexusadmin.api.domain.log.LogType;

/**
 * 系统日志工厂，提供平台运行事件的标准日志构建方法。
 *
 * <p>所有方法返回预填充 type 的 LogEntry，调用方通过 LogWriter.write()
 * 或直接使用（SLF4J + MDC 路由）将日志写入存储。</p>
 */
public final class SystemLogFactory {

    /** 属性键：组件名称 */
    public static final String ATTR_COMPONENT = "component";
    /** 属性键：事件类型 */
    public static final String ATTR_EVENT = "event";
    /** 属性键：请求耗时（毫秒） */
    public static final String ATTR_DURATION_MS = "durationMs";

    private SystemLogFactory() {
    }

    /**
     * 插件生命周期事件。
     *
     * @param pluginId 插件ID
     * @param event    事件描述（如 "初始化完成"、"已启动"、"已停止"）
     * @param detail   详细信息（可选）
     * @return 日志条目
     */
    public static LogEntry pluginLifecycle(String pluginId, String event, String detail) {
        LogEntry.Builder builder = LogEntry.builder()
                .type(LogType.systemPluginLifecycle())
                .level(LogLevel.INFO)
                .message(String.format("插件 [%s] %s", pluginId, event))
                .attribute(ATTR_COMPONENT, pluginId)
                .attribute(ATTR_EVENT, event);
        if (detail != null) {
            builder.attribute("detail", detail);
        }
        return builder.build();
    }

    /**
     * API 请求日志。
     *
     * @param method     HTTP 方法
     * @param path       请求路径
     * @param status     HTTP 状态码
     * @param durationMs 请求耗时（毫秒）
     * @return 日志条目
     */
    public static LogEntry apiRequest(String method, String path, int status, long durationMs) {
        return LogEntry.builder()
                .type(LogType.systemApiRequest())
                .level(status >= 400 ? LogLevel.WARN : LogLevel.INFO)
                .message(String.format("%s %s → %d (%dms)", method, path, status, durationMs))
                .attribute("method", method)
                .attribute("path", path)
                .attribute("status", String.valueOf(status))
                .attribute(ATTR_DURATION_MS, String.valueOf(durationMs))
                .build();
    }

    /**
     * 配置变更日志。
     *
     * @param key      配置键
     * @param oldValue 旧值
     * @param newValue 新值
     * @return 日志条目
     */
    public static LogEntry configChange(String key, String oldValue, String newValue) {
        return LogEntry.builder()
                .type(LogType.systemConfigChange())
                .level(LogLevel.INFO)
                .message(String.format("配置变更: %s = %s → %s", key, oldValue, newValue))
                .attribute("configKey", key)
                .attribute("oldValue", oldValue)
                .attribute("newValue", newValue)
                .build();
    }

    /**
     * AI 调用日志。
     *
     * @param model      模型名称
     * @param promptHash 提示词哈希（避免存储完整提示词）
     * @param durationMs 调用耗时（毫秒）
     * @return 日志条目
     */
    public static LogEntry aiCall(String model, String promptHash, long durationMs) {
        return LogEntry.builder()
                .type(LogType.systemAiCall())
                .level(LogLevel.INFO)
                .message(String.format("AI 调用: %s (%dms)", model, durationMs))
                .attribute("model", model)
                .attribute("promptHash", promptHash)
                .attribute(ATTR_DURATION_MS, String.valueOf(durationMs))
                .build();
    }
}
