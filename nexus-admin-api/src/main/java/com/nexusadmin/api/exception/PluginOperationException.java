package com.nexusadmin.api.exception;

/**
 * 插件操作异常，用于表示插件管理操作失败。
 *
 * <p>该异常由管理门面接口的实现类抛出，供管理面板统一处理错误。</p>
 */
public class PluginOperationException extends RuntimeException {

    /**
     * 操作类型枚举。
     */
    public enum Operation {
        START("启动"),
        STOP("停止"),
        ENABLE("启用"),
        DISABLE("禁用"),
        UNLOAD("卸载"),
        LOAD("加载");

        private final String displayName;

        Operation(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String pluginId;
    private final Operation operation;

    /**
     * 创建插件操作异常。
     *
     * @param pluginId  插件标识
     * @param operation 操作类型
     * @param message   错误消息
     */
    public PluginOperationException(String pluginId, Operation operation, String message) {
        super(String.format("插件 %s %s失败: %s", pluginId, operation.getDisplayName(), message));
        this.pluginId = pluginId;
        this.operation = operation;
    }

    /**
     * 创建插件操作异常。
     *
     * @param pluginId  插件标识
     * @param operation 操作类型
     * @param message   错误消息
     * @param cause     原因异常
     */
    public PluginOperationException(String pluginId, Operation operation, String message, Throwable cause) {
        super(String.format("插件 %s %s失败: %s", pluginId, operation.getDisplayName(), message), cause);
        this.pluginId = pluginId;
        this.operation = operation;
    }

    /**
     * 获取插件标识。
     *
     * @return 插件标识
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * 获取操作类型。
     *
     * @return 操作类型
     */
    public Operation getOperation() {
        return operation;
    }
}
