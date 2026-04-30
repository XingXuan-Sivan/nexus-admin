package com.nexusadmin.app.config;

import com.nexusadmin.app.config.properties.PluginProperties;
import com.nexusadmin.core.DefaultPluginManager;
import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.plugin.PluginRegistry;
import com.nexusadmin.core.plugin.RuntimeMode;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorFinder;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorParser;
import com.nexusadmin.core.plugin.discovery.PluginSource;
import com.nexusadmin.core.plugin.discovery.finder.DevDirectoryFinder;
import com.nexusadmin.core.plugin.discovery.finder.JarFinder;
import com.nexusadmin.core.plugin.discovery.finder.PackedDirectoryFinder;
import com.nexusadmin.core.plugin.discovery.parser.JsonPluginDescriptorParser;
import com.nexusadmin.core.plugin.discovery.source.ClasspathPluginSource;
import com.nexusadmin.core.plugin.discovery.source.LocalDirectorySource;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.resolve.DependenceManager;
import com.nexusadmin.core.plugin.resolve.VersionManager;
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
     *
     * @param pluginRegistry    插件注册中心
     * @param extensionRegistry 扩展注册中心
     * @param eventBus          事件总线
     * @param runtimeMode       运行模式
     * @param properties        插件配置属性
     * @param sources           插件源列表
     * @param finders           描述文件查找器列表
     * @param parsers           描述文件解析器列表
     * @param versionManager    版本管理器
     * @param dependenceManager 依赖管理器
     * @param pluginLoader      插件加载器
     * @return PluginManager 实例
     */
    @Bean
    public PluginManager pluginManager(
            PluginRegistry pluginRegistry,
            ExtensionRegistry extensionRegistry,
            EventBus eventBus,
            RuntimeMode runtimeMode,
            PluginProperties properties,
            List<PluginSource> sources,
            List<PluginDescriptorFinder> finders,
            List<PluginDescriptorParser> parsers,
            VersionManager versionManager,
            DependenceManager dependenceManager,
            PluginLoader pluginLoader) {

        return new DefaultPluginManager(
                pluginRegistry,
                extensionRegistry,
                eventBus,
                runtimeMode,
                Paths.get(properties.getDataPath()),
                sources,
                finders,
                parsers,
                versionManager,
                dependenceManager,
                pluginLoader
        );
    }

    // ==================== 配置管理器初始化 ====================

    /**
     * 配置管理器。
     * <p>
     * 从 DefaultPluginManager 中获取配置管理器实例。
     *
     * @param pluginManager 插件管理器
     * @return ConfigManager 实例，如果不可用则返回 null
     */
    @Bean
    public com.nexusadmin.core.config.ConfigManager configManager(PluginManager pluginManager) {
        if (pluginManager instanceof DefaultPluginManager dpm) {
            return dpm.configManager();
        }
        return null;
    }
}