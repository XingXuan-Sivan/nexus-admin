package com.nexusadmin.api.config;

import com.nexusadmin.api.facade.AdminFacade;
import com.nexusadmin.api.facade.impl.AdminFacadeImpl;
import com.nexusadmin.core.DefaultPluginManager;
import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.config.ConfigManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 管理面板自动配置。
 * <p>
 * 负责装配管理面板门面组件并注册到平台服务注册中心。
 * 认证相关组件已拆分到 {@link AuthExtensionConfig}。
 */
@Configuration
@ComponentScan("com.nexusadmin.api")
public class AdminFacadeAutoConfig {

    /**
     * 管理门面。
     * <p>
     * 创建管理门面并注册到平台服务注册中心，供插件使用。
     *
     * @param pluginManager 插件管理器
     * @param configManager 配置管理器
     * @return AdminFacade 实例
     */
    @Bean
    public AdminFacade adminFacade(PluginManager pluginManager,
                                    ConfigManager configManager) {
        AdminFacade facade = new AdminFacadeImpl(pluginManager, configManager);

        // 将 AdminFacade 注册到平台服务注册中心
        if (pluginManager instanceof DefaultPluginManager dpm) {
            dpm.platformServices().register(AdminFacade.class, facade);
        }

        return facade;
    }
}
