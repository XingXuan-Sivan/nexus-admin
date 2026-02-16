package com.nexusadmin.api;

import java.nio.file.Path;

/**
 * 插件运行上下文，封装插件在平台中的元数据、SPI 注册中心及类加载等信息。
 */
public final class PluginContext {
    /**
     * 插件描述信息。
     */
    private final PluginDescriptor descriptor;
    /**
     * 平台级 SPI 注册中心，插件可通过它注册或获取 SPI 实现。
     */
    private final SpiRegistry spiRegistry;
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
     * @param descriptor  插件描述信息
     * @param spiRegistry SPI 注册中心
     * @param classLoader 插件类加载器
     * @param pluginPath  插件路径
     */
    public PluginContext(PluginDescriptor descriptor,
                         SpiRegistry spiRegistry,
                         ClassLoader classLoader,
                         Path pluginPath) {
        this.descriptor = descriptor;
        this.spiRegistry = spiRegistry;
        this.classLoader = classLoader;
        this.pluginPath = pluginPath;
    }

    public PluginDescriptor descriptor() {
        return descriptor;
    }

    public SpiRegistry spiRegistry() {
        return spiRegistry;
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    public Path pluginPath() {
        return pluginPath;
    }
}
