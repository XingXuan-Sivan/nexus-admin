package com.nexusadmin.core.plugin.loader;

/**
 * 插件加载器策略接口。
 * <p>负责将已解析的插件元数据转换为可运行的插件实例。</p>
 */
public interface PluginLoader {

    /**
     * 加载候选插件，返回已加载插件对象。
     *
     * @param metadata 插件元数据
     * @return 已加载插件包装对象
     */
    PluginWrapper load(PluginMetadata metadata);
}
