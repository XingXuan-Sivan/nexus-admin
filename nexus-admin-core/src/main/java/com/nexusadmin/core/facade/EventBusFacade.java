package com.nexusadmin.core.facade;

import com.nexusadmin.core.event.Event;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.event.EventListener;
import com.nexusadmin.core.event.EventScopeMatcher;

import java.util.Objects;

/**
 * 事件总线门面。
 * <p>聚合 {@link EventBus}，提供统一的事件发布与订阅入口，避免业务代码直接依赖底层事件总线实现。</p>
 */
public class EventBusFacade {

    private final EventBus eventBus;

    /**
     * 构造事件总线门面。
     *
     * @param eventBus 事件总线实例，不能为空
     */
    public EventBusFacade(EventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "事件总线不能为空");
    }

    /**
     * 发布事件到总线。
     *
     * @param event 事件对象，不能为空
     */
    public void publish(Event event) {
        eventBus.publish(event);
    }

    /**
     * 订阅指定类型的事件，接收所有作用域的事件。
     *
     * @param type     事件类型类
     * @param listener 事件监听器
     * @param <T>      事件类型
     */
    public <T extends Event> void subscribe(Class<T> type, EventListener<T> listener) {
        eventBus.subscribe(type, listener);
    }

    /**
     * 订阅指定类型和作用域的事件。
     *
     * @param type         事件类型类
     * @param listener     事件监听器
     * @param scopeMatcher 作用域匹配器
     * @param pluginId     订阅者所属插件ID
     * @param <T>          事件类型
     */
    public <T extends Event> void subscribe(Class<T> type, EventListener<T> listener,
                                            EventScopeMatcher scopeMatcher, String pluginId) {
        eventBus.subscribe(type, listener, scopeMatcher, pluginId);
    }

    /**
     * 取消订阅指定监听器。
     *
     * @param listener 要取消的监听器
     */
    public void unsubscribe(EventListener<?> listener) {
        eventBus.unsubscribe(listener);
    }

    /**
     * 取消指定插件注册的所有监听器。
     *
     * @param pluginId 插件唯一标识
     */
    public void unsubscribeByPlugin(String pluginId) {
        eventBus.unsubscribeByPlugin(pluginId);
    }

    /**
     * 获取底层事件总线实例。
     *
     * @return 事件总线
     */
    public EventBus eventBus() {
        return eventBus;
    }
}
