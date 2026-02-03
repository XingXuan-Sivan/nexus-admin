package com.nexusadmin.core.event;

import java.time.Instant;
import java.util.Map;

/**
 * 领域事件接口，定义领域内发生的各类事件通用结构。
 */
public interface DomainEvent {
    /**
     * 返回事件唯一标识。
     *
     * @return 事件 ID
     */
    String eventId();

    /**
     * 返回事件类型名称。
     *
     * @return 事件类型
     */
    String eventType();

    /**
     * 返回事件发生时间。
     *
     * @return 发生时间
     */
    Instant occurredAt();

    /**
     * 返回事件元数据。
     *
     * @return 元数据集合
     */
    Map<String, String> metadata();
}
