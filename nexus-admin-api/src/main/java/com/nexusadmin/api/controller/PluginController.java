package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.domain.view.PluginDetailView;
import com.nexusadmin.api.domain.view.PluginView;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.domain.result.Result;
import com.nexusadmin.api.service.PluginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 插件管理控制器。
 * <p>
 * 提供插件列表查询、启停控制等管理 API。
 * 作为平台内建能力，直接映射到 /admin/v1/plugins 路径。
 */
@RestController
@RequestMapping("/admin/v1/plugins")
@Tag(name = "插件管理")
public class PluginController {

    private final PluginService pluginService;

    /**
     * 构造插件管理控制器。
     *
     * @param pluginService 插件管理服务
     */
    public PluginController(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    /**
     * 获取所有插件列表。
     *
     * @return 插件摘要列表
     */
    @GetMapping
    @RequirePermission("plugins.view")
    @Operation(summary = "获取插件列表")
    public DataResult<List<PluginView>> listAll() {
        return DataResult.success(pluginService.listAll());
    }

    /**
     * 获取插件详情。
     *
     * @param pluginId 插件标识
     * @return 插件详情
     */
    @GetMapping("/{pluginId}")
    @RequirePermission("plugins.view")
    @Operation(summary = "获取插件详情")
    public DataResult<PluginDetailView> getDetail(@PathVariable("pluginId") String pluginId) {
        return DataResult.success(pluginService.getDetail(pluginId).orElse(null));
    }

    /**
     * 启动插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @PostMapping("/{pluginId}/start")
    @RequirePermission("plugins.manage")
    @Operation(summary = "启动插件")
    public Result start(@PathVariable("pluginId") String pluginId) {
        pluginService.start(pluginId);
        return Result.success();
    }

    /**
     * 停止插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @PostMapping("/{pluginId}/stop")
    @RequirePermission("plugins.manage")
    @Operation(summary = "停止插件")
    public Result stop(@PathVariable("pluginId") String pluginId) {
        pluginService.stop(pluginId);
        return Result.success();
    }

    /**
     * 启用插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @PostMapping("/{pluginId}/enable")
    @RequirePermission("plugins.manage")
    @Operation(summary = "启用插件")
    public Result enable(@PathVariable("pluginId") String pluginId) {
        pluginService.enable(pluginId);
        return Result.success();
    }

    /**
     * 禁用插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @PostMapping("/{pluginId}/disable")
    @RequirePermission("plugins.manage")
    @Operation(summary = "禁用插件")
    public Result disable(@PathVariable("pluginId") String pluginId) {
        pluginService.disable(pluginId);
        return Result.success();
    }

    /**
     * 卸载插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @DeleteMapping("/{pluginId}")
    @RequirePermission("plugins.manage")
    @Operation(summary = "卸载插件")
    public Result unload(@PathVariable("pluginId") String pluginId) {
        pluginService.unload(pluginId);
        return Result.success();
    }
}
