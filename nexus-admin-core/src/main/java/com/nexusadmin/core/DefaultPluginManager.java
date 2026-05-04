package com.nexusadmin.core;

import com.nexusadmin.core.facade.ConfigFacade;
import com.nexusadmin.core.facade.EventBusFacade;
import com.nexusadmin.core.facade.ExtensionFacade;
import com.nexusadmin.core.facade.PluginFacade;
import com.nexusadmin.core.plugin.loader.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * 默认插件管理器实现。
 * <p>继承 {@link AbstractPluginManager}，负责生命周期管理的具体策略实现。</p>
 */
public class DefaultPluginManager extends AbstractPluginManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultPluginManager.class);

    /**
     * 构造默认插件管理器。
     *
     * @param coreConfig       核心运行时配置
     * @param pluginFacade     插件组件门面
     * @param extensionFacade  扩展注册中心门面
     * @param configFacade     配置管理门面
     * @param eventBusFacade   事件总线门面
     */
    public DefaultPluginManager(CoreConfig coreConfig,
                                PluginFacade pluginFacade,
                                ExtensionFacade extensionFacade,
                                ConfigFacade configFacade,
                                EventBusFacade eventBusFacade) {
        super(coreConfig, pluginFacade, extensionFacade, configFacade, eventBusFacade);
    }

    @Override
    protected void autoStartIfNecessary() {
        // 从配置中心读取自动启动配置，若不可用则默认为 true
        boolean shouldAutoStart = configFacade.get("platform", "autoStart", Boolean.class).orElse(true);

        if (!shouldAutoStart) {
            return;
        }

        for (PluginWrapper wrapper : list()) {
            String pluginId = wrapper.getPluginId();

            // 如果插件在禁用列表中，设置为 DISABLED 状态并跳过
            if (configFacade.isPluginDisabled(pluginId)) {
                wrapper.state(PluginState.DISABLED);
                log.debug("插件 {} 已被禁用，跳过启动", pluginId);
                continue;
            }

            if (wrapper.descriptor().hasEntryPoint() && wrapper.state() == PluginState.INITIALIZED) {
                try {
                    start(pluginId);
                } catch (Exception ex) {
                    // 启动失败已在事件监听器中记录
                }
            }
        }
    }

    @Override
    public PluginWrapper get(String pluginId) {
        return pluginFacade.getPlugin(pluginId);
    }

    @Override
    public Collection<PluginWrapper> list() {
        return pluginFacade.listPlugins();
    }
}
