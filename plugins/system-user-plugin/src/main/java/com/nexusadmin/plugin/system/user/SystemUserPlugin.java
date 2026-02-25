package com.nexusadmin.plugin.system.user;


import com.nexusadmin.core.Plugin;
import com.nexusadmin.core.context.PluginContext;

/**
 * 系统用户插件入口，负责在插件启动与停止时完成 SPI 注册与资源管理。
 */
public class SystemUserPlugin implements Plugin {

    @Override
    public void onInitialize(PluginContext context) throws Exception {
        // 插件初始化阶段，可以在此处初始化配置、数据结构等
        System.out.println("SystemUserPlugin 初始化完成");
    }

    @Override
    public void onStart() throws Exception {
        // 预留扩展点：在此处通过 context.extensionRegistry() 注册自定义的认证、权限等扩展实现。
        System.out.println("SystemUserPlugin 启动完成");
    }

    @Override
    public void onStop() throws Exception {
        // 预留扩展点：在此处从扩展注册中心注销实现并释放资源。
        System.out.println("SystemUserPlugin 停止完成");
    }

    @Override
    public void onUnload() throws Exception {
        // 清理外部资源
        System.out.println("SystemUserPlugin 卸载完成");
    }
}
