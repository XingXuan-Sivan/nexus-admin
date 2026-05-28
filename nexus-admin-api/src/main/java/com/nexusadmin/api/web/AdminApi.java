package com.nexusadmin.api.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 Controller 为管理面板 API。
 * <p>
 * 被 @AdminApi 标记的 Controller 将自动映射到 /admin 路径前缀，
 * 而非默认的 /api 前缀。
 * <p>
 * 使用示例：
 * <pre>
 * &#064;RestController
 * &#064;AdminApi
 * &#064;RequestMapping("/plugins")
 * public class PluginController {
 *     // 实际访问路径: /admin/plugins
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminApi {
}
