package com.nexusadmin.api.extension.ui;

import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.PluginState;
import com.nexusadmin.core.plugin.discovery.PluginContributes;
import com.nexusadmin.core.plugin.discovery.PluginContributes.MenuContribution;
import com.nexusadmin.core.plugin.discovery.PluginContributes.RouteContribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UI 贡献注册表实现，聚合所有已激活插件的 contributes 声明。
 *
 * <p>通过 PluginManager 获取已激活插件列表，从每个插件的描述符中提取 contributes 信息。</p>
 */
public class UIContributionRegistryImpl implements UIContributionRegistry {

    private static final Logger log = LoggerFactory.getLogger(UIContributionRegistryImpl.class);

    private final PluginManager pluginManager;

    /**
     * 构造 UI 贡献注册表。
     *
     * @param pluginManager 插件管理器
     */
    public UIContributionRegistryImpl(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public Map<String, PluginContributes> getManifest() {
        Map<String, PluginContributes> manifest = new LinkedHashMap<>();
        for (var wrapper : pluginManager.listByState(PluginState.ACTIVE)) {
            PluginContributes contributes = wrapper.descriptor().contributes();
            if (!contributes.isEmpty()) {
                manifest.put(wrapper.getPluginId(), contributes);
            }
        }
        return manifest;
    }

    @Override
    public List<MenuContribution> getMenus() {
        return pluginManager.listByState(PluginState.ACTIVE).stream()
                .flatMap(wrapper -> wrapper.descriptor().contributes().menus().stream())
                .sorted(Comparator.comparingInt(MenuContribution::order))
                .toList();
    }

    @Override
    public List<RouteContribution> getRoutes() {
        return pluginManager.listByState(PluginState.ACTIVE).stream()
                .flatMap(wrapper -> wrapper.descriptor().contributes().routes().stream())
                .toList();
    }
}
