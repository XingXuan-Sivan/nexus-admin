package com.nexusadmin.api.config;

import com.nexusadmin.core.config.ConfigManager;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.facade.ConfigFacade;
import com.nexusadmin.core.runtime.ConfigRuntime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 配置中心 Spring 桥接配置。
 * <p>
 * 将 ConfigRuntime 装配的核心组件桥接到 Spring 容器，不参与组件创建与依赖注入逻辑——
 * 组件创建由 {@link ConfigRuntime} 全权负责。
 * <p>
 * 所有 Bean 均带 {@link ConditionalOnMissingBean} 保护，应用层可通过声明同类型 Bean
 * 覆盖任意组件。也可直接声明 {@link ConfigRuntime} Bean 一次性替换全部组件。
 * <p>
 * <strong>覆盖优先级：</strong>app @Bean &gt; api @ConditionalOnMissingBean &gt; ConfigRuntime 默认值
 */
@Configuration
public class ConfigAutoConfig {

    /**
     * 配置中心运行时。
     * <p>声明此类型的 Bean 可一次性替换配置中心全部组件。</p>
     *
     * @param eventBus 事件总线（与其他运行时共享同一总线）
     * @param dataPath 插件数据目录路径
     * @return ConfigRuntime 实例
     */
    @Bean
    @ConditionalOnMissingBean(ConfigRuntime.class)
    public ConfigRuntime configRuntime(EventBus eventBus,
                                        @Value("${plugin.data-path:plugins-data}") String dataPath) {
        Path configDir = Paths.get(dataPath).resolve("config");
        return ConfigRuntime.builder()
                .eventBus(eventBus)
                .configDir(configDir)
                .build();
    }

    /**
     * 配置管理器。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt 配置中心运行时
     * @return ConfigManager 实例
     */
    @Bean
    @ConditionalOnMissingBean(ConfigManager.class)
    public ConfigManager configManager(ConfigRuntime rt) {
        return rt.configManager();
    }

    /**
     * 配置管理门面。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt 配置中心运行时
     * @return ConfigFacade 实例
     */
    @Bean
    @ConditionalOnMissingBean(ConfigFacade.class)
    public ConfigFacade configFacade(ConfigRuntime rt) {
        return rt.facade();
    }
}
