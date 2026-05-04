package com.nexusadmin.api.domain.view;

/**
 * 插件状态视图枚举，用于管理面板展示插件状态。
 *
 * <p>该枚举与 core 模块的 PluginState 对应，但独立定义以解耦 API 与核心模块。</p>
 */
public enum PluginStateView {

    /**
     * 已发现状态，插件已被扫描但尚未加载。
     */
    DISCOVERED("已发现"),

    /**
     * 已加载状态，插件类已加载但尚未初始化。
     */
    LOADED("已加载"),

    /**
     * 已初始化状态，插件已完成初始化。
     */
    INITIALIZED("已初始化"),

    /**
     * 活跃状态，插件正在运行。
     */
    ACTIVE("运行中"),

    /**
     * 已停止状态，插件已停止但仍在内存中。
     */
    STOPPED("已停止"),

    /**
     * 禁用状态，插件被禁用，不会自动启动。
     */
    DISABLED("已禁用"),

    /**
     * 失败状态，插件加载或启动失败。
     */
    FAILED("失败");

    private final String displayName;

    PluginStateView(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 获取状态的显示名称。
     *
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
}
