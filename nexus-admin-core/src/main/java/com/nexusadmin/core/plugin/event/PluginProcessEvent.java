package com.nexusadmin.core.plugin.event;

import java.time.Instant;

/**
 * 插件处理阶段事件。
 *
 * <p>
 * 用于 discover / resolve / load 等阶段通知。
 * 不与 PluginState 直接耦合。
 * </p>
 */
public final class PluginProcessEvent extends PluginEvent {

    /**
     * 处理阶段枚举。
     */
    public enum Phase {
        /**
         * 发现阶段。
         */
        DISCOVER,

        /**
         * 解析阶段。
         */
        RESOLVE,

        /**
         * 加载阶段。
         */
        LOAD,

        /**
         * 删除阶段。
         */
        DELETE
    }

    /**
     * 阶段状态枚举。
     */
    public enum Stage {
        /**
         * 开始。
         */
        START,

        /**
         * 结束。
         */
        END
    }

    private final Phase phase;
    private final Stage stage;
    private final int count;
    private final Instant occurredAt;

    /**
     * 构造插件处理阶段事件。
     *
     * @param phase 处理阶段
     * @param stage 阶段状态
     * @param count 处理数量
     */
    public PluginProcessEvent(
            Phase phase,
            Stage stage,
            int count) {

        super(null); // 系统级事件

        this.phase = phase;
        this.stage = stage;
        this.count = count;
        this.occurredAt = Instant.now();
    }

    /**
     * 获取处理阶段。
     *
     * @return 处理阶段
     */
    public Phase phase() {
        return phase;
    }

    /**
     * 获取阶段状态。
     *
     * @return 阶段状态
     */
    public Stage stage() {
        return stage;
    }

    /**
     * 获取处理数量。
     *
     * @return 处理数量
     */
    public int count() {
        return count;
    }

    /**
     * 获取事件发生时间。
     *
     * @return 事件发生时间
     */
    public Instant occurredAt() {
        return occurredAt;
    }
}
