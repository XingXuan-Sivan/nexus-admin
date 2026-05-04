package com.nexusadmin.api.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限注解，标记方法或类需要特定的权限才能访问。
 * <p>
 * 特殊值 "*" 表示仅需认证，不检查具体权限。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 所需权限标识，格式为 {module}.{action}（如 "plugins.view"）。
     * 特殊值 "*" 表示仅需认证，不检查具体权限。
     *
     * @return 权限标识
     */
    String value();
}
