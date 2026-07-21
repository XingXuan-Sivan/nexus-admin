package com.nexusadmin.api.configuration;

/** 请求的配置域不存在。 */
public final class ConfigDomainNotFoundException extends RuntimeException {
    public ConfigDomainNotFoundException(String scopeId) {
        super("配置域不存在: " + scopeId);
    }
}
