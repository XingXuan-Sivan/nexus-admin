package com.nexusadmin.api;

/**
 * 插件生命周期接口，所有可被平台加载的插件都需要实现该接口。
 * <p>通过 {@link #start(PluginContext)} 和 {@link #stop(PluginContext)} 方法完成扩展点注册与资源管理。</p>
 *
 * @author NexusAdmin
 * @since 1.0.0
 */
public interface Plugin {

    /**
     * 返回插件自身的描述信息，通常与插件打包时的 plugin.json 一一对应。
     *
     * @return 插件描述
     */
    PluginDescriptor descriptor();

    /**
     * 插件安装阶段回调。
     * <p>此时插件已被加载到内存，但尚未启动。用于初始化一次性资源。</p>
     *
     * @param context 插件运行上下文
     */
    default void install(PluginContext context) {
    }

    /**
     * 插件启动入口，在此方法中可以通过 {@link PluginContext#extensionRegistry()} 注册各类扩展点实现。
     *
     * @param context 插件运行上下文，包含描述信息、扩展注册中心、类加载器等
     */
    void start(PluginContext context);

    /**
     * 插件停止时调用的回调方法，用于释放资源或从扩展注册中心注销实现。
     *
     * @param context 插件运行上下文
     */
    void stop(PluginContext context);

    /**
     * 插件卸载阶段回调。
     * <p>用于清理外部副作用或删除临时文件。</p>
     *
     * @param context 插件运行上下文
     */
    default void uninstall(PluginContext context) {
    }
}
