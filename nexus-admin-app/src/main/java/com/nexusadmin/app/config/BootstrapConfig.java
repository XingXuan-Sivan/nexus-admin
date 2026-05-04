package com.nexusadmin.app.config;

import com.nexusadmin.app.config.properties.PluginProperties;
import com.nexusadmin.core.CoreConfig;
import com.nexusadmin.core.DefaultPluginManager;
import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.context.PlatformServices;
import com.nexusadmin.core.facade.ConfigFacade;
import com.nexusadmin.core.facade.EventBusFacade;
import com.nexusadmin.core.facade.ExtensionFacade;
import com.nexusadmin.core.facade.PluginFacade;
import com.nexusadmin.core.plugin.RuntimeMode;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorFinder;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorParser;
import com.nexusadmin.core.plugin.discovery.PluginSource;
import com.nexusadmin.core.plugin.discovery.impl.DevDirectoryFinder;
import com.nexusadmin.core.plugin.discovery.impl.JarFinder;
import com.nexusadmin.core.plugin.discovery.impl.PackedDirectoryFinder;
import com.nexusadmin.core.plugin.discovery.impl.JsonPluginDescriptorParser;
import com.nexusadmin.core.plugin.discovery.impl.ClasspathPluginSource;
import com.nexusadmin.core.plugin.discovery.impl.LocalDirectorySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;
import java.util.List;

/**
 * 平台核心的启动配置，负责在 Spring 容器中装配核心组件。
 *
 * <p><strong>设计原则：</strong></p>
 * <ul>
 *   <li>所有协作者通过构造函数注入，保持可替换性</li>
 *   <li>PluginManager 只负责生命周期编排</li>
 *   <li>按功能模块组织配置，避免配置类膨胀</li>
 * </ul>
 */
@Configuration
public class BootstrapConfig {

    // ==================== 核心组件初始化 ====================

    /**
     * 运行模式。
     *
     * @param properties 插件配置属性
     * @return RuntimeMode 实例
     */
    @Bean
    public RuntimeMode runtimeMode(PluginProperties properties) {
        return properties.getRuntimeMode();
    }

    /**
     * 核心运行时配置。
     *
     * @param properties 插件配置属性
     * @return CoreConfig 实例
     */
    @Bean
    public CoreConfig coreConfig(PluginProperties properties) {
        return CoreConfig.of(
                properties.getRuntimeMode(),
                Paths.get(properties.getDataPath())
        );
    }

    /**
     * 描述文件查找器列表。
     *
     * @return 描述文件查找器列表
     */
    @Bean
    public List<PluginDescriptorFinder> descriptorFinders() {
        return List.of(
                new PackedDirectoryFinder(),
                new DevDirectoryFinder(),
                new JarFinder()
        );
    }

    /**
     * 描述文件解析器列表。
     *
     * @param finders 描述文件查找器列表
     * @return 描述文件解析器列表
     */
    @Bean
    public List<PluginDescriptorParser> descriptorParsers(List<PluginDescriptorFinder> finders) {
        return List.of(new JsonPluginDescriptorParser(finders));
    }

    /**
     * 插件源列表。
     *
     * @param properties 插件配置属性
     * @param parsers    描述文件解析器列表
     * @return 插件源列表
     */
    @Bean
    public List<PluginSource> pluginSources(
            PluginProperties properties,
            List<PluginDescriptorParser> parsers) {
        return List.of(
                new LocalDirectorySource(
                        Paths.get(properties.getPath()),
                        parsers
                ),
                new ClasspathPluginSource()
        );
    }

    // ==================== 插件管理器初始化 ====================

    /**
     * 配置插件管理器。
     * <p>通过 4 个门面聚合底层组件，DefaultPluginManager 仅负责生命周期编排。</p>
     *
     * @param coreConfig       核心运行时配置
     * @param pluginFacade     插件组件门面
     * @param extensionFacade  扩展注册中心门面
     * @param configFacade     配置管理门面
     * @param eventBusFacade   事件总线门面
     * @return PluginManager 实例
     */
    @Bean
    public PluginManager pluginManager(CoreConfig coreConfig,
                                       PluginFacade pluginFacade,
                                       ExtensionFacade extensionFacade,
                                       ConfigFacade configFacade,
                                       EventBusFacade eventBusFacade) {
        return new DefaultPluginManager(
                coreConfig,
                pluginFacade,
                extensionFacade,
                configFacade,
                eventBusFacade
        );
    }

    // ==================== 平台服务注册中心 ====================

    /**
     * 平台服务注册中心。
     * <p>从 DefaultPluginManager 中提取，供外部组件注册服务。</p>
     *
     * @param pluginManager 插件管理器
     * @return PlatformServices 实例
     */
    @Bean
    public PlatformServices platformServices(PluginManager pluginManager) {
        if (pluginManager instanceof DefaultPluginManager dpm) {
            return dpm.platformServices();
        }
        return new PlatformServices();
    }
}
