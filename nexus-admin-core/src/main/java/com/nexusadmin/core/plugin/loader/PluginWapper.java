package com.nexusadmin.core.plugin.loader;

import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.context.PluginContext;
import com.nexusadmin.core.plugin.descriptor.PluginDescriptor;
import com.nexusadmin.core.plugin.Plugin;
import com.nexusadmin.core.plugin.PluginState;

import java.nio.file.Path;

/**
 * 已加载的插件封装类，持有插件的描述信息、实例、类加载器及当前状态。
 */
public final class PluginWapper {
    /**
     * 插件描述信息。
     */
    private final PluginDescriptor descriptor;
    /**
     * 插件实例，可能为 null（如果插件仅提供扩展点实现而无入口类）。
     */
    private final Plugin plugin;
    /**
     * 插件专属类加载器。
     */
    private final ClassLoader classLoader;
    /**
     * 插件所在的物理路径。
     */
    private final Path pluginPath;
    /**
     * 插件当前生命周期状态。
     */
    private PluginState state;

    /**
     * 构造已加载插件对象。
     *
     * @param descriptor  插件描述信息
     * @param plugin      插件实例
     * @param classLoader 类加载器
     * @param pluginPath  插件路径
     */
    public PluginWapper(PluginDescriptor descriptor,
                        Plugin plugin,
                        ClassLoader classLoader,
                        Path pluginPath) {
        this.descriptor = descriptor;
        this.plugin = plugin;
        this.classLoader = classLoader;
        this.pluginPath = pluginPath;
        this.state = PluginState.INSTALLED;
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
     * 获取插件实例。
     *
     * @return 插件实例
     */
    public Plugin plugin() {
        return plugin;
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
     * 获取插件物理路径。
     *
     * @return 插件路径
     */
    public Path pluginPath() {
        return pluginPath;
    }

    /**
     * 获取插件当前状态。
     *
     * @return 插件状态
     */
    public PluginState state() {
        return state;
    }

    /**
     * 设置插件当前状态。
     *
     * @param state 目标状态
     */
    public void state(PluginState state) {
        this.state = state;
    }

    /**
     * 为该插件创建运行上下文。
     *
     * @param registry 全局扩展注册中心
     * @return 插件上下文实例
     */
    public PluginContext createContext(ExtensionRegistry registry) {
        return new PluginContext(descriptor, registry, classLoader, pluginPath);
    }
}
