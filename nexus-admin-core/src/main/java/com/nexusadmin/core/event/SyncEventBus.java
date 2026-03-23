package com.nexusadmin.core.event;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 同步事件总线实现。
 * <p>在发布者线程中同步执行所有监听器，简单高效。</p>
 * <p>支持事件作用域机制，根据事件作用域和订阅者的作用域匹配器进行精确分发。</p>
 */
public class SyncEventBus implements EventBus {

    /**
     * 事件类型 -> 作用域感知监听器集合 的映射。
     */
    private final ConcurrentHashMap<Class<? extends Event>, CopyOnWriteArraySet<ScopedListenerWrapper<?>>> listeners =
            new ConcurrentHashMap<>();

    /**
     * 插件ID -> 监听器集合 的映射，用于插件卸载时清理。
     */
    private final ConcurrentHashMap<String, Set<EventListener<?>>> pluginListeners = new ConcurrentHashMap<>();

    @Override
    public void publish(Event event) {
        Objects.requireNonNull(event, "事件对象不能为空");

        Class<? extends Event> eventType = event.getClass();
        EventScope scope = event.scope();

        // 分发到该事件类型的监听器
        dispatchToListeners(eventType, event, scope);

        // 同时分发到父类事件类型的监听器（支持事件继承）
        Class<?> superClass = eventType.getSuperclass();
        while (superClass != null && Event.class.isAssignableFrom(superClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends Event> superEventType = (Class<? extends Event>) superClass;
            dispatchToListeners(superEventType, event, scope);
            superClass = superClass.getSuperclass();
        }
    }

    /**
     * 将事件分发到指定类型的监听器。
     * <p>只有作用域匹配器匹配成功的监听器才会收到事件。</p>
     *
     * @param type  事件类型
     * @param event 事件对象
     * @param scope 事件作用域
     */
    private void dispatchToListeners(Class<? extends Event> type, Event event, EventScope scope) {
        CopyOnWriteArraySet<ScopedListenerWrapper<?>> typeListeners = listeners.get(type);
        if (typeListeners == null) {
            return;
        }
        for (ScopedListenerWrapper<?> wrapper : typeListeners) {
            dispatchSingle(wrapper, event, scope);
        }
    }

    /**
     * 向单个监听器分发事件。
     * <p>首先检查作用域是否匹配，匹配失败则跳过。</p>
     *
     * @param wrapper 作用域感知监听器包装
     * @param event   事件对象
     * @param scope   事件作用域
     */
    @SuppressWarnings("unchecked")
    private void dispatchSingle(ScopedListenerWrapper<?> wrapper, Event event, EventScope scope) {
        if (!wrapper.scopeMatcher.matches(scope)) {
            return;
        }
        try {
            ((EventListener<Event>) wrapper.listener).onEvent(event);
        } catch (Exception e) {
            // 监听器异常不应影响其他监听器
            // TODO: 接入统一日志系统
            System.err.println("事件监听器执行失败: " + e.getMessage());
        }
    }

    @Override
    public <T extends Event> void subscribe(Class<T> type, EventListener<T> listener,
                                            EventScopeMatcher scopeMatcher, String pluginId) {
        Objects.requireNonNull(type, "事件类型不能为空");
        Objects.requireNonNull(listener, "监听器不能为空");
        Objects.requireNonNull(scopeMatcher, "作用域匹配器不能为空");

        listeners.computeIfAbsent(type, k -> new CopyOnWriteArraySet<>())
                .add(new ScopedListenerWrapper<>(listener, scopeMatcher, pluginId));

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
        for (CopyOnWriteArraySet<ScopedListenerWrapper<?>> typeListeners : listeners.values()) {
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
        for (CopyOnWriteArraySet<ScopedListenerWrapper<?>> typeListeners : listeners.values()) {
            typeListeners.removeIf(wrapper ->
                    pluginId.equals(wrapper.pluginId) ||
                            pluginListenerSet.contains(wrapper.listener));
        }
    }

    /**
     * 作用域感知监听器包装类。
     * <p>包装监听器及其作用域匹配器和所属插件信息。</p>
     */
    private static class ScopedListenerWrapper<T extends Event> {
        final EventListener<T> listener;
        final EventScopeMatcher scopeMatcher;
        final String pluginId;

        ScopedListenerWrapper(EventListener<T> listener,
                              EventScopeMatcher scopeMatcher,
                              String pluginId) {
            this.listener = listener;
            this.scopeMatcher = scopeMatcher;
            this.pluginId = pluginId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScopedListenerWrapper<?> that = (ScopedListenerWrapper<?>) o;
            return listener.equals(that.listener);
        }

        @Override
        public int hashCode() {
            return listener.hashCode();
        }
    }
}
