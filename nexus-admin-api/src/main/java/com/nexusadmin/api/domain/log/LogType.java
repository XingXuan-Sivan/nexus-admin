package com.nexusadmin.api.domain.log;

/**
 * 日志类型枚举，定义平台内的日志分类。
 */
public enum LogType {
    /** 系统日志，记录系统级别的运行信息 */
    SYSTEM,
    /** 审计日志，记录用户操作和权限验证等审计事件 */
    AUDIT,
    /** AI 日志，记录 AI 相关操作和调用 */
    AI,
    /** 技术日志，记录技术层面的日志信息 */
    TECH
}
