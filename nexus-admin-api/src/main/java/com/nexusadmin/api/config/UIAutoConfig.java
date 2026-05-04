package com.nexusadmin.api.config;

import com.nexusadmin.api.controller.PluginStaticResourceController;
import com.nexusadmin.api.extension.ui.UIContributionRegistry;
import com.nexusadmin.api.extension.ui.UIContributionRegistryImpl;
import com.nexusadmin.core.PluginManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 插件 UI 基础设施装配配置。
 * <p>
 * 装配插件 UI 贡献注册表与插件静态资源托管控制器，为管理面板前端提供
 * 插件元数据（菜单/路由/清单）与插件运行时静态资源（JS/CSS/图片等）。
 * <p>
 * 所有 Bean 均带 {@link ConditionalOnMissingBean} 保护，应用层可通过声明同类型 Bean
 * 覆盖任意组件。
 * <p>
 * <strong>覆盖优先级：</strong>app @Bean &gt; api @ConditionalOnMissingBean &gt; 默认实现
 */
@Configuration
public class UIAutoConfig {

    /**
     * UI 贡献注册表实现。
     * <p>聚合所有已激活插件的菜单、路由等 UI 贡献声明，供 UIController 查询。</p>
     *
     * @param pluginManager 插件管理器
     * @return UIContributionRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean(UIContributionRegistry.class)
    public UIContributionRegistry uiContributionRegistry(PluginManager pluginManager) {
        return new UIContributionRegistryImpl(pluginManager);
    }

    /**
     * 插件静态资源托管控制器。
     * <p>从插件 ClassLoader 中加载 static/ 目录下的资源文件（JS/CSS/图片/字体等），
     * 通过 {@code GET /plugins/{pluginId}/assets/**} 路径对外提供。</p>
     *
     * @param pluginManager 插件管理器
     * @return PluginStaticResourceController 实例
     */
    @Bean
    @ConditionalOnMissingBean(PluginStaticResourceController.class)
    public PluginStaticResourceController pluginStaticResourceController(PluginManager pluginManager) {
        return new PluginStaticResourceController(pluginManager);
    }
}
