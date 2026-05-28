package com.nexusadmin.api.ui;

import com.nexusadmin.core.plugin.discovery.PluginContributes;

import java.util.List;
import java.util.Map;

/**
 * UI 贡献注册表接口，聚合所有已激活插件的 contributes 声明。
 *
 * <p>提供菜单树、动态路由、完整清单等查询能力，供 UIController 委托调用。</p>
 *
 * <p>具体实现由 app 模块提供，因为需要访问 PluginManager 获取已激活插件列表。</p>
 */
public interface UIContributionRegistry {

    /**
     * 聚合所有已激活插件的 UI 贡献声明。
     *
     * <p>返回结构为以插件 ID 为键、对应 contributes 为值的映射。</p>
     *
     * @return 插件贡献映射，不为空
     */
    Map<String, PluginContributes> getManifest();

    /**
     * 获取聚合后的菜单树。
     *
     * <p>从所有已激活插件的菜单贡献中合并构建。</p>
     *
     * @return 菜单贡献列表，不为空
     */
    List<PluginContributes.MenuContribution> getMenus();

    /**
     * 获取聚合后的动态路由列表。
     *
     * <p>从所有已激活插件的路由贡献中合并构建。</p>
     *
     * @return 路由贡献列表，不为空
     */
    List<PluginContributes.RouteContribution> getRoutes();
}
