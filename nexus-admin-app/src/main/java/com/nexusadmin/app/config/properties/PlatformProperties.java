package com.nexusadmin.app.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 平台级配置属性，统一管理所有平台级自定义配置项。
 * <p>
 * 该配置类位于 app 启动装配层，聚合了平台基本信息配置与插件系统配置。
 * API 层专用配置（如引导认证凭据）由 {@code com.nexusadmin.api.config.properties} 包独立管理。
 */
@Component
@ConfigurationProperties(prefix = "platform")
public class PlatformProperties {

    /**
     * 平台基本信息配置。
     */
    private PlatformInfoProperties info = new PlatformInfoProperties();

    /**
     * 插件系统配置。
     */
    private PluginProperties plugin = new PluginProperties();

    /**
     * 获取平台基本信息配置。
     *
     * @return 平台基本信息
     */
    public PlatformInfoProperties getInfo() {
        return info;
    }

    /**
     * 设置平台基本信息配置。
     *
     * @param info 平台基本信息
     */
    public void setInfo(PlatformInfoProperties info) {
        this.info = info;
    }

    /**
     * 获取插件系统配置。
     *
     * @return 插件配置
     */
    public PluginProperties getPlugin() {
        return plugin;
    }

    /**
     * 设置插件系统配置。
     *
     * @param plugin 插件配置
     */
    public void setPlugin(PluginProperties plugin) {
        this.plugin = plugin;
    }
}
