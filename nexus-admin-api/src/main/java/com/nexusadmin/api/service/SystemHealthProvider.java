package com.nexusadmin.api.service;

import java.util.List;
import java.util.Map;

/**
 * 健康检查接口，提供平台各组件的健康状态。
 */
public interface SystemHealthProvider {

    /** 获取整体健康状态 */
    HealthStatus getOverallHealth();

    /** 获取各组件健康详情 */
    List<ComponentHealth> getComponentHealths();

    /** 健康状态 */
    enum HealthStatus { UP, DOWN, DEGRADED }

    /** 组件健康信息 */
    record ComponentHealth(String name, HealthStatus status,
                            String details, Map<String, Object> metrics) {}
}
