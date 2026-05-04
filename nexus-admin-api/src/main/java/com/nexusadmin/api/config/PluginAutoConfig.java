package com.nexusadmin.api.config;

import com.nexusadmin.core.facade.PluginFacade;
import com.nexusadmin.core.plugin.PluginRegistry;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorFinder;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorParser;
import com.nexusadmin.core.plugin.discovery.PluginSource;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.resolve.DependenceManager;
import com.nexusadmin.core.plugin.resolve.VersionManager;
import com.nexusadmin.core.runtime.PluginRuntime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 插件子系统 Spring 桥接配置。
 * <p>
 * 将 PluginRuntime 装配的核心组件桥接到 Spring 容器，不参与组件创建与依赖注入逻辑——
 * 组件创建由 {@link PluginRuntime} 全权负责。
 * <p>
 * 所有 Bean 均带 {@link ConditionalOnMissingBean} 保护，应用层可通过声明同类型 Bean
 * 覆盖任意组件。也可直接声明 {@link PluginRuntime} Bean 一次性替换全部组件。
 * <p>
 * <strong>覆盖优先级：</strong>app @Bean &gt; api @ConditionalOnMissingBean &gt; PluginRuntime 默认值
 */
@Configuration
public class PluginAutoConfig {

    /**
     * 插件运行时。
     * <p>声明此类型的 Bean 可一次性替换插件子系统全部组件。</p>
     * <p>注入容器中的插件源、查找器与解析器列表，由应用层 BootstrapConfig 提供。</p>
     *
     * @param sources 插件源列表
     * @param finders 描述文件查找器列表
     * @param parsers 描述文件解析器列表
     * @return PluginRuntime 实例
     */
    @Bean
    @ConditionalOnMissingBean(PluginRuntime.class)
    public PluginRuntime pluginRuntime(List<PluginSource> sources,
                                       List<PluginDescriptorFinder> finders,
                                       List<PluginDescriptorParser> parsers) {
        return PluginRuntime.builder()
                .sources(sources)
                .finders(finders)
                .parsers(parsers)
                .build();
    }

    /**
     * 插件注册中心。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt 插件运行时
     * @return PluginRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean(PluginRegistry.class)
    public PluginRegistry pluginRegistry(PluginRuntime rt) {
        return rt.pluginRegistry();
    }

    /**
     * 插件组件门面。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt 插件运行时
     * @return PluginFacade 实例
     */
    @Bean
    @ConditionalOnMissingBean(PluginFacade.class)
    public PluginFacade pluginFacade(PluginRuntime rt) {
        return rt.facade();
    }

    /**
     * 版本管理器。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt 插件运行时
     * @return VersionManager 实例
     */
    @Bean
    @ConditionalOnMissingBean(VersionManager.class)
    public VersionManager versionManager(PluginRuntime rt) {
        return rt.versionManager();
    }

    /**
     * 依赖管理器。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt 插件运行时
     * @return DependenceManager 实例
     */
    @Bean
    @ConditionalOnMissingBean(DependenceManager.class)
    public DependenceManager dependenceManager(PluginRuntime rt) {
        return rt.dependenceManager();
    }

    /**
     * 插件加载器。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param rt 插件运行时
     * @return PluginLoader 实例
     */
    @Bean
    @ConditionalOnMissingBean(PluginLoader.class)
    public PluginLoader pluginLoader(PluginRuntime rt) {
        return rt.pluginLoader();
    }
}
