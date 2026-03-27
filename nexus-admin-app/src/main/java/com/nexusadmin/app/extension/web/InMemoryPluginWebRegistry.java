package com.nexusadmin.app.extension.web;

import com.nexusadmin.api.extension.web.PluginWebRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于内存的插件 Web 端点注册表实现。
 * <p>
 * 使用 ConcurrentHashMap 和 CopyOnWriteArrayList 保证线程安全，
 * 支持在插件卸载时精确清理端点。
 */
public class InMemoryPluginWebRegistry implements PluginWebRegistry {

    private final Map<String, List<Object>> mappings = new ConcurrentHashMap<>();

    @Override
    public void add(String pluginId, Object mappingInfo) {
        mappings.computeIfAbsent(pluginId, k -> new CopyOnWriteArrayList<>()).add(mappingInfo);
    }

    @Override
    public List<Object> get(String pluginId) {
        return mappings.getOrDefault(pluginId, Collections.emptyList());
    }

    @Override
    public void remove(String pluginId) {
        mappings.remove(pluginId);
    }

    @Override
    public boolean hasMappings(String pluginId) {
        List<Object> list = mappings.get(pluginId);
        return list != null && !list.isEmpty();
    }

    @Override
    public int size() {
        return mappings.size();
    }

    @Override
    public void clear() {
        mappings.clear();
    }
}
