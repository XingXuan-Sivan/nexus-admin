package com.nexusadmin.core.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 事件基类，所有领域事件都需要继承此类。
 * <p>提供事件的基本属性：唯一标识、发生时间戳。</p>
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
     * 构造事件对象，自动生成ID和时间戳。
     */
    protected Event() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    /**
     * 构造事件对象，指定ID和时间戳。
     *
     * @param id        事件唯一标识
     * @param timestamp 事件发生时间戳
     */
    protected Event(String id, Instant timestamp) {
        this.id = id;
        this.timestamp = timestamp;
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
}
