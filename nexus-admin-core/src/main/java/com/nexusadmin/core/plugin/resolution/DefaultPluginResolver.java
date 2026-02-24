package com.nexusadmin.core.plugin.resolution;

import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.plugin.event.PluginProcessEvent;
import com.nexusadmin.core.plugin.loader.PluginMetadata;
import com.nexusadmin.core.plugin.loader.SourceType;
import com.nexusadmin.core.plugin.version.VersionManager;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 默认插件解析器实现。
 * <p>基于来源类型优先级和版本号进行冲突裁决。</p>
 */
public class DefaultPluginResolver implements PluginResolver {

    private final EventBus eventBus;
    private final VersionManager versionManager;

    /**
     * 构造插件解析器。
     *
     * @param eventBus       事件总线
     * @param versionManager 版本管理器
     */
    public DefaultPluginResolver(EventBus eventBus, VersionManager versionManager) {
        this.eventBus = Objects.requireNonNull(eventBus, "事件总线不能为空");
        this.versionManager = Objects.requireNonNull(versionManager, "版本管理器不能为空");
    }

    @Override
    public List<PluginMetadata> resolve(List<PluginMetadata> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        Map<String, List<PluginMetadata>> grouped = candidates.stream()
                .collect(Collectors.groupingBy(PluginMetadata::pluginId));

        List<PluginMetadata> resolved = grouped.values().stream()
                .map(this::selectBest)
                .collect(Collectors.toList());

        return resolved;
    }

    /**
     * 从同一插件的多个候选中选择最佳版本。
     * <p>优先级：来源类型 > 版本号</p>
     *
     * @param list 同一插件的候选列表
     * @return 最佳候选
     */
    private PluginMetadata selectBest(List<PluginMetadata> list) {
        return list.stream()
                .max(Comparator
                        .comparingInt((PluginMetadata c) -> score(c.sourceType()))
                        .thenComparing(c -> c.descriptor().version(), versionManager::compare))
                .orElseThrow();
    }

    /**
     * 计算来源类型的优先级分数。
     *
     * @param type 来源类型
     * @return 优先级分数（越高越优先）
     */
    private int score(SourceType type) {
        return switch (type) {
            case EXTERNAL -> 100;
            case BUILTIN -> 50;
            case CLASSPATH -> 10;
            case REMOTE -> 5;
        };
    }
}
