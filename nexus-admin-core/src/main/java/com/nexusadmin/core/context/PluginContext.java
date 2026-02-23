package com.nexusadmin.core.context;

import com.nexusadmin.core.event.EventPublisher;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.plugin.RuntimeMode;
import com.nexusadmin.core.plugin.descriptor.PluginDescriptor;

import java.nio.file.Path;

/**
 * 插件运行上下文，封装插件在平台中的元数据、扩展注册中心及类加载等信息。
 * <p>插件通过此上下文与平台交互，禁止直接获取 PluginManager。</p>
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
     * 事件发布者，插件可通过它发布事件。
     */
    private final EventPublisher eventPublisher;

    /**
     * 当前运行模式（开发/生产）。
     */
    private final RuntimeMode runtimeMode;

    /**
     * 平台核心版本号。
     */
    private final String coreVersion;

    /**
     * 构造插件上下文。
     *
     * @param descriptor        插件描述信息
     * @param extensionRegistry 扩展注册中心
     * @param classLoader       插件类加载器
     * @param pluginPath        插件路径
     * @param eventPublisher    事件发布者
     * @param runtimeMode       运行模式
     * @param coreVersion       核心版本号
     */
    public PluginContext(PluginDescriptor descriptor,
                         ExtensionRegistry extensionRegistry,
                         ClassLoader classLoader,
                         Path pluginPath,
                         EventPublisher eventPublisher,
                         RuntimeMode runtimeMode,
                         String coreVersion) {
        this.descriptor = descriptor;
        this.extensionRegistry = extensionRegistry;
        this.classLoader = classLoader;
        this.pluginPath = pluginPath;
        this.eventPublisher = eventPublisher;
        this.runtimeMode = runtimeMode;
        this.coreVersion = coreVersion;
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

    /**
     * 获取事件发布者。
     *
     * @return 事件发布者
     */
    public EventPublisher eventPublisher() {
        return eventPublisher;
    }

    /**
     * 获取平台核心版本号。
     *
     * @return 核心版本号
     */
    public String coreVersion() {
        return coreVersion;
    }

    /**
     * 获取当前运行模式。
     *
     * @return 运行模式
     */
    public RuntimeMode runtimeMode() {
        return runtimeMode;
    }

    /**
     * 检查当前是否为开发模式。
     *
     * @return 如果是开发模式返回 true
     */
    public boolean isDevelopment() {
        return runtimeMode == RuntimeMode.DEVELOPMENT;
    }

    /**
     * 检查当前是否为部署模式（生产模式）。
     *
     * @return 如果是部署模式返回 true
     */
    public boolean isDeployment() {
        return runtimeMode == RuntimeMode.DEPLOYMENT;
    }
}
