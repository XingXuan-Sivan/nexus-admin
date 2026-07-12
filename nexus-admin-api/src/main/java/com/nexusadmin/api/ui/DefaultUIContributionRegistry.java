package com.nexusadmin.api.ui;

import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.PluginState;
import com.nexusadmin.core.plugin.discovery.PluginContributes;
import com.nexusadmin.core.plugin.discovery.PluginContributes.MenuContribution;
import com.nexusadmin.core.plugin.discovery.PluginContributes.RouteContribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UI 贡献注册表实现，聚合所有已激活插件的 contributes 声明。
 *
 * <p>通过 PluginManager 获取已激活插件列表，从每个插件的描述符中提取 contributes 信息。
 * 同时提供平台内建菜单作为基础导航结构。</p>
 */
public class DefaultUIContributionRegistry implements UIContributionRegistry {

    private static final Logger log = LoggerFactory.getLogger(DefaultUIContributionRegistry.class);

    /** 平台内建菜单：仪表盘 */
    private static final MenuContribution MENU_DASHBOARD = new MenuContribution(
            "dashboard", "仪表盘", "ri:dashboard-line", "", 0, "/dashboard"
    );

    /** 平台内建菜单：Nexus Copilot（叶子菜单） */
    private static final MenuContribution MENU_NEXUS_COPILOT = new MenuContribution(
            "nexus-copilot", "Nexus Copilot", "ri:robot-2-line", "", 5, "/nexus-copilot"
    );

    /** 平台内建菜单：插件管理（分组目录） */
    private static final MenuContribution MENU_PLUGINS = new MenuContribution(
            "plugins", "插件管理", "ri:plug-line", "", 10, "/plugins"
    );

    /** 平台内建菜单：插件列表（子菜单） */
    private static final MenuContribution MENU_PLUGINS_LIST = new MenuContribution(
            "plugins.list", "插件列表", "", "plugins", 1, "list"
    );

    /** 平台内建菜单：配置管理（子菜单） */
    private static final MenuContribution MENU_PLUGINS_CONFIG = new MenuContribution(
            "plugins.config", "配置管理", "", "plugins", 2, "config"
    );

    /** 平台内建菜单：AI 管理（分组目录） */
    private static final MenuContribution MENU_AI_MANAGEMENT = new MenuContribution(
            "ai-management", "AI 管理", "ri:brain-line", "", 20, "/ai"
    );

    /** 平台内建菜单：模型管理（子菜单，mock 先行） */
    private static final MenuContribution MENU_AI_MODELS = new MenuContribution(
            "ai.models", "模型管理", "", "ai-management", 1, "models"
    );

    /** 平台内建菜单：MCP 客户端管理（子菜单） */
    private static final MenuContribution MENU_AI_MCP_CLIENTS = new MenuContribution(
            "ai.mcp-clients", "MCP 客户端管理", "", "ai-management", 2, "mcp-clients"
    );

    /** 平台内建菜单：存储管理（分组目录，mock 先行） */
    private static final MenuContribution MENU_STORAGE_MANAGEMENT = new MenuContribution(
            "storage-management", "存储管理", "ri:hard-drive-2-line", "", 30, "/storage"
    );

    /** 平台内建菜单：缓存管理（子菜单，mock 先行） */
    private static final MenuContribution MENU_STORAGE_CACHE = new MenuContribution(
            "storage.cache", "缓存管理", "", "storage-management", 1, "cache"
    );

    /** 平台内建菜单：存储方式管理（子菜单，mock 先行） */
    private static final MenuContribution MENU_STORAGE_METHODS = new MenuContribution(
            "storage.methods", "存储方式管理", "", "storage-management", 2, "methods"
    );

    /** 平台内建菜单：日志管理（分组目录） */
    private static final MenuContribution MENU_LOGS_MANAGEMENT = new MenuContribution(
            "logs-management", "日志管理", "ri:file-list-3-line", "", 40, "/logs"
    );

    /** 平台内建菜单：登录日志（子菜单） */
    private static final MenuContribution MENU_LOGS_LOGIN = new MenuContribution(
            "logs.login", "登录日志", "", "logs-management", 1, "login"
    );

    /** 平台内建菜单：操作日志（子菜单） */
    private static final MenuContribution MENU_LOGS_OPERATION = new MenuContribution(
            "logs.operation", "操作日志", "", "logs-management", 2, "operation"
    );

    /** 平台内建菜单：任务日志（子菜单，mock 先行） */
    private static final MenuContribution MENU_LOGS_TASK = new MenuContribution(
            "logs.task", "任务日志", "", "logs-management", 3, "task"
    );

    /** 平台内建菜单：接口文档（叶子菜单，iframe 嵌入） */
    private static final MenuContribution MENU_API_DOCS = new MenuContribution(
            "api-docs", "接口文档", "ri:book-open-line", "", 50, "/api-docs"
    );

    /** 平台内建菜单：业务管理（分组目录，不可点击） */
    private static final MenuContribution MENU_BUSINESS = new MenuContribution(
            "business", "业务管理", "ri:briefcase-line", "", 60, "/business"
    );

    /** 平台内建菜单列表 */
    private static final List<MenuContribution> PLATFORM_MENUS = List.of(
            // 顶层叶子菜单
            MENU_DASHBOARD,
            MENU_NEXUS_COPILOT,
            // 插件管理（分组 + 子菜单）
            MENU_PLUGINS,
            MENU_PLUGINS_LIST,
            MENU_PLUGINS_CONFIG,
            // AI 管理（分组 + 子菜单，mock 先行）
            MENU_AI_MANAGEMENT,
            MENU_AI_MODELS,
            MENU_AI_MCP_CLIENTS,
            // 存储管理（分组 + 子菜单，mock 先行）
            MENU_STORAGE_MANAGEMENT,
            MENU_STORAGE_CACHE,
            MENU_STORAGE_METHODS,
            // 日志管理（分组 + 子菜单）
            MENU_LOGS_MANAGEMENT,
            MENU_LOGS_LOGIN,
            MENU_LOGS_OPERATION,
            MENU_LOGS_TASK,
            // 顶层叶子菜单
            MENU_API_DOCS,
            // 业务管理（分组目录，暂无子菜单）
            MENU_BUSINESS
    );

    /** 平台内建路由列表 */
    private static final List<RouteContribution> PLATFORM_ROUTES = List.of(
            new RouteContribution("/dashboard", "DashboardPage", "仪表盘", "ri:dashboard-line", List.of()),
            new RouteContribution("/nexus-copilot", "NexusCopilotPage", "Nexus Copilot", "ri:robot-2-line", List.of()),
            new RouteContribution("/plugins/list", "PluginListPage", "插件列表", "", List.of("plugins.view")),
            new RouteContribution("/plugins/config", "PluginConfigPage", "配置管理", "", List.of("config.view")),
            new RouteContribution("/ai/models", "AiModelsPage", "模型管理", "", List.of("ai.view")),
            new RouteContribution("/ai/mcp-clients", "AiMcpClientsPage", "MCP 客户端管理", "", List.of("ai.view")),
            new RouteContribution("/storage/cache", "StorageCachePage", "缓存管理", "", List.of("storage.view")),
            new RouteContribution("/storage/methods", "StorageMethodsPage", "存储方式管理", "", List.of("storage.view")),
            new RouteContribution("/logs/login", "LogsLoginPage", "登录日志", "", List.of("logs.view")),
            new RouteContribution("/logs/operation", "LogsOperationPage", "操作日志", "", List.of("logs.view")),
            new RouteContribution("/logs/task", "LogsTaskPage", "任务日志", "", List.of("logs.view")),
            new RouteContribution("/api-docs", "ApiDocsPage", "接口文档", "ri:book-open-line", List.of())
    );

    private final PluginManager pluginManager;

    /**
     * 构造 UI 贡献注册表。
     *
     * @param pluginManager 插件管理器
     */
    public DefaultUIContributionRegistry(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public Map<String, PluginContributes> getManifest() {
        Map<String, PluginContributes> manifest = new LinkedHashMap<>();
        for (var wrapper : pluginManager.listByState(PluginState.ACTIVE)) {
            PluginContributes contributes = wrapper.descriptor().contributes();
            if (!contributes.isEmpty()) {
                manifest.put(wrapper.getPluginId(), contributes);
            }
        }
        return manifest;
    }

    @Override
    public List<MenuContribution> getMenus() {
        List<MenuContribution> allMenus = new ArrayList<>(PLATFORM_MENUS);
        pluginManager.listByState(PluginState.ACTIVE).stream()
                .flatMap(wrapper -> wrapper.descriptor().contributes().menus().stream())
                .forEach(allMenus::add);
        allMenus.sort(Comparator.comparingInt(MenuContribution::order));
        return List.copyOf(allMenus);
    }

    @Override
    public List<RouteContribution> getRoutes() {
        List<RouteContribution> allRoutes = new ArrayList<>(PLATFORM_ROUTES);
        pluginManager.listByState(PluginState.ACTIVE).stream()
                .flatMap(wrapper -> wrapper.descriptor().contributes().routes().stream())
                .forEach(allRoutes::add);
        return List.copyOf(allRoutes);
    }
}
