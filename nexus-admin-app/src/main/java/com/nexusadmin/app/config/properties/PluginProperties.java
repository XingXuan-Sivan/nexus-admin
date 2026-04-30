package com.nexusadmin.app.config.properties;

import com.nexusadmin.core.plugin.RuntimeMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 插件系统配置。
 * <p>
 * 管理插件运行时所需的基础设施参数，仅包含启动后不可变的配置项。
 * 运行时可变的配置（如 autoStart、coreVersion）已迁移至配置中心（ConfigManager）。
 */
@Component
@ConfigurationProperties(prefix = "plugin")
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
     * 运行模式，可选值：DEV、PROD。
     */
    private RuntimeMode runtimeMode = RuntimeMode.DEV;

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
}
