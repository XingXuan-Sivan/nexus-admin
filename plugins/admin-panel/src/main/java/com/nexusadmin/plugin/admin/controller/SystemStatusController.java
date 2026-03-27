package com.nexusadmin.plugin.admin.controller;

import com.nexusadmin.api.extension.web.AdminApi;
import com.nexusadmin.api.management.AdminFacade;
import com.nexusadmin.api.management.PlatformInfoView;
import com.nexusadmin.api.management.SystemStatusView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统状态控制器。
 * <p>
 * 提供系统运行状态查询 API。
 * 使用 @AdminApi 注解，实际访问路径为 /admin/system。
 */
@RestController
@AdminApi
@RequestMapping("/system")
public class SystemStatusController {

    private final AdminFacade adminFacade;

    /**
     * 构造系统状态控制器。
     *
     * @param adminFacade 管理门面
     */
    public SystemStatusController(AdminFacade adminFacade) {
        this.adminFacade = adminFacade;
    }

    /**
     * 获取系统状态。
     *
     * @return 系统状态视图
     */
    @GetMapping("/status")
    public SystemStatusView getStatus() {
        return adminFacade.system().getStatus();
    }

    /**
     * 获取平台信息。
     *
     * @return 平台信息视图
     */
    @GetMapping("/info")
    public PlatformInfoView getInfo() {
        return adminFacade.system().getPlatformInfo();
    }
}
