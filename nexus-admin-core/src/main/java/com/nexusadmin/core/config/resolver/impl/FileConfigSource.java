package com.nexusadmin.core.config.resolver.impl;

import com.nexusadmin.core.config.resolver.ConfigSource;
import com.nexusadmin.core.config.store.ConfigStore;
import com.nexusadmin.core.config.store.FileConfigStore;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/** 文件持久化层配置源。 */
public final class FileConfigSource implements ConfigSource {

    private final ConfigStore store;

    public FileConfigSource(ConfigStore store) {
        this.store = store;
    }

    public FileConfigSource(Path configDir) {
        this(new FileConfigStore(configDir));
    }

    @Override
    public Optional<String> get(String scope, String key) {
        return getObject(scope, key).map(Object::toString);
    }

    @Override
    public Optional<Object> getObject(String scope, String key) {
        return Optional.ofNullable(nestedValue(store.getScope(scope), key));
    }

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public String name() {
        return "File";
    }

    @Override
    public String sourceType() {
        return "file";
    }

    public Map<String, Object> getConfigMap(String scope) {
        return store.getScope(scope);
    }

    public void invalidateCache(String scope) {
        store.invalidateCache(scope);
    }

    public void invalidateAllCache() {
        store.invalidateAllCache();
    }

    private Object nestedValue(Map<String, Object> config, String key) {
        Object current = config;
        for (String part : key.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }
}
