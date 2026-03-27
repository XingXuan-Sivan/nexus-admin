package com.nexusadmin.core;

import com.nexusadmin.core.config.ConfigManager;
import com.nexusadmin.core.context.PlatformAccess;
import com.nexusadmin.core.context.PlatformServices;
import com.nexusadmin.core.context.PluginContext;
import com.nexusadmin.core.context.PluginInfo;
import com.nexusadmin.core.context.PluginRuntime;
import com.nexusadmin.core.context.PluginWorkspace;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.exception.PluginLoadException;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.plugin.RuntimeMode;
import com.nexusadmin.core.plugin.discovery.PluginDiscoverer;
import com.nexusadmin.core.plugin.event.PluginStateChangedEvent;
import com.nexusadmin.core.plugin.event.PluginProcessEvent;
import com.nexusadmin.core.plugin.event.PluginFailureEvent;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.loader.PluginMetadata;
import com.nexusadmin.core.plugin.loader.PluginWrapper;
import com.nexusadmin.core.plugin.PluginRegistry;
import com.nexusadmin.core.plugin.resolve.PluginResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    protected final RuntimeMode runtimeMode;
    protected final String coreVersion;
    protected final Path pluginsDataRoot;

    protected PluginDiscoverer pluginDiscoverer;
    protected PluginResolver pluginResolver;
    protected PluginLoader pluginLoader;

    // ===== 配置中心相关 =====
    protected ConfigManager configManager;

    // ===== 平台服务注册中心 =====
    protected final PlatformServices platformServices = new PlatformServices();

    /**
     * 插件上下文缓存，用于生命周期方法调用。
     */
    private final ConcurrentHashMap<String, PluginContext> pluginContexts = new ConcurrentHashMap<>();

    /**
     * 生命周期专用锁，保证状态迁移原子性。
     */
    protected final Object lifecycleLock = new Object();

    /**
     * 构造抽象插件管理器。
     * <p>子类通过实现工厂方法提供 Discoverer、Resolver 和 Loader。</p>
     *
     * @param pluginRegistry    插件注册中心
     * @param extensionRegistry 扩展注册中心
     * @param eventBus          事件总线
     * @param runtimeMode       运行模式
     * @param coreVersion       核心版本号
     * @param pluginsDataRoot   插件数据根目录
     */
    protected AbstractPluginManager(PluginRegistry pluginRegistry,
                                    ExtensionRegistry extensionRegistry,
                                    EventBus eventBus,
                                    RuntimeMode runtimeMode,
                                    String coreVersion,
                                    Path pluginsDataRoot) {
        this.pluginRegistry = Objects.requireNonNull(pluginRegistry, "插件注册中心不能为空");
        this.extensionRegistry = Objects.requireNonNull(extensionRegistry, "扩展注册中心不能为空");
        this.eventBus = Objects.requireNonNull(eventBus, "事件总线不能为空");
        this.runtimeMode = Objects.requireNonNull(runtimeMode, "运行模式不能为空");
        this.coreVersion = Objects.requireNonNull(coreVersion, "核心版本号不能为空");
        this.pluginsDataRoot = Objects.requireNonNull(pluginsDataRoot, "插件数据根目录不能为空");
    }

    /**
     * 初始化插件组件。
     * <p>子类应在构造函数中调用此方法，完成 Discoverer、Resolver 和 Loader 的初始化。</p>
     */
    protected void initializeComponents() {
        this.pluginDiscoverer = createPluginDiscoverer();
        this.pluginResolver = createPluginResolver();
        this.pluginLoader = createPluginLoader();
    }

    /**
     * 创建插件发现器。
     *
     * @return 插件发现器实例
     */
    protected abstract PluginDiscoverer createPluginDiscoverer();

    /**
     * 创建插件解析器。
     *
     * @return 插件解析器实例
     */
    protected abstract PluginResolver createPluginResolver();

    /**
     * 创建插件加载器。
     *
     * @return 插件加载器实例
     */
    protected abstract PluginLoader createPluginLoader();

    /**
     * 创建插件上下文。
     * <p>在插件初始化前调用，构造四层模型上下文。</p>
     *
     * @param wrapper 插件包装对象
     * @return 插件上下文实例
     */
    protected PluginContext createPluginContext(PluginWrapper wrapper) {
        String pluginId = wrapper.getPluginId();

        // 构建四层模型
        PluginInfo info = new PluginInfo(
                wrapper.descriptor(),
                wrapper.classLoader(),
                wrapper.physicalPath()
        );

        PluginRuntime runtime = new PluginRuntime(wrapper::state);

        // Workspace 采用懒加载，首次访问时才创建目录
        PluginWorkspace workspace = new PluginWorkspace(pluginsDataRoot.resolve("workspace").resolve(pluginId));

        PlatformAccess platform = new PlatformAccess(
                extensionRegistry,
                eventBus::publish,
                runtimeMode,
                coreVersion,
                configManager,
                platformServices
        );

        return new PluginContext(info, runtime, workspace, platform);
    }

    /**
     * 获取或创建插件上下文。
     *
     * @param wrapper 插件包装对象
     * @return 插件上下文实例
     */
    protected PluginContext getOrCreateContext(PluginWrapper wrapper) {
        return pluginContexts.computeIfAbsent(wrapper.getPluginId(), id -> createPluginContext(wrapper));
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

            try {
                PluginWrapper wrapper = pluginLoader.load(metadata);
                transition(wrapper, PluginState.LOADED);
                pluginRegistry.register(wrapper);

                // 加载并注册 Schema
                loadAndRegisterSchema(wrapper);

                wrappers.add(wrapper);
            } catch (Exception e) {
                log.error("插件加载失败: {} (来源类型: {})",
                        pluginId, metadata.sourceType(), e);
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
     * 加载并注册插件 Schema。
     *
     * @param wrapper 插件包装对象
     */
    protected void loadAndRegisterSchema(PluginWrapper wrapper) {
        if (configManager == null) {
            return;
        }

        try {
            String pluginId = wrapper.getPluginId();
            ClassLoader classLoader = wrapper.classLoader();
            configManager.registerPlugin(pluginId, classLoader);
        } catch (Exception e) {
            log.warn("加载插件 Schema 失败: {}", wrapper.getPluginId(), e);
        }
    }

    @Override
    public void initialize(String pluginId) {
        PluginWrapper wrapper = require(pluginId);
        Plugin plugin = wrapper.plugin();

        if (plugin != null) {
            try {
                PluginContext context = getOrCreateContext(wrapper);
                plugin.onInitialize(context);
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
                PluginContext context = getOrCreateContext(wrapper);
                plugin.onStart(context);
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
                PluginContext context = getOrCreateContext(wrapper);
                plugin.onStop(context);
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
                PluginContext context = getOrCreateContext(wrapper);
                plugin.onUnload(context);
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

        // 6. 移除上下文缓存
        pluginContexts.remove(pluginId);

        // 7. 迁移到 UNLOADED 状态
        transition(wrapper, PluginState.UNLOADED);
    }

    @Override
    public void delete(String pluginId) {
        PluginWrapper wrapper = require(pluginId);

        // 执行物理删除（删除文件）
        if (wrapper.supportsPhysicalRemoval()) {
            wrapper.removePhysically();
        }

        // 删除插件 workspace
        PluginContext context = pluginContexts.get(pluginId);
        if (context != null) {
            try {
                context.workspace().delete();
            } catch (Exception e) {
                log.warn("删除插件 workspace 失败: {}", pluginId, e);
            }
        }

        // 从注册中心移除
        pluginRegistry.unregister(pluginId);

        // 移除上下文缓存
        pluginContexts.remove(pluginId);

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

        transition(wrapper, PluginState.STARTING);

        // 移除禁用状态持久化
        if (configManager != null) {
            configManager.setPluginDisabled(pluginId, false);
        }

        Plugin plugin = wrapper.plugin();
        if (plugin != null) {
            try {
                PluginContext context = getOrCreateContext(wrapper);
                plugin.onEnable(context);
                plugin.onStart(context);
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

        // 持久化禁用状态
        if (configManager != null) {
            configManager.setPluginDisabled(pluginId, true);
        }

        Plugin plugin = wrapper.plugin();
        if (plugin != null) {
            try {
                PluginContext context = getOrCreateContext(wrapper);
                plugin.onDisable(context);
            } catch (Exception e) {
                handleFailure(wrapper, e);
                throw new PluginLoadException("插件禁用失败: " + pluginId, e);
            }
        }
    }

    @Override
    public boolean isEnabled(String pluginId) {
        PluginWrapper wrapper = pluginRegistry.get(pluginId);
        return wrapper != null && wrapper.state() != PluginState.DISABLED;
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

    /**
     * 获取平台服务注册中心。
     * <p>宿主应用通过此方法注册服务供插件使用。</p>
     *
     * @return 平台服务注册中心
     */
    public PlatformServices platformServices() {
        return platformServices;
    }
}
