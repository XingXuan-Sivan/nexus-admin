package com.nexusadmin.core.event;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 同步事件总线实现。
 * <p>在发布者线程中同步执行所有监听器，简单高效。</p>
 */
public class SyncEventBus implements EventBus {

    /**
     * 事件类型 -> 监听器集合 的映射。
     */
    private final ConcurrentHashMap<Class<? extends Event>, CopyOnWriteArraySet<ListenerWrapper<?>>> listeners =
            new ConcurrentHashMap<>();

    /**
     * 插件ID -> 监听器集合 的映射，用于插件卸载时清理。
     */
    private final ConcurrentHashMap<String, Set<EventListener<?>>> pluginListeners = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public void publish(Event event) {
        Objects.requireNonNull(event, "事件对象不能为空");

        Class<? extends Event> eventType = event.getClass();

        // 获取该事件类型的所有监听器
        CopyOnWriteArraySet<ListenerWrapper<?>> typeListeners = listeners.get(eventType);
        if (typeListeners != null) {
            for (ListenerWrapper<?> wrapper : typeListeners) {
                try {
                    ((EventListener<Event>) wrapper.listener).onEvent(event);
                } catch (Exception e) {
                    // 监听器异常不应影响其他监听器
                    // 实际应用中应该记录日志
                    System.err.println("事件监听器执行失败: " + e.getMessage());
                }
            }
        }

        // 同时通知父类事件的监听器
        Class<?> superClass = eventType.getSuperclass();
        while (superClass != null && Event.class.isAssignableFrom(superClass)) {
            CopyOnWriteArraySet<ListenerWrapper<?>> superListeners =
                    listeners.get((Class<? extends Event>) superClass);
            if (superListeners != null) {
                for (ListenerWrapper<?> wrapper : superListeners) {
                    try {
                        ((EventListener<Event>) wrapper.listener).onEvent(event);
                    } catch (Exception e) {
                        System.err.println("事件监听器执行失败: " + e.getMessage());
                    }
                }
            }
            superClass = superClass.getSuperclass();
        }
    }

    @Override
    public <T extends Event> void subscribe(Class<T> type, EventListener<T> listener) {
        Objects.requireNonNull(type, "事件类型不能为空");
        Objects.requireNonNull(listener, "监听器不能为空");

        listeners.computeIfAbsent(type, k -> new CopyOnWriteArraySet<>())
                .add(new ListenerWrapper<>(listener, null));
    }

    /**
     * 订阅事件并关联插件ID，用于后续自动清理。
     *
     * @param type     事件类型
     * @param listener 监听器
     * @param pluginId 插件ID
     * @param <T>      事件类型
     */
    public <T extends Event> void subscribe(Class<T> type, EventListener<T> listener, String pluginId) {
        Objects.requireNonNull(type, "事件类型不能为空");
        Objects.requireNonNull(listener, "监听器不能为空");

        listeners.computeIfAbsent(type, k -> new CopyOnWriteArraySet<>())
                .add(new ListenerWrapper<>(listener, pluginId));

        if (pluginId != null && !pluginId.isBlank()) {
            pluginListeners.computeIfAbsent(pluginId, k -> ConcurrentHashMap.newKeySet())
                    .add(listener);
        }
    }

    @Override
    public void unsubscribe(EventListener<?> listener) {
        if (listener == null) {
            return;
        }

        // 从所有事件类型中移除该监听器
        for (CopyOnWriteArraySet<ListenerWrapper<?>> typeListeners : listeners.values()) {
            typeListeners.removeIf(wrapper -> wrapper.listener == listener);
        }

        // 从插件监听器映射中移除
        for (Set<EventListener<?>> pluginListenerSet : pluginListeners.values()) {
            pluginListenerSet.remove(listener);
        }
    }

    @Override
    public void unsubscribeByPlugin(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return;
        }

        Set<EventListener<?>> pluginListenerSet = pluginListeners.remove(pluginId);
        if (pluginListenerSet == null) {
            return;
        }

        // 从所有事件类型中移除该插件关联的监听器
        for (CopyOnWriteArraySet<ListenerWrapper<?>> typeListeners : listeners.values()) {
            typeListeners.removeIf(wrapper ->
                    pluginId.equals(wrapper.pluginId) ||
                            pluginListenerSet.contains(wrapper.listener));
        }
    }

    /**
     * 监听器包装类，用于关联插件ID。
     */
    private static class ListenerWrapper<T extends Event> {
        final EventListener<T> listener;
        final String pluginId;

        ListenerWrapper(EventListener<T> listener, String pluginId) {
            this.listener = listener;
            this.pluginId = pluginId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ListenerWrapper<?> that = (ListenerWrapper<?>) o;
            return listener.equals(that.listener);
        }

        @Override
        public int hashCode() {
            return listener.hashCode();
        }
    }
}
