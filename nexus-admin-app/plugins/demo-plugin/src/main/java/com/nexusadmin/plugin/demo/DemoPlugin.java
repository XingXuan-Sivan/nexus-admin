package com.nexusadmin.plugin.demo;

import com.nexusadmin.core.AbstractPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 示例插件入口，负责在插件启动与停止时完成 SPI 注册与资源管理。
 */
public class DemoPlugin extends AbstractPlugin {

    private static final Logger log = LoggerFactory.getLogger(DemoPlugin.class);

    @Override
    protected void initialize() throws Exception {
        // 插件初始化阶段，可以在此处初始化配置、数据结构等
    }

    @Override
    protected void start() throws Exception {
        // 预留扩展点：在此处通过 extensions() 注册自定义的认证、权限等扩展实现。
        log.info("DemoPlugin 已启动，插件ID=" + pluginId());
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
