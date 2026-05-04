package com.nexusadmin.core.runtime;

import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.event.SyncEventBus;
import com.nexusadmin.core.extension.DefaultExtensionRegistry;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.facade.ExtensionFacade;

import java.util.Objects;

/**
 * 扩展注册中心运行时。
 * <p>
 * 负责装配 {@link ExtensionRegistry} 的默认实例，不依赖任何外部 DI 容器。
 * 组件可通过 Builder 选择性覆盖，未指定的使用默认实现。
 * <p>
 * ExtensionRegistry 依赖 {@link EventBus} 做扩展变更事件通知，
 * 若未指定则自动创建 {@link SyncEventBus} 作为默认实现。
 * <p>
 * <strong>使用示例：</strong>
 * <pre>{@code
 * // 全默认装配（内部创建 SyncEventBus）
 * ExtensionRuntime rt = ExtensionRuntime.defaults();
 *
 * // 注入外部 EventBus（推荐，与其他运行时共享同一总线）
 * ExtensionRuntime rt = ExtensionRuntime.builder()
 *     .eventBus(sharedEventBus)
 *     .build();
 *
 * // 访问组件
 * ExtensionRegistry registry = rt.extensionRegistry();
 * ExtensionFacade facade = rt.facade();
 * }</pre>
 */
public final class ExtensionRuntime {

    private final EventBus eventBus;
    private final ExtensionRegistry extensionRegistry;

    /**
     * 构造 ExtensionRuntime。
     * <p>依赖关系在构造时完成注入，实例创建后不可变。</p>
     *
     * @param eventBus           事件总线实例
     * @param extensionRegistry  扩展注册中心实例
     */
    private ExtensionRuntime(EventBus eventBus, ExtensionRegistry extensionRegistry) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus 不能为空");
        this.extensionRegistry = Objects.requireNonNull(extensionRegistry, "extensionRegistry 不能为空");
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
     * 获取扩展注册中心。
     *
     * @return ExtensionRegistry 实例
     */
    public ExtensionRegistry extensionRegistry() {
        return extensionRegistry;
    }

    /**
     * 获取扩展注册中心门面。
     *
     * @return ExtensionFacade 实例
     */
    public ExtensionFacade facade() {
        return new ExtensionFacade(extensionRegistry);
    }

    // ==================== 快速创建 ====================

    /**
     * 使用全部默认实现创建 ExtensionRuntime。
     * <p>内部自动创建 {@link SyncEventBus} 和 {@link DefaultExtensionRegistry}。</p>
     *
     * @return 配置完成的 ExtensionRuntime 实例
     */
    public static ExtensionRuntime defaults() {
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
     * ExtensionRuntime 建造器。
     * <p>通过链式调用选择性覆盖组件，{@link #build()} 时补全默认值。</p>
     */
    public static class Builder {
        private EventBus eventBus;
        private ExtensionRegistry extensionRegistry;

        /**
         * 设置事件总线。
         * <p>若未指定，构建时自动创建 {@link SyncEventBus}。</p>
         *
         * @param val EventBus 实例
         * @return Builder 自身（链式调用）
         */
        public Builder eventBus(EventBus val) {
            this.eventBus = val;
            return this;
        }

        /**
         * 覆盖默认扩展注册中心。
         * <p>若未指定，构建时自动使用 DefaultExtensionRegistry 并注入 EventBus。</p>
         *
         * @param val ExtensionRegistry 实例
         * @return Builder 自身（链式调用）
         */
        public Builder extensionRegistry(ExtensionRegistry val) {
            this.extensionRegistry = val;
            return this;
        }

        /**
         * 构建 ExtensionRuntime，未指定的组件使用默认实现。
         *
         * @return ExtensionRuntime 实例
         */
        public ExtensionRuntime build() {
            if (this.eventBus == null) {
                this.eventBus = new SyncEventBus();
            }
            if (this.extensionRegistry == null) {
                this.extensionRegistry = new DefaultExtensionRegistry(this.eventBus);
            }
            return new ExtensionRuntime(this.eventBus, this.extensionRegistry);
        }
    }
}
