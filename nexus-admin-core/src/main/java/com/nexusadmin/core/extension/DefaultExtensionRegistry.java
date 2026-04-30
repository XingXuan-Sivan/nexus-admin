package com.nexusadmin.core.extension;

import com.nexusadmin.core.event.EventBus;

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
     * 事件总线，用于发布扩展变更事件。可为 null（向后兼容）。
     */
    private final EventBus eventBus;

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

    /**
     * 构造默认扩展注册中心（无事件总线）。
     * <p>向后兼容的构造方法，不会发布扩展变更事件。</p>
     */
    public DefaultExtensionRegistry() {
        this(null);
    }

    /**
     * 构造默认扩展注册中心，指定事件总线。
     * <p>当 eventBus 不为空时，扩展的注册/注销/清空操作会自动发布 {@link ExtensionChangedEvent}。</p>
     *
     * @param eventBus 事件总线，可为 null
     */
    public DefaultExtensionRegistry(EventBus eventBus) {
        this.eventBus = eventBus;
    }

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

        // 发布扩展注册事件
        publishEvent(new ExtensionChangedEvent(pointType,
                ExtensionChangedEvent.ChangeType.REGISTERED, implementation));
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

        // 发布扩展注销事件
        publishEvent(new ExtensionChangedEvent(pointType,
                ExtensionChangedEvent.ChangeType.UNREGISTERED, implementation));
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
    @SuppressWarnings("unchecked")
    public void unregisterByPluginId(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return;
        }

        List<RegisteredExtension> extensions = pluginExtensions.remove(pluginId);
        if (extensions == null || extensions.isEmpty()) {
            return;
        }

        // 收集被移除扩展对应的扩展点类型，用于发布事件
        Map<Class<? extends ExtensionPoint>, List<RegisteredExtension>> removedByType = new LinkedHashMap<>();

        // 从所有扩展点注册表中移除该插件的扩展
        for (Map.Entry<Class<? extends ExtensionPoint>, CopyOnWriteArrayList<RegisteredExtension>> entry : registry.entrySet()) {
            Class<? extends ExtensionPoint> pointType = entry.getKey();
            CopyOnWriteArrayList<RegisteredExtension> list = entry.getValue();

            List<RegisteredExtension> removed = list.stream()
                    .filter(registered -> pluginId.equals(registered.pluginId))
                    .collect(Collectors.toList());

            if (!removed.isEmpty()) {
                list.removeIf(registered -> pluginId.equals(registered.pluginId));
                removedByType.put(pointType, removed);
            }
        }

        // 对每个移除的扩展发布 UNREGISTERED 事件
        for (Map.Entry<Class<? extends ExtensionPoint>, List<RegisteredExtension>> entry : removedByType.entrySet()) {
            for (RegisteredExtension removed : entry.getValue()) {
                publishEvent(new ExtensionChangedEvent(entry.getKey(),
                        ExtensionChangedEvent.ChangeType.UNREGISTERED,
                        (ExtensionPoint) removed.implementation));
            }
        }
    }

    @Override
    public <T extends ExtensionPoint> void clear(Class<T> pointType) {
        if (pointType == null) {
            return;
        }
        registry.remove(pointType);

        // 发布扩展清空事件
        publishEvent(new ExtensionChangedEvent(pointType,
                ExtensionChangedEvent.ChangeType.CLEARED, null));
    }

    @Override
    public void clearAll() {
        // 先收集所有扩展点类型，用于发布事件
        Set<Class<? extends ExtensionPoint>> pointTypes = new LinkedHashSet<>(registry.keySet());

        registry.clear();
        pluginExtensions.clear();

        // 对每个类型发布 CLEARED 事件
        for (Class<? extends ExtensionPoint> pointType : pointTypes) {
            publishEvent(new ExtensionChangedEvent(pointType,
                    ExtensionChangedEvent.ChangeType.CLEARED, null));
        }
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

    /**
     * 发布扩展变更事件。
     * <p>当 eventBus 为 null 时不发布事件（向后兼容）。</p>
     *
     * @param event 扩展变更事件
     */
    private void publishEvent(ExtensionChangedEvent event) {
        if (eventBus != null) {
            eventBus.publish(event);
        }
    }
}
