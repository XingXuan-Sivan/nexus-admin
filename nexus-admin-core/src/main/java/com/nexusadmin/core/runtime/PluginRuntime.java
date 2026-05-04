package com.nexusadmin.core.runtime;

import com.nexusadmin.core.facade.PluginFacade;
import com.nexusadmin.core.plugin.DefaultPluginRegistry;
import com.nexusadmin.core.plugin.PluginRegistry;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorFinder;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorParser;
import com.nexusadmin.core.plugin.discovery.PluginSource;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.loader.impl.DefaultPluginLoader;
import com.nexusadmin.core.plugin.resolve.DependenceManager;
import com.nexusadmin.core.plugin.resolve.VersionManager;
import com.nexusadmin.core.plugin.resolve.impl.DefaultDependenceManager;
import com.nexusadmin.core.plugin.resolve.impl.DefaultVersionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 插件运行时。
 * <p>
 * 负责装配插件子系统的全部默认实例，不依赖任何外部 DI 容器。
 * 所有组件均可通过 Builder 选择性覆盖，未指定的使用默认实现。
 * <p>
 * DependenceManager 依赖 {@link VersionManager}，
 * 若未指定则自动创建 {@link DefaultDependenceManager} 并注入 VersionManager。
 * <p>
 * <strong>使用示例：</strong>
 * <pre>{@code
 * // 全默认装配
 * PluginRuntime rt = PluginRuntime.defaults();
 *
 * // 选择性覆盖 + 注入插件源
 * PluginRuntime rt = PluginRuntime.builder()
 *     .sources(List.of(new DevPluginSource(Path.of("plugins"))))
 *     .build();
 *
 * // 访问组件
 * PluginRegistry registry = rt.pluginRegistry();
 * PluginFacade facade = rt.facade();
 * }</pre>
 */
public final class PluginRuntime {

    private final PluginRegistry pluginRegistry;
    private final VersionManager versionManager;
    private final DependenceManager dependenceManager;
    private final PluginLoader pluginLoader;
    private final List<PluginSource> sources;
    private final List<PluginDescriptorFinder> finders;
    private final List<PluginDescriptorParser> parsers;

    /**
     * 构造 PluginRuntime。
     * <p>依赖关系在构造时完成注入，实例创建后不可变。</p>
     *
     * @param pluginRegistry    插件注册中心
     * @param versionManager    版本管理器
     * @param dependenceManager 依赖管理器
     * @param pluginLoader      插件加载器
     * @param sources           插件源列表
     * @param finders           描述文件查找器列表
     * @param parsers           描述文件解析器列表
     */
    private PluginRuntime(PluginRegistry pluginRegistry,
                          VersionManager versionManager,
                          DependenceManager dependenceManager,
                          PluginLoader pluginLoader,
                          List<PluginSource> sources,
                          List<PluginDescriptorFinder> finders,
                          List<PluginDescriptorParser> parsers) {
        this.pluginRegistry = Objects.requireNonNull(pluginRegistry, "pluginRegistry 不能为空");
        this.versionManager = Objects.requireNonNull(versionManager, "versionManager 不能为空");
        this.dependenceManager = Objects.requireNonNull(dependenceManager, "dependenceManager 不能为空");
        this.pluginLoader = Objects.requireNonNull(pluginLoader, "pluginLoader 不能为空");
        this.sources = List.copyOf(sources);
        this.finders = List.copyOf(finders);
        this.parsers = List.copyOf(parsers);
    }

    // ==================== 组件访问器 ====================

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

    /**
     * 获取插件源列表。
     *
     * @return 插件源列表
     */
    public List<PluginSource> sources() {
        return sources;
    }

    /**
     * 获取描述文件查找器列表。
     *
     * @return 查找器列表
     */
    public List<PluginDescriptorFinder> finders() {
        return finders;
    }

    /**
     * 获取描述文件解析器列表。
     *
     * @return 解析器列表
     */
    public List<PluginDescriptorParser> parsers() {
        return parsers;
    }

    /**
     * 获取插件组件门面。
     *
     * @return PluginFacade 实例
     */
    public PluginFacade facade() {
        return new PluginFacade(pluginRegistry, pluginLoader, versionManager,
                dependenceManager, sources, finders, parsers);
    }

    // ==================== 快速创建 ====================

    /**
     * 使用全部默认实现创建 PluginRuntime。
     *
     * @return 配置完成的 PluginRuntime 实例
     */
    public static PluginRuntime defaults() {
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
     * PluginRuntime 建造器。
     * <p>通过链式调用选择性覆盖组件，{@link #build()} 时补全默认值。</p>
     */
    public static class Builder {
        private PluginRegistry pluginRegistry;
        private VersionManager versionManager;
        private DependenceManager dependenceManager;
        private PluginLoader pluginLoader;
        private List<PluginSource> sources;
        private List<PluginDescriptorFinder> finders;
        private List<PluginDescriptorParser> parsers;

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
         * 设置插件源列表。
         * <p>默认为空列表，由外部配置注入。</p>
         *
         * @param val 插件源列表
         * @return Builder 自身（链式调用）
         */
        public Builder sources(List<PluginSource> val) {
            this.sources = val;
            return this;
        }

        /**
         * 设置描述文件查找器列表。
         * <p>默认为空列表，由外部配置注入。</p>
         *
         * @param val 查找器列表
         * @return Builder 自身（链式调用）
         */
        public Builder finders(List<PluginDescriptorFinder> val) {
            this.finders = val;
            return this;
        }

        /**
         * 设置描述文件解析器列表。
         * <p>默认为空列表，由外部配置注入。</p>
         *
         * @param val 解析器列表
         * @return Builder 自身（链式调用）
         */
        public Builder parsers(List<PluginDescriptorParser> val) {
            this.parsers = val;
            return this;
        }

        /**
         * 构建 PluginRuntime，未指定的组件使用默认实现。
         *
         * @return PluginRuntime 实例
         */
        public PluginRuntime build() {
            if (this.pluginRegistry == null) {
                this.pluginRegistry = new DefaultPluginRegistry();
            }
            if (this.versionManager == null) {
                this.versionManager = new DefaultVersionManager();
            }
            if (this.dependenceManager == null) {
                this.dependenceManager = new DefaultDependenceManager(this.versionManager);
            }
            if (this.pluginLoader == null) {
                this.pluginLoader = new DefaultPluginLoader();
            }
            List<PluginSource> srcList = this.sources != null
                    ? new ArrayList<>(this.sources) : List.of();
            List<PluginDescriptorFinder> finderList = this.finders != null
                    ? new ArrayList<>(this.finders) : List.of();
            List<PluginDescriptorParser> parserList = this.parsers != null
                    ? new ArrayList<>(this.parsers) : List.of();

            return new PluginRuntime(this.pluginRegistry, this.versionManager,
                    this.dependenceManager, this.pluginLoader,
                    srcList, finderList, parserList);
        }
    }
}
