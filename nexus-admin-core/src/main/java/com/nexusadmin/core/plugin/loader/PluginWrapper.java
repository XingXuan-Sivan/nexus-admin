package com.nexusadmin.core.plugin.loader;

import com.nexusadmin.core.event.EventPublisher;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.context.PluginContext;
import com.nexusadmin.core.Plugin;
import com.nexusadmin.core.PluginState;
import com.nexusadmin.core.plugin.RuntimeMode;
import com.nexusadmin.core.plugin.discovery.PluginDescriptor;
import com.nexusadmin.core.plugin.discovery.PluginSource;

import java.nio.file.Path;

/**
 * 已加载的插件封装类，持有插件的描述信息、实例、类加载器及当前状态。
 * <p>提供状态迁移控制，确保生命周期状态转换的合法性。</p>
 */
public final class PluginWrapper {

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
     * 插件来源，包含路径和物理卸载能力。
     */
    private final PluginSource source;

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
     * @param source      插件来源
     */
    public PluginWrapper(PluginDescriptor descriptor,
                         Plugin plugin,
                         ClassLoader classLoader,
                         PluginSource source) {
        this.descriptor = descriptor;
        this.plugin = plugin;
        this.classLoader = classLoader;
        this.source = source;
        this.state = PluginState.RESOLVED;
    }

    /**
     * 获取插件 ID。
     *
     * @return 插件唯一标识
     */
    public String getPluginId() {
        return descriptor.id();
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
     * 获取插件来源。
     *
     * @return 插件来源
     */
    public PluginSource source() {
        return source;
    }

    /**
     * 获取插件来源类型
     *
     * @return 插件来源类型
     */
    public SourceType sourceType() {
        return source.getType();
    }

    /**
     * 获取插件物理路径。
     *
     * @return 插件物理路径，可能为 null
     */
    public Path physicalPath() {
        return source.getPhysicalPath();
    }

    /**
     * 是否支持物理卸载。
     *
     * @return 如果支持物理卸载返回 true
     */
    public boolean supportsPhysicalRemoval() {
        return source.supportsPhysicalRemoval();
    }

    /**
     * 执行物理卸载。
     */
    public void removePhysically() {
        source.removePhysically();
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
     * 设置插件当前状态（内部使用，不验证状态迁移）。
     * <p>状态迁移合法性由 PluginStateTransitions 统一校验。</p>
     *
     * @param state 目标状态
     */
    public void state(PluginState state) {
        this.state = state;
    }

    /**
     * 为该插件创建运行上下文。
     *
     * @param registry      全局扩展注册中心
     * @param eventPublisher 事件发布者
     * @param runtimeMode   运行模式
     * @param coreVersion   核心版本号
     * @return 插件上下文实例
     */
    public PluginContext createContext(ExtensionRegistry registry,
                                       EventPublisher eventPublisher,
                                       RuntimeMode runtimeMode,
                                       String coreVersion) {
        return new PluginContext(descriptor, registry, classLoader, source,
                eventPublisher, runtimeMode, coreVersion);
    }
}
