package com.nexusadmin.app.config.properties;

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

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public Plugin getPlugin() {
        return plugin;
    }

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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDescription() {
            return description;
        }

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
         * 是否在加载后自动启动具有入口点的插件。
         */
        private boolean autoStart = true;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public boolean isAutoStart() {
            return autoStart;
        }

        public void setAutoStart(boolean autoStart) {
            this.autoStart = autoStart;
        }
    }
}
