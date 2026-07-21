package com.nexusadmin.api.configuration;

/** 原始文档因敏感字段或安全策略被锁定。 */
public final class ConfigDocumentLockedException extends RuntimeException {
    public ConfigDocumentLockedException() {
        super("该配置域包含敏感字段，当前平台未提供近期重新认证能力，原始文档编辑已关闭");
    }
}
