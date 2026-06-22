package com.nexusadmin.api.log.factory;

import com.nexusadmin.api.domain.log.LogEntry;
import com.nexusadmin.api.domain.log.LogLevel;
import com.nexusadmin.api.domain.log.LogType;

/**
 * 审计日志工厂，提供安全审计事件的标准日志构建方法。
 *
 * <p>审计日志用于记录登录、权限检查、敏感操作等需要保留不可否认证据的安全事件。</p>
 */
public final class AuditLogFactory {

    /** 属性键：客户端 IP */
    public static final String ATTR_IP = "ip";
    /** 属性键：操作结果（SUCCESS / FAILURE / DENIED） */
    public static final String ATTR_RESULT = "result";
    /** 属性键：失败/拒绝原因 */
    public static final String ATTR_REASON = "reason";
    /** 属性键：被操作的资源 */
    public static final String ATTR_RESOURCE = "resource";
    /** 属性键：操作类型 */
    public static final String ATTR_ACTION = "action";

    private AuditLogFactory() {
    }

    /**
     * 登录成功。
     *
     * @param userId 用户ID
     * @param ip     客户端 IP
     * @return 日志条目
     */
    public static LogEntry loginSuccess(String userId, String ip) {
        return LogEntry.builder()
                .type(LogType.auditLogin())
                .level(LogLevel.INFO)
                .message("用户登录成功")
                .userId(userId)
                .attribute(ATTR_IP, ip)
                .attribute(ATTR_RESULT, "SUCCESS")
                .build();
    }

    /**
     * 登录失败。
     *
     * @param username 尝试登录的用户名
     * @param ip       客户端 IP
     * @param reason   失败原因
     * @return 日志条目
     */
    public static LogEntry loginFailure(String username, String ip, String reason) {
        return LogEntry.builder()
                .type(LogType.auditLogin())
                .level(LogLevel.WARN)
                .message("用户登录失败: " + reason)
                .userId(username)
                .attribute(ATTR_IP, ip)
                .attribute(ATTR_RESULT, "FAILURE")
                .attribute(ATTR_REASON, reason)
                .build();
    }

    /**
     * 用户登出。
     *
     * @param userId 用户ID
     * @return 日志条目
     */
    public static LogEntry logout(String userId) {
        return LogEntry.builder()
                .type(LogType.auditLogin())
                .level(LogLevel.INFO)
                .message("用户登出")
                .userId(userId)
                .attribute(ATTR_RESULT, "LOGOUT")
                .build();
    }

    /**
     * 权限拒绝。
     *
     * @param userId   用户ID
     * @param resource 被访问的资源
     * @param action   尝试的操作
     * @return 日志条目
     */
    public static LogEntry permissionDenied(String userId, String resource, String action) {
        return LogEntry.builder()
                .type(LogType.auditPermission())
                .level(LogLevel.WARN)
                .message(String.format("权限拒绝: %s 尝试 %s %s", userId, action, resource))
                .userId(userId)
                .attribute(ATTR_RESOURCE, resource)
                .attribute(ATTR_ACTION, action)
                .attribute(ATTR_RESULT, "DENIED")
                .build();
    }

    /**
     * 用户敏感操作审计。
     *
     * @param userId   用户ID
     * @param resource 被操作的资源
     * @param action   操作类型
     * @param detail   操作详情
     * @return 日志条目
     */
    public static LogEntry operation(String userId, String resource, String action, String detail) {
        return LogEntry.builder()
                .type(LogType.auditOperation())
                .level(LogLevel.INFO)
                .message(String.format("审计操作: %s %s %s", userId, action, resource))
                .userId(userId)
                .attribute(ATTR_RESOURCE, resource)
                .attribute(ATTR_ACTION, action)
                .attribute("detail", detail)
                .attribute(ATTR_RESULT, "SUCCESS")
                .build();
    }
}
