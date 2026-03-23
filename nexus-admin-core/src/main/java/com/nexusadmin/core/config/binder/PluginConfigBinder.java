package com.nexusadmin.core.config.binder;

import com.nexusadmin.core.config.ConfigManager;
import com.nexusadmin.core.config.event.ConfigListener;

/**
 * 插件配置绑定器，将插件配置自动绑定到对象字段。
 * <p>支持配置热更新时自动重新绑定。</p>
 * <p>这是 {@link ConfigBinder} 的插件友好包装层，内部委托给 ConfigBinder 实现。</p>
 */
public class PluginConfigBinder {

    /**
     * 底层配置绑定器。
     */
    private final ConfigBinder binder;

    /**
     * 构造插件配置绑定器。
     *
     * @param configManager 配置管理器
     */
    public PluginConfigBinder(ConfigManager configManager) {
        this.binder = new ConfigBinder(configManager);
    }

    /**
     * 将插件配置绑定到对象。
     * <p>对象字段名将作为配置键名，从配置管理器获取值并设置到字段。</p>
     *
     * @param pluginId 插件ID
     * @param target   目标对象
     */
    public void bind(String pluginId, Object target) {
        binder.bind(pluginId, target);
    }

    /**
     * 解绑插件配置。
     *
     * @param pluginId 插件ID
     * @param target   目标对象
     */
    public void unbind(String pluginId, Object target) {
        binder.unbind(pluginId, target);
    }

    /**
     * 解绑插件的所有配置。
     *
     * @param pluginId 插件ID
     */
    public void unbindAll(String pluginId) {
        binder.unbindAll(pluginId);
    }

    /**
     * 重新绑定指定插件的所有对象。
     *
     * @param pluginId 插件ID
     */
    public void rebind(String pluginId) {
        binder.rebind(pluginId);
    }

    /**
     * 创建配置变更监听器。
     * <p>当插件配置变更时自动重新绑定。</p>
     *
     * @param pluginId 插件ID
     * @return 配置监听器
     */
    public ConfigListener createListener(String pluginId) {
        return binder.createListener(pluginId);
    }
}
