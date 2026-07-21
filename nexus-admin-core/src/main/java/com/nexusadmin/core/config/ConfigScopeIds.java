package com.nexusadmin.core.config;

import java.util.Set;
import java.util.regex.Pattern;

/** 配置域与插件 ID 的统一规范。 */
public final class ConfigScopeIds {

    public static final String PLATFORM = "platform";
    public static final String PLATFORM_DISABLED = "platform.disabled";

    private static final Pattern PATTERN = Pattern.compile(
            "[a-z][a-z0-9_-]*(?:\\.[a-z0-9_-]+)*");
    private static final Set<String> RESERVED = Set.of(PLATFORM, PLATFORM_DISABLED);

    private ConfigScopeIds() {
    }

    public static boolean isValid(String scopeId) {
        return scopeId != null
                && scopeId.length() <= 128
                && PATTERN.matcher(scopeId).matches();
    }

    public static void requireValid(String scopeId) {
        if (!isValid(scopeId)) {
            throw new IllegalArgumentException(
                    "配置域标识必须为小写 URL-safe 分段 ID，长度不超过 128");
        }
    }

    public static boolean isReserved(String scopeId) {
        return RESERVED.contains(scopeId);
    }

    public static boolean isPlatform(String scopeId) {
        return isReserved(scopeId);
    }
}
