package com.nexusadmin.api.service.impl;

import com.nexusadmin.api.service.IPluginStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件状态持久化的默认内存实现。
 */
public class InMemoryPluginStateStore implements IPluginStateStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryPluginStateStore.class);

    private final ConcurrentHashMap<String, PluginStateRecord> store = new ConcurrentHashMap<>();

    @Override
    public Optional<PluginStateRecord> getState(String pluginId) {
        return Optional.ofNullable(store.get(pluginId));
    }

    @Override
    public void saveState(PluginStateRecord record) {
        store.put(record.pluginId(), record);
        log.info("保存插件状态：{}，已启用={}", record.pluginId(), record.enabled());
    }

    @Override
    public void deleteState(String pluginId) {
        store.remove(pluginId);
        log.info("删除插件状态：{}", pluginId);
    }

    @Override
    public List<PluginStateRecord> listAll() {
        return new ArrayList<>(store.values());
    }

    /**
     * 从 disabled.yml 加载初始状态（供 ConfigFacade 调用）。
     */
    public void loadFromDisabledList(List<String> disabledPluginIds) {
        for (String pluginId : disabledPluginIds) {
            store.put(pluginId, new PluginStateRecord(pluginId, false,
                    "从 disabled.yml 加载", Instant.now(), "system"));
        }
    }
}
