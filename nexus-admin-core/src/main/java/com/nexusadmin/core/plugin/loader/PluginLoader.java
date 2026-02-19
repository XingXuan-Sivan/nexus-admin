package com.nexusadmin.core.plugin.loader;

import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.registry.Composable;

/**
 * 插件加载器，负责将候选插件加载为可运行状态。
 * <p>专注类加载和实例化，不参与发现阶段。</p>
 * <p>实现 {@link Composable} 以支持注册中心统一管理。</p>
 */
public interface PluginLoader extends Composable {

    /**
     * 检查是否支持加载该候选插件。
     *
     * @param candidate 候选插件
     * @return 如果支持则返回 true
     */
    boolean supports(CandidatePlugin candidate);

    /**
     * 加载候选插件，返回已加载插件对象。
     *
     * @param candidate 候选插件
     * @param registry  扩展注册中心
     * @return 已加载插件
     */
    LoadedPlugin load(CandidatePlugin candidate, ExtensionRegistry registry);

    /**
     * 是否支持物理卸载。
     * <p>classpath 等只读插件应返回 false。</p>
     *
     * @return 如果支持物理卸载则返回 true
     */
    default boolean supportsRemove() {
        return false;
    }

    /**
     * 执行物理卸载。
     *
     * @param plugin 已加载插件
     */
    default void remove(LoadedPlugin plugin) {
        throw new UnsupportedOperationException("不支持物理卸载");
    }
}
