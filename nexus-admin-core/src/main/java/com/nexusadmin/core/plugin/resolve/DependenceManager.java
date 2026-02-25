package com.nexusadmin.core.plugin.resolve;

import com.nexusadmin.core.plugin.loader.PluginMetadata;

import java.util.List;

/**
 * 依赖管理器接口，负责插件依赖的校验与排序。
 * <p>提供依赖拓扑排序和循环依赖检测能力。</p>
 */
public interface DependenceManager {

    /**
     * 校验插件依赖是否满足。
     * <p>检查所有插件的依赖是否在候选列表中存在，以及版本是否兼容。</p>
     *
     * @param plugins 待校验的插件列表
     * @throws com.nexusadmin.core.exception.PluginException 当依赖不满足时抛出
     */
    void validateDependencies(List<PluginMetadata> plugins);

    /**
     * 按依赖关系对插件进行拓扑排序。
     * <p>确保被依赖的插件排在依赖它的插件之前。</p>
     *
     * @param plugins 待排序的插件列表
     * @return 排序后的插件列表
     * @throws com.nexusadmin.core.exception.PluginException 当存在循环依赖时抛出
     */
    List<PluginMetadata> sortByDependency(List<PluginMetadata> plugins);
}
