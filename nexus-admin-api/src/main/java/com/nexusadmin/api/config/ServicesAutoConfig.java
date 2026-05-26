package com.nexusadmin.api.config;

import com.nexusadmin.api.extension.cache.CacheProvider;
import com.nexusadmin.api.extension.cache.InMemoryCacheProvider;
import com.nexusadmin.api.extension.storage.LocalStorageProvider;
import com.nexusadmin.api.extension.storage.StorageProvider;
import com.nexusadmin.api.service.DepartmentService;
import com.nexusadmin.api.service.DictionaryService;
import com.nexusadmin.api.service.LogService;
import com.nexusadmin.api.service.PositionService;
import com.nexusadmin.api.service.SystemHealthProvider;
import com.nexusadmin.api.service.impl.DefaultSystemHealthProvider;
import com.nexusadmin.api.service.impl.InMemoryDepartmentService;
import com.nexusadmin.api.service.impl.InMemoryDictionaryService;
import com.nexusadmin.api.service.impl.InMemoryLogService;
import com.nexusadmin.api.service.impl.InMemoryPositionService;
import com.nexusadmin.core.facade.PluginFacade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 平台 Service 默认实现装配配置。
 *
 * <p>为 DictionaryService、DepartmentService、PositionService、LogService、
 * SystemHealthProvider、StorageProvider、CacheProvider 提供默认内存实现。</p>
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
    @ConditionalOnMissingBean(LogService.class)
    public LogService logService() {
        return new InMemoryLogService();
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
