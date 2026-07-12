package com.nexusadmin.plugin.system.user;

import com.nexusadmin.api.web.EnableWebEndpoints;
import com.nexusadmin.core.AbstractPlugin;

/**
 * 系统用户插件入口，负责在插件启动与停止时完成 SPI 注册与资源管理。
 */
@EnableWebEndpoints
public class SystemUserPlugin extends AbstractPlugin {

    @Override
    protected void initialize() throws Exception {
        // 插件初始化阶段，可以在此处初始化配置、数据结构等
    }

    @Override
    protected void start() throws Exception {
        // 预留扩展点：在此处通过 extensions() 注册自定义的认证、权限等扩展实现。
    }

    @Override
    protected void stop() throws Exception {
        // 预留扩展点：在此处从扩展注册中心注销实现并释放资源。
    }

    @Override
    protected void unload() throws Exception {
        // 清理外部资源
    }
}
