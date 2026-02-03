package com.nexusadmin.app.config;

import com.nexusadmin.core.service.CoreRuntime;
import com.nexusadmin.core.spi.SpiRegistry;
import com.nexusadmin.plugin.loader.PluginManager;
import com.nexusadmin.plugin.loader.impl.ClasspathPluginLoader;
import com.nexusadmin.plugin.loader.impl.JarPluginLoader;
import com.nexusadmin.plugin.registry.DefaultSpiRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 插件与核心运行时的启动配置，负责在 Spring 容器中装配 SpiRegistry、PluginManager 和 CoreRuntime。
 */
@Configuration
public class BootstrapConfig {
    private static final Logger log = LoggerFactory.getLogger(BootstrapConfig.class);
    
    /**
     * 创建全局 SPI 注册中心实现，供核心运行时和插件使用。
     *
     * @return 默认的 SPI 注册中心实现
     */
    @Bean
    public SpiRegistry spiRegistry() {
        log.info("初始化 SPI 注册中心...");
        return new DefaultSpiRegistry();
    }

    /**
     * 创建插件管理器，用于加载和管理插件生命周期。
     *
     * @param registry SPI 注册中心
     * @return 插件管理器实例
     */
    @Bean
    public PluginManager pluginManager(SpiRegistry registry) {
        log.info("初始化插件管理器...");
        PluginManager manager = new PluginManager(registry);

        // 手动注册核心加载器
        manager.registerLoader(new ClasspathPluginLoader());
        manager.registerLoader(new JarPluginLoader());

        return manager;
    }

    /**
     * 创建核心运行时门面，供上层业务统一调用平台能力。
     *
     * @param registry SPI 注册中心
     * @return 核心运行时实例
     */
    @Bean
    public CoreRuntime coreRuntime(SpiRegistry registry) {
        log.info("初始化核心运行时...");
        CoreRuntime runtime = new CoreRuntime(registry);
        return runtime;
    }
}
