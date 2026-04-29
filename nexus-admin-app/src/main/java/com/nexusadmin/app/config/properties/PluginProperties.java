package com.nexusadmin.app.config.properties;

import com.nexusadmin.core.plugin.RuntimeMode;

/**
 * 插件系统配置。
 * <p>
 * 管理插件运行时所需的各项参数，包括目录路径、版本、运行模式等。
 * 作为 {@link PlatformProperties} 的子配置组件。
 */
public class PluginProperties {

    /**
     * 插件根目录路径。
     */
    private String path = "plugins";

    /**
     * 插件数据目录路径。
     */
    private String dataPath = "plugins-data";

    /**
     * 插件核心版本号。
     */
    private String coreVersion = "1.0.0";

    /**
     * 运行模式，可选值：DEVELOPMENT、DEPLOYMENT。
     */
    private RuntimeMode runtimeMode = RuntimeMode.DEVELOPMENT;

    /**
     * 是否在加载后自动启动具有入口点的插件。
     */
    private boolean autoStart = true;

    /**
     * 获取插件根目录路径。
     *
     * @return 插件根目录路径
     */
    public String getPath() {
        return path;
    }

    /**
     * 设置插件根目录路径。
     *
     * @param path 插件根目录路径
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * 获取插件数据目录路径。
     *
     * @return 插件数据目录路径
     */
    public String getDataPath() {
        return dataPath;
    }

    /**
     * 设置插件数据目录路径。
     *
     * @param dataPath 插件数据目录路径
     */
    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    /**
     * 获取插件核心版本号。
     *
     * @return 插件核心版本号
     */
    public String getCoreVersion() {
        return coreVersion;
    }

    /**
     * 设置插件核心版本号。
     *
     * @param coreVersion 插件核心版本号
     */
    public void setCoreVersion(String coreVersion) {
        this.coreVersion = coreVersion;
    }

    /**
     * 获取运行模式。
     *
     * @return 运行模式
     */
    public RuntimeMode getRuntimeMode() {
        return runtimeMode;
    }

    /**
     * 设置运行模式。
     *
     * @param runtimeMode 运行模式
     */
    public void setRuntimeMode(RuntimeMode runtimeMode) {
        this.runtimeMode = runtimeMode;
    }

    /**
     * 获取是否在加载后自动启动具有入口点的插件。
     *
     * @return 是否自动启动
     */
    public boolean isAutoStart() {
        return autoStart;
    }

    /**
     * 设置是否在加载后自动启动具有入口点的插件。
     *
     * @param autoStart 是否自动启动
     */
    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }
}
