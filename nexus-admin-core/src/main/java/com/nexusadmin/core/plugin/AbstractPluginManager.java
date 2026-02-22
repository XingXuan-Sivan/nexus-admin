package com.nexusadmin.core.plugin;

import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.exception.PluginLoadException;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.plugin.discovery.PluginDiscoverer;
import com.nexusadmin.core.plugin.event.PluginStateChangedEvent;
import com.nexusadmin.core.plugin.event.PluginProcessEvent;
import com.nexusadmin.core.plugin.event.PluginFailureEvent;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.loader.PluginMetadata;
import com.nexusadmin.core.plugin.loader.PluginWrapper;
import com.nexusadmin.core.plugin.registry.PluginRegistry;
import com.nexusadmin.core.plugin.resolution.PluginResolver;
import com.nexusadmin.core.plugin.version.VersionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 抽象插件管理器，实现生命周期流程编排。
 * <p>子类只需实现具体的加载逻辑，状态管理和事件发布由基类统一处理。</p>
 */
public abstract class AbstractPluginManager implements PluginManager {

    private static final Logger log = LoggerFactory.getLogger(AbstractPluginManager.class);

    protected final PluginRegistry pluginRegistry;
    protected final ExtensionRegistry extensionRegistry;
    protected final EventBus eventBus;
    protected final VersionManager versionManager;
    protected final RuntimeMode runtimeMode;
    protected final String coreVersion;
    protected final PluginDiscoverer pluginDiscoverer;
    protected final PluginResolver pluginResolver;

    /**
     * 插件ID到加载器的映射，用于卸载时执行物理删除。
     */
    protected final Map<String, PluginLoader> pluginLoaders = new ConcurrentHashMap<>();

    /**
     * 已禁用的插件ID集合。
     */
    protected final List<String> disabledPlugins = new ArrayList<>();

    /**
     * 生命周期专用锁，保证状态迁移原子性。
     */
    protected final Object lifecycleLock = new Object();

    /**
     * 构造抽象插件管理器。
     * <p>构造函数中自动执行初始化，发布初始化完成事件。</p>
     *
     * @param pluginRegistry    插件注册中心
     * @param extensionRegistry 扩展注册中心
     * @param eventBus          事件总线
     * @param versionManager    版本管理器
     * @param runtimeMode       运行模式
     * @param coreVersion       核心版本号
     * @param pluginDiscoverer  插件发现器
     * @param pluginResolver    插件解析器
     */
    protected AbstractPluginManager(PluginRegistry pluginRegistry,
                                    ExtensionRegistry extensionRegistry,
                                    EventBus eventBus,
                                    VersionManager versionManager,
                                    RuntimeMode runtimeMode,
                                    String coreVersion,
                                    PluginDiscoverer pluginDiscoverer,
                                    PluginResolver pluginResolver) {
        this.pluginRegistry = Objects.requireNonNull(pluginRegistry, "插件注册中心不能为空");
        this.extensionRegistry = Objects.requireNonNull(extensionRegistry, "扩展注册中心不能为空");
        this.eventBus = Objects.requireNonNull(eventBus, "事件总线不能为空");
        this.versionManager = Objects.requireNonNull(versionManager, "版本管理器不能为空");
        this.runtimeMode = Objects.requireNonNull(runtimeMode, "运行模式不能为空");
        this.coreVersion = Objects.requireNonNull(coreVersion, "核心版本号不能为空");
        this.pluginDiscoverer = Objects.requireNonNull(pluginDiscoverer, "插件发现器不能为空");
        this.pluginResolver = Objects.requireNonNull(pluginResolver, "插件解析器不能为空");
    }

    // ===== 状态迁移控制 =====

    /**
     * 核心状态迁移方法。
     * <p>验证状态迁移合法性，同时发布状态变更事件。</p>
     *
     * @param wrapper 插件包装对象
     * @param target  目标状态
     * @throws IllegalStateException 如果状态迁移不合法
     */
    protected void transition(PluginWrapper wrapper, PluginState target) {
        synchronized (lifecycleLock) {
            PluginState from = wrapper.state();

            if (!PluginStateTransitions.canTransition(from, target)) {
                throw new IllegalStateException(
                        String.format("插件 %s 状态迁移非法: %s -> %s",
                                wrapper.getPluginId(), from, target));
            }

            wrapper.state(target);
            eventBus.publish(new PluginStateChangedEvent(wrapper, from, target));
        }
    }

    /**
     * 统一失败处理。
     * <p>将插件状态迁移到 FAILED，并发布失败事件。</p>
     *
     * @param wrapper 插件包装对象
     * @param e       异常对象
     */
    protected void handleFailure(PluginWrapper wrapper, Throwable e) {
        synchronized (lifecycleLock) {
            PluginState from = wrapper.state();
            wrapper.state(PluginState.FAILED);
            eventBus.publish(new PluginStateChangedEvent(wrapper, from, PluginState.FAILED));
            eventBus.publish(new PluginFailureEvent(wrapper.descriptor(), e));
        }
    }

    @Override
    public void bootstrap() {
        List<PluginMetadata> discovered = discover();
        List<PluginMetadata> resolved = resolve(discovered);
        List<PluginWrapper> loaded = load(resolved);

        for (PluginWrapper wrapper : loaded) {
            initialize(wrapper.getPluginId());
        }

        autoStartIfNecessary();
    }

    /**
     * 根据需要自动启动插件。
     */
    protected abstract void autoStartIfNecessary();

    @Override
    public List<PluginMetadata> discover() {
        eventBus.publish(new PluginProcessEvent(
                PluginProcessEvent.Phase.DISCOVER,
                PluginProcessEvent.Stage.START,
                0));

        List<PluginMetadata> candidates = pluginDiscoverer.discover();

        eventBus.publish(new PluginProcessEvent(
                PluginProcessEvent.Phase.DISCOVER,
                PluginProcessEvent.Stage.END,
                candidates.size()));

        return candidates;
    }

    @Override
    public List<PluginMetadata> resolve(List<PluginMetadata> discovered) {
        eventBus.publish(new PluginProcessEvent(
                PluginProcessEvent.Phase.RESOLVE,
                PluginProcessEvent.Stage.START,
                discovered.size()));

        List<PluginMetadata> resolved = pluginResolver.resolve(discovered);

        eventBus.publish(new PluginProcessEvent(
                PluginProcessEvent.Phase.RESOLVE,
                PluginProcessEvent.Stage.END,
                resolved.size()));

        return resolved;
    }

    @Override
    public List<PluginWrapper> load(List<PluginMetadata> resolved) {
        eventBus.publish(new PluginProcessEvent(
                PluginProcessEvent.Phase.LOAD,
                PluginProcessEvent.Stage.START,
                resolved.size()));

        List<PluginWrapper> wrappers = new ArrayList<>();

        for (PluginMetadata metadata : resolved) {
            String pluginId = metadata.pluginId();

            if (pluginRegistry.contains(pluginId)) {
                log.debug("插件已存在，跳过加载: {}", pluginId);
                continue;
            }

            // 版本兼容性检查
            String requiredVersion = metadata.descriptor().coreVersion();
            if (!versionManager.isCompatible(coreVersion, requiredVersion)) {
                log.warn("插件版本不兼容，跳过加载: {} (需要版本: {}, 当前版本: {})", 
                        pluginId, requiredVersion, coreVersion);
                continue;
            }

            try {
                PluginWrapper wrapper = doLoad(metadata);
                transition(wrapper, PluginState.LOADED);
                pluginRegistry.register(wrapper);
                wrappers.add(wrapper);
            } catch (Exception e) {
                log.error("插件加载失败: {} (来源: {}, 类型: {})", 
                        pluginId, metadata.sourcePath(), metadata.sourceType(), e);
                eventBus.publish(new PluginFailureEvent(metadata.descriptor(), e));
            }
        }

        eventBus.publish(new PluginProcessEvent(
                PluginProcessEvent.Phase.LOAD,
                PluginProcessEvent.Stage.END,
                wrappers.size()));

        return wrappers;
    }

    /**
     * 执行实际的加载操作。
     * <p>子类实现此方法完成具体的加载逻辑。</p>
     *
     * @param metadata 插件元数据
     * @return 加载后的插件包装对象
     */
    protected abstract PluginWrapper doLoad(PluginMetadata metadata);

    @Override
    public void initialize(String pluginId) {
        PluginWrapper wrapper = require(pluginId);
        Plugin plugin = wrapper.plugin();

        if (plugin != null) {
            try {
                plugin.onInitialize(wrapper.createContext(
                        extensionRegistry, eventBus::publish, runtimeMode, coreVersion));
                transition(wrapper, PluginState.INITIALIZED);
            } catch (Exception e) {
                handleFailure(wrapper, e);
                throw new PluginLoadException("插件初始化失败: " + pluginId, e);
            }
        } else {
            transition(wrapper, PluginState.INITIALIZED);
        }
    }

    @Override
    public void start(String pluginId) {
        PluginWrapper wrapper = require(pluginId);

        if (wrapper.state() == PluginState.ACTIVE) {
            return;
        }

        // 禁用状态的插件不允许启动
        if (wrapper.state() == PluginState.DISABLED) {
            return;
        }

        // 如果处于 STOPPED 状态，需要先迁移到 STARTING
        if (wrapper.state() == PluginState.STOPPED) {
            transition(wrapper, PluginState.STARTING);
        } else if (wrapper.state() == PluginState.INITIALIZED) {
            transition(wrapper, PluginState.STARTING);
        }

        Plugin plugin = wrapper.plugin();
        if (plugin != null) {
            try {
                plugin.onStart();
                transition(wrapper, PluginState.ACTIVE);
            } catch (Exception e) {
                handleFailure(wrapper, e);
                throw new PluginLoadException("插件启动失败: " + pluginId, e);
            }
        } else {
            transition(wrapper, PluginState.ACTIVE);
        }
    }

    @Override
    public void stop(String pluginId) {
        PluginWrapper wrapper = require(pluginId);

        if (wrapper.state() != PluginState.ACTIVE) {
            return;
        }

        transition(wrapper, PluginState.STOPPING);

        Plugin plugin = wrapper.plugin();
        if (plugin != null) {
            try {
                plugin.onStop();
            } catch (Exception e) {
                handleFailure(wrapper, e);
                throw new PluginLoadException("插件停止失败: " + pluginId, e);
            }
        }

        transition(wrapper, PluginState.STOPPED);
    }

    @Override
    public void unload(String pluginId) {
        PluginWrapper wrapper = require(pluginId);

        // 1. 如果处于活跃状态，先执行停止
        if (wrapper.state() == PluginState.ACTIVE) {
            try {
                stop(pluginId);
            } catch (Exception ignored) {
                // 卸载时忽略停止失败，强制继续
            }
        }

        // 2. 调用插件的 onUnload 生命周期方法
        Plugin plugin = wrapper.plugin();
        if (plugin != null) {
            try {
                plugin.onUnload();
            } catch (Exception ignored) {
                // 卸载时忽略插件逻辑错误
            }
        }

        // 3. 从扩展注册中心清理该插件注册的所有扩展
        extensionRegistry.unregisterByPluginId(pluginId);

        // 4. 从事件总线取消该插件的所有监听器
        eventBus.unsubscribeByPlugin(pluginId);

        // 5. 释放 JVM 资源
        closeIfPossible(wrapper.classLoader());

        // 6. 迁移到 UNLOADED 状态
        transition(wrapper, PluginState.UNLOADED);
    }

    @Override
    public void delete(String pluginId) {
        PluginWrapper wrapper = require(pluginId);

        // 执行物理删除（删除文件）
        PluginLoader loader = pluginLoaders.remove(pluginId);
        if (loader != null && loader.supportsRemove()) {
            loader.remove(wrapper);
        }

        // 从注册中心移除
        pluginRegistry.unregister(pluginId);

        eventBus.publish(new PluginProcessEvent(
                PluginProcessEvent.Phase.DELETE,
                PluginProcessEvent.Stage.END,
                1));
    }

    @Override
    public void enable(String pluginId) {
        PluginWrapper wrapper = require(pluginId);

        if (wrapper.state() != PluginState.DISABLED) {
            return;
        }

        disabledPlugins.remove(pluginId);

        transition(wrapper, PluginState.STARTING);

        Plugin plugin = wrapper.plugin();
        if (plugin != null) {
            try {
                plugin.onEnable();
                plugin.onStart();
            } catch (Exception e) {
                handleFailure(wrapper, e);
                throw new PluginLoadException("插件启用失败: " + pluginId, e);
            }
        }

        transition(wrapper, PluginState.ACTIVE);
    }

    @Override
    public void disable(String pluginId) {
        PluginWrapper wrapper = require(pluginId);

        if (wrapper.state() == PluginState.ACTIVE) {
            stop(pluginId);
        }

        transition(wrapper, PluginState.DISABLED);

        Plugin plugin = wrapper.plugin();
        if (plugin != null) {
            try {
                plugin.onDisable();
            } catch (Exception e) {
                handleFailure(wrapper, e);
                throw new PluginLoadException("插件禁用失败: " + pluginId, e);
            }
        }

        disabledPlugins.add(pluginId);
    }

    @Override
    public boolean isEnabled(String pluginId) {
        return !disabledPlugins.contains(pluginId);
    }

    @Override
    public void upgradeCold(String pluginId, PluginMetadata newVersion) {
        // 留给子类实现
        throw new UnsupportedOperationException("冷升级功能尚未实现");
    }

    @Override
    public void upgradeHot(String pluginId, PluginMetadata newVersion) {
        // 留给子类实现
        throw new UnsupportedOperationException("热升级功能尚未实现");
    }

    @Override
    public PluginState getState(String pluginId) {
        return require(pluginId).state();
    }

    @Override
    public Collection<PluginWrapper> listByState(PluginState state) {
        return pluginRegistry.list().stream()
                .filter(w -> w.state() == state)
                .toList();
    }

    @Override
    public boolean isActive(String pluginId) {
        PluginWrapper wrapper = pluginRegistry.get(pluginId);
        return wrapper != null && wrapper.state() == PluginState.ACTIVE;
    }

    @Override
    public void startAll(List<PluginWrapper> plugins) {
        for (PluginWrapper wrapper : plugins) {
            try {
                start(wrapper.getPluginId());
            } catch (Exception ignored) {
                // 批量启动时忽略单个失败
            }
        }
    }

    @Override
    public void stopAll(List<PluginWrapper> plugins) {
        for (PluginWrapper wrapper : plugins) {
            try {
                stop(wrapper.getPluginId());
            } catch (Exception ignored) {
                // 批量停止时忽略单个失败
            }
        }
    }

    @Override
    public void reloadFailed(List<PluginWrapper> plugins) {
        for (PluginWrapper wrapper : plugins) {
            try {
                // 尝试重新加载失败的插件
                unload(wrapper.getPluginId());
            } catch (Exception ignored) {
                // 忽略重试失败
            }
        }
    }

    /**
     * 获取指定插件，如果不存在则抛出异常。
     *
     * @param pluginId 插件唯一标识
     * @return 插件包装对象
     * @throws PluginLoadException 如果插件不存在
     */
    protected PluginWrapper require(String pluginId) {
        PluginWrapper wrapper = pluginRegistry.get(pluginId);
        if (wrapper == null) {
            throw new PluginLoadException("插件不存在: " + pluginId);
        }
        return wrapper;
    }

    /**
     * 尝试关闭类加载器。
     *
     * @param classLoader 类加载器
     */
    protected void closeIfPossible(ClassLoader classLoader) {
        if (classLoader instanceof Closeable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // 忽略关闭异常
            }
        }
    }
}
