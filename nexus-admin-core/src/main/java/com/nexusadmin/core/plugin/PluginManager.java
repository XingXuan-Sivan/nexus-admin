package com.nexusadmin.core.plugin;

import com.nexusadmin.core.context.PluginContext;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.exception.PluginLoadException;
import com.nexusadmin.core.plugin.event.PluginLifecycleEvent;
import com.nexusadmin.core.plugin.event.PluginLifecycleListener;
import com.nexusadmin.core.plugin.loader.PluginMetadata;
import com.nexusadmin.core.plugin.loader.PluginWapper;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.loader.SourceType;
import com.nexusadmin.core.plugin.source.PluginSource;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 插件管理器，协调平台层生命周期各阶段。
 * <p>阶段：Discover → Resolve → Load → Install → Start/Stop/Uninstall</p>
 *
 * <p><strong>职责说明：</strong></p>
 * <ul>
 *   <li>本类只负责插件生命周期管理（Discover → Resolve → Load → Install → Start/Stop/Uninstall）</li>
 *   <li>所有组件（Source、Loader、Listener）的注册交由 ComponentRegistry 统一管理</li>
 *   <li>通过构造函数注入所需组件，不再提供注册方法</li>
 *   <li>插件卸载时自动清理该插件注册的所有扩展点实现</li>
 * </ul>
 */
public class PluginManager {

    private final ExtensionRegistry extensionRegistry;
    private final List<PluginSource> sources;
    private final List<PluginLoader> loaders;
    private final List<PluginLifecycleListener> listeners;
    private final Map<String, PluginWapper> plugins = new ConcurrentHashMap<>();
    private final Map<String, PluginLoader> pluginLoaders = new ConcurrentHashMap<>();

    /**
     * 构造函数：所有依赖通过参数注入。
     *
     * @param extensionRegistry 扩展注册中心
     * @param sources           插件源列表（由 ComponentRegistry 提供）
     * @param loaders           插件加载器列表（由 ComponentRegistry 提供）
     * @param listeners         生命周期监听器列表（由 ComponentRegistry 提供）
     */
    public PluginManager(ExtensionRegistry extensionRegistry,
                         List<PluginSource> sources,
                         List<PluginLoader> loaders,
                         List<PluginLifecycleListener> listeners) {
        this.extensionRegistry = Objects.requireNonNull(extensionRegistry, "扩展注册中心不能为空");
        this.sources = List.copyOf(sources != null ? sources : List.of());
        this.loaders = List.copyOf(loaders != null ? loaders : List.of());
        this.listeners = List.copyOf(listeners != null ? listeners : List.of());
    }

    // ==================== 第一阶段：Discover ====================

    /**
     * 从所有注册的源发现候选插件。
     *
     * @return 候选插件列表
     */
    public List<PluginMetadata> discover() {
        fireEvent(PluginLifecycleEvent.discoverStart());

        List<PluginMetadata> candidates = sources.stream()
                .flatMap(s -> s.scan().stream())
                .peek(c -> fireEvent(PluginLifecycleEvent.discovered(c)))
                .collect(Collectors.toList());

        fireEvent(PluginLifecycleEvent.discoverEnd(candidates.size()));
        return candidates;
    }

    // ==================== 第二阶段：Resolve ====================

    /**
     * 解析候选插件，处理冲突和依赖。
     *
     * @param candidates 候选插件列表
     * @return 经过裁决后的有效插件列表
     */
    public List<PluginMetadata> resolve(List<PluginMetadata> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        fireEvent(PluginLifecycleEvent.resolveStart(candidates.size()));

        Map<String, List<PluginMetadata>> grouped = candidates.stream()
                .collect(Collectors.groupingBy(PluginMetadata::pluginId));

        List<PluginMetadata> resolved = grouped.values().stream()
                .map(this::selectBest)
                .peek(c -> fireEvent(PluginLifecycleEvent.resolved(c)))
                .collect(Collectors.toList());

        fireEvent(PluginLifecycleEvent.resolveEnd(resolved.size()));
        return resolved;
    }

    private PluginMetadata selectBest(List<PluginMetadata> list) {
        return list.stream()
                .max(Comparator
                        .comparing((PluginMetadata c) -> score(c.sourceType()))
                        .thenComparing(c -> c.descriptor().version()))
                .orElseThrow();
    }

    private int score(SourceType type) {
        return switch (type) {
            case EXTERNAL -> 100;
            case BUILTIN -> 50;
            case CLASSPATH -> 10;
            case REMOTE -> 5;
        };
    }

    // ==================== 第三阶段：Load & Install ====================

    /**
     * 加载并安装解析后的插件。
     *
     * @param candidates 经过解析的候选插件列表
     */
    public void install(List<PluginMetadata> candidates) {
        fireEvent(PluginLifecycleEvent.installStart(candidates.size()));

        for (PluginMetadata candidate : candidates) {
            if (plugins.containsKey(candidate.pluginId())) {
                fireEvent(PluginLifecycleEvent.skipped(candidate, "已存在"));
                continue;
            }

            try {
                PluginWapper loaded = doLoadAndInstall(candidate);
                fireEvent(PluginLifecycleEvent.installed(loaded));
            } catch (Exception e) {
                fireEvent(PluginLifecycleEvent.failed(candidate, e));
            }
        }

        fireEvent(PluginLifecycleEvent.installEnd(plugins.size()));
    }

    private PluginWapper doLoadAndInstall(PluginMetadata candidate) {
        // 选择合适的加载器
        PluginLoader loader = loaders.stream()
                .filter(l -> l.supports(candidate))
                .findFirst()
                .orElseThrow(() -> new PluginLoadException("无可用加载器: " + candidate.pluginId()));

        // 加载
        PluginWapper loaded = loader.load(candidate, extensionRegistry);

        // 安装（调用插件 install 回调）
        if (loaded.plugin() != null) {
            loaded.plugin().install(loaded.createContext(extensionRegistry));
        }

        loaded.state(PluginState.INSTALLED);
        plugins.put(candidate.pluginId(), loaded);
        pluginLoaders.put(candidate.pluginId(), loader);

        return loaded;
    }

    // ==================== 事件通知 ====================

    private void fireEvent(PluginLifecycleEvent event) {
        for (PluginLifecycleListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception ignored) {
                // 监听器异常不应影响主流程
            }
        }
    }

    // ==================== 第四阶段：生命周期操作 ====================

    /**
     * 启动指定 ID 的插件。
     * <p>仅允许处于 INSTALLED 或 STOPPED 状态的插件启动。该操作具有幂等性。</p>
     *
     * @param pluginId 插件 ID
     */
    public void start(String pluginId) {
        PluginWapper loaded = require(pluginId);

        if (loaded.state() == PluginState.STARTED) {
            return;
        }

        if (loaded.state() != PluginState.INSTALLED && loaded.state() != PluginState.STOPPED) {
            throw new PluginLoadException("插件当前状态不可启动: " + loaded.state());
        }

        Plugin plugin = loaded.plugin();
        if (plugin == null) {
            throw new PluginLoadException("插件无入口类，无法启动: " + pluginId);
        }

        fireEvent(PluginLifecycleEvent.starting(loaded));

        PluginContext context = loaded.createContext(extensionRegistry);
        try {
            plugin.start(context);
            loaded.state(PluginState.STARTED);
            fireEvent(PluginLifecycleEvent.started(loaded));
        } catch (Exception ex) {
            loaded.state(PluginState.FAILED);
            throw new PluginLoadException("启动插件失败: " + pluginId, ex);
        }
    }

    /**
     * 停止指定 ID 的插件。
     * <p>仅允许处于 STARTED 状态的插件停止。非运行状态调用将直接返回。</p>
     *
     * @param pluginId 插件 ID
     */
    public void stop(String pluginId) {
        PluginWapper loaded = require(pluginId);

        if (loaded.state() != PluginState.STARTED) {
            return;
        }

        fireEvent(PluginLifecycleEvent.stopping(loaded));

        Plugin plugin = loaded.plugin();
        if (plugin == null) {
            loaded.state(PluginState.STOPPED);
            fireEvent(PluginLifecycleEvent.stopped(loaded));
            return;
        }

        PluginContext context = loaded.createContext(extensionRegistry);
        try {
            plugin.stop(context);
            loaded.state(PluginState.STOPPED);
            fireEvent(PluginLifecycleEvent.stopped(loaded));
        } catch (Exception ex) {
            loaded.state(PluginState.FAILED);
            throw new PluginLoadException("停止插件失败: " + pluginId, ex);
        }
    }

    /**
     * 卸载指定 ID 的插件。
     * <p>如果插件处于运行中，会先尝试停止。随后执行插件的 uninstall 回调，释放资源并从内存中移除。</p>
     *
     * @param pluginId 插件 ID
     */
    public void uninstall(String pluginId) {
        PluginWapper loaded = require(pluginId);

        fireEvent(PluginLifecycleEvent.uninstalling(loaded));

        // 1. 如果处于启动状态，先执行停止
        if (loaded.state() == PluginState.STARTED) {
            try {
                stop(pluginId);
            } catch (Exception ignored) {
                // 卸载时忽略停止失败，强制继续
            }
        }

        // 2. 调用插件的 uninstall 生命周期方法
        Plugin plugin = loaded.plugin();
        if (plugin != null) {
            PluginContext context = loaded.createContext(extensionRegistry);
            try {
                plugin.uninstall(context);
            } catch (Exception ignored) {
                // 卸载时忽略插件逻辑错误
            }
        }

        // 3. 从扩展注册中心清理该插件注册的所有扩展
        extensionRegistry.unregisterByPluginId(pluginId);

        // 4. 释放 JVM 资源
        closeIfPossible(loaded.classLoader());

        // 5. 执行物理卸载（删除文件）
        PluginLoader loader = pluginLoaders.remove(pluginId);
        if (loader != null && loader.supportsRemove()) {
            loader.remove(loaded);
        }

        plugins.remove(pluginId);
        loaded.state(PluginState.UNINSTALLED);

        fireEvent(PluginLifecycleEvent.uninstalled(loaded));
    }

    /**
     * 获取指定 ID 的插件信息。
     *
     * @param pluginId 插件 ID
     * @return 对应的 {@link PluginWapper}，不存在时返回 null
     */
    public PluginWapper get(String pluginId) {
        return plugins.get(pluginId);
    }

    /**
     * 列出当前所有已安装的插件。
     *
     * @return 已安装插件集合视图
     */
    public Collection<PluginWapper> list() {
        return plugins.values();
    }

    /**
     * 获取指定 ID 的已加载插件信息，如果不存在则抛出异常。
     *
     * @param pluginId 插件 ID
     * @return 对应的 {@link PluginWapper}
     */
    private PluginWapper require(String pluginId) {
        PluginWapper loaded = plugins.get(pluginId);
        if (loaded == null) {
            throw new PluginLoadException("plugin not found: " + pluginId);
        }
        return loaded;
    }

    /**
     * 尝试关闭指定类加载器。
     *
     * @param classLoader 类加载器
     */
    private void closeIfPossible(ClassLoader classLoader) {
        if (classLoader instanceof Closeable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // ignored
            }
        }
    }
}
