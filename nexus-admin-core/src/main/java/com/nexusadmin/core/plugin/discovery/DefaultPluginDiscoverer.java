package com.nexusadmin.core.plugin.discovery;

import com.nexusadmin.core.plugin.loader.PluginMetadata;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 默认插件发现器实现。
 * <p>从所有注册的插件源扫描并收集候选插件。</p>
 */
public class DefaultPluginDiscoverer implements PluginDiscoverer {

    private final List<PluginSource> sources;
    private final List<PluginDescriptorFinder> finders;
    private final List<PluginDescriptorParser> parsers;

    /**
     * 构造插件发现器。
     *
     * @param sources 插件源列表
     * @param finders 描述文件查找器列表
     * @param parsers 描述文件解析器列表
     */
    public DefaultPluginDiscoverer(List<PluginSource> sources,
                                   List<PluginDescriptorFinder> finders,
                                   List<PluginDescriptorParser> parsers) {
        this.sources = List.copyOf(sources != null ? sources : List.of());
        this.finders = List.copyOf(finders != null ? finders : List.of());
        this.parsers = List.copyOf(parsers != null ? parsers : List.of());
    }

    @Override
    public List<PluginMetadata> discover() {
        return sources.stream()
                .flatMap(s -> s.scan().stream())
                .collect(Collectors.toList());
    }
}
