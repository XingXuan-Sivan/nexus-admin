package com.nexusadmin.api;

import com.nexusadmin.api.extension.ExtensionRegistry;

import java.nio.file.Path;

/**
 * 插件运行上下文，封装插件在平台中的元数据、扩展注册中心及类加载等信息。
 *
 * @author NexusAdmin
 * @since 1.0.0
 */
public final class PluginContext {
    /**
     * 插件描述信息。
     */
    private final PluginDescriptor descriptor;
    /**
     * 平台级扩展注册中心，插件可通过它注册或获取扩展点实现。
     */
    private final ExtensionRegistry extensionRegistry;
    /**
     * 插件专属类加载器，用于隔离插件依赖。
     */
    private final ClassLoader classLoader;
    /**
     * 插件所在的物理路径，可以是目录或 JAR 文件。
     */
    private final Path pluginPath;

    /**
     * 构造插件上下文。
     *
     * @param descriptor        插件描述信息
     * @param extensionRegistry 扩展注册中心
     * @param classLoader       插件类加载器
     * @param pluginPath        插件路径
     */
    public PluginContext(PluginDescriptor descriptor,
                         ExtensionRegistry extensionRegistry,
                         ClassLoader classLoader,
                         Path pluginPath) {
        this.descriptor = descriptor;
        this.extensionRegistry = extensionRegistry;
        this.classLoader = classLoader;
        this.pluginPath = pluginPath;
    }

    /**
     * 获取插件描述信息。
     *
     * @return 插件描述
     */
    public PluginDescriptor descriptor() {
        return descriptor;
    }

    /**
     * 获取扩展注册中心。
     *
     * @return 扩展注册中心
     */
    public ExtensionRegistry extensionRegistry() {
        return extensionRegistry;
    }

    /**
     * 获取插件类加载器。
     *
     * @return 类加载器
     */
    public ClassLoader classLoader() {
        return classLoader;
    }

    /**
     * 获取插件路径。
     *
     * @return 插件路径
     */
    public Path pluginPath() {
        return pluginPath;
    }
}
