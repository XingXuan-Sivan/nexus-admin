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
        // 获取管理门面服务
        adminFacade = service(AdminFacade.class).orElse(null);
        if (adminFacade == null) {
            log.warn("管理门面服务未就绪，部分功能可能受限");
        }
    }

    @Override
    protected void start() throws Exception {
        // 启动逻辑由基类日志记录
    }

    @Override
    protected void stop() throws Exception {
        // 停止逻辑由基类日志记录
    }

    @Override
    protected void unload() throws Exception {
        // 卸载逻辑由基类日志记录
    }

    @Override
    public List<Object> getControllers() {
        if (adminFacade == null) {
            adminFacade = service(AdminFacade.class).orElse(null);
        }
        if (adminFacade == null) {
            log.warn("管理门面服务未就绪，无法提供 Controller");
            return List.of();
        }
        return List.of(
                new PluginManageController(adminFacade.plugins()),
                new SystemStatusController(adminFacade.system())
        );
    }
}
