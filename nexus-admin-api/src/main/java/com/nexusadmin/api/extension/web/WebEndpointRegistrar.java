package com.nexusadmin.api.extension.web;

/**
 * Web 端点注册器。
 * <p>
 * 负责将插件中的控制器动态注册到当前使用的 Web 框架，
 * 并在插件卸载时清理已注册的端点。
 */
public interface WebEndpointRegistrar {

    /**
     * 为指定插件注册一个控制器提供的所有端点。
     *
     * @param pluginId   插件唯一标识
     * @param controller 控制器实例
     */
    void register(String pluginId, Object controller);

    /**
     * 卸载指定插件注册的所有端点。
     *
     * @param pluginId 插件唯一标识
     */
    void unregister(String pluginId);
}
