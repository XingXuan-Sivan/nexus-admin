package com.nexusadmin.api.facade;

/**
 * 控制平面门面接口，为管理面板提供统一的系统能力访问入口。
 *
 * <p>该接口定义了管理面板所需的核心操作能力，实现类由 app 模块提供，
 * 通过依赖注入解耦管理插件与核心模块。</p>
 *
 * <p><strong>职责边界：</strong></p>
 * <ul>
 *   <li>仅提供控制与查询能力，不处理业务逻辑</li>
 *   <li>操作结果通过 DTO 类型返回，隐藏内部实现细节</li>
 *   <li>所有方法应保证幂等性或明确标注副作用</li>
 * </ul>
 *
 * <p><strong>实现要求：</strong></p>
 * <ul>
 *   <li>实现类应注入 PluginManager、ConfigManager 等核心组件</li>
 *   <li>异常应转换为业务异常，便于统一处理</li>
 *   <li>敏感操作应记录审计日志</li>
 * </ul>
 */
public interface AdminFacade {

    /**
     * 获取插件管理门面。
     *
     * @return 插件管理门面
     */
    PluginFacade plugins();

    /**
     * 获取配置管理门面。
     *
     * @return 配置管理门面
     */
    ConfigFacade config();

    /**
     * 获取系统状态门面。
     *
     * @return 系统状态门面
     */
    SystemStatusFacade system();
}
