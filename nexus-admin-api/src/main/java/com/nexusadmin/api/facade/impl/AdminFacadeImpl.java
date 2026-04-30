package com.nexusadmin.api.facade.impl;

import com.nexusadmin.api.facade.AdminFacade;
import com.nexusadmin.api.facade.ConfigFacade;
import com.nexusadmin.api.facade.PluginFacade;
import com.nexusadmin.api.facade.SystemStatusFacade;
import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.config.ConfigManager;

/**
 * 管理门面实现类。
 * <p>
 * 为管理面板提供统一的系统能力访问入口，实现控制平面门面接口。
 */
public class AdminFacadeImpl implements AdminFacade {

    private final PluginFacade pluginFacade;
    private final ConfigFacade configFacade;
    private final SystemStatusFacade systemStatusFacade;

    /**
     * 构造管理门面。
     *
     * @param pluginManager 插件管理器
     * @param configManager 配置管理器
     */
    public AdminFacadeImpl(PluginManager pluginManager, ConfigManager configManager) {
        this.pluginFacade = new PluginFacadeImpl(pluginManager);
        this.configFacade = new ConfigFacadeImpl(configManager);
        this.systemStatusFacade = new SystemStatusFacadeImpl(pluginManager, configManager);
    }

    @Override
    public PluginFacade plugins() {
        return pluginFacade;
    }

    @Override
    public ConfigFacade config() {
        return configFacade;
    }

    @Override
    public SystemStatusFacade system() {
        return systemStatusFacade;
    }
}
