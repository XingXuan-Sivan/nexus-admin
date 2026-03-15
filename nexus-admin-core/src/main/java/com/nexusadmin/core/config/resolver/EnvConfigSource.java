package com.nexusadmin.core.config.resolver;

import java.util.Optional;

/**
 * 环境变量配置源，从系统环境变量读取配置。
 * <p>优先级最高，用于覆盖其他配置源的值。</p>
 * <p>环境变量命名规范：NEXUS_&lt;SCOPE&gt;_&lt;KEY&gt;，如 NEXUS_PLUGIN_ORDER_TIMEOUT</p>
 */
public class EnvConfigSource implements ConfigSource {

    /**
     * 环境变量前缀。
     */
    private static final String ENV_PREFIX = "NEXUS_";

    @Override
    public Optional<String> get(String scope, String key) {
        String envKey = buildEnvKey(scope, key);
        String value = System.getenv(envKey);
        return Optional.ofNullable(value);
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public String name() {
        return "Environment";
    }

    /**
     * 构建环境变量键名。
     * <p>将作用域和键名转换为大写下划线格式。</p>
     *
     * @param scope 配置作用域
     * @param key   配置键名
     * @return 环境变量键名
     */
    private String buildEnvKey(String scope, String key) {
        String normalizedScope = scope.toUpperCase().replace(".", "_").replace("-", "_");
        String normalizedKey = key.toUpperCase().replace(".", "_").replace("-", "_");
        return ENV_PREFIX + normalizedScope + "_" + normalizedKey;
    }
}
