package com.nexusadmin.core.context;

import java.util.Objects;

/**
 * 插件运行上下文，聚合四层能力模型。
 *
 * <p>插件通过此上下文与平台交互，禁止直接获取 PluginManager。</p>
 *
 * <p>四层模型：</p>
 * <ul>
 *   <li>PluginInfo: 静态元数据（描述符、类加载器、物理路径）</li>
 *   <li>PluginRuntime: 运行时状态访问（状态查询）</li>
 *   <li>PluginWorkspace: 运行时工作空间（配置、数据、缓存、日志目录管理）</li>
 *   <li>PlatformAccess: 平台能力访问（扩展注册、事件发布）</li>
 * </ul>
 *
 * <p>
 * 其中 workspace 表示插件运行期间使用的工作空间，
 * 用于存储插件配置、运行数据、缓存以及日志等信息，采用懒加载机制按需创建。
 * </p>
 */
public final class PluginContext {

    private final PluginInfo info;
    private final PluginRuntime runtime;
    private final PluginWorkspace workspace;
    private final PlatformAccess platform;

    /**
     * 构造插件上下文。
     *
     * @param info      插件静态信息
     * @param runtime   插件运行时状态
     * @param workspace 插件运行时工作空间
     * @param platform  平台能力访问
     */
    public PluginContext(PluginInfo info, PluginRuntime runtime, PluginWorkspace workspace, PlatformAccess platform) {
        this.info = Objects.requireNonNull(info, "插件信息不能为空");
        this.runtime = Objects.requireNonNull(runtime, "插件运行时不能为空");
        this.workspace = Objects.requireNonNull(workspace, "插件工作空间不能为空");
        this.platform = Objects.requireNonNull(platform, "平台访问不能为空");
    }

    /**
     * 获取插件静态信息。
     *
     * @return 插件信息
     */
    public PluginInfo info() {
        return info;
    }

    /**
     * 获取插件运行时状态访问。
     *
     * @return 插件运行时
     */
    public PluginRuntime runtime() {
        return runtime;
    }

    /**
     * 获取插件运行时工作空间。
     *
     * @return 插件工作空间
     */
    public PluginWorkspace workspace() {
        return workspace;
    }

    /**
     * 获取平台能力访问。
     *
     * @return 平台访问
     */
    public PlatformAccess platform() {
        return platform;
    }
}
