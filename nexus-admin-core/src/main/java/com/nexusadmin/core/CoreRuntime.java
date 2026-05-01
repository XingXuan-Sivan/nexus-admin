package com.nexusadmin.core;

import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.event.SyncEventBus;
import com.nexusadmin.core.extension.DefaultExtensionRegistry;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.plugin.DefaultPluginRegistry;
import com.nexusadmin.core.plugin.PluginRegistry;
import com.nexusadmin.core.plugin.loader.DefaultPluginLoader;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.resolve.DefaultDependenceManager;
import com.nexusadmin.core.plugin.resolve.DependenceManager;
import com.nexusadmin.core.plugin.resolve.VersionManager;
import com.nexusadmin.core.plugin.resolve.version.DefaultVersionManager;

import java.util.Objects;

/**
 * Core 运行时聚合根。
 * <p>
 * 负责装配核心组件的默认实例，不依赖任何外部 DI 容器。
 * 所有组件均可通过 Builder 选择性覆盖，未指定的使用默认实现。
 * <p>
 * <strong>使用示例：</strong>
 * <pre>{@code
 * // 全默认装配
 * CoreRuntime rt = CoreRuntime.defaults();
 *
 * // 选择性覆盖
 * CoreRuntime rt = CoreRuntime.builder()
 *     .eventBus(new MyAsyncEventBus())
 *     .build();
 *
 * // 访问组件
 * EventBus bus = rt.eventBus();
 * ExtensionRegistry registry = rt.extensionRegistry();
 * }</pre>
 */
public final class CoreRuntime {

    private final EventBus eventBus;
    private final ExtensionRegistry extensionRegistry;
    private final PluginRegistry pluginRegistry;
    private final VersionManager versionManager;
    private final DependenceManager dependenceManager;
    private final PluginLoader pluginLoader;

    /**
     * 构造 CoreRuntime。
     * <p>依赖关系在构造时完成注入，实例创建后不可变。</p>
     *
     * @param builder Builder 实例
     */
    private CoreRuntime(Builder builder) {
        this.eventBus = Objects.requireNonNull(builder.eventBus, "eventBus 不能为空");
        this.pluginRegistry = Objects.requireNonNull(builder.pluginRegistry, "pluginRegistry 不能为空");
        this.versionManager = Objects.requireNonNull(builder.versionManager, "versionManager 不能为空");
        this.pluginLoader = Objects.requireNonNull(builder.pluginLoader, "pluginLoader 不能为空");

        // ExtensionRegistry 依赖 EventBus，若未指定则使用默认实现自动注入
        this.extensionRegistry = builder.extensionRegistry != null
                ? builder.extensionRegistry
                : new DefaultExtensionRegistry(this.eventBus);

        // DependenceManager 依赖 VersionManager，若未指定则使用默认实现自动注入
        this.dependenceManager = builder.dependenceManager != null
                ? builder.dependenceManager
                : new DefaultDependenceManager(this.versionManager);
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
     * 获取插件注册中心。
     *
     * @return PluginRegistry 实例
     */
    public PluginRegistry pluginRegistry() {
        return pluginRegistry;
    }

    /**
     * 获取版本管理器。
     *
     * @return VersionManager 实例
     */
    public VersionManager versionManager() {
        return versionManager;
    }

    /**
     * 获取依赖管理器。
     *
     * @return DependenceManager 实例
     */
    public DependenceManager dependenceManager() {
        return dependenceManager;
    }

    /**
     * 获取插件加载器。
     *
     * @return PluginLoader 实例
     */
    public PluginLoader pluginLoader() {
        return pluginLoader;
    }

    // ==================== 快速创建 ====================

    /**
     * 使用全部默认实现创建 CoreRuntime。
     *
     * @return 配置完成的 CoreRuntime 实例
     */
    public static CoreRuntime defaults() {
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
     * CoreRuntime 建造器。
     * <p>通过链式调用选择性覆盖组件，{@link #build()} 时补全默认值。</p>
     */
    public static class Builder {
        private EventBus eventBus;
        private ExtensionRegistry extensionRegistry;
        private PluginRegistry pluginRegistry;
        private VersionManager versionManager;
        private DependenceManager dependenceManager;
        private PluginLoader pluginLoader;

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
         * 覆盖默认扩展注册中心。
         *
         * @param val ExtensionRegistry 实例
         * @return Builder 自身（链式调用）
         */
        public Builder extensionRegistry(ExtensionRegistry val) {
            this.extensionRegistry = val;
            return this;
        }

        /**
         * 覆盖默认插件注册中心。
         *
         * @param val PluginRegistry 实例
         * @return Builder 自身（链式调用）
         */
        public Builder pluginRegistry(PluginRegistry val) {
            this.pluginRegistry = val;
            return this;
        }

        /**
         * 覆盖默认版本管理器。
         *
         * @param val VersionManager 实例
         * @return Builder 自身（链式调用）
         */
        public Builder versionManager(VersionManager val) {
            this.versionManager = val;
            return this;
        }

        /**
         * 覆盖默认依赖管理器。
         * <p>若未指定，构建时自动使用 DefaultDependenceManager 并注入 VersionManager。</p>
         *
         * @param val DependenceManager 实例
         * @return Builder 自身（链式调用）
         */
        public Builder dependenceManager(DependenceManager val) {
            this.dependenceManager = val;
            return this;
        }

        /**
         * 覆盖默认插件加载器。
         *
         * @param val PluginLoader 实例
         * @return Builder 自身（链式调用）
         */
        public Builder pluginLoader(PluginLoader val) {
            this.pluginLoader = val;
            return this;
        }

        /**
         * 构建 CoreRuntime，未指定的组件使用默认实现。
         *
         * @return CoreRuntime 实例
         */
        public CoreRuntime build() {
            if (this.eventBus == null) {
                this.eventBus = new SyncEventBus();
            }
            if (this.pluginRegistry == null) {
                this.pluginRegistry = new DefaultPluginRegistry();
            }
            if (this.versionManager == null) {
                this.versionManager = new DefaultVersionManager();
            }
            if (this.pluginLoader == null) {
                this.pluginLoader = new DefaultPluginLoader();
            }
            return new CoreRuntime(this);
        }
    }
}
