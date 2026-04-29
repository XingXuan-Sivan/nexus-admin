package com.nexusadmin.api.controller;

import com.nexusadmin.api.management.AdminFacade;
import com.nexusadmin.api.management.PluginDetailView;
import com.nexusadmin.api.management.PluginView;
import com.nexusadmin.api.result.DataResult;
import com.nexusadmin.api.result.Result;
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
 * 作为平台内建能力，直接映射到 /admin/plugins 路径。
 */
@RestController
@RequestMapping("/admin/plugins")
public class PluginManageController {

    private final AdminFacade adminFacade;

    /**
     * 构造插件管理控制器。
     *
     * @param adminFacade 管理门面
     */
    public PluginManageController(AdminFacade adminFacade) {
        this.adminFacade = adminFacade;
    }

    /**
     * 获取所有插件列表。
     *
     * @return 插件摘要列表
     */
    @GetMapping
    public DataResult<List<PluginView>> listAll() {
        return DataResult.success(adminFacade.plugins().listAll());
    }

    /**
     * 获取插件详情。
     *
     * @param pluginId 插件标识
     * @return 插件详情
     */
    @GetMapping("/{pluginId}")
    public DataResult<PluginDetailView> getDetail(@PathVariable("pluginId") String pluginId) {
        return DataResult.success(adminFacade.plugins().getDetail(pluginId).orElse(null));
    }

    /**
     * 启动插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @PostMapping("/{pluginId}/start")
    public Result start(@PathVariable("pluginId") String pluginId) {
        adminFacade.plugins().start(pluginId);
        return Result.success();
    }

    /**
     * 停止插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @PostMapping("/{pluginId}/stop")
    public Result stop(@PathVariable("pluginId") String pluginId) {
        adminFacade.plugins().stop(pluginId);
        return Result.success();
    }

    /**
     * 启用插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @PostMapping("/{pluginId}/enable")
    public Result enable(@PathVariable("pluginId") String pluginId) {
        adminFacade.plugins().enable(pluginId);
        return Result.success();
    }

    /**
     * 禁用插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @PostMapping("/{pluginId}/disable")
    public Result disable(@PathVariable("pluginId") String pluginId) {
        adminFacade.plugins().disable(pluginId);
        return Result.success();
    }

    /**
     * 卸载插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @DeleteMapping("/{pluginId}")
    public Result unload(@PathVariable("pluginId") String pluginId) {
        adminFacade.plugins().unload(pluginId);
        return Result.success();
    }
}
