package com.nexusadmin.core.facade;

import com.nexusadmin.core.PluginState;
import com.nexusadmin.core.plugin.PluginRegistry;
import com.nexusadmin.core.plugin.discovery.DefaultPluginDiscoverer;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorFinder;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorParser;
import com.nexusadmin.core.plugin.discovery.PluginDiscoverer;
import com.nexusadmin.core.plugin.discovery.PluginSource;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.loader.PluginWrapper;
import com.nexusadmin.core.plugin.resolve.DependenceManager;
import com.nexusadmin.core.plugin.resolve.PluginResolver;
import com.nexusadmin.core.plugin.resolve.VersionManager;
import com.nexusadmin.core.plugin.resolve.impl.DefaultPluginResolver;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 插件组件门面。
 * <p>聚合插件发现、解析、加载、注册等全部子组件，提供统一的插件查询、组件创建与底层访问入口。</p>
 */
public class PluginFacade {

    private final PluginRegistry pluginRegistry;
    private final PluginLoader pluginLoader;
    private final VersionManager versionManager;
    private final DependenceManager dependenceManager;
    private final List<PluginSource> sources;
    private final List<PluginDescriptorFinder> finders;
    private final List<PluginDescriptorParser> parsers;

    /**
     * 构造插件组件门面。
     *
     * @param pluginRegistry    插件注册中心，不能为空
     * @param pluginLoader      插件加载器，不能为空
     * @param versionManager    版本管理器，不能为空
     * @param dependenceManager 依赖管理器，不能为空
     * @param sources           插件源列表
     * @param finders           描述文件查找器列表
     * @param parsers           描述文件解析器列表
     */
    public PluginFacade(PluginRegistry pluginRegistry,
                        PluginLoader pluginLoader,
                        VersionManager versionManager,
                        DependenceManager dependenceManager,
                        List<PluginSource> sources,
                        List<PluginDescriptorFinder> finders,
                        List<PluginDescriptorParser> parsers) {
        this.pluginRegistry = Objects.requireNonNull(pluginRegistry, "插件注册中心不能为空");
        this.pluginLoader = Objects.requireNonNull(pluginLoader, "插件加载器不能为空");
        this.versionManager = Objects.requireNonNull(versionManager, "版本管理器不能为空");
        this.dependenceManager = Objects.requireNonNull(dependenceManager, "依赖管理器不能为空");
        this.sources = List.copyOf(sources != null ? sources : List.of());
        this.finders = List.copyOf(finders != null ? finders : List.of());
        this.parsers = List.copyOf(parsers != null ? parsers : List.of());
    }

    /**
     * 根据插件ID获取已注册的插件。
     *
     * @param pluginId 插件唯一标识
     * @return 插件包装对象，不存在则返回 null
     */
    public PluginWrapper getPlugin(String pluginId) {
        return pluginRegistry.get(pluginId);
    }

    /**
     * 获取所有已注册的插件列表。
     *
     * @return 插件包装对象集合
     */
    public Collection<PluginWrapper> listPlugins() {
        return pluginRegistry.list();
    }

    /**
     * 获取指定状态下的所有插件。
     *
     * @param state 目标状态
     * @return 符合条件的插件包装对象集合
     */
    public Collection<PluginWrapper> listByState(PluginState state) {
        return pluginRegistry.list().stream()
                .filter(w -> w.state() == state)
                .toList();
    }

    /**
     * 创建插件发现器实例。
     *
     * @return 插件发现器
     */
    public PluginDiscoverer createDiscoverer() {
        return new DefaultPluginDiscoverer(sources, finders, parsers);
    }

    /**
     * 创建插件解析器实例。
     *
     * @return 插件解析器
     */
    public PluginResolver createResolver() {
        return new DefaultPluginResolver(versionManager, dependenceManager);
    }

    /**
     * 获取插件注册中心。
     *
     * @return 插件注册中心
     */
    public PluginRegistry pluginRegistry() {
        return pluginRegistry;
    }

    /**
     * 获取插件加载器。
     *
     * @return 插件加载器
     */
    public PluginLoader pluginLoader() {
        return pluginLoader;
    }

    /**
     * 获取版本管理器。
     *
     * @return 版本管理器
     */
    public VersionManager versionManager() {
        return versionManager;
    }

    /**
     * 获取依赖管理器。
     *
     * @return 依赖管理器
     */
    public DependenceManager dependenceManager() {
        return dependenceManager;
    }

    /**
     * 获取插件源列表。
     *
     * @return 插件源列表
     */
    public List<PluginSource> sources() {
        return sources;
    }

    /**
     * 获取描述文件查找器列表。
     *
     * @return 查找器列表
     */
    public List<PluginDescriptorFinder> finders() {
        return finders;
    }

    /**
     * 获取描述文件解析器列表。
     *
     * @return 解析器列表
     */
    public List<PluginDescriptorParser> parsers() {
        return parsers;
    }
}
