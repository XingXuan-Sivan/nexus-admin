package com.nexusadmin.api.management.impl;

import com.nexusadmin.api.exception.PluginOperationException;
import com.nexusadmin.api.management.PluginAdminFacade;
import com.nexusadmin.api.management.PluginDetailView;
import com.nexusadmin.api.management.PluginStateView;
import com.nexusadmin.api.management.PluginView;
import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.PluginState;
import com.nexusadmin.core.plugin.loader.PluginWrapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 插件管理门面实现类。
 */
public class PluginAdminFacadeImpl implements PluginAdminFacade {

    private final PluginManager pluginManager;

    /**
     * 构造插件管理门面。
     *
     * @param pluginManager 插件管理器
     */
    public PluginAdminFacadeImpl(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public List<PluginView> listAll() {
        return pluginManager.list().stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public List<PluginView> listByState(PluginStateView state) {
        PluginState coreState = toCoreState(state);
        return pluginManager.listByState(coreState).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public Optional<PluginDetailView> getDetail(String pluginId) {
        PluginWrapper wrapper = pluginManager.get(pluginId);
        if (wrapper == null) {
            return Optional.empty();
        }
        return Optional.of(toDetailView(wrapper));
    }

    @Override
    public void start(String pluginId) {
        try {
            pluginManager.start(pluginId);
        } catch (Exception e) {
            throw new PluginOperationException(
                    pluginId,
                    PluginOperationException.Operation.START,
                    e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void stop(String pluginId) {
        try {
            pluginManager.stop(pluginId);
        } catch (Exception e) {
            throw new PluginOperationException(
                    pluginId,
                    PluginOperationException.Operation.STOP,
                    e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void enable(String pluginId) {
        try {
            pluginManager.enable(pluginId);
        } catch (Exception e) {
            throw new PluginOperationException(
                    pluginId,
                    PluginOperationException.Operation.ENABLE,
                    e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void disable(String pluginId) {
        try {
            pluginManager.disable(pluginId);
        } catch (Exception e) {
            throw new PluginOperationException(
                    pluginId,
                    PluginOperationException.Operation.DISABLE,
                    e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void unload(String pluginId) {
        try {
            pluginManager.unload(pluginId);
        } catch (Exception e) {
            throw new PluginOperationException(
                    pluginId,
                    PluginOperationException.Operation.UNLOAD,
                    e.getMessage(),
                    e
            );
        }
    }

    @Override
    public boolean isEnabled(String pluginId) {
        return pluginManager.isEnabled(pluginId);
    }

    @Override
    public boolean isActive(String pluginId) {
        return pluginManager.isActive(pluginId);
    }

    /**
     * 转换为插件视图。
     */
    private PluginView toView(PluginWrapper wrapper) {
        return new PluginView(
                wrapper.getPluginId(),
                wrapper.descriptor().version(),
                wrapper.descriptor().name(),
                wrapper.descriptor().description(),
                toStateView(wrapper.state()),
                wrapper.descriptor().author()
        );
    }

    /**
     * 转换为插件详情视图。
     */
    private PluginDetailView toDetailView(PluginWrapper wrapper) {
        Set<String> dependencies = wrapper.descriptor().dependencies() != null
                ? wrapper.descriptor().dependencies().keySet()
                : Set.of();

        java.util.Map<String, String> metadata = wrapper.descriptor().metadata() != null
                ? wrapper.descriptor().metadata().entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                java.util.Map.Entry::getKey,
                                e -> String.valueOf(e.getValue())
                        ))
                : java.util.Map.of();

        return new PluginDetailView(
                wrapper.getPluginId(),
                wrapper.descriptor().version(),
                wrapper.descriptor().name(),
                wrapper.descriptor().description(),
                toStateView(wrapper.state()),
                wrapper.descriptor().author(),
                wrapper.descriptor().mainClass(),
                dependencies,
                List.of(),
                null,
                null,
                metadata
        );
    }

    /**
     * 转换状态视图。
     */
    private PluginStateView toStateView(PluginState state) {
        return switch (state) {
            case DISCOVERED -> PluginStateView.DISCOVERED;
            case LOADED -> PluginStateView.LOADED;
            case INITIALIZED -> PluginStateView.INITIALIZED;
            case ACTIVE -> PluginStateView.ACTIVE;
            case STOPPED -> PluginStateView.STOPPED;
            case DISABLED -> PluginStateView.DISABLED;
            case FAILED -> PluginStateView.FAILED;
            default -> PluginStateView.DISCOVERED;
        };
    }

    /**
     * 转换核心状态。
     */
    private PluginState toCoreState(PluginStateView view) {
        return switch (view) {
            case DISCOVERED -> PluginState.DISCOVERED;
            case LOADED -> PluginState.LOADED;
            case INITIALIZED -> PluginState.INITIALIZED;
            case ACTIVE -> PluginState.ACTIVE;
            case STOPPED -> PluginState.STOPPED;
            case DISABLED -> PluginState.DISABLED;
            case FAILED -> PluginState.FAILED;
            default -> PluginState.DISCOVERED;
        };
    }
}
