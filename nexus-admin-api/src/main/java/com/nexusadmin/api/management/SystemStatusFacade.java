package com.nexusadmin.api.management;

/**
 * 系统状态门面接口，提供系统运行状态的查询能力。
 *
 * <p>该接口封装系统状态查询操作，为管理面板提供观测能力。</p>
 */
public interface SystemStatusFacade {

    /**
     * 获取系统运行状态。
     *
     * @return 系统状态视图，不为空
     */
    SystemStatusView getStatus();

    /**
     * 获取平台基本信息。
     *
     * @return 平台信息视图，不为空
     */
    PlatformInfoView getPlatformInfo();

    /**
     * 检查系统是否健康。
     *
     * @return 健康状态
     */
    boolean isHealthy();
}
