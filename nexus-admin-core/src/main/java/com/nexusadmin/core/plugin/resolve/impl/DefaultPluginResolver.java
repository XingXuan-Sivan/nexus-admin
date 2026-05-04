package com.nexusadmin.core.plugin.resolve.impl;

import com.nexusadmin.core.plugin.loader.PluginMetadata;
import com.nexusadmin.core.plugin.loader.SourceType;
import com.nexusadmin.core.plugin.resolve.DependenceManager;
import com.nexusadmin.core.plugin.resolve.PluginResolver;
import com.nexusadmin.core.plugin.resolve.VersionManager;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 默认插件解析器实现。
 * <p>基于来源类型优先级和版本号进行冲突裁决，支持依赖校验与排序。</p>
 */
public class DefaultPluginResolver implements PluginResolver {

    private final VersionManager versionManager;
    private final DependenceManager dependenceManager;

    /**
     * 构造插件解析器。
     *
     * @param versionManager    版本管理器
     * @param dependenceManager 依赖管理器
     */
    public DefaultPluginResolver(VersionManager versionManager, DependenceManager dependenceManager) {
        this.versionManager = Objects.requireNonNull(versionManager, "版本管理器不能为空");
        this.dependenceManager = Objects.requireNonNull(dependenceManager, "依赖管理器不能为空");
    }

    @Override
    public List<PluginMetadata> resolve(List<PluginMetadata> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        // 1. 冲突裁决：同一插件选择最佳版本
        Map<String, List<PluginMetadata>> grouped = candidates.stream()
                .collect(Collectors.groupingBy(PluginMetadata::pluginId));

        List<PluginMetadata> resolved = grouped.values().stream()
                .map(this::selectBest)
                .collect(Collectors.toList());

        // 2. 依赖校验
        dependenceManager.validateDependencies(resolved);

        // 3. 依赖排序
        return dependenceManager.sortByDependency(resolved);
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
