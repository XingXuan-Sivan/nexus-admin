package com.nexusadmin.api.configuration;

/** 当前用户缺少配置操作所需的附加动作权限。 */
public final class ConfigPermissionDeniedException extends RuntimeException {

    private final String permission;

    public ConfigPermissionDeniedException(String permission) {
        super("缺少配置操作权限: " + permission);
        this.permission = permission;
    }

    public String permission() {
        return permission;
    }
}
