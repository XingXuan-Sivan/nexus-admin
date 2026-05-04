package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.extension.ui.UIContributionRegistry;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.core.plugin.discovery.PluginContributes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * UI 元数据控制器。
 * <p>
 * 提供插件 UI 贡献声明、菜单树、动态路由等查询 API。
 */
@RestController
@RequestMapping("/admin/v1/ui")
@Tag(name = "UI 元数据")
public class UIController {

    private final UIContributionRegistry uiContributionRegistry;

    /**
     * 构造 UI 元数据控制器。
     *
     * @param uiContributionRegistry UI 贡献注册表
     */
    public UIController(UIContributionRegistry uiContributionRegistry) {
        this.uiContributionRegistry = uiContributionRegistry;
    }

    /**
     * 聚合所有已激活插件的 UI 贡献声明。
     *
     * @return 插件贡献映射
     */
    @GetMapping("/manifest")
    @RequirePermission("*")
    @Operation(summary = "获取 UI 贡献清单")
    public DataResult<Map<String, PluginContributes>> getManifest() {
        return DataResult.success(uiContributionRegistry.getManifest());
    }

    /**
     * 获取聚合后的菜单树。
     *
     * @return 菜单贡献列表
     */
    @GetMapping("/menus")
    @RequirePermission("*")
    @Operation(summary = "获取菜单树")
    public DataResult<List<PluginContributes.MenuContribution>> getMenus() {
        return DataResult.success(uiContributionRegistry.getMenus());
    }

    /**
     * 获取聚合后的动态路由列表。
     *
     * @return 路由贡献列表
     */
    @GetMapping("/routes")
    @RequirePermission("*")
    @Operation(summary = "获取动态路由")
    public DataResult<List<PluginContributes.RouteContribution>> getRoutes() {
        return DataResult.success(uiContributionRegistry.getRoutes());
    }
}
