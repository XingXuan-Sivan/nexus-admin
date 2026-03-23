package com.nexusadmin.core.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 事件基类，所有领域事件都需要继承此类。
 * <p>提供事件的基本属性：唯一标识、发生时间戳、作用域。</p>
 * <p>作用域用于实现事件的隔离与路由，确保事件在正确的逻辑空间内传播。</p>
 */
public abstract class Event {

    /**
     * 事件唯一标识。
     */
    private final String id;

    /**
     * 事件发生时间戳。
     */
    private final Instant timestamp;

    /**
     * 事件作用域，标识事件所属的逻辑空间。
     */
    private final EventScope scope;

    /**
     * 构造事件对象，自动生成ID、时间戳和全局作用域。
     * <p>默认使用全局作用域，适用于简单场景。</p>
     */
    protected Event() {
        this(UUID.randomUUID().toString(), Instant.now(), EventScope.global());
    }

    /**
     * 构造事件对象，指定作用域。
     * <p>推荐使用此构造方法，显式指定事件的作用域。</p>
     *
     * @param scope 事件作用域，不能为空
     */
    protected Event(EventScope scope) {
        this(UUID.randomUUID().toString(), Instant.now(), scope);
    }

    /**
     * 构造事件对象，指定ID、时间戳和作用域。
     *
     * @param id        事件唯一标识
     * @param timestamp 事件发生时间戳
     * @param scope     事件作用域
     */
    protected Event(String id, Instant timestamp, EventScope scope) {
        this.id = Objects.requireNonNull(id, "事件ID不能为空");
        this.timestamp = Objects.requireNonNull(timestamp, "事件时间戳不能为空");
        this.scope = Objects.requireNonNull(scope, "事件作用域不能为空");
    }

    /**
     * 获取事件唯一标识。
     *
     * @return 事件ID
     */
    public String id() {
        return id;
    }

    /**
     * 获取事件发生时间戳。
     *
     * @return 时间戳
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * 获取事件作用域。
     *
     * @return 事件作用域
     */
    public EventScope scope() {
        return scope;
    }
}
