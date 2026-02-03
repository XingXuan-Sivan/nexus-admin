package com.nexusadmin.core.event;

/**
 * 领域事件发布者接口，负责将领域事件发布到事件总线。
 */
public interface DomainEventPublisher {
    /**
     * 发布一个领域事件。
     *
     * @param event 待发布的领域事件
     */
    void publish(DomainEvent event);
}
