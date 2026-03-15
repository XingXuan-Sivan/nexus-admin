package com.nexusadmin.core.config.resolver;

import java.util.Optional;

/**
 * 配置源接口，定义从特定来源获取配置的能力。
 * <p>支持多配置源优先级组合，如环境变量、配置文件、默认配置等。</p>
 */
public interface ConfigSource {

    /**
     * 从指定作用域获取配置值。
     *
     * @param scope 配置作用域，如 "platform" 或 "plugin.order-plugin"
     * @param key   配置键名
     * @return 配置值，如果不存在则返回空 Optional
     */
    Optional<String> get(String scope, String key);

    /**
     * 获取配置源优先级，数值越小优先级越高。
     * <p>默认优先级为 100，子类可覆盖以调整优先级。</p>
     *
     * @return 优先级数值
     */
    default int priority() {
        return 100;
    }

    /**
     * 获取配置源名称。
     *
     * @return 配置源名称
     */
    String name();
}
