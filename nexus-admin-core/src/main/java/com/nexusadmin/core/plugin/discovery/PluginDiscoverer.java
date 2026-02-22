package com.nexusadmin.core.plugin.discovery;

import com.nexusadmin.core.plugin.loader.PluginMetadata;

import java.util.List;

/**
 * 插件发现器接口，负责从不同来源发现候选插件。
 * <p>将发现逻辑从 PluginManager 中抽离，支持可替换的实现。</p>
 */
public interface PluginDiscoverer {

    /**
     * 从所有注册的源发现候选插件。
     *
     * @return 候选插件列表，不会返回 null
     */
    List<PluginMetadata> discover();
}
