package com.nexusadmin.core.plugin.event;

import com.nexusadmin.core.plugin.descriptor.PluginDescriptor;

import java.time.Instant;

/**
 * 插件失败事件。
 *
 * <p>
 * 任何阶段出现异常都可发布。
 * 通常伴随状态迁移至 FAILED。
 * </p>
 */
public final class PluginFailureEvent extends PluginEvent {

    private final PluginDescriptor descriptor;
    private final Throwable error;
    private final Instant occurredAt;

    /**
     * 构造插件失败事件。
     *
     * @param descriptor 插件描述信息
     * @param error      异常对象
     */
    public PluginFailureEvent(
            PluginDescriptor descriptor,
            Throwable error) {

        super(descriptor.id());

        this.descriptor = descriptor;
        this.error = error;
        this.occurredAt = Instant.now();
    }

    /**
     * 获取插件描述信息。
     *
     * @return 插件描述信息
     */
    public PluginDescriptor descriptor() {
        return descriptor;
    }

    /**
     * 获取异常对象。
     *
     * @return 异常对象
     */
    public Throwable error() {
        return error;
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
