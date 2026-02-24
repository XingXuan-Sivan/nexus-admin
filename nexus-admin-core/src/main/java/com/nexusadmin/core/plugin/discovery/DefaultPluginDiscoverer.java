package com.nexusadmin.core.plugin.discovery;

import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.plugin.RuntimeMode;
import com.nexusadmin.core.plugin.event.PluginProcessEvent;
import com.nexusadmin.core.plugin.loader.PluginMetadata;
import com.nexusadmin.core.plugin.source.PluginSource;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 默认插件发现器实现。
 * <p>从所有注册的插件源扫描并收集候选插件。</p>
 */
public class DefaultPluginDiscoverer implements PluginDiscoverer {

    private final List<PluginSource> sources;
    private final EventBus eventBus;
    private final String coreVersion;
    private final RuntimeMode runtimeMode;

    /**
     * 构造插件发现器。
     *
     * @param sources     插件源列表
     * @param eventBus    事件总线
     * @param coreVersion 核心版本号
     * @param runtimeMode 运行模式
     */
    public DefaultPluginDiscoverer(List<PluginSource> sources, EventBus eventBus,
                                   String coreVersion, RuntimeMode runtimeMode) {
        this.sources = List.copyOf(sources != null ? sources : List.of());
        this.eventBus = Objects.requireNonNull(eventBus, "事件总线不能为空");
        this.coreVersion = Objects.requireNonNull(coreVersion, "核心版本号不能为空");
        this.runtimeMode = Objects.requireNonNull(runtimeMode, "运行模式不能为空");
    }

    @Override
    public List<PluginMetadata> discover() {
        List<PluginMetadata> candidates = sources.stream()
                .flatMap(s -> s.scan().stream())
                .collect(Collectors.toList());

        return candidates;
    }
}
