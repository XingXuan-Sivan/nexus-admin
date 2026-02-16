package com.nexusadmin.plugin.system.user;


import com.nexusadmin.api.Plugin;
import com.nexusadmin.api.PluginContext;
import com.nexusadmin.api.PluginDescriptor;

/**
 * 系统用户插件入口，负责在插件启动与停止时完成 SPI 注册与资源管理。
 */
public class SystemUserPlugin implements Plugin {

    @Override
    public PluginDescriptor descriptor() {
        // 实际的插件描述信息由平台通过 plugin.yml 加载并管理。
        throw new UnsupportedOperationException("PluginDescriptor 由平台运行时管理，插件无需直接构造。");
    }

    @Override
    public void start(PluginContext context) {
        // 预留扩展点：在此处通过 context.spiRegistry() 注册自定义的认证、权限等 SPI 实现。
        System.out.println("<<待定义启动代码>>。");
    }

    @Override
    public void stop(PluginContext context) {
        // 预留扩展点：在此处从 SPI 注册中心注销实现并释放资源。
    }
}
