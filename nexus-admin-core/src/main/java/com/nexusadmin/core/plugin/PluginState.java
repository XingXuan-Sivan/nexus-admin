package com.nexusadmin.core.plugin;

public enum PluginState {
    /**
     * 已安装（已加载到内存，已完成实例化，待启动）。
     */
    INSTALLED,
    /**
     * 已启动（已调用 start 方法，运行中）。
     */
    STARTED,
    /**
     * 已停止（已调用 stop 方法，可重新启动）。
     */
    STOPPED,
    /**
     * 已卸载（类加载器已释放，从注册表中移除）。
     */
    UNINSTALLED,
    /**
     * 失败（生命周期流转过程中发生异常）。
     */
    FAILED
}
