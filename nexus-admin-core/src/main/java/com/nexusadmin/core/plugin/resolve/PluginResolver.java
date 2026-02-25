package com.nexusadmin.core.plugin.resolve;

import com.nexusadmin.core.plugin.loader.PluginMetadata;

import java.util.List;

/**
 * 插件解析器接口，负责处理候选插件的冲突和依赖。
 * <p>将解析逻辑从 PluginManager 中抽离，支持可替换的实现。</p>
 */
public interface PluginResolver {

    /**
     * 解析候选插件，处理冲突和依赖。
     *
     * @param candidates 候选插件列表
     * @return 经过裁决后的有效插件列表
     */
    List<PluginMetadata> resolve(List<PluginMetadata> candidates);
}
