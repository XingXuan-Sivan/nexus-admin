package com.nexusadmin.api.result;

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
    INTERNAL_ERROR(500, "服务器内部错误");

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
