package com.nexusadmin.core.plugin.event;

import com.nexusadmin.core.registry.Composable;

/**
 * 插件生命周期监听器。
 * <p>用于监听和响应插件生命周期各阶段的事件。</p>
 * <p>实现 {@link Composable} 以支持注册中心统一管理。</p>
 */
@FunctionalInterface
public interface PluginLifecycleListener extends Composable {

    /**
     * 当生命周期事件发生时调用。
     *
     * @param event 生命周期事件
     */
    void onEvent(PluginLifecycleEvent event);
}
