package com.nexusadmin.core.plugin.event;

import com.nexusadmin.core.plugin.PluginState;
import com.nexusadmin.core.plugin.loader.PluginWrapper;

import java.time.Instant;
import java.util.Objects;

/**
 * 插件状态迁移事件。
 *
 * <p>
 * 由 AbstractPluginManager.transition(...) 唯一发布。
 * 表示插件从一个稳定/过渡状态迁移到另一个状态。
 * </p>
 *
 * <p>
 * 该事件是插件生命周期的唯一真实信号源。
 * </p>
 */
public final class PluginStateChangedEvent extends PluginEvent {

    private final PluginWrapper plugin;
    private final PluginState from;
    private final PluginState to;
    private final Instant occurredAt;

    /**
     * 构造插件状态迁移事件。
     *
     * @param plugin 插件包装对象
     * @param from   源状态
     * @param to     目标状态
     */
    public PluginStateChangedEvent(
            PluginWrapper plugin,
            PluginState from,
            PluginState to) {

        super(plugin.getPluginId());

        this.plugin = Objects.requireNonNull(plugin);
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
        this.occurredAt = Instant.now();
    }

    /**
     * 获取插件包装对象。
     *
     * @return 插件包装对象
     */
    public PluginWrapper plugin() {
        return plugin;
    }

    /**
     * 获取源状态。
     *
     * @return 源状态
     */
    public PluginState from() {
        return from;
    }

    /**
     * 获取目标状态。
     *
     * @return 目标状态
     */
    public PluginState to() {
        return to;
    }

    /**
     * 获取事件发生时间。
     *
     * @return 事件发生时间
     */
    public Instant occurredAt() {
        return occurredAt;
    }

    /**
     * 判断是否正在进入指定状态。
     *
     * @param state 目标状态
     * @return 如果正在进入该状态返回 true
     */
    public boolean isEntering(PluginState state) {
        return to == state;
    }

    /**
     * 判断是否正在离开指定状态。
     *
     * @param state 源状态
     * @return 如果正在离开该状态返回 true
     */
    public boolean isLeaving(PluginState state) {
        return from == state;
    }
}
