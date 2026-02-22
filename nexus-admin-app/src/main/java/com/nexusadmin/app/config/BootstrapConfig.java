package com.nexusadmin.app.config;

import com.nexusadmin.app.config.properties.PlatformProperties;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.event.SyncEventBus;
import com.nexusadmin.core.extension.DefaultExtensionRegistry;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.plugin.DefaultPluginManager;
import com.nexusadmin.core.plugin.PluginManager;
import com.nexusadmin.core.plugin.RuntimeMode;
import com.nexusadmin.core.plugin.descriptor.PluginDescriptorFinder;
import com.nexusadmin.core.plugin.descriptor.PluginDescriptorReader;
import com.nexusadmin.core.plugin.descriptor.finder.DevDirectoryFinder;
import com.nexusadmin.core.plugin.descriptor.finder.JarFinder;
import com.nexusadmin.core.plugin.descriptor.finder.PackedDirectoryFinder;
import com.nexusadmin.core.plugin.descriptor.parser.JsonPluginDescriptorParser;
import com.nexusadmin.core.plugin.descriptor.reader.JsonDescriptorReader;
import com.nexusadmin.core.plugin.loader.ClasspathPluginLoader;
import com.nexusadmin.core.plugin.loader.JarPluginLoader;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.registry.DefaultPluginRegistry;
import com.nexusadmin.core.plugin.registry.PluginRegistry;
import com.nexusadmin.core.plugin.discovery.DefaultPluginDiscoverer;
import com.nexusadmin.core.plugin.discovery.PluginDiscoverer;
import com.nexusadmin.core.plugin.resolution.DefaultPluginResolver;
import com.nexusadmin.core.plugin.resolution.PluginResolver;
import com.nexusadmin.core.plugin.source.ClasspathPluginSource;
import com.nexusadmin.core.plugin.source.LocalDirectorySource;
import com.nexusadmin.core.plugin.source.PluginSource;
import com.nexusadmin.core.plugin.version.DefaultVersionManager;
import com.nexusadmin.core.plugin.version.VersionManager;
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
     * 扩展注册中心（插件级别）。
     *
     * @return ExtensionRegistry 实例
     */
    @Bean
    public ExtensionRegistry extensionRegistry() {
        return new DefaultExtensionRegistry();
    }

    /**
     * 插件注册中心。
     *
     * @return PluginRegistry 实例
     */
    @Bean
    public PluginRegistry pluginRegistry() {
        return new DefaultPluginRegistry();
    }

    /**
     * 事件总线。
     *
     * @return EventBus 实例
     */
    @Bean
    public EventBus eventBus() {
        return new SyncEventBus();
    }

    /**
     * 版本管理器。
     *
     * @return VersionManager 实例
     */
    @Bean
    public VersionManager versionManager() {
        return new DefaultVersionManager();
    }

    /**
     * 运行模式。
     *
     * @param properties 平台配置属性
     * @return RuntimeMode 实例
     */
    @Bean
    public RuntimeMode runtimeMode(PlatformProperties properties) {
        return properties.getPlugin().getRuntimeMode();
    }

    /**
     * JSON 描述文件解析器。
     *
     * @return JsonPluginDescriptorParser 实例
     */
    @Bean
    public JsonPluginDescriptorParser jsonPluginDescriptorParser() {
        return new JsonPluginDescriptorParser();
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
     * 描述文件读取器。
     *
     * @param parser  JSON 描述文件解析器
     * @param finders 描述文件查找器列表
     * @return PluginDescriptorReader 实例
     */
    @Bean
    public PluginDescriptorReader pluginDescriptorReader(
            JsonPluginDescriptorParser parser,
            List<PluginDescriptorFinder> finders) {
        return new JsonDescriptorReader(parser, finders);
    }

    /**
     * 插件源列表。
     *
     * @param properties 平台配置属性
     * @param reader     描述文件读取器
     * @param parser     JSON 描述文件解析器
     * @return 插件源列表
     */
    @Bean
    public List<PluginSource> pluginSources(
            PlatformProperties properties,
            PluginDescriptorReader reader,
            JsonPluginDescriptorParser parser) {
        return List.of(
                new LocalDirectorySource(
                        Paths.get(properties.getPlugin().getPath()),
                        reader
                ),
                new ClasspathPluginSource(parser)
        );
    }

    /**
     * 插件加载器列表。
     *
     * @return 插件加载器列表
     */
    @Bean
    public List<PluginLoader> pluginLoaders() {
        return List.of(
                new JarPluginLoader(),
                new ClasspathPluginLoader()
        );
    }

    /**
     * 插件发现器。
     *
     * @param sources     插件源列表
     * @param eventBus    事件总线
     * @param properties  平台配置属性
     * @param runtimeMode 运行模式
     * @return PluginDiscoverer 实例
     */
    @Bean
    public PluginDiscoverer pluginDiscoverer(
            List<PluginSource> sources,
            EventBus eventBus,
            PlatformProperties properties,
            RuntimeMode runtimeMode) {
        return new DefaultPluginDiscoverer(sources, eventBus,
                properties.getPlugin().getCoreVersion(), runtimeMode);
    }

    /**
     * 插件解析器。
     *
     * @param eventBus       事件总线
     * @param versionManager 版本管理器
     * @return PluginResolver 实例
     */
    @Bean
    public PluginResolver pluginResolver(EventBus eventBus, VersionManager versionManager) {
        return new DefaultPluginResolver(eventBus, versionManager);
    }

    // ==================== 插件管理器初始化 ====================

    /**
     * 配置插件管理器。
     *
     * @param pluginRegistry    插件注册中心
     * @param extensionRegistry 扩展注册中心
     * @param eventBus          事件总线
     * @param versionManager    版本管理器
     * @param runtimeMode       运行模式
     * @param properties        平台配置属性
     * @param pluginDiscoverer  插件发现器
     * @param pluginResolver    插件解析器
     * @param loaders           插件加载器列表
     * @param finders           描述文件查找器列表
     * @param sources           插件源列表
     * @return PluginManager 实例
     */
    @Bean
    public PluginManager pluginManager(
            PluginRegistry pluginRegistry,
            ExtensionRegistry extensionRegistry,
            EventBus eventBus,
            VersionManager versionManager,
            RuntimeMode runtimeMode,
            PlatformProperties properties,
            PluginDiscoverer pluginDiscoverer,
            PluginResolver pluginResolver,
            List<PluginLoader> loaders,
            List<PluginDescriptorFinder> finders,
            List<PluginSource> sources) {

        return new DefaultPluginManager(
                pluginRegistry,
                extensionRegistry,
                eventBus,
                versionManager,
                runtimeMode,
                properties.getPlugin().getCoreVersion(),
                pluginDiscoverer,
                pluginResolver,
                loaders,
                finders,
                sources,
                properties.getPlugin().isAutoStart()
        );
    }
}