package com.nexusadmin.app.config.properties;

import com.nexusadmin.core.plugin.RuntimeMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 平台级配置属性，统一管理所有自定义配置项。
 */
@Component
@ConfigurationProperties(prefix = "platform")
public class PlatformProperties {
    /**
     * 平台基本信息配置。
     */
    private Info info = new Info();
    /**
     * 插件系统配置。
     */
    private Plugin plugin = new Plugin();

    /**
     * 获取平台基本信息配置。
     *
     * @return 平台基本信息
     */
    public Info getInfo() {
        return info;
    }

    /**
     * 设置平台基本信息配置。
     *
     * @param info 平台基本信息
     */
    public void setInfo(Info info) {
        this.info = info;
    }

    /**
     * 获取插件系统配置。
     *
     * @return 插件配置
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * 设置插件系统配置。
     *
     * @param plugin 插件配置
     */
    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 平台基本信息。
     */
    public static class Info {
        /**
         * 平台名称。
         */
        private String name;
        /**
         * 平台版本号。
         */
        private String version;
        /**
         * 平台描述信息。
         */
        private String description;

        /**
         * 获取平台名称。
         *
         * @return 平台名称
         */
        public String getName() {
            return name;
        }

        /**
         * 设置平台名称。
         *
         * @param name 平台名称
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * 获取平台版本号。
         *
         * @return 平台版本号
         */
        public String getVersion() {
            return version;
        }

        /**
         * 设置平台版本号。
         *
         * @param version 平台版本号
         */
        public void setVersion(String version) {
            this.version = version;
        }

        /**
         * 获取平台描述信息。
         *
         * @return 平台描述信息
         */
        public String getDescription() {
            return description;
        }

        /**
         * 设置平台描述信息。
         *
         * @param description 平台描述信息
         */
        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * 插件系统配置。
     */
    public static class Plugin {
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
}
