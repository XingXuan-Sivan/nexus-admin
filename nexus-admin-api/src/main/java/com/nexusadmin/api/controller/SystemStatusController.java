package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.domain.view.PlatformInfoView;
import com.nexusadmin.api.domain.view.SystemStatusView;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.service.SystemStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统状态控制器。
 * <p>
 * 提供系统运行状态查询 API。
 * 作为平台内建能力，直接映射到 /admin/v1/system 路径。
 */
@RestController
@RequestMapping("/admin/v1/system")
@Tag(name = "系统状态")
public class SystemStatusController {

    private final SystemStatusService systemStatusService;

    /**
     * 构造系统状态控制器。
     *
     * @param systemStatusService 系统状态服务
     */
    public SystemStatusController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    /**
     * 获取系统状态。
     *
     * @return 系统状态视图
     */
    @GetMapping("/status")
    @RequirePermission("system.view")
    @Operation(summary = "获取系统状态")
    public DataResult<SystemStatusView> getStatus() {
        return DataResult.success(systemStatusService.getStatus());
    }

    /**
     * 获取平台信息。
     *
     * @return 平台信息视图
     */
    @GetMapping("/info")
    @RequirePermission("system.view")
    @Operation(summary = "获取平台信息")
    public DataResult<PlatformInfoView> getInfo() {
        return DataResult.success(systemStatusService.getPlatformInfo());
    }
}
