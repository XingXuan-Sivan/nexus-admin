package com.nexusadmin.app.config;

import com.nexusadmin.api.management.AdminFacade;
import com.nexusadmin.api.management.ConfigAdminFacade;
import com.nexusadmin.api.management.PluginAdminFacade;
import com.nexusadmin.api.management.SystemStatusFacade;
import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.config.ConfigManager;

/**
 * 管理门面实现类。
 * <p>
 * 为管理面板提供统一的系统能力访问入口，实现控制平面门面接口。
 */
public class AdminFacadeImpl implements AdminFacade {

    private final PluginAdminFacade pluginAdminFacade;
    private final ConfigAdminFacade configAdminFacade;
    private final SystemStatusFacade systemStatusFacade;

    /**
     * 构造管理门面。
     *
     * @param pluginManager 插件管理器
     * @param configManager 配置管理器
     */
    public AdminFacadeImpl(PluginManager pluginManager, ConfigManager configManager) {
        this.pluginAdminFacade = new PluginAdminFacadeImpl(pluginManager);
        this.configAdminFacade = new ConfigAdminFacadeImpl(configManager);
        this.systemStatusFacade = new SystemStatusFacadeImpl(pluginManager);
    }

    @Override
    public PluginAdminFacade plugins() {
        return pluginAdminFacade;
    }

    @Override
    public ConfigAdminFacade config() {
        return configAdminFacade;
    }

    @Override
    public SystemStatusFacade system() {
        return systemStatusFacade;
    }
}
