package com.nexusadmin.api.domain.view;

/** 插件配置能力元数据。 */
public record PluginConfigurationView(
        String scopeId,
        boolean configurable,
        boolean hasSchema,
        String schemaStatus,
        boolean canView,
        boolean canEdit
) {
}
