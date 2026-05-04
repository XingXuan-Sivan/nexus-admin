package com.nexusadmin.api.domain.result;

/**
 * 通用状态码枚举，定义系统级别的标准响应码。
 * <p>
 * 自定义业务状态码可实现 {@link StatusCode} 接口，无需扩展此枚举。
 */
public enum StatusCodes implements StatusCode {

    /** 操作成功。 */
    SUCCESS(200, "操作成功"),

    /** 创建成功。 */
    CREATED(201, "创建成功"),

    /** 请求参数错误。 */
    BAD_REQUEST(400, "请求参数错误"),

    /** 未认证。 */
    UNAUTHORIZED(401, "未认证"),

    /** 无权限。 */
    FORBIDDEN(403, "无权限"),

    /** 资源不存在。 */
    NOT_FOUND(404, "资源不存在"),

    /** 服务器内部错误。 */
    INTERNAL_ERROR(500, "服务器内部错误"),

    // ==================== 认证相关 (1xxx) ====================

    /** 认证失败。 */
    AUTHENTICATION_FAILED(1001, "认证失败"),

    /** Token 已过期。 */
    TOKEN_EXPIRED(1002, "Token 已过期"),

    /** Token 无效。 */
    TOKEN_INVALID(1003, "Token 无效"),

    /** 权限不足。 */
    PERMISSION_DENIED(1004, "权限不足"),

    /** 账号已锁定。 */
    ACCOUNT_LOCKED(1005, "账号已锁定"),

    /** 账号已禁用。 */
    ACCOUNT_DISABLED(1006, "账号已禁用"),

    // ==================== 插件相关 (2xxx) ====================

    /** 插件不存在。 */
    PLUGIN_NOT_FOUND(2001, "插件不存在"),

    /** 插件状态不合法。 */
    PLUGIN_STATE_INVALID(2002, "插件状态不合法"),

    /** 插件依赖未满足。 */
    PLUGIN_DEPENDENCY_MISSING(2003, "插件依赖未满足"),

    /** 插件加载失败。 */
    PLUGIN_LOAD_FAILED(2004, "插件加载失败"),

    /** 插件描述文件无效。 */
    PLUGIN_DESCRIPTOR_INVALID(2005, "插件描述文件无效"),

    /** 插件已存在。 */
    PLUGIN_ALREADY_EXISTS(2006, "插件已存在"),

    // ==================== 配置相关 (3xxx) ====================

    /** 配置项不存在。 */
    CONFIG_NOT_FOUND(3001, "配置项不存在"),

    /** 配置值不合法。 */
    CONFIG_VALUE_INVALID(3002, "配置值不合法"),

    /** 配置 Schema 无效。 */
    CONFIG_SCHEMA_INVALID(3003, "配置 Schema 无效"),

    /** 配置域不存在。 */
    CONFIG_SCOPE_NOT_FOUND(3004, "配置域不存在"),

    /** 配置项为只读。 */
    CONFIG_READ_ONLY(3005, "配置项为只读"),

    // ==================== 系统相关 (4xxx) ====================

    /** 系统不可用。 */
    SYSTEM_UNAVAILABLE(4001, "系统不可用"),

    /** 系统维护中。 */
    SYSTEM_MAINTENANCE(4002, "系统维护中"),

    // ==================== 业务相关 (5xxx) ====================

    /** 用户不存在。 */
    USER_NOT_FOUND(5001, "用户不存在"),

    /** 用户已存在。 */
    USER_ALREADY_EXISTS(5002, "用户已存在"),

    /** 角色不存在。 */
    ROLE_NOT_FOUND(5003, "角色不存在"),

    /** 角色已存在。 */
    ROLE_ALREADY_EXISTS(5004, "角色已存在");

    private final int code;
    private final String message;

    StatusCodes(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
