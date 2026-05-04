package com.nexusadmin.core.plugin.resolve.impl;

import com.nexusadmin.core.exception.PluginException;
import com.nexusadmin.core.plugin.loader.PluginMetadata;
import com.nexusadmin.core.plugin.resolve.DependenceManager;
import com.nexusadmin.core.plugin.resolve.VersionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * 默认依赖管理器实现。
 * <p>基于拓扑排序算法实现依赖排序，支持循环依赖检测。</p>
 */
public class DefaultDependenceManager implements DependenceManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultDependenceManager.class);

    private final VersionManager versionManager;

    public DefaultDependenceManager(VersionManager versionManager) {
        this.versionManager = versionManager;
    }

    @Override
    public void validateDependencies(List<PluginMetadata> plugins) {
        if (plugins == null || plugins.isEmpty()) {
            return;
        }

        Map<String, PluginMetadata> pluginMap = new HashMap<>();
        for (PluginMetadata plugin : plugins) {
            pluginMap.put(plugin.pluginId(), plugin);
        }

        for (PluginMetadata plugin : plugins) {
            Map<String, String> dependencies = plugin.descriptor().dependencies();
            if (dependencies == null || dependencies.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, String> entry : dependencies.entrySet()) {
                String depId = entry.getKey();
                String versionRange = entry.getValue();

                PluginMetadata depPlugin = pluginMap.get(depId);
                if (depPlugin == null) {
                    throw new PluginException(
                            String.format("插件 [%s] 依赖的插件 [%s] 不存在", plugin.pluginId(), depId));
                }

                String depVersion = depPlugin.descriptor().version();
                if (!isVersionSatisfied(depVersion, versionRange)) {
                    throw new PluginException(
                            String.format("插件 [%s] 依赖的插件 [%s] 版本不满足，需要 [%s]，实际 [%s]",
                                    plugin.pluginId(), depId, versionRange, depVersion));
                }
            }
        }

        log.debug("依赖校验通过，共 {} 个插件", plugins.size());
    }

    @Override
    public List<PluginMetadata> sortByDependency(List<PluginMetadata> plugins) {
        if (plugins == null || plugins.size() <= 1) {
            return plugins != null ? new ArrayList<>(plugins) : Collections.emptyList();
        }

        Map<String, PluginMetadata> pluginMap = new HashMap<>();
        Map<String, Set<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        for (PluginMetadata plugin : plugins) {
            pluginMap.put(plugin.pluginId(), plugin);
            graph.put(plugin.pluginId(), new HashSet<>());
            inDegree.put(plugin.pluginId(), 0);
        }

        for (PluginMetadata plugin : plugins) {
            Map<String, String> dependencies = plugin.descriptor().dependencies();
            if (dependencies == null || dependencies.isEmpty()) {
                continue;
            }

            for (String depId : dependencies.keySet()) {
                if (pluginMap.containsKey(depId)) {
                    graph.get(depId).add(plugin.pluginId());
                    inDegree.merge(plugin.pluginId(), 1, Integer::sum);
                }
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<PluginMetadata> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String pluginId = queue.poll();
            sorted.add(pluginMap.get(pluginId));

            for (String dependent : graph.get(pluginId)) {
                int newDegree = inDegree.get(dependent) - 1;
                inDegree.put(dependent, newDegree);
                if (newDegree == 0) {
                    queue.offer(dependent);
                }
            }
        }

        if (sorted.size() != plugins.size()) {
            throw new PluginException("检测到插件间存在循环依赖，无法进行拓扑排序");
        }

        log.debug("依赖拓扑排序完成，共 {} 个插件", sorted.size());
        return sorted;
    }

    /**
     * 检查版本是否满足范围要求。
     * <p>支持精确版本和通配符版本（如 1.0.*）。</p>
     *
     * @param version 实际版本
     * @param range   版本范围要求
     * @return 如果满足返回 true
     */
    private boolean isVersionSatisfied(String version, String range) {
        if (range == null || range.isEmpty() || "*".equals(range)) {
            return true;
        }

        if (range.endsWith(".*")) {
            String prefix = range.substring(0, range.length() - 1);
            return version.startsWith(prefix);
        }

        return version.equals(range);
    }
}
