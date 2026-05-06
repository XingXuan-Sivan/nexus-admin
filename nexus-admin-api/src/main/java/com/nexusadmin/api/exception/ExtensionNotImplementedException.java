package com.nexusadmin.api.exception;

/**
 * 功能未实现异常，表示当前操作为骨架占位，需由插件提供具体实现。
 *
 * <p>当用户、角色、权限等管理服务尚未安装对应插件时，骨架 Service 抛出此异常。
 * 与 {@code UnsupportedOperationException} 的区别在于此异常携带业务语义，
 * GlobalExceptionHandler 可将其统一转换为 HTTP 501 响应并输出 WARN 级别日志。</p>
 */
public class ExtensionNotImplementedException extends RuntimeException {

    /**
     * 构造功能未实现异常。
     *
     * @param message 描述哪些功能由拓展点实现
     */
    public ExtensionNotImplementedException(String message) {
        super(message);
    }
}
