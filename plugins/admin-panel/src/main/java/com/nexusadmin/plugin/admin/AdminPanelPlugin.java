package com.nexusadmin.plugin.admin;

import com.nexusadmin.api.extension.web.WebControllerProvider;
import com.nexusadmin.api.management.AdminFacade;
import com.nexusadmin.api.management.PluginAdminFacade;
import com.nexusadmin.api.management.SystemStatusFacade;
import com.nexusadmin.core.AbstractPlugin;
import com.nexusadmin.plugin.admin.controller.PluginManageController;
import com.nexusadmin.plugin.admin.controller.SystemStatusController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 管理面板插件入口。
 * <p>
 * 提供平台管理 REST API 和基础认证能力。
 * Controller 通过 WebControllerProvider 接口提供，由 WebEndpointLifecycleListener 自动扫描注册。
 */
public class AdminPanelPlugin extends AbstractPlugin implements WebControllerProvider {

    private static final Logger log = LoggerFactory.getLogger(AdminPanelPlugin.class);

    private AdminFacade adminFacade;

    @Override
    protected void initialize() throws Exception {
        log.info("管理面板插件初始化开始，插件ID={}", pluginId());

        // 获取管理门面
        adminFacade = platform().adminFacade(AdminFacade.class).orElse(null);
        if (adminFacade == null) {
            log.warn("管理门面未就绪，部分功能可能受限");
        }

        log.info("管理面板插件初始化完成");
    }

    @Override
    protected void start() throws Exception {
        log.info("管理面板插件启动中...");
        log.info("管理面板插件启动完成");
    }

    @Override
    protected void stop() throws Exception {
        log.info("管理面板插件停止中...");
        log.info("管理面板插件停止完成");
    }

    @Override
    protected void unload() throws Exception {
        log.info("管理面板插件卸载完成");
    }

    @Override
    public List<Object> getControllers() {
        if (adminFacade == null) {
            adminFacade = platform().adminFacade(AdminFacade.class).orElse(null);
        }
        if (adminFacade == null) {
            log.warn("管理门面未就绪，无法提供 Controller");
            return List.of();
        }
        return List.of(
                new PluginManageController(adminFacade.plugins()),
                new SystemStatusController(adminFacade.system())
        );
    }
}
