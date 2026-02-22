package com.nexusadmin.core.event;

/**
 * 事件总线接口，负责事件的发布和订阅。
 * <p>替代传统的 Listener 模式，提供更灵活的事件通信机制。</p>
 */
public interface EventBus {

    /**
     * 发布事件到总线。
     * <p>所有订阅了该事件类型的监听器都会收到通知。</p>
     *
     * @param event 事件对象，不能为空
     * @throws IllegalArgumentException 如果 event 为 null
     */
    void publish(Event event);

    /**
     * 订阅指定类型的事件。
     *
     * @param type     事件类型类
     * @param listener 事件监听器
     * @param <T>      事件类型
     * @throws IllegalArgumentException 如果 type 或 listener 为 null
     */
    <T extends Event> void subscribe(Class<T> type, EventListener<T> listener);

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
