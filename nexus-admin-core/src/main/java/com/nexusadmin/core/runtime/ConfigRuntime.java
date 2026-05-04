package com.nexusadmin.core.runtime;

import com.nexusadmin.core.config.ConfigManager;
import com.nexusadmin.core.config.DefaultConfigManager;
import com.nexusadmin.core.config.binder.ConfigUIBuilder;
import com.nexusadmin.core.config.resolver.ConfigResolver;
import com.nexusadmin.core.config.resolver.ConfigSource;
import com.nexusadmin.core.config.resolver.impl.DefaultConfigSource;
import com.nexusadmin.core.config.resolver.impl.EnvConfigSource;
import com.nexusadmin.core.config.resolver.impl.FileConfigSource;
import com.nexusadmin.core.config.schema.SchemaProvider;
import com.nexusadmin.core.config.schema.SchemaRegistry;
import com.nexusadmin.core.config.schema.SchemaValidator;
import com.nexusadmin.core.config.schema.impl.JsonPluginSchemaProvider;
import com.nexusadmin.core.config.schema.impl.JsonSchemaValidator;
import com.nexusadmin.core.config.schema.impl.PlatformSchemaProvider;
import com.nexusadmin.core.config.store.ConfigStore;
import com.nexusadmin.core.config.store.FileConfigStore;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.facade.ConfigFacade;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 配置中心运行时。
 * <p>
 * 负责装配配置中心全部子组件的默认实例，将原本散落在
 * DefaultPluginManager.initializeConfigComponents() 和 DefaultConfigManager 构造函数中的
 * 硬编码装配逻辑统一收敛到此运行时。
 * <p>
 * 不依赖任何外部 DI 容器，所有组件均可通过 Builder 选择性覆盖，未指定的使用默认实现。
 * <p>
 * <strong>使用示例：</strong>
 * <pre>{@code
 * // 最简装配（必须提供 configDir 和 eventBus）
 * ConfigRuntime rt = ConfigRuntime.builder()
 *     .configDir(Path.of("plugins-data/config"))
 *     .eventBus(sharedEventBus)
 *     .build();
 *
 * // 选择性覆盖
 * ConfigRuntime rt = ConfigRuntime.builder()
 *     .configDir(Path.of("plugins-data/config"))
 *     .eventBus(sharedEventBus)
 *     .schemaProviders(List.of(myCustomProvider))
 *     .build();
 *
 * // 访问组件
 * ConfigManager mgr = rt.configManager();
 * ConfigFacade facade = rt.facade();
 * }</pre>
 */
public final class ConfigRuntime {

    private final ConfigManager configManager;
    private final SchemaRegistry schemaRegistry;
    private final ConfigResolver configResolver;
    private final ConfigStore configStore;
    private final ConfigUIBuilder uiBuilder;
    private final EventBus eventBus;
    private final Path configDir;
    private final ClassLoader platformClassLoader;

    /**
     * 构造 ConfigRuntime。
     * <p>依赖关系在构造时完成注入，实例创建后不可变。</p>
     *
     * @param configManager       配置管理器
     * @param schemaRegistry      Schema 注册中心
     * @param configResolver      配置解析器
     * @param configStore         配置存储
     * @param uiBuilder           UI 构建器
     * @param eventBus            事件总线
     * @param configDir           配置目录
     * @param platformClassLoader 平台类加载器
     */
    private ConfigRuntime(ConfigManager configManager,
                          SchemaRegistry schemaRegistry,
                          ConfigResolver configResolver,
                          ConfigStore configStore,
                          ConfigUIBuilder uiBuilder,
                          EventBus eventBus,
                          Path configDir,
                          ClassLoader platformClassLoader) {
        this.configManager = Objects.requireNonNull(configManager, "configManager 不能为空");
        this.schemaRegistry = Objects.requireNonNull(schemaRegistry, "schemaRegistry 不能为空");
        this.configResolver = Objects.requireNonNull(configResolver, "configResolver 不能为空");
        this.configStore = Objects.requireNonNull(configStore, "configStore 不能为空");
        this.uiBuilder = Objects.requireNonNull(uiBuilder, "uiBuilder 不能为空");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus 不能为空");
        this.configDir = configDir;
        this.platformClassLoader = platformClassLoader;
    }

    // ==================== 组件访问器 ====================

    /**
     * 获取配置管理器。
     *
     * @return ConfigManager 实例
     */
    public ConfigManager configManager() {
        return configManager;
    }

    /**
     * 获取 Schema 注册中心。
     *
     * @return SchemaRegistry 实例
     */
    public SchemaRegistry schemaRegistry() {
        return schemaRegistry;
    }

    /**
     * 获取配置解析器。
     *
     * @return ConfigResolver 实例
     */
    public ConfigResolver configResolver() {
        return configResolver;
    }

    /**
     * 获取配置存储。
     *
     * @return ConfigStore 实例
     */
    public ConfigStore configStore() {
        return configStore;
    }

    /**
     * 获取 UI 构建器。
     *
     * @return ConfigUIBuilder 实例
     */
    public ConfigUIBuilder uiBuilder() {
        return uiBuilder;
    }

    /**
     * 获取事件总线。
     *
     * @return EventBus 实例
     */
    public EventBus eventBus() {
        return eventBus;
    }

    /**
     * 获取配置目录。
     *
     * @return 配置目录路径，可能为 null
     */
    public Path configDir() {
        return configDir;
    }

    /**
     * 获取平台类加载器。
     *
     * @return 平台类加载器，可能为 null
     */
    public ClassLoader platformClassLoader() {
        return platformClassLoader;
    }

    /**
     * 获取配置管理门面。
     *
     * @return ConfigFacade 实例
     */
    public ConfigFacade facade() {
        return new ConfigFacade(configManager);
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
     * ConfigRuntime 建造器。
     * <p>通过链式调用选择性覆盖组件，{@link #build()} 时补全默认值。</p>
     * <p>必填字段：eventBus、configDir（未提供 configStore 时必须指定）。</p>
     */
    public static class Builder {
        private ConfigStore configStore;
        private SchemaRegistry schemaRegistry;
        private ConfigResolver configResolver;
        private ConfigUIBuilder uiBuilder;
        private EventBus eventBus;
        private Path configDir;
        private ClassLoader platformClassLoader;
        private List<SchemaProvider> schemaProviders;
        private List<SchemaValidator> schemaValidators;
        private List<ConfigSource> configSources;

        /**
         * 覆盖默认配置存储。
         * <p>若未指定，构建时自动创建 {@link FileConfigStore}（需提供 configDir）。</p>
         *
         * @param val ConfigStore 实例
         * @return Builder 自身（链式调用）
         */
        public Builder configStore(ConfigStore val) {
            this.configStore = val;
            return this;
        }

        /**
         * 覆盖默认 Schema 注册中心。
         * <p>若未指定，构建时自动创建 {@link SchemaRegistry}。</p>
         *
         * @param val SchemaRegistry 实例
         * @return Builder 自身（链式调用）
         */
        public Builder schemaRegistry(SchemaRegistry val) {
            this.schemaRegistry = val;
            return this;
        }

        /**
         * 覆盖默认配置解析器。
         * <p>若未指定，构建时自动创建 {@link ConfigResolver} 并注册默认配置源。</p>
         *
         * @param val ConfigResolver 实例
         * @return Builder 自身（链式调用）
         */
        public Builder configResolver(ConfigResolver val) {
            this.configResolver = val;
            return this;
        }

        /**
         * 覆盖默认 UI 构建器。
         * <p>若未指定，构建时自动创建 {@link ConfigUIBuilder}。</p>
         *
         * @param val ConfigUIBuilder 实例
         * @return Builder 自身（链式调用）
         */
        public Builder uiBuilder(ConfigUIBuilder val) {
            this.uiBuilder = val;
            return this;
        }

        /**
         * 设置事件总线（外部注入）。
         * <p>ConfigManager 依赖 EventBus 发布配置变更事件，必须提供。</p>
         *
         * @param val EventBus 实例
         * @return Builder 自身（链式调用）
         */
        public Builder eventBus(EventBus val) {
            this.eventBus = val;
            return this;
        }

        /**
         * 设置配置目录。
         * <p>未提供 configStore 时必须指定，用于创建默认的 FileConfigStore 和 FileConfigSource。</p>
         *
         * @param val 配置目录路径
         * @return Builder 自身（链式调用）
         */
        public Builder configDir(Path val) {
            this.configDir = val;
            return this;
        }

        /**
         * 设置平台类加载器。
         * <p>用于加载平台 Schema 资源和平台默认配置。</p>
         *
         * @param val 平台类加载器
         * @return Builder 自身（链式调用）
         */
        public Builder platformClassLoader(ClassLoader val) {
            this.platformClassLoader = val;
            return this;
        }

        /**
         * 设置自定义 Schema 提供者列表，覆盖默认提供者。
         * <p>若未指定，构建时自动注册 {@link JsonPluginSchemaProvider} 和 {@link PlatformSchemaProvider}。</p>
         *
         * @param val Schema 提供者列表
         * @return Builder 自身（链式调用）
         */
        public Builder schemaProviders(List<SchemaProvider> val) {
            this.schemaProviders = val;
            return this;
        }

        /**
         * 设置自定义 Schema 验证器列表，覆盖默认验证器。
         * <p>若未指定，构建时自动注册 {@link JsonSchemaValidator}。</p>
         *
         * @param val Schema 验证器列表
         * @return Builder 自身（链式调用）
         */
        public Builder schemaValidators(List<SchemaValidator> val) {
            this.schemaValidators = val;
            return this;
        }

        /**
         * 设置自定义配置源列表，覆盖默认配置源。
         * <p>若未指定，构建时自动注册 EnvConfigSource、FileConfigSource 和 DefaultConfigSource。</p>
         *
         * @param val 配置源列表
         * @return Builder 自身（链式调用）
         */
        public Builder configSources(List<ConfigSource> val) {
            this.configSources = val;
            return this;
        }

        /**
         * 构建 ConfigRuntime，未指定的组件使用默认实现。
         * <p>装配顺序：</p>
         * <ol>
         *   <li>创建 ConfigStore（默认 FileConfigStore）</li>
         *   <li>创建 SchemaRegistry</li>
         *   <li>创建 ConfigResolver 并注册 ConfigSource</li>
         *   <li>创建 ConfigUIBuilder</li>
         *   <li>创建 DefaultConfigManager</li>
         *   <li>注册 SchemaProvider</li>
         *   <li>注册 SchemaValidator</li>
         * </ol>
         *
         * @return ConfigRuntime 实例
         */
        public ConfigRuntime build() {
            Objects.requireNonNull(this.eventBus, "eventBus 不能为空（配置中心依赖事件总线发布变更事件）");

            // 1. ConfigStore
            ConfigStore store = this.configStore;
            if (store == null) {
                Objects.requireNonNull(this.configDir,
                        "configDir 不能为空（未提供 configStore 时必须指定配置目录）");
                store = new FileConfigStore(this.configDir);
            }

            // 2. SchemaRegistry
            SchemaRegistry registry = this.schemaRegistry;
            if (registry == null) {
                registry = new SchemaRegistry();
            }

            // 3. ConfigResolver + ConfigSource
            ConfigResolver resolver = this.configResolver;
            boolean resolverCreated = (resolver == null);
            if (resolverCreated) {
                resolver = new ConfigResolver();
            }

            if (this.configSources != null) {
                // 使用自定义配置源列表
                for (ConfigSource source : this.configSources) {
                    resolver.addSource(source);
                }
            } else if (resolverCreated) {
                // 添加默认配置源：环境变量 > 文件 > 插件默认
                resolver.addSource(new EnvConfigSource());
                resolver.addSource(new FileConfigSource(this.configDir));
                resolver.addSource(new DefaultConfigSource(pluginId ->
                        "platform".equals(pluginId) ? this.platformClassLoader : null));
            }

            // 4. ConfigUIBuilder
            ConfigUIBuilder builder = this.uiBuilder;
            if (builder == null) {
                builder = new ConfigUIBuilder(registry);
            }

            // 5. 创建 DefaultConfigManager
            DefaultConfigManager manager = new DefaultConfigManager(
                    resolver, registry, store, this.eventBus, builder);

            // 6. SchemaProvider
            if (this.schemaProviders != null) {
                for (SchemaProvider provider : this.schemaProviders) {
                    manager.registerSchemaProvider(provider);
                }
            } else {
                manager.registerSchemaProvider(new JsonPluginSchemaProvider());
                manager.registerSchemaProvider(new PlatformSchemaProvider());
            }

            // 7. SchemaValidator
            if (this.schemaValidators != null) {
                for (SchemaValidator validator : this.schemaValidators) {
                    manager.registerSchemaValidator(validator);
                }
            } else {
                manager.registerSchemaValidator(new JsonSchemaValidator());
            }

            return new ConfigRuntime(manager, registry, resolver, store, builder,
                    this.eventBus, this.configDir, this.platformClassLoader);
        }
    }
}
