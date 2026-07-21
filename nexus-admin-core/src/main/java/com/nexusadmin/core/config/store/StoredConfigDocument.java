package com.nexusadmin.core.config.store;

/** 配置存储中的受控文档。 */
public record StoredConfigDocument(
        String scopeId,
        String displayName,
        String format,
        String content,
        String revision,
        int maxBytes
) {
}
