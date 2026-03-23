package com.nexusadmin.core.event;

import java.util.Objects;

/**
 * 事件总线接口，负责事件的发布和订阅。
 * <p>替代传统的 Listener 模式，提供更灵活的事件通信机制。</p>
 * <p>支持事件作用域机制，实现事件的隔离与路由。</p>
 */
public interface EventBus {

    /**
     * 发布事件到总线。
     * <p>根据事件的作用域进行匹配分发，只有订阅了对应作用域的监听器才会收到通知。</p>
     *
     * @param event 事件对象，不能为空
     * @throws IllegalArgumentException 如果 event 为 null
     */
    void publish(Event event);

    /**
     * 订阅指定类型的事件，接收所有作用域的事件。
     * <p>这是简化方法，内部使用 {@link EventScopeMatcher#all()} 匹配所有作用域。</p>
     *
     * @param type     事件类型类
     * @param listener 事件监听器
     * @param <T>      事件类型
     * @throws IllegalArgumentException 如果 type 或 listener 为 null
     */
    default <T extends Event> void subscribe(Class<T> type, EventListener<T> listener) {
        subscribe(type, listener, EventScopeMatcher.all(), null);
    }

    /**
     * 订阅指定类型和作用域的事件。
     * <p>推荐使用此方法，可以精确控制接收哪些作用域的事件。</p>
     *
     * @param type          事件类型类
     * @param listener      事件监听器
     * @param scopeMatcher  作用域匹配器，不能为空
     * @param pluginId      订阅者所属插件ID，可为空（用于插件卸载时自动清理）
     * @param <T>           事件类型
     * @throws IllegalArgumentException 如果 type、listener 或 scopeMatcher 为 null
     */
    <T extends Event> void subscribe(Class<T> type, EventListener<T> listener,
                                     EventScopeMatcher scopeMatcher, String pluginId);

    /**
     * 取消订阅指定监听器。
     * <p>该监听器将不再接收任何事件通知。</p>
     *
     * @param listener 要取消的监听器
     */
    void unsubscribe(EventListener<?> listener);

    /**
     * 取消指定插件注册的所有监听器。
     * <p>在插件卸载时调用，防止监听器泄露。</p>
     *
     * @param pluginId 插件唯一标识
     */
    void unsubscribeByPlugin(String pluginId);
}
