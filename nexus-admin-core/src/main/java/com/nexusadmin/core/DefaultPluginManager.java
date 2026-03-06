package com.nexusadmin.core;

import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.plugin.RuntimeMode;
import com.nexusadmin.core.plugin.discovery.DefaultPluginDiscoverer;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorFinder;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorParser;
import com.nexusadmin.core.plugin.discovery.PluginDiscoverer;
import com.nexusadmin.core.plugin.discovery.PluginSource;
import com.nexusadmin.core.plugin.discovery.finder.DevDirectoryFinder;
import com.nexusadmin.core.plugin.discovery.finder.PackedDirectoryFinder;
import com.nexusadmin.core.plugin.discovery.source.ClasspathPluginSource;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.loader.PluginWrapper;
import com.nexusadmin.core.plugin.PluginRegistry;
import com.nexusadmin.core.plugin.resolve.DefaultPluginResolver;
import com.nexusadmin.core.plugin.resolve.DependenceManager;
import com.nexusadmin.core.plugin.resolve.PluginResolver;
import com.nexusadmin.core.plugin.resolve.VersionManager;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * 默认插件管理器实现。
 * <p>继承 {@link AbstractPluginManager}，负责策略组件的组装与生命周期管理。</p>
 */
public class DefaultPluginManager extends AbstractPluginManager {

    private final List<PluginSource> sources;
    private final List<PluginDescriptorFinder> finders;
    private final List<PluginDescriptorParser> parsers;
    private final VersionManager versionManager;
    private final DependenceManager dependenceManager;
    private final PluginLoader pluginLoader;

    private final boolean autoStart;

    /**
     * 构造默认插件管理器。
     *
     * @param pluginRegistry    插件注册中心
     * @param extensionRegistry 扩展注册中心
     * @param eventBus          事件总线
     * @param runtimeMode       运行模式
     * @param coreVersion       核心版本号
     * @param pluginsDataRoot   插件数据根目录
     * @param sources           插件源列表
     * @param finders           描述文件查找器列表
     * @param parsers           描述文件解析器列表
     * @param versionManager    版本管理器
     * @param dependenceManager 依赖管理器
     * @param pluginLoader      插件加载器
     * @param autoStart         是否自动启动
     */
    public DefaultPluginManager(PluginRegistry pluginRegistry,
                                ExtensionRegistry extensionRegistry,
                                EventBus eventBus,
                                RuntimeMode runtimeMode,
                                String coreVersion,
                                Path pluginsDataRoot,
                                List<PluginSource> sources,
                                List<PluginDescriptorFinder> finders,
                                List<PluginDescriptorParser> parsers,
                                VersionManager versionManager,
                                DependenceManager dependenceManager,
                                PluginLoader pluginLoader,
                                boolean autoStart) {
        super(pluginRegistry, extensionRegistry, eventBus, runtimeMode, coreVersion, pluginsDataRoot);
        this.sources = List.copyOf(sources != null ? sources : List.of());
        this.finders = List.copyOf(finders != null ? finders : List.of());
        this.parsers = List.copyOf(parsers != null ? parsers : List.of());
        this.versionManager = versionManager;
        this.dependenceManager = dependenceManager;
        this.pluginLoader = pluginLoader;
        this.autoStart = autoStart;
        initializeComponents();
    }

    @Override
    protected PluginDiscoverer createPluginDiscoverer() {
        return new DefaultPluginDiscoverer(
                filterSources(sources),
                filterFinders(finders),
                parsers
        );
    }

    @Override
    protected PluginResolver createPluginResolver() {
        return new DefaultPluginResolver(versionManager, dependenceManager);
    }

    @Override
    protected PluginLoader createPluginLoader() {
        return pluginLoader;
    }

    /**
     * 过滤查找器，DEPLOYMENT 模式下移除 DevDirectoryFinder 和 PackedDirectoryFinder。
     *
     * @param finders 原始查找器列表
     * @return 过滤后的查找器列表
     */
    private List<PluginDescriptorFinder> filterFinders(List<PluginDescriptorFinder> finders) {
        if (runtimeMode != RuntimeMode.DEPLOYMENT) {
            return finders;
        }
        return finders.stream()
                .filter(f -> !(f instanceof DevDirectoryFinder) && !(f instanceof PackedDirectoryFinder))
                .toList();
    }

    /**
     * 过滤插件源，DEPLOYMENT 模式下移除 ClasspathPluginSource。
     *
     * @param sources 原始插件源列表
     * @return 过滤后的插件源列表
     */
    private List<PluginSource> filterSources(List<PluginSource> sources) {
        if (runtimeMode != RuntimeMode.DEPLOYMENT) {
            return sources;
        }
        return sources.stream()
                .filter(s -> !(s instanceof ClasspathPluginSource))
                .toList();
    }

    @Override
    protected void autoStartIfNecessary() {
        if (!autoStart) {
            return;
        }
        for (PluginWrapper wrapper : list()) {
            if (wrapper.descriptor().hasEntryPoint() && wrapper.state() == PluginState.INITIALIZED) {
                try {
                    start(wrapper.getPluginId());
                } catch (Exception ex) {
                    // 启动失败已在事件监听器中记录
                }
            }
        }
    }

    @Override
    public PluginWrapper get(String pluginId) {
        return pluginRegistry.get(pluginId);
    }

    @Override
    public Collection<PluginWrapper> list() {
        return pluginRegistry.list();
    }
}
