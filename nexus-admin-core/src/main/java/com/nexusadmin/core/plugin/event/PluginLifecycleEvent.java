package com.nexusadmin.core.plugin.event;

import com.nexusadmin.core.plugin.loader.CandidatePlugin;
import com.nexusadmin.core.plugin.loader.LoadedPlugin;

/**
 * 插件生命周期事件。
 * <p>封装生命周期各阶段的事件信息，用于事件监听和日志记录。</p>
 *
 * @param type    事件类型
 * @param message 事件描述信息
 * @param payload 事件载荷（可选）
 */
public record PluginLifecycleEvent(
        Type type,
        String message,
        Object payload
) {

    /**
     * 事件类型枚举。
     */
    public enum Type {
        // 发现阶段
        DISCOVER_START, DISCOVER_END, DISCOVERED,
        // 解析阶段
        RESOLVE_START, RESOLVE_END, RESOLVED,
        // 安装阶段
        INSTALL_START, INSTALL_END, INSTALLED, SKIPPED, FAILED,
        // 启动/停止阶段
        STARTING, STARTED, STOPPING, STOPPED,
        // 卸载阶段
        UNINSTALLING, UNINSTALLED
    }

    // ==================== 工厂方法 ====================

    public static PluginLifecycleEvent discoverStart() {
        return new PluginLifecycleEvent(Type.DISCOVER_START, "开始扫描插件...", null);
    }

    public static PluginLifecycleEvent discoverEnd(int count) {
        return new PluginLifecycleEvent(Type.DISCOVER_END,
                String.format("发现候选插件数: %d", count), count);
    }

    public static PluginLifecycleEvent discovered(CandidatePlugin candidate) {
        return new PluginLifecycleEvent(Type.DISCOVERED,
                "发现插件: " + candidate.pluginId(), candidate);
    }

    public static PluginLifecycleEvent resolveStart(int count) {
        return new PluginLifecycleEvent(Type.RESOLVE_START,
                String.format("开始解析 %d 个候选插件...", count), count);
    }

    public static PluginLifecycleEvent resolveEnd(int count) {
        return new PluginLifecycleEvent(Type.RESOLVE_END,
                String.format("解析完成，有效插件数: %d", count), count);
    }

    public static PluginLifecycleEvent resolved(CandidatePlugin candidate) {
        return new PluginLifecycleEvent(Type.RESOLVED,
                "解析通过: " + candidate.pluginId(), candidate);
    }

    public static PluginLifecycleEvent installStart(int count) {
        return new PluginLifecycleEvent(Type.INSTALL_START,
                String.format("开始安装 %d 个插件...", count), count);
    }

    public static PluginLifecycleEvent installEnd(int count) {
        return new PluginLifecycleEvent(Type.INSTALL_END,
                String.format("安装完成，已安装插件数: %d", count), count);
    }

    public static PluginLifecycleEvent installed(LoadedPlugin loaded) {
        return new PluginLifecycleEvent(Type.INSTALLED,
                "插件安装成功: " + loaded.descriptor().id(), loaded);
    }

    public static PluginLifecycleEvent skipped(CandidatePlugin candidate, String reason) {
        return new PluginLifecycleEvent(Type.SKIPPED,
                String.format("跳过插件 %s: %s", candidate.pluginId(), reason), candidate);
    }

    public static PluginLifecycleEvent failed(CandidatePlugin candidate, Throwable error) {
        return new PluginLifecycleEvent(Type.FAILED,
                String.format("插件 %s 处理失败: %s", candidate.pluginId(), error.getMessage()),
                new FailurePayload(candidate, error));
    }

    public static PluginLifecycleEvent starting(LoadedPlugin loaded) {
        return new PluginLifecycleEvent(Type.STARTING,
                "正在启动插件: " + loaded.descriptor().id(), loaded);
    }

    public static PluginLifecycleEvent started(LoadedPlugin loaded) {
        return new PluginLifecycleEvent(Type.STARTED,
                "插件已启动: " + loaded.descriptor().id(), loaded);
    }

    public static PluginLifecycleEvent stopping(LoadedPlugin loaded) {
        return new PluginLifecycleEvent(Type.STOPPING,
                "正在停止插件: " + loaded.descriptor().id(), loaded);
    }

    public static PluginLifecycleEvent stopped(LoadedPlugin loaded) {
        return new PluginLifecycleEvent(Type.STOPPED,
                "插件已停止: " + loaded.descriptor().id(), loaded);
    }

    public static PluginLifecycleEvent uninstalling(LoadedPlugin loaded) {
        return new PluginLifecycleEvent(Type.UNINSTALLING,
                "正在卸载插件: " + loaded.descriptor().id(), loaded);
    }

    public static PluginLifecycleEvent uninstalled(LoadedPlugin loaded) {
        return new PluginLifecycleEvent(Type.UNINSTALLED,
                "插件已卸载: " + loaded.descriptor().id(), loaded);
    }

    /**
     * 失败事件载荷。
     */
    public record FailurePayload(CandidatePlugin candidate, Throwable error) {
    }
}
