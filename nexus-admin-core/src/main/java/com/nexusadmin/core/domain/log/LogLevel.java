package com.nexusadmin.core.domain.log;

/**
 * 日志级别枚举，定义日志的严重程度分级。
 */
public enum LogLevel {
    /** 跟踪级别，最详细的日志信息 */
    TRACE,
    /** 调试级别，用于开发调试 */
    DEBUG,
    /** 信息级别，记录一般信息 */
    INFO,
    /** 警告级别，记录潜在问题 */
    WARN,
    /** 错误级别，记录错误信息 */
    ERROR
}
