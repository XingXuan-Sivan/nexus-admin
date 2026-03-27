package com.nexusadmin.plugin.admin.controller;

import com.nexusadmin.api.extension.web.AdminApi;
import com.nexusadmin.api.management.PluginAdminFacade;
import com.nexusadmin.api.management.PluginDetailView;
import com.nexusadmin.api.management.PluginView;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * 插件管理控制器。
 * <p>
 * 提供插件列表查询、启停控制等管理 API。
 * 使用 @AdminApi 注解，实际访问路径为 /admin/plugins。
 */
@RestController
@AdminApi
@RequestMapping("/plugins")
public class PluginManageController {

    private final PluginAdminFacade pluginAdminFacade;

    /**
     * 构造插件管理控制器。
     *
     * @param pluginAdminFacade 插件管理门面
     */
    public PluginManageController(PluginAdminFacade pluginAdminFacade) {
        this.pluginAdminFacade = pluginAdminFacade;
    }

    /**
     * 获取所有插件列表。
     *
     * @return 插件摘要列表
     */
    @GetMapping
    public List<PluginView> listAll() {
        return pluginAdminFacade.listAll();
    }

    /**
     * 获取插件详情。
     *
     * @param pluginId 插件标识
     * @return 插件详情
     */
    @GetMapping("/{pluginId}")
    public Optional<PluginDetailView> getDetail(@PathVariable("pluginId") String pluginId) {
        return pluginAdminFacade.getDetail(pluginId);
    }

    /**
     * 启动插件。
     *
     * @param pluginId 插件标识
     */
    @PostMapping("/{pluginId}/start")
    public void start(@PathVariable("pluginId") String pluginId) {
        pluginAdminFacade.start(pluginId);
    }

    /**
     * 停止插件。
     *
     * @param pluginId 插件标识
     */
    @PostMapping("/{pluginId}/stop")
    public void stop(@PathVariable("pluginId") String pluginId) {
        pluginAdminFacade.stop(pluginId);
    }

    /**
     * 启用插件。
     *
     * @param pluginId 插件标识
     */
    @PostMapping("/{pluginId}/enable")
    public void enable(@PathVariable("pluginId") String pluginId) {
        pluginAdminFacade.enable(pluginId);
    }

    /**
     * 禁用插件。
     *
     * @param pluginId 插件标识
     */
    @PostMapping("/{pluginId}/disable")
    public void disable(@PathVariable("pluginId") String pluginId) {
        pluginAdminFacade.disable(pluginId);
    }

    /**
     * 卸载插件。
     *
     * @param pluginId 插件标识
     */
    @DeleteMapping("/{pluginId}")
    public void unload(@PathVariable("pluginId") String pluginId) {
        pluginAdminFacade.unload(pluginId);
    }
}
