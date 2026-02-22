package com.nexusadmin.core.event;

/**
 * 事件发布者接口。
 * <p>插件通过此接口发布事件，而非直接获取 EventBus。</p>
 */
@FunctionalInterface
public interface EventPublisher {

    /**
     * 发布事件。
     *
     * @param event 事件对象
     */
    void publish(Event event);
}
