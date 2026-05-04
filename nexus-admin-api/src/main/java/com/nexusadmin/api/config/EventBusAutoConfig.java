package com.nexusadmin.api.config;

import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.facade.EventBusFacade;
import com.nexusadmin.core.runtime.EventBusRuntime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 事件总线 Spring 桥接配置。
 * <p>
 * 将 EventBusRuntime 装配的核心组件桥接到 Spring 容器，不参与组件创建与依赖注入逻辑——
 * 组件创建由 {@link EventBusRuntime} 全权负责。
 * <p>
 * 所有 Bean 均带 {@link ConditionalOnMissingBean} 保护，应用层可通过声明同类型 Bean
 * 覆盖任意组件。也可直接声明 {@link EventBusRuntime} Bean 一次性替换全部组件。
 * <p>
 * <strong>覆盖优先级：</strong>app @Bean &gt; api @ConditionalOnMissingBean &gt; EventBusRuntime 默认值
 */
@Configuration
public class EventBusAutoConfig {

    /**
     * 事件总线运行时。
     * <p>声明此类型的 Bean 可一次性替换事件总线全部组件。</p>
     *
     * @return EventBusRuntime 实例
     */
    @Bean
    @ConditionalOnMissingBean(EventBusRuntime.class)
    public EventBusRuntime eventBusRuntime() {
        return EventBusRuntime.defaults();
    }

    /**
     * 事件总线。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt 事件总线运行时
     * @return EventBus 实例
     */
    @Bean
    @ConditionalOnMissingBean(EventBus.class)
    public EventBus eventBus(EventBusRuntime rt) {
        return rt.eventBus();
    }

    /**
     * 事件总线门面。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt 事件总线运行时
     * @return EventBusFacade 实例
     */
    @Bean
    @ConditionalOnMissingBean(EventBusFacade.class)
    public EventBusFacade eventBusFacade(EventBusRuntime rt) {
        return rt.facade();
    }
}
