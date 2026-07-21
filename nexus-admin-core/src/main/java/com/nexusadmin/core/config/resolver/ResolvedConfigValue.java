package com.nexusadmin.core.config.resolver;

/** 带来源元数据的配置解析结果。 */
public record ResolvedConfigValue(Object value, String source) {
}
