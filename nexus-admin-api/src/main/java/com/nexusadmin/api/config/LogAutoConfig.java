package com.nexusadmin.api.config;

import com.nexusadmin.api.log.LogStorage;
import com.nexusadmin.api.log.LogWriter;
import com.nexusadmin.api.log.impl.InMemoryLogStorage;
import com.nexusadmin.api.log.impl.LogbackLogWriter;
import com.nexusadmin.api.service.LogService;
import com.nexusadmin.api.service.impl.DefaultLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 日志系统统一装配配置。
 *
 * <p>统一装配日志系统的全部组件，所有 Bean 带 {@code @ConditionalOnMissingBean} 保护，
 * 允许插件或应用层通过声明同类型 Bean 覆盖默认实现。</p>
 *
 * <p>装配顺序：LogStorage → LogWriter → LogService，确保依赖注入正确。</p>
 */
@Configuration
public class LogAutoConfig {

    private static final Logger log = LoggerFactory.getLogger(LogAutoConfig.class);

    /**
     * 日志存储默认实现（内存存储）。
     * <p>可通过声明同类型 Bean 替换为 ElasticsearchLogStorage、DatabaseLogStorage 等。</p>
     */
    @Bean
    @ConditionalOnMissingBean(LogStorage.class)
    public LogStorage logStorage() {
        log.info("日志存储已初始化为默认内存实现");
        return new InMemoryLogStorage();
    }

    /**
     * 日志写入器默认实现（Logback Appender 桥接）。
     * <p>同时作为 LogWriter ExtensionPoint 和 Logback Appender，
     * 自动拦截所有 SLF4J 日志汇入平台日志系统。</p>
     */
    @Bean
    @ConditionalOnMissingBean(LogWriter.class)
    public LogWriter logWriter(LogStorage logStorage) {
        LogbackLogWriter writer = new LogbackLogWriter(logStorage);
        writer.start();
        log.info("日志写入器已启动，Logback Appender 已附加到 Root Logger");
        return writer;
    }

    /**
     * 日志管理服务默认实现。
     * <p>委托 LogStorage 完成查询与清理，自身管理保留策略。</p>
     */
    @Bean
    @ConditionalOnMissingBean(LogService.class)
    public LogService logService(LogStorage logStorage) {
        log.info("日志管理服务已初始化");
        return new DefaultLogService(logStorage);
    }
}
