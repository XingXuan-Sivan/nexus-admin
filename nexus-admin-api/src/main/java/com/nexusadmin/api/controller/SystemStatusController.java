package com.nexusadmin.api.controller;

import com.nexusadmin.api.management.AdminFacade;
import com.nexusadmin.api.management.PlatformInfoView;
import com.nexusadmin.api.management.SystemStatusView;
import com.nexusadmin.api.result.DataResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统状态控制器。
 * <p>
 * 提供系统运行状态查询 API。
 * 作为平台内建能力，直接映射到 /admin/system 路径。
 */
@RestController
@RequestMapping("/admin/system")
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
    public DataResult<SystemStatusView> getStatus() {
        return DataResult.success(adminFacade.system().getStatus());
    }

    /**
     * 获取平台信息。
     *
     * @return 平台信息视图
     */
    @GetMapping("/info")
    public DataResult<PlatformInfoView> getInfo() {
        return DataResult.success(adminFacade.system().getPlatformInfo());
    }
}
