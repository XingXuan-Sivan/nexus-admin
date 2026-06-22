package com.nexusadmin.api.config;

import com.nexusadmin.api.cache.CacheProvider;
import com.nexusadmin.api.cache.InMemoryCacheProvider;
import com.nexusadmin.api.storage.LocalStorageProvider;
import com.nexusadmin.api.storage.StorageProvider;
import com.nexusadmin.api.service.DepartmentService;
import com.nexusadmin.api.service.DictionaryService;
import com.nexusadmin.api.service.PositionService;
import com.nexusadmin.api.service.SystemHealthProvider;
import com.nexusadmin.api.service.impl.DefaultSystemHealthProvider;
import com.nexusadmin.api.service.impl.InMemoryDepartmentService;
import com.nexusadmin.api.service.impl.InMemoryDictionaryService;
import com.nexusadmin.api.service.impl.InMemoryPositionService;
import com.nexusadmin.core.facade.PluginFacade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 平台 Service 默认实现装配配置。
 *
 * <p>为 DictionaryService、DepartmentService、PositionService、
 * SystemHealthProvider、StorageProvider、CacheProvider 提供默认内存实现。</p>
 *
 * <p>LogService 由 {@link LogAutoConfig} 统一装配，不在此配置。</p>
 */
@Configuration
public class ServicesAutoConfig {

    @Bean
    @ConditionalOnMissingBean(DictionaryService.class)
    public DictionaryService dictionaryService() {
        return new InMemoryDictionaryService();
    }

    @Bean
    @ConditionalOnMissingBean(DepartmentService.class)
    public DepartmentService departmentService() {
        return new InMemoryDepartmentService();
    }

    @Bean
    @ConditionalOnMissingBean(PositionService.class)
    public PositionService positionService() {
        return new InMemoryPositionService();
    }

    @Bean
    @ConditionalOnMissingBean(SystemHealthProvider.class)
    public SystemHealthProvider systemHealthProvider(PluginFacade pluginFacade) {
        return new DefaultSystemHealthProvider(pluginFacade);
    }

    @Bean
    @ConditionalOnMissingBean(StorageProvider.class)
    public StorageProvider storageProvider() {
        return new LocalStorageProvider();
    }

    @Bean
    @ConditionalOnMissingBean(CacheProvider.class)
    public CacheProvider cacheProvider() {
        return new InMemoryCacheProvider();
    }
}
