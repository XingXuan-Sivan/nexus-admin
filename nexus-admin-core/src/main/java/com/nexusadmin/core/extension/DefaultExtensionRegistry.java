package com.nexusadmin.core.extension;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 默认的扩展注册中心实现。
 * <p>基于线程安全的并发集合实现，支持优先级排序和插件级生命周期管理。</p>
 */
public class DefaultExtensionRegistry implements ExtensionRegistry {

    /**
     * 注册的扩展信息内部类。
     */
    private static final class RegisteredExtension {
        private final Object implementation;
        private final int priority;
        private final long sequence;
        private final String pluginId;

        private RegisteredExtension(Object implementation, int priority, long sequence, String pluginId) {
            this.implementation = implementation;
            this.priority = priority;
            this.sequence = sequence;
            this.pluginId = pluginId != null ? pluginId : "";
        }
    }

    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    /**
     * 扩展点类型到注册扩展列表的映射。
     */
    private final ConcurrentHashMap<Class<? extends ExtensionPoint>, CopyOnWriteArrayList<RegisteredExtension>>
            registry = new ConcurrentHashMap<>();

    /**
     * 插件ID到其注册的扩展列表的映射，用于插件卸载时清理。
     */
    private final ConcurrentHashMap<String, List<RegisteredExtension>> pluginExtensions = new ConcurrentHashMap<>();

    @Override
    public <T extends ExtensionPoint> void register(Class<T> pointType, T implementation) {
        register(pointType, implementation, 0);
    }

    @Override
    public <T extends ExtensionPoint> void register(Class<T> pointType, T implementation, int priority) {
        if (pointType == null || implementation == null) {
            return;
        }

        // 从实现类中尝试获取插件ID（如果实现了特定接口）
        String pluginId = extractPluginId(implementation);

        RegisteredExtension registered = new RegisteredExtension(
                implementation, priority, SEQUENCE.incrementAndGet(), pluginId);

        registry.computeIfAbsent(pointType, key -> new CopyOnWriteArrayList<>()).add(registered);

        // 记录到插件扩展映射中
        if (!pluginId.isEmpty()) {
            pluginExtensions.computeIfAbsent(pluginId, key -> new CopyOnWriteArrayList<>()).add(registered);
        }
    }

    @Override
    public <T extends ExtensionPoint> void unregister(Class<T> pointType, T implementation) {
        if (pointType == null || implementation == null) {
            return;
        }

        CopyOnWriteArrayList<RegisteredExtension> list = registry.get(pointType);
        if (list != null) {
            list.removeIf(registered ->
                    registered.implementation == implementation ||
                            registered.implementation.equals(implementation));
        }

        // 同时从插件扩展映射中移除
        String pluginId = extractPluginId(implementation);
        if (!pluginId.isEmpty()) {
            List<RegisteredExtension> pluginList = pluginExtensions.get(pluginId);
            if (pluginList != null) {
                pluginList.removeIf(registered ->
                        registered.implementation == implementation ||
                                registered.implementation.equals(implementation));
            }
        }
    }

    @Override
    public <T extends ExtensionPoint> Optional<T> get(Class<T> pointType) {
        List<T> list = getAll(pointType);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ExtensionPoint> List<T> getAll(Class<T> pointType) {
        if (pointType == null) {
            return List.of();
        }

        CopyOnWriteArrayList<RegisteredExtension> list = registry.get(pointType);
        if (list == null || list.isEmpty()) {
            return List.of();
        }

        return list.stream()
                .filter(registered -> pointType.isInstance(registered.implementation))
                .sorted(Comparator
                        .comparingInt((RegisteredExtension r) -> r.priority).reversed()
                        .thenComparing((RegisteredExtension r) -> r.sequence, Comparator.reverseOrder()))
                .map(registered -> (T) registered.implementation)
                .collect(Collectors.toList());
    }

    @Override
    public void unregisterByPluginId(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return;
        }

        List<RegisteredExtension> extensions = pluginExtensions.remove(pluginId);
        if (extensions == null || extensions.isEmpty()) {
            return;
        }

        // 从所有扩展点注册表中移除该插件的扩展
        for (CopyOnWriteArrayList<RegisteredExtension> list : registry.values()) {
            list.removeIf(registered -> pluginId.equals(registered.pluginId));
        }
    }

    @Override
    public <T extends ExtensionPoint> void clear(Class<T> pointType) {
        if (pointType == null) {
            return;
        }
        registry.remove(pointType);
    }

    @Override
    public void clearAll() {
        registry.clear();
        pluginExtensions.clear();
    }

    /**
     * 从扩展实现中提取插件ID。
     * <p>默认实现返回空字符串，子类可覆盖以支持特定插件标识机制。</p>
     *
     * @param implementation 扩展实现实例
     * @return 插件ID
     */
    protected String extractPluginId(Object implementation) {
        // 可通过实现特定接口或注解来获取插件ID
        // 默认返回空字符串
        return "";
    }
}
