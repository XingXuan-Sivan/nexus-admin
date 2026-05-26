package com.nexusadmin.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 插件状态持久化接口，用于跨重启保持插件的运行时状态。
 *
 * <p><strong>设计原则：</strong>文件系统始终是插件发现的唯一权威源。
 * 此接口只存储运行时状态（启用/禁用、操作记录），不替代文件发现。</p>
 */
public interface IPluginStateStore {

    Optional<PluginStateRecord> getState(String pluginId);
    void saveState(PluginStateRecord record);
    void deleteState(String pluginId);
    List<PluginStateRecord> listAll();

    record PluginStateRecord(String pluginId, boolean enabled, String disabledReason,
                              Instant lastOperationTime, String lastOperator) {}
}
