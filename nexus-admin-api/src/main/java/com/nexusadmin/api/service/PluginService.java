package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.view.PluginDetailView;
import com.nexusadmin.api.domain.view.PluginStateView;
import com.nexusadmin.api.domain.view.PluginView;
import com.nexusadmin.api.exception.PluginOperationException;
import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.PluginState;
import com.nexusadmin.core.plugin.loader.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 插件管理服务。
 *
 * <p>提供插件生命周期的控制与查询能力，包括状态枚举转换、视图映射、生命周期操作的异常翻译。</p>
 * <p>支持通过声明同类型 Bean 覆盖，便于插件提供定制实现。</p>
 */
@Service
public class PluginService {

    private static final Logger log = LoggerFactory.getLogger(PluginService.class);

    private final PluginManager pluginManager;
    private final com.nexusadmin.core.facade.PluginFacade pluginFacade;

    /**
     * 构造插件管理服务。
     *
     * @param pluginManager 插件管理器
     * @param pluginFacade  核心插件组件门面
     */
    public PluginService(PluginManager pluginManager,
                         com.nexusadmin.core.facade.PluginFacade pluginFacade) {
        this.pluginManager = pluginManager;
        this.pluginFacade = pluginFacade;
    }

    /**
     * 获取所有插件的摘要列表。
     *
     * @return 插件摘要列表，不为空
     */
    public List<PluginView> listAll() {
        return pluginFacade.listPlugins().stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 按状态筛选插件。
     *
     * @param state 插件状态
     * @return 符合条件的插件列表，不为空
     */
    public List<PluginView> listByState(PluginStateView state) {
        PluginState coreState = toCoreState(state);
        return pluginFacade.listByState(coreState).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 获取插件详情。
     *
     * @param pluginId 插件标识
     * @return 插件详情，不存在则返回空
     */
    public Optional<PluginDetailView> getDetail(String pluginId) {
        PluginWrapper wrapper = pluginFacade.getPlugin(pluginId);
        if (wrapper == null) {
            return Optional.empty();
        }
        return Optional.of(toDetailView(wrapper));
    }

    /**
     * 启动插件。
     *
     * @param pluginId 插件标识
     * @throws PluginOperationException 启动失败时抛出
     */
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

    /**
     * 停止插件。
     *
     * @param pluginId 插件标识
     * @throws PluginOperationException 停止失败时抛出
     */
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

    /**
     * 启用插件。
     *
     * @param pluginId 插件标识
     * @throws PluginOperationException 启用失败时抛出
     */
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

    /**
     * 禁用插件。
     *
     * @param pluginId 插件标识
     * @throws PluginOperationException 禁用失败时抛出
     */
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

    /**
     * 卸载插件。
     *
     * @param pluginId 插件标识
     * @throws PluginOperationException 卸载失败时抛出
     */
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

    /**
     * 检查插件是否已启用。
     *
     * @param pluginId 插件标识
     * @return 启用状态
     */
    public boolean isEnabled(String pluginId) {
        PluginWrapper wrapper = pluginFacade.getPlugin(pluginId);
        return wrapper != null && wrapper.state() != PluginState.DISABLED;
    }

    /**
     * 检查插件是否处于活跃状态。
     *
     * @param pluginId 插件标识
     * @return 活跃状态
     */
    public boolean isActive(String pluginId) {
        PluginWrapper wrapper = pluginFacade.getPlugin(pluginId);
        return wrapper != null && wrapper.state() == PluginState.ACTIVE;
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

        Map<String, String> metadata = wrapper.descriptor().metadata() != null
                ? wrapper.descriptor().metadata().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> String.valueOf(e.getValue())
                        ))
                : Map.of();

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
