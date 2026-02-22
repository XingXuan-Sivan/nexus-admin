package com.nexusadmin.core.plugin;

import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.exception.PluginLoadException;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.plugin.descriptor.PluginDescriptorFinder;
import com.nexusadmin.core.plugin.descriptor.finder.DevDirectoryFinder;
import com.nexusadmin.core.plugin.descriptor.finder.PackedDirectoryFinder;
import com.nexusadmin.core.plugin.discovery.PluginDiscoverer;
import com.nexusadmin.core.plugin.event.PluginProcessEvent;
import com.nexusadmin.core.plugin.loader.ClasspathPluginLoader;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.loader.PluginMetadata;
import com.nexusadmin.core.plugin.loader.PluginWrapper;
import com.nexusadmin.core.plugin.registry.PluginRegistry;
import com.nexusadmin.core.plugin.resolution.PluginResolver;
import com.nexusadmin.core.plugin.source.ClasspathPluginSource;
import com.nexusadmin.core.plugin.source.PluginSource;
import com.nexusadmin.core.plugin.version.VersionManager;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 默认插件管理器实现。
 * <p>继承 {@link AbstractPluginManager}，实现具体的插件加载逻辑。</p>
 */
public class DefaultPluginManager extends AbstractPluginManager {

    protected final List<PluginLoader> rawLoaders;
    protected final List<PluginDescriptorFinder> rawFinders;
    protected final List<PluginSource> rawSources;

    protected List<PluginLoader> effectiveLoaders;
    protected List<PluginDescriptorFinder> effectiveFinders;
    protected List<PluginSource> effectiveSources;

    private final boolean autoStart;

    /**
     * 构造默认插件管理器。
     *
     * @param pluginRegistry    插件注册中心
     * @param extensionRegistry 扩展注册中心
     * @param eventBus          事件总线
     * @param versionManager    版本管理器
     * @param runtimeMode       运行模式
     * @param coreVersion       核心版本号
     * @param pluginDiscoverer  插件发现器
     * @param pluginResolver    插件解析器
     * @param loaders           插件加载器列表
     * @param finders           描述文件查找器列表
     * @param sources           插件源列表
     * @param autoStart         是否自动启动
     */
    public DefaultPluginManager(PluginRegistry pluginRegistry,
                                ExtensionRegistry extensionRegistry,
                                EventBus eventBus,
                                VersionManager versionManager,
                                RuntimeMode runtimeMode,
                                String coreVersion,
                                PluginDiscoverer pluginDiscoverer,
                                PluginResolver pluginResolver,
                                List<PluginLoader> loaders,
                                List<PluginDescriptorFinder> finders,
                                List<PluginSource> sources,
                                boolean autoStart) {
        super(pluginRegistry, extensionRegistry, eventBus, versionManager, runtimeMode,
                coreVersion, pluginDiscoverer, pluginResolver);
        this.rawLoaders = List.copyOf(loaders != null ? loaders : List.of());
        this.rawFinders = List.copyOf(finders != null ? finders : List.of());
        this.rawSources = List.copyOf(sources != null ? sources : List.of());
        this.autoStart = autoStart;
        initializeStrategies();
    }

    /**
     * 初始化策略，根据运行模式裁剪。
     */
    private void initializeStrategies() {
        this.effectiveLoaders = filterLoaders(rawLoaders);
        this.effectiveFinders = filterFinders(rawFinders);
        this.effectiveSources = filterSources(rawSources);
    }

    /**
     * 过滤加载器，DEPLOYMENT 模式下移除 ClasspathPluginLoader。
     *
     * @param loaders 原始加载器列表
     * @return 过滤后的加载器列表
     */
    private List<PluginLoader> filterLoaders(List<PluginLoader> loaders) {
        if (runtimeMode != RuntimeMode.DEPLOYMENT) {
            return loaders;
        }
        return loaders.stream()
                .filter(l -> !(l instanceof ClasspathPluginLoader))
                .toList();
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
    protected PluginWrapper doLoad(PluginMetadata candidate) {
        PluginLoader loader = effectiveLoaders.stream()
                .filter(l -> l.supports(candidate))
                .findFirst()
                .orElseThrow(() -> new PluginLoadException("无可用加载器: " + candidate.pluginId()));

        PluginWrapper wrapper = loader.load(candidate, extensionRegistry);

        pluginLoaders.put(candidate.pluginId(), loader);

        return wrapper;
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
