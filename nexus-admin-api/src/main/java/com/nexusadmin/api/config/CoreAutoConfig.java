package com.nexusadmin.api.config;

import com.nexusadmin.core.CoreRuntime;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.plugin.PluginRegistry;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.resolve.DependenceManager;
import com.nexusadmin.core.plugin.resolve.VersionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core 模块 Spring 桥接配置。
 * <p>
 * 将 CoreRuntime 装配的核心组件桥接到 Spring 容器，不参与组件创建与依赖注入逻辑——
 * 组件创建由 {@link CoreRuntime} 全权负责。
 * <p>
 * 所有 Bean 均带 {@link ConditionalOnMissingBean} 保护，应用层可通过声明同类型 Bean
 * 覆盖任意组件。也可直接声明 {@link CoreRuntime} Bean 一次性替换全部组件。
 * <p>
 * <strong>覆盖优先级：</strong>app @Bean &gt; api @ConditionalOnMissingBean &gt; CoreRuntime 默认值
 */
@Configuration
public class CoreAutoConfig {

    /**
     * Core 运行时聚合根。
     * <p>声明此类型的 Bean 可一次性替换所有核心组件。</p>
     *
     * @return CoreRuntime 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public CoreRuntime coreRuntime() {
        return CoreRuntime.defaults();
    }

    /**
     * 事件总线。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt Core 运行时
     * @return EventBus 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public EventBus eventBus(CoreRuntime rt) {
        return rt.eventBus();
    }

    /**
     * 扩展注册中心。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt Core 运行时
     * @return ExtensionRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ExtensionRegistry extensionRegistry(CoreRuntime rt) {
        return rt.extensionRegistry();
    }

    /**
     * 插件注册中心。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt Core 运行时
     * @return PluginRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginRegistry pluginRegistry(CoreRuntime rt) {
        return rt.pluginRegistry();
    }

    /**
     * 版本管理器。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt Core 运行时
     * @return VersionManager 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public VersionManager versionManager(CoreRuntime rt) {
        return rt.versionManager();
    }

    /**
     * 依赖管理器。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt Core 运行时
     * @return DependenceManager 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public DependenceManager dependenceManager(CoreRuntime rt) {
        return rt.dependenceManager();
    }

    /**
     * 插件加载器。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt Core 运行时
     * @return PluginLoader 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginLoader pluginLoader(CoreRuntime rt) {
        return rt.pluginLoader();
    }
}
