package com.nexusadmin.core.runtime;

import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.event.SyncEventBus;
import com.nexusadmin.core.facade.EventBusFacade;

import java.util.Objects;

/**
 * 事件总线运行时。
 * <p>
 * 负责装配 {@link EventBus} 的默认实例，不依赖任何外部 DI 容器。
 * 组件可通过 Builder 选择性覆盖，未指定的使用默认实现。
 * <p>
 * <strong>使用示例：</strong>
 * <pre>{@code
 * // 全默认装配
 * EventBusRuntime rt = EventBusRuntime.defaults();
 *
 * // 选择性覆盖
 * EventBusRuntime rt = EventBusRuntime.builder()
 *     .eventBus(new MyAsyncEventBus())
 *     .build();
 *
 * // 访问组件
 * EventBus bus = rt.eventBus();
 * EventBusFacade facade = rt.facade();
 * }</pre>
 */
public final class EventBusRuntime {

    private final EventBus eventBus;

    /**
     * 构造 EventBusRuntime。
     * <p>依赖关系在构造时完成注入，实例创建后不可变。</p>
     *
     * @param eventBus 事件总线实例
     */
    private EventBusRuntime(EventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus 不能为空");
    }

    // ==================== 组件访问器 ====================

    /**
     * 获取事件总线。
     *
     * @return EventBus 实例
     */
    public EventBus eventBus() {
        return eventBus;
    }

    /**
     * 获取事件总线门面。
     *
     * @return EventBusFacade 实例
     */
    public EventBusFacade facade() {
        return new EventBusFacade(eventBus);
    }

    // ==================== 快速创建 ====================

    /**
     * 使用全部默认实现创建 EventBusRuntime。
     *
     * @return 配置完成的 EventBusRuntime 实例
     */
    public static EventBusRuntime defaults() {
        return builder().build();
    }

    /**
     * 创建 Builder 实例。
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    // ==================== Builder ====================

    /**
     * EventBusRuntime 建造器。
     * <p>通过链式调用选择性覆盖组件，{@link #build()} 时补全默认值。</p>
     */
    public static class Builder {
        private EventBus eventBus;

        /**
         * 覆盖默认事件总线。
         *
         * @param val EventBus 实例
         * @return Builder 自身（链式调用）
         */
        public Builder eventBus(EventBus val) {
            this.eventBus = val;
            return this;
        }

        /**
         * 构建 EventBusRuntime，未指定的组件使用默认实现。
         *
         * @return EventBusRuntime 实例
         */
        public EventBusRuntime build() {
            if (this.eventBus == null) {
                this.eventBus = new SyncEventBus();
            }
            return new EventBusRuntime(this.eventBus);
        }
    }
}
