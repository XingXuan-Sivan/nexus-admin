package com.nexusadmin.core;

import com.nexusadmin.core.context.PluginContext;

/**
 * 插件生命周期接口，所有可被平台加载的插件都需要实现该接口。
 * <p>通过生命周期钩子方法完成扩展点注册、资源管理和状态控制。</p>
 */
public interface Plugin {

    /**
     * 插件初始化阶段回调。
     * <p>状态迁移：LOADED → INITIALIZED</p>
     * <p>此时插件类已加载，在此方法中完成初始化操作。</p>
     *
     * @param context 插件运行上下文
     * @throws Exception 初始化过程中发生异常
     */
    void onInitialize(PluginContext context) throws Exception;

    /**
     * 插件启动入口。
     * <p>状态迁移：STARTING → ACTIVE</p>
     * <p>在此方法中注册扩展点实现、启动后台服务等。</p>
     *
     * @throws Exception 启动过程中发生异常
     */
    void onStart() throws Exception;

    /**
     * 插件停止时调用的回调方法。
     * <p>状态迁移：ACTIVE → STOPPING</p>
     * <p>用于释放资源或从扩展注册中心注销实现。</p>
     *
     * @throws Exception 停止过程中发生异常
     */
    void onStop() throws Exception;

    /**
     * 插件卸载阶段回调。
     * <p>状态迁移：STOPPED → UNLOADED</p>
     * <p>用于清理外部副作用或删除临时文件。</p>
     *
     * @throws Exception 卸载过程中发生异常
     */
    void onUnload() throws Exception;

    /**
     * 热升级钩子（可选）。
     * <p>状态：UPGRADING</p>
     * <p>在热升级过程中调用，用于数据迁移或状态同步。</p>
     *
     * @param newContext 新版本插件的运行上下文
     * @throws Exception 升级过程中发生异常
     */
    default void onUpgrade(PluginContext newContext) throws Exception {
        // 默认无操作
    }

    /**
     * 禁用通知（可选）。
     * <p>状态迁移：ACTIVE → DISABLED</p>
     * <p>插件被禁用时调用。</p>
     *
     * @throws Exception 禁用过程中发生异常
     */
    default void onDisable() throws Exception {
        // 默认无操作
    }

    /**
     * 启用通知（可选）。
     * <p>状态迁移：DISABLED → STARTING</p>
     * <p>插件被重新启用时调用。</p>
     *
     * @throws Exception 启用过程中发生异常
     */
    default void onEnable() throws Exception {
        // 默认无操作
    }
}
