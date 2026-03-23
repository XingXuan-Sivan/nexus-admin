package com.nexusadmin.core.event;

/**
 * 事件发布者接口。
 * <p>插件通过此接口发布事件，而非直接获取 EventBus。</p>
 * <p>事件的作用域由事件对象本身携带，发布者只需构造正确作用域的事件即可。</p>
 */
@FunctionalInterface
public interface EventPublisher {

    /**
     * 发布事件。
     * <p>事件将根据其作用域进行路由，只有订阅了对应作用域的监听器才会收到通知。</p>
     *
     * @param event 事件对象，不能为空
     */
    void publish(Event event);
}
