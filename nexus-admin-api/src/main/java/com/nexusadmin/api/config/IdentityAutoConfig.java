package com.nexusadmin.api.config;

import com.nexusadmin.api.auth.impl.DefaultPermissionResolver;
import com.nexusadmin.api.auth.PermissionResolver;
import com.nexusadmin.api.service.IPluginStateStore;
import com.nexusadmin.api.service.IdentityService;
import com.nexusadmin.api.service.impl.InMemoryIdentityService;
import com.nexusadmin.api.service.impl.InMemoryPluginStateStore;
import com.nexusadmin.core.extension.ExtensionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 身份管理体系装配配置。
 *
 * <p>负责装配 IdentityService、DefaultPermissionResolver、PluginStateStore。
 * 所有 Bean 均带 {@link ConditionalOnMissingBean} 保护。</p>
 */
@Configuration
public class IdentityAutoConfig {

    @Bean
    @ConditionalOnMissingBean(IdentityService.class)
    public IdentityService identityService() {
        return new InMemoryIdentityService();
    }

    @Bean
    @ConditionalOnMissingBean(DefaultPermissionResolver.class)
    public DefaultPermissionResolver defaultPermissionResolver(
            IdentityService identityService,
            ExtensionRegistry extensionRegistry) {
        DefaultPermissionResolver resolver = new DefaultPermissionResolver(identityService);
        extensionRegistry.register(PermissionResolver.class, resolver, 25);
        return resolver;
    }

    @Bean
    @ConditionalOnMissingBean(IPluginStateStore.class)
    public IPluginStateStore pluginStateStore() {
        return new InMemoryPluginStateStore();
    }
}
