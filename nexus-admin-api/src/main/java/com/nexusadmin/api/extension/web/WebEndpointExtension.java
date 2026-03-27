package com.nexusadmin.api.extension.web;

import com.nexusadmin.core.extension.ExtensionPoint;

/**
 * Web 接入统一扩展点。
 * <p>
 * 每个实现负责在插件生命周期内，为指定插件注册和卸载 Web 端点。
 * 平台提供默认的 Spring MVC 实现，插件也可通过实现此扩展点来定制 Web 接入行为。
 */
public interface WebEndpointExtension extends ExtensionPoint {

    /**
     * 在插件激活时，为该插件注册所有 Web 端点。
     *
     * @param context 注册上下文
     */
    void registerEndpoints(Context context);

    /**
     * 在插件停止前，为该插件卸载所有已注册的 Web 端点。
     *
     * @param context 注册上下文
     */
    void unregisterEndpoints(Context context);

    /**
     * Web 端点注册上下文。
     * <p>
     * 封装了 Web 端点注册所需的所有上下文信息，包括插件标识、控制器提供者、
     * 注册器、解析器和注册表。
     *
     * @param pluginId         插件唯一标识
     * @param plugin           插件实例
     * @param controllerProvider 插件的 Web 控制器提供者，可为 null
     * @param registrar        Web 端点注册器
     * @param mappingResolver  映射解析器
     * @param registry         插件 Web 端点注册表
     */
    record Context(
            String pluginId,
            Object plugin,
            WebControllerProvider controllerProvider,
            WebEndpointRegistrar registrar,
            MappingResolver mappingResolver,
            PluginWebRegistry registry
    ) {
    }
}
