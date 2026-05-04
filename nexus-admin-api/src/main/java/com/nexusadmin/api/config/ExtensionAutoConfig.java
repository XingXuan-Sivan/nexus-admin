package com.nexusadmin.api.config;

import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.facade.ExtensionFacade;
import com.nexusadmin.core.runtime.ExtensionRuntime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 扩展注册中心 Spring 桥接配置。
 * <p>
 * 将 ExtensionRuntime 装配的核心组件桥接到 Spring 容器，不参与组件创建与依赖注入逻辑——
 * 组件创建由 {@link ExtensionRuntime} 全权负责。
 * <p>
 * 所有 Bean 均带 {@link ConditionalOnMissingBean} 保护，应用层可通过声明同类型 Bean
 * 覆盖任意组件。也可直接声明 {@link ExtensionRuntime} Bean 一次性替换全部组件。
 * <p>
 * <strong>覆盖优先级：</strong>app @Bean &gt; api @ConditionalOnMissingBean &gt; ExtensionRuntime 默认值
 */
@Configuration
public class ExtensionAutoConfig {

    /**
     * 扩展注册中心运行时。
     * <p>声明此类型的 Bean 可一次性替换扩展注册中心全部组件。</p>
     *
     * @param eventBus 事件总线（与其他运行时共享同一总线）
     * @return ExtensionRuntime 实例
     */
    @Bean
    @ConditionalOnMissingBean(ExtensionRuntime.class)
    public ExtensionRuntime extensionRuntime(EventBus eventBus) {
        return ExtensionRuntime.builder().eventBus(eventBus).build();
    }

    /**
     * 扩展注册中心。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt 扩展注册中心运行时
     * @return ExtensionRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean(ExtensionRegistry.class)
    public ExtensionRegistry extensionRegistry(ExtensionRuntime rt) {
        return rt.extensionRegistry();
    }

    /**
     * 扩展注册中心门面。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt 扩展注册中心运行时
     * @return ExtensionFacade 实例
     */
    @Bean
    @ConditionalOnMissingBean(ExtensionFacade.class)
    public ExtensionFacade extensionFacade(ExtensionRuntime rt) {
        return rt.facade();
    }
}
