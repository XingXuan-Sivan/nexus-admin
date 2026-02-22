package com.nexusadmin.core.event;

/**
 * 事件监听器接口。
 *
 * @param <T> 监听的事件类型
 */
@FunctionalInterface
public interface EventListener<T extends Event> {

    /**
     * 处理事件。
     *
     * @param event 事件对象
     */
    void onEvent(T event);
}
