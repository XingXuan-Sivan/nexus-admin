package com.nexusadmin.app.config;

import com.nexusadmin.api.SpiRegistry;
import com.nexusadmin.app.config.properties.PlatformProperties;
import com.nexusadmin.core.plugin.DefaultSpiRegistry;
import com.nexusadmin.core.plugin.PluginManager;
import com.nexusadmin.core.plugin.descriptor.PluginDescriptorPathResolver;
import com.nexusadmin.core.plugin.descriptor.PluginDescriptorReader;
import com.nexusadmin.core.plugin.descriptor.parser.JsonPluginDescriptorParser;
import com.nexusadmin.core.plugin.descriptor.reader.JsonDescriptorReader;
import com.nexusadmin.core.plugin.descriptor.resolver.DevDirectoryResolver;
import com.nexusadmin.core.plugin.descriptor.resolver.JarResolver;
import com.nexusadmin.core.plugin.descriptor.resolver.PackedDirectoryResolver;
import com.nexusadmin.core.plugin.event.LoggingLifecycleListener;
import com.nexusadmin.core.plugin.event.PluginLifecycleListener;
import com.nexusadmin.core.plugin.loader.ClasspathPluginLoader;
import com.nexusadmin.core.plugin.loader.JarPluginLoader;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.source.ClasspathPluginSource;
import com.nexusadmin.core.plugin.source.LocalDirectorySource;
import com.nexusadmin.core.plugin.source.PluginSource;
import com.nexusadmin.core.registry.ComponentRegistry;
import com.nexusadmin.core.registry.Composable;
import com.nexusadmin.core.registry.DefaultComponentRegistry;
import com.nexusadmin.core.registry.GenericComposite;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * 平台核心的启动配置，负责在 Spring 容器中装配核心组件。
 *
 * <p><strong>设计原则：</strong></p>
 * <ul>
 *   <li>所有核心组件通过 ComponentRegistry 统一管理</li>
 *   <li>PluginManager 只负责生命周期，不负责组件注册</li>
 *   <li>按核心组件大类组织配置，避免配置类膨胀</li>
 *   <li>支持选择性启用组件，方便未来扩展</li>
 * </ul>
 */
@Configuration
public class BootstrapConfig {

    // ==================== 注册中心初始化 ====================

    /**
     * SPI 注册中心（插件级别）。
     *
     * @return SpiRegistry 实例
     */
    @Bean
    public SpiRegistry spiRegistry() {
        return new DefaultSpiRegistry();
    }

    /**
     * 系统核心组件注册中心（平台级别）。
     * 在此方法中完成所有核心组件的注册配置。
     *
     * @param properties 平台配置属性
     * @return 配置完成的 ComponentRegistry 实例
     */
    @Bean
    public ComponentRegistry<Composable> componentRegistry(PlatformProperties properties) {
        ComponentRegistry<Composable> registry = new DefaultComponentRegistry();

        // 配置各类核心组件
        configureDescriptorComponents(registry, properties);
        configurePluginSourceComponents(registry, properties);
        configurePluginLoaderComponents(registry);
        configureLifecycleListenerComponents(registry);

        return registry;
    }

    // ==================== 插件管理器初始化 ====================

    /**
     * 配置插件管理器。
     * 从 ComponentRegistry 获取所有必要组件并组装。
     *
     * @param spiRegistry SPI 注册中心
     * @param registry    组件注册中心
     * @return PluginManager 实例
     */
    @Bean
    public PluginManager pluginManager(
            SpiRegistry spiRegistry,
            ComponentRegistry<Composable> registry) {

        List<PluginSource> sources = registry.getAll(PluginSource.class);
        List<PluginLoader> loaders = registry.getAll(PluginLoader.class);
        List<PluginLifecycleListener> listeners = registry.getAll(PluginLifecycleListener.class);

        return new PluginManager(spiRegistry, sources, loaders, listeners);
    }

    // ==================== 各系统核心组件配置 ====================

    /**
     * 配置描述符解析相关组件。
     * 包括：路径解析器、描述文件读取器。
     *
     * @param registry   组件注册中心
     * @param properties 平台配置属性
     */
    private void configureDescriptorComponents(
            ComponentRegistry<Composable> registry,
            PlatformProperties properties) {

        // 1. 配置路径解析器（使用 GenericComposite 组合多个解析器）
        GenericComposite<PluginDescriptorPathResolver> pathResolverComposite =
                new GenericComposite<>(PluginDescriptorPathResolver::priority);

        pathResolverComposite.addMember(new PackedDirectoryResolver());
        pathResolverComposite.addMember(new DevDirectoryResolver());
        pathResolverComposite.addMember(new JarResolver());

        // 注意：GenericComposite 本身也是 Composable，可以被注册
        // 但不能直接注册为 PluginDescriptorPathResolver 类型
        // 因为它不实现该接口，只是组合了实现该接口的组件

        // 2. 配置描述文件读取器（使用 GenericComposite 作为路径解析器）
        // 创建一个实现了 PluginDescriptorPathResolver 的包装类
        PluginDescriptorPathResolver pathResolverWrapper = new PluginDescriptorPathResolver() {
            @Override
            public Optional<Path> resolve(Path pluginPath) {
                return pathResolverComposite.executeFirst(r -> r.resolve(pluginPath));
            }

            @Override
            public int priority() {
                return pathResolverComposite.getMembers().stream()
                        .mapToInt(PluginDescriptorPathResolver::priority)
                        .max()
                        .orElse(0);
            }
        };

        JsonPluginDescriptorParser parser = new JsonPluginDescriptorParser();
        JsonDescriptorReader reader = new JsonDescriptorReader(parser, pathResolverWrapper);

        registry.register(PluginDescriptorReader.class, reader, 0);
    }

    /**
     * 配置插件源组件。
     *
     * @param registry   组件注册中心
     * @param properties 平台配置属性
     */
    private void configurePluginSourceComponents(
            ComponentRegistry<Composable> registry,
            PlatformProperties properties) {

        // 获取描述文件读取器
        PluginDescriptorReader reader = registry.get(PluginDescriptorReader.class)
                .orElseThrow(() -> new IllegalStateException("描述文件读取器未配置"));

        // 本地目录源
        String pluginPath = properties.getPlugin().getPath();
        LocalDirectorySource localSource = new LocalDirectorySource(
                Paths.get(pluginPath), reader);
        registry.register(PluginSource.class, localSource, 100);

        // 类路径源
        ClasspathPluginSource classpathSource =
                new ClasspathPluginSource(new JsonPluginDescriptorParser());
        registry.register(PluginSource.class, classpathSource, 50);
    }

    /**
     * 配置插件加载器组件。
     *
     * @param registry 组件注册中心
     */
    private void configurePluginLoaderComponents(
            ComponentRegistry<Composable> registry) {

        // JAR 加载器（高优先级）
        registry.register(PluginLoader.class, new JarPluginLoader(), 100);

        // 类路径加载器
        registry.register(PluginLoader.class, new ClasspathPluginLoader(), 50);
    }

    /**
     * 配置生命周期监听组件。
     *
     * @param registry 组件注册中心
     */
    private void configureLifecycleListenerComponents(
            ComponentRegistry<Composable> registry) {

        registry.register(PluginLifecycleListener.class,
                new LoggingLifecycleListener(), 0);
    }
}