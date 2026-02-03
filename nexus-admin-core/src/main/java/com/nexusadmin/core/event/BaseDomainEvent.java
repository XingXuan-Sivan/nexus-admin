package com.nexusadmin.core.event;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 领域事件基类，提供领域事件的通用实现。
 * <p>自动生成事件 ID 和发生时间，子类只需指定事件类型和元数据。</p>
 */
public abstract class BaseDomainEvent implements DomainEvent {
    /**
     * 事件唯一标识。
     */
    private final String eventId;
    /**
     * 事件类型名称。
     */
    private final String eventType;
    /**
     * 事件发生时间。
     */
    private final Instant occurredAt;
    /**
     * 事件元数据。
     */
    private final Map<String, String> metadata;

    /**
     * 构造领域事件，自动生成事件 ID 和发生时间。
     *
     * @param eventType 事件类型
     * @param metadata  事件元数据
     */
    protected BaseDomainEvent(String eventType, Map<String, String> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.occurredAt = Instant.now();
        this.metadata = metadata == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    @Override
    public String eventId() {
        return eventId;
    }

    @Override
    public String eventType() {
        return eventType;
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }

    @Override
    public Map<String, String> metadata() {
        return metadata;
    }
}
