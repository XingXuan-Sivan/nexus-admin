package com.nexusadmin.plugin.admin;

import com.nexusadmin.api.extension.web.EnableWebEndpoints;
import com.nexusadmin.core.AbstractPlugin;

/**
 * 管理面板插件入口。
 * <p>
 * 提供平台管理 REST API 和基础认证能力。
 * 通过 {@link EnableWebEndpoints} 注解启用自动扫描，
 * 控制器由平台自动发现并注册到 Web 框架。
 */
@EnableWebEndpoints
public class AdminPanelPlugin extends AbstractPlugin {
}
