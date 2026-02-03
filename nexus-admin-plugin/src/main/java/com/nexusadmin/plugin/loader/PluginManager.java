package com.nexusadmin.plugin.loader;

import com.nexusadmin.core.spi.SpiRegistry;
import com.nexusadmin.plugin.context.PluginContext;
import com.nexusadmin.plugin.descriptor.PluginDescriptor;
import com.nexusadmin.plugin.exception.PluginLoadException;
import com.nexusadmin.plugin.lifecycle.Plugin;
import com.nexusadmin.plugin.lifecycle.PluginState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 插件管理器，负责插件的安装、启动、停止及卸载，并维护插件 ID 到已安装插件的映射。
 * <p>本类严格遵循插件生命周期模型：install -> start -> stop -> uninstall。</p>
 */
public class PluginManager {
    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);
    /**
     * SPI 注册中心，用于插件的 SPI 服务注册与发现。
     */
    private final SpiRegistry spiRegistry;
    /**
     * 已注册的插件加载器列表。
     */
    private final List<PluginLoader> loaders = new CopyOnWriteArrayList<>();
    /**
     * 已安装插件集合，key 为插件 ID。
     */
    private final Map<String, LoadedPlugin> plugins = new ConcurrentHashMap<>();
    /**
     * 记录插件 ID 与其对应加载器的映射，用于生命周期管理（如物理卸载）。
     */
    private final Map<String, PluginLoader> pluginLoaders = new ConcurrentHashMap<>();

    /**
     * 创建插件管理器。
     *
     * @param spiRegistry SPI 注册中心
     */
    public PluginManager(SpiRegistry spiRegistry) {
        this.spiRegistry = spiRegistry;
    }

    /**
     * 注册一个新的插件加载器。
     *
     * @param loader 加载器实现
     */
    public void registerLoader(PluginLoader loader) {
        this.loaders.add(loader);
    }

    /**
     * 从指定路径安装插件。
     * <p>遵循标准流程：发现 -> 解析 -> 安装。</p>
     *
     * @param path 插件所在路径（目录或 JAR）
     * @return 已安装插件的封装对象，如果已存在则返回现有对象
     */
    public LoadedPlugin install(java.nio.file.Path path) {
        PluginCandidate candidate = discover(path);
        List<PluginCandidate> resolved = resolve(List.of(candidate));
        if (resolved.isEmpty()) {
            return get(candidate.pluginId());
        }
        
        installResolved(resolved);
        return get(candidate.pluginId());
    }

    /**
     * 发现指定路径下的插件。
     *
     * @param path 插件路径
     * @return 候选插件对象
     */
    public PluginCandidate discover(java.nio.file.Path path) {
        PluginLoader selectedLoader = loaders.stream()
                .filter(l -> l.canLoad(path))
                .findFirst()
                .orElseThrow(() -> new PluginLoadException("没有找到合适的加载器来处理路径: " + path));

        return selectedLoader.discover(path, spiRegistry);
    }

    /**
     * 第一阶段：发现（Discover）
     * 触发所有已注册加载器的自动发现机制。
     *
     * @return 发现的所有候选插件列表
     */
    public List<PluginCandidate> discoverAll() {
        List<PluginCandidate> candidates = new ArrayList<>();
        for (PluginLoader loader : loaders) {
            try {
                candidates.addAll(loader.discoverAll(spiRegistry));
            } catch (Exception e) {
                log.warn("加载器 {} 发现插件失败", loader.getClass().getSimpleName(), e);
            }
        }
        return candidates;
    }

    /**
     * 第二阶段：解析（Resolve）
     * 执行插件去重、来源裁决和优先级排序。
     *
     * @param candidates 候选插件列表
     * @return 经过裁决后的有效插件列表
     */
    public List<PluginCandidate> resolve(List<PluginCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        Map<String, List<PluginCandidate>> grouped = candidates.stream()
                .collect(Collectors.groupingBy(PluginCandidate::pluginId));

        List<PluginCandidate> resolved = new ArrayList<>();
        int duplicates = 0;

        for (Map.Entry<String, List<PluginCandidate>> entry : grouped.entrySet()) {
            List<PluginCandidate> list = entry.getValue();
            if (list.size() > 1) {
                duplicates += (list.size() - 1);
            }
            resolved.add(selectBest(list));
        }

        if (duplicates > 0) {
            log.info("插件解析完成：解析后有效插件数 {}, 重复插件裁决数 {}", resolved.size(), duplicates);
        } else {
            log.info("插件解析完成：有效插件数 {}", resolved.size());
        }

        return resolved;
    }

    /**
     * 第三阶段：安装（Install）
     * 批量安装经过解析裁决后的插件。
     *
     * @param resolved 经过解析后的有效插件列表
     */
    public void installResolved(List<PluginCandidate> resolved) {
        for (PluginCandidate candidate : resolved) {
            // 强幂等：如果插件已安装，则直接跳过，不抛异常也不打告警日志
            if (plugins.containsKey(candidate.pluginId())) {
                continue;
            }

            try {
                LoadedPlugin loaded = candidate.loader().load(candidate, spiRegistry);
                doInstall(loaded, candidate.loader());
                log.info("插件安装成功: {}", candidate.pluginId());
            } catch (Exception e) {
                log.error("安装插件 {} 失败", candidate.pluginId(), e);
            }
        }
    }

    /**
     * 裁决策略：选择最优的候选插件。
     * 规则：EXTERNAL > BUILTIN > CLASSPATH，同来源下按 priority 排序。
     */
    private PluginCandidate selectBest(List<PluginCandidate> list) {
        return list.stream()
                .sorted(Comparator
                        .<PluginCandidate, SourceType>comparing(PluginCandidate::sourceType, (s1, s2) -> {
                            // 优先级定义: EXTERNAL(1) > BUILTIN(2) > CLASSPATH(0)
                            // 简单起见，映射为数值比较，EXTERNAL 最大
                            int p1 = score(s1);
                            int p2 = score(s2);
                            return Integer.compare(p2, p1); // 逆序，大的在前
                        })
                        .thenComparing(c -> c.descriptor().priority(), Comparator.reverseOrder())
                )
                .findFirst()
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

    /**
     * 执行实际的安装逻辑，包括调用插件 install 回调并维护状态。
     */
    private LoadedPlugin doInstall(LoadedPlugin loaded, PluginLoader loader) {
        String pluginId = loaded.descriptor().id();
        if (plugins.containsKey(pluginId)) {
            throw new PluginLoadException("插件已存在: " + pluginId);
        }

        // 1. 调用插件的 install 生命周期方法
        Plugin plugin = loaded.plugin();
        if (plugin != null) {
            PluginContext context = loaded.createContext(spiRegistry);
            try {
                plugin.install(context);
            } catch (Exception e) {
                loaded.state(PluginState.FAILED);
                throw new PluginLoadException("执行插件 install 生命周期失败: " + pluginId, e);
            }
        }

        // 2. 注册到管理器
        pluginLoaders.put(pluginId, loader);
        plugins.put(pluginId, loaded);
        loaded.state(PluginState.INSTALLED);

        return loaded;
    }

    /**
     * 启动指定 ID 的插件。
     * <p>仅允许处于 INSTALLED 或 STOPPED 状态的插件启动。该操作具有幂等性。</p>
     *
     * @param pluginId 插件 ID
     */
    public void start(String pluginId) {
        LoadedPlugin loaded = require(pluginId);
        
        // 幂等处理
        if (loaded.state() == PluginState.STARTED) {
            return;
        }

        // 状态校验
        if (loaded.state() != PluginState.INSTALLED && loaded.state() != PluginState.STOPPED) {
            throw new PluginLoadException("插件当前状态不可启动: " + loaded.state());
        }

        Plugin plugin = loaded.plugin();
        if (plugin == null) {
            throw new PluginLoadException("插件无入口类，无法启动: " + pluginId);
        }

        PluginContext context = loaded.createContext(spiRegistry);
        try {
            plugin.start(context);
            loaded.state(PluginState.STARTED);
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
        LoadedPlugin loaded = require(pluginId);
        
        // 幂等处理：非启动状态直接返回
        if (loaded.state() != PluginState.STARTED) {
            return;
        }

        Plugin plugin = loaded.plugin();
        if (plugin == null) {
            loaded.state(PluginState.STOPPED);
            return;
        }

        PluginContext context = loaded.createContext(spiRegistry);
        try {
            plugin.stop(context);
            loaded.state(PluginState.STOPPED);
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
        LoadedPlugin loaded = require(pluginId);

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
            PluginContext context = loaded.createContext(spiRegistry);
            try {
                plugin.uninstall(context);
            } catch (Exception ignored) {
                // 卸载时忽略插件逻辑错误
            }
        }

        // 3. 释放 JVM 资源
        closeIfPossible(loaded.classLoader());

        // 4. 执行物理卸载（删除文件）
        PluginLoader loader = pluginLoaders.remove(pluginId);
        if (loader != null && loader.supportsRemove()) {
            loader.remove(loaded);
        }

        plugins.remove(pluginId);
        loaded.state(PluginState.UNINSTALLED);
    }

    /**
     * 获取指定 ID 的插件信息。
     *
     * @param pluginId 插件 ID
     * @return 对应的 {@link LoadedPlugin}，不存在时返回 null
     */
    public LoadedPlugin get(String pluginId) {
        return plugins.get(pluginId);
    }

    /**
     * 列出当前所有已安装的插件。
     *
     * @return 已安装插件集合视图
     */
    public Collection<LoadedPlugin> list() {
        return plugins.values();
    }

    /**
     * 获取指定 ID 的已加载插件信息，如果不存在则抛出异常。
     *
     * @param pluginId 插件 ID
     * @return 对应的 {@link LoadedPlugin}
     */
    private LoadedPlugin require(String pluginId) {
        LoadedPlugin loaded = plugins.get(pluginId);
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
