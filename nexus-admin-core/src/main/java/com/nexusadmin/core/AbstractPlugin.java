package com.nexusadmin.core;

import com.nexusadmin.core.config.ConfigManager;
import com.nexusadmin.core.context.PlatformAccess;
import com.nexusadmin.core.context.PlatformServices;
import com.nexusadmin.core.context.PluginContext;
import com.nexusadmin.core.context.PluginInfo;
import com.nexusadmin.core.context.PluginRuntime;
import com.nexusadmin.core.context.PluginWorkspace;
import com.nexusadmin.core.event.EventPublisher;
import com.nexusadmin.core.extension.ExtensionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 插件抽象基类，提供上下文封装和生命周期模板方法。
 * <p>
 * 继承此类的插件可直接通过受保护的方法访问插件上下文、扩展注册中心、事件发布器等平台能力，
 * 无需在每个生命周期方法中手动传递 PluginContext 参数。
 * </p>
 * <p>
 * 生命周期模板方法设计：
 * </p>
 * <ul>
 *   <li>{@link #onInitialize} 标记为 final，负责缓存上下文并调用 {@link #initialize()}</li>
 *   <li>{@link #onStart} 标记为 final，负责缓存上下文并调用 {@link #start()}</li>
 *   <li>{@link #onStop} 标记为 final，负责缓存上下文并调用 {@link #stop()}</li>
 *   <li>{@link #onUnload} 标记为 final，负责缓存上下文并调用 {@link #unload()}</li>
 * </ul>
 * <p>
 * 插件实现类只需覆写 {@link #initialize()}、{@link #start()}、{@link #stop()}、{@link #unload()} 等方法，
 * 即可在生命周期回调内通过 {@link #context()} 等便捷方法访问平台能力。
 * </p>
 */
public abstract class AbstractPlugin implements Plugin {

    /** SLF4J 日志记录器，子类可继承使用 */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 当前插件运行上下文。
     * <p>在生命周期回调时由平台注入，生命周期外访问将抛出异常。</p>
     */
    private PluginContext context;

    /**
     * 初始化阶段回调。
     * <p>状态迁移：LOADED → INITIALIZED</p>
     * <p>缓存插件上下文并调用 {@link #initialize()} 模板方法。</p>
     *
     * @param context 插件运行上下文
     * @throws Exception 初始化过程中发生异常
     */
    @Override
    public final void onInitialize(PluginContext context) throws Exception {
        this.context = context;
        log.debug("插件 [{}] 正在初始化", pluginId());
        initialize();
        log.debug("插件 [{}] 初始化完成", pluginId());
    }

    /**
     * 启动阶段回调。
     * <p>状态迁移：STARTING → ACTIVE</p>
     * <p>缓存插件上下文并调用 {@link #start()} 模板方法。</p>
     *
     * @param context 插件运行上下文
     * @throws Exception 启动过程中发生异常
     */
    @Override
    public final void onStart(PluginContext context) throws Exception {
        this.context = context;
        log.info("插件 [{}] 正在启动", pluginId());
        start();
        log.info("插件 [{}] 启动完成", pluginId());
    }

    /**
     * 停止阶段回调。
     * <p>状态迁移：ACTIVE → STOPPING</p>
     * <p>缓存插件上下文并调用 {@link #stop()} 模板方法。</p>
     *
     * @param context 插件运行上下文
     * @throws Exception 停止过程中发生异常
     */
    @Override
    public final void onStop(PluginContext context) throws Exception {
        this.context = context;
        log.info("插件 [{}] 正在停止", pluginId());
        stop();
        log.info("插件 [{}] 停止完成", pluginId());
    }

    /**
     * 卸载阶段回调。
     * <p>状态迁移：STOPPED → UNLOADED</p>
     * <p>缓存插件上下文并调用 {@link #unload()} 模板方法。</p>
     *
     * @param context 插件运行上下文
     * @throws Exception 卸载过程中发生异常
     */
    @Override
    public final void onUnload(PluginContext context) throws Exception {
        this.context = context;
        log.debug("插件 [{}] 正在卸载", pluginId());
        unload();
        log.debug("插件 [{}] 卸载完成", pluginId());
    }

    // ==================== 生命周期模板方法 ====================

    /**
     * 初始化阶段模板方法。
     * <p>子类可覆写此方法实现初始化逻辑，如加载配置、验证运行环境等。</p>
     *
     * @throws Exception 初始化过程中发生异常
     */
    protected void initialize() throws Exception {
        // 默认无操作
    }

    /**
     * 启动阶段模板方法。
     * <p>子类可覆写此方法实现启动逻辑，如注册扩展点实现、启动后台服务等。</p>
     *
     * @throws Exception 启动过程中发生异常
     */
    protected void start() throws Exception {
        // 默认无操作
    }

    /**
     * 停止阶段模板方法。
     * <p>子类可覆写此方法实现停止逻辑，如注销扩展点实现、停止后台服务、释放资源等。</p>
     *
     * @throws Exception 停止过程中发生异常
     */
    protected void stop() throws Exception {
        // 默认无操作
    }

    /**
     * 卸载阶段模板方法。
     * <p>子类可覆写此方法实现卸载逻辑，如清理外部状态、删除临时文件等。</p>
     *
     * @throws Exception 卸载过程中发生异常
     */
    protected void unload() throws Exception {
        // 默认无操作
    }

    // ==================== 上下文快捷访问 ====================

    /**
     * 获取插件运行上下文。
     * <p>只能在生命周期回调内调用，否则将抛出异常。</p>
     *
     * @return 插件运行上下文
     * @throws IllegalStateException 如果在生命周期回调外调用
     */
    protected final PluginContext context() {
        if (context == null) {
            throw new IllegalStateException("插件上下文尚未初始化，请在生命周期回调内访问");
        }
        return context;
    }

    /**
     * 获取插件静态信息。
     *
     * @return 插件信息
     */
    protected final PluginInfo info() {
        return context().info();
    }

    /**
     * 获取插件唯一标识。
     *
     * @return 插件ID
     */
    protected final String pluginId() {
        return info().id();
    }

    /**
     * 获取插件版本号。
     *
     * @return 版本号
     */
    protected final String version() {
        return info().version();
    }

    /**
     * 获取插件运行时状态访问。
     *
     * @return 插件运行时
     */
    protected final PluginRuntime runtime() {
        return context().runtime();
    }

    /**
     * 获取插件运行时工作空间。
     *
     * @return 插件工作空间
     */
    protected final PluginWorkspace workspace() {
        return context().workspace();
    }

    /**
     * 获取平台能力访问。
     *
     * @return 平台访问
     */
    protected final PlatformAccess platform() {
        return context().platform();
    }

    /**
     * 获取扩展注册中心。
     *
     * @return 扩展注册中心
     */
    protected final ExtensionRegistry extensions() {
        return platform().extensions();
    }

    /**
     * 获取事件发布者。
     *
     * @return 事件发布者
     */
    protected final EventPublisher events() {
        return platform().events();
    }

    /**
     * 获取配置管理器。
     * <p>注意：配置管理器可能为空，取决于平台是否启用了配置中心。</p>
     *
     * @return 配置管理器，可能为 null
     */
    protected final ConfigManager config() {
        return platform().config();
    }

    /**
     * 获取平台服务注册中心。
     *
     * @return 平台服务注册中心
     */
    protected final PlatformServices services() {
        return platform().services();
    }

    /**
     * 获取指定类型的服务实例。
     * <p>这是 {@link #services()} 的便捷方法。</p>
     *
     * @param <T>  服务类型
     * @param type 服务接口类型
     * @return 服务实例的 Optional，如果未注册则返回空
     */
    protected final <T> Optional<T> service(Class<T> type) {
        return platform().service(type);
    }
}
