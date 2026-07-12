package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.view.PlatformInfoView;
import com.nexusadmin.api.domain.view.SystemStatusView;
import com.nexusadmin.core.PluginState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 系统状态服务。
 *
 * <p>提供系统运行状态的查询能力，聚合插件状态、JVM 信息、平台元信息。</p>
 * <p>支持通过声明同类型 Bean 覆盖，便于插件提供定制实现。</p>
 */
@Service
public class SystemStatusService {

    private static final Logger log = LoggerFactory.getLogger(SystemStatusService.class);

    private final com.nexusadmin.core.facade.PluginFacade pluginFacade;
    private final com.nexusadmin.core.facade.ConfigFacade configFacade;

    /**
     * 构造系统状态服务。
     *
     * @param pluginFacade 核心插件组件门面
     * @param configFacade 核心配置管理门面
     */
    public SystemStatusService(com.nexusadmin.core.facade.PluginFacade pluginFacade,
                               com.nexusadmin.core.facade.ConfigFacade configFacade) {
        this.pluginFacade = pluginFacade;
        this.configFacade = configFacade;
    }

    /**
     * 获取系统运行状态。
     *
     * @return 系统状态视图，不为空
     */
    public SystemStatusView getStatus() {
        int totalPlugins = pluginFacade.listPlugins().size();
        int activePlugins = pluginFacade.listByState(PluginState.ACTIVE).size();
        int disabledPlugins = pluginFacade.listByState(PluginState.DISABLED).size();
        int failedPlugins = pluginFacade.listByState(PluginState.FAILED).size();

        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

        double systemCpuLoad = osBean.getSystemCpuLoad();
        double processCpuLoad = osBean.getProcessCpuLoad();
        // 归一化：首次调用或不可用时返回 -1，NaN 表示无数据，统一置 0
        if (Double.isNaN(systemCpuLoad) || systemCpuLoad < 0) {
            systemCpuLoad = 0;
        }
        if (Double.isNaN(processCpuLoad) || processCpuLoad < 0) {
            processCpuLoad = 0;
        }

        Map<String, String> jvmInfo = Map.of(
                "maxMemory", String.valueOf(Runtime.getRuntime().maxMemory()),
                "totalMemory", String.valueOf(Runtime.getRuntime().totalMemory()),
                "freeMemory", String.valueOf(Runtime.getRuntime().freeMemory()),
                "availableProcessors", String.valueOf(Runtime.getRuntime().availableProcessors()),
                "javaVersion", System.getProperty("java.version"),
                "osTotalMemory", String.valueOf(osBean.getTotalMemorySize()),
                "osFreeMemory", String.valueOf(osBean.getFreeMemorySize()),
                "systemCpuLoad", String.valueOf(systemCpuLoad),
                "processCpuLoad", String.valueOf(processCpuLoad)
        );

        return new SystemStatusView(
                SystemStatusView.STATUS_UP,
                totalPlugins,
                activePlugins,
                disabledPlugins,
                failedPlugins,
                ManagementFactory.getRuntimeMXBean().getUptime(),
                jvmInfo,
                Map.of()
        );
    }

    /**
     * 获取平台基本信息。
     *
     * @return 平台信息视图，不为空
     */
    public PlatformInfoView getPlatformInfo() {
        String name = configFacade.get("platform", "infoName").orElse("Nexus Admin");
        String version = configFacade.get("platform", "infoVersion").orElse("0.1.0-SNAPSHOT");
        String description = configFacade.get("platform", "infoDescription").orElse("插件化系统拓展平台");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String startTime = sdf.format(new Date(ManagementFactory.getRuntimeMXBean().getStartTime()));
        return new PlatformInfoView(
                name,
                version,
                description,
                Map.of("startTime", startTime),
                Map.of("osName", System.getProperty("os.name"))
        );
    }

    /**
     * 检查系统是否健康。
     *
     * @return 健康状态
     */
    public boolean isHealthy() {
        return pluginFacade.listByState(PluginState.FAILED).isEmpty();
    }
}
