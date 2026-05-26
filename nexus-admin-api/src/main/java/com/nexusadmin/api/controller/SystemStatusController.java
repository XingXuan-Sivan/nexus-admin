package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.domain.view.PlatformInfoView;
import com.nexusadmin.api.domain.view.SystemStatusView;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.service.SystemHealthProvider;
import com.nexusadmin.api.service.SystemStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 系统状态控制器。
 * <p>
 * 提供系统运行状态查询与健康检查 API。
 * 作为平台内建能力，直接映射到 /admin/v1/system 路径。
 */
@RestController
@RequestMapping("/admin/v1/system")
@Tag(name = "系统状态")
public class SystemStatusController {

    private final SystemStatusService systemStatusService;
    private final SystemHealthProvider healthProvider;

    /**
     * 构造系统状态控制器。
     *
     * @param systemStatusService 系统状态服务
     * @param healthProvider      健康检查提供者（可选）
     */
    public SystemStatusController(SystemStatusService systemStatusService,
                                  @Nullable SystemHealthProvider healthProvider) {
        this.systemStatusService = systemStatusService;
        this.healthProvider = healthProvider;
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

    /**
     * 获取系统健康状态。
     *
     * @return 健康状态视图
     */
    @GetMapping("/health")
    @RequirePermission("system.view")
    @Operation(summary = "获取系统健康状态")
    public DataResult<Map<String, Object>> getHealth() {
        boolean healthy = systemStatusService.isHealthy();
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("healthy", healthy);
        result.put("overall", healthy ? "UP" : "DEGRADED");
        if (healthProvider != null) {
            result.put("overall", healthProvider.getOverallHealth().name());
            List<Map<String, Object>> components = healthProvider.getComponentHealths().stream()
                    .map(ch -> {
                        Map<String, Object> m = new java.util.LinkedHashMap<>();
                        m.put("name", ch.name());
                        m.put("status", ch.status().name());
                        m.put("details", ch.details());
                        m.put("metrics", ch.metrics());
                        return m;
                    })
                    .toList();
            result.put("components", components);
        }
        return DataResult.success(result);
    }
}
