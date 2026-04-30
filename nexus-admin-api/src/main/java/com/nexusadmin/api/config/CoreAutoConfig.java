package com.nexusadmin.api.config;

import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.event.SyncEventBus;
import com.nexusadmin.core.extension.DefaultExtensionRegistry;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.plugin.DefaultPluginRegistry;
import com.nexusadmin.core.plugin.PluginRegistry;
import com.nexusadmin.core.plugin.loader.DefaultPluginLoader;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.resolve.DefaultDependenceManager;
import com.nexusadmin.core.plugin.resolve.DependenceManager;
import com.nexusadmin.core.plugin.resolve.VersionManager;
import com.nexusadmin.core.plugin.resolve.version.DefaultVersionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core 模块默认装配配置。
 * <p>
 * 负责在 Spring 容器中装配 core 模块的默认实现，所有 Bean 均带 {@link ConditionalOnMissingBean}
 * 保护，允许应用层或插件通过声明同类型 Bean 进行覆盖。
 * <p>
 * <strong>装配顺序说明：</strong>
 * <ul>
 *   <li>EventBus 优先于 ExtensionRegistry，因为后者依赖前者发布扩展变更事件</li>
 *   <li>VersionManager 优先于 DependenceManager，因为后者依赖前者进行版本兼容性校验</li>
 * </ul>
 */
@Configuration
public class CoreAutoConfig {

    /**
     * 默认事件总线（同步实现）。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @return EventBus 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public EventBus eventBus() {
        return new SyncEventBus();
    }

    /**
     * 默认扩展注册中心。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param eventBus 事件总线，用于发布扩展变更事件
     * @return ExtensionRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ExtensionRegistry extensionRegistry(EventBus eventBus) {
        return new DefaultExtensionRegistry(eventBus);
    }

    /**
     * 默认插件注册中心。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @return PluginRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginRegistry pluginRegistry() {
        return new DefaultPluginRegistry();
    }

    /**
     * 默认版本管理器。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @return VersionManager 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public VersionManager versionManager() {
        return new DefaultVersionManager();
    }

    /**
     * 默认依赖管理器。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param versionManager 版本管理器
     * @return DependenceManager 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public DependenceManager dependenceManager(VersionManager versionManager) {
        return new DefaultDependenceManager(versionManager);
    }

    /**
     * 默认插件加载器。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @return PluginLoader 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginLoader pluginLoader() {
        return new DefaultPluginLoader();
    }
}
