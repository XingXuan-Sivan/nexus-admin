package com.nexusadmin.core.exception;

/**
 * 扩展未找到异常，当从注册中心获取指定扩展点实现但未找到时抛出。
 */
public class ExtensionNotFoundException extends CoreException {

    /**
     * 构造扩展未找到异常。
     *
     * @param extensionType 未找到的扩展点接口类型
     */
    public ExtensionNotFoundException(Class<?> extensionType) {
        super("扩展点未找到: " + (extensionType == null ? "未知" : extensionType.getName()));
    }

    /**
     * 构造扩展未找到异常。
     *
     * @param extensionType 未找到的扩展点接口类型
     * @param message       自定义错误消息
     */
    public ExtensionNotFoundException(Class<?> extensionType, String message) {
        super("扩展点未找到 [" + (extensionType == null ? "未知" : extensionType.getName()) + "]: " + message);
    }
}
