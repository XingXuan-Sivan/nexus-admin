package com.nexusadmin.core.plugin;

import com.nexusadmin.core.plugin.loader.PluginWrapper;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认插件注册中心实现。
 * <p>基于线程安全的 {@link ConcurrentHashMap} 实现，支持高并发访问。</p>
 */
public class DefaultPluginRegistry implements PluginRegistry {

    /**
     * 插件存储映射：插件ID -> 插件包装对象。
     */
    private final ConcurrentHashMap<String, PluginWrapper> plugins = new ConcurrentHashMap<>();

    @Override
    public void register(PluginWrapper plugin) {
        Objects.requireNonNull(plugin, "插件对象不能为空");
        Objects.requireNonNull(plugin.descriptor(), "插件描述信息不能为空");

        String pluginId = plugin.descriptor().id();
        Objects.requireNonNull(pluginId, "插件ID不能为空");

        PluginWrapper existing = plugins.putIfAbsent(pluginId, plugin);
        if (existing != null) {
            throw new IllegalStateException("插件已存在: " + pluginId);
        }
    }

    @Override
    public void unregister(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("插件ID不能为空");
        }
        plugins.remove(pluginId);
    }

    @Override
    public PluginWrapper get(String pluginId) {
        return plugins.get(pluginId);
    }

    @Override
    public Collection<PluginWrapper> list() {
        return Collections.unmodifiableCollection(plugins.values());
    }

    @Override
    public boolean contains(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return false;
        }
        return plugins.containsKey(pluginId);
    }

    @Override
    public void clear() {
        plugins.clear();
    }
}
