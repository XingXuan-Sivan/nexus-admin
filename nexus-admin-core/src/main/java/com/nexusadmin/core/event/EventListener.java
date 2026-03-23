package com.nexusadmin.core.event;

/**
 * 事件监听器接口。
 * <p>监听器通过 {@link EventBus#subscribe} 方法注册到事件总线。</p>
 * <p>可以通过 {@link EventScopeMatcher} 精确控制接收哪些作用域的事件。</p>
 *
 * @param <T> 监听的事件类型
 */
@FunctionalInterface
public interface EventListener<T extends Event> {

    /**
     * 处理事件。
     * <p>监听器应快速处理事件，避免阻塞事件总线。耗时操作应异步执行。</p>
     *
     * @param event 事件对象
     */
    void onEvent(T event);
}
