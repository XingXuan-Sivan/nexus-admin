package com.nexusadmin.api.configuration;

import java.util.List;

/** 配置值未通过服务端 Schema 校验。 */
public final class ConfigValidationException extends RuntimeException {

    private final List<ConfigModels.ValidationIssue> issues;

    public ConfigValidationException(List<ConfigModels.ValidationIssue> issues) {
        super("配置校验失败，共 " + issues.size() + " 个问题");
        this.issues = List.copyOf(issues);
    }

    public List<ConfigModels.ValidationIssue> issues() {
        return issues;
    }
}
