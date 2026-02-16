package com.nexusadmin.core.plugin.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志记录生命周期监听器。
 * <p>将插件生命周期事件记录到日志中。</p>
 */
public class LoggingLifecycleListener implements PluginLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingLifecycleListener.class);

    @Override
    public void onEvent(PluginLifecycleEvent event) {
        switch (event.type()) {
            // 阶段开始
            case DISCOVER_START, RESOLVE_START, INSTALL_START ->
                    log.info("=====[{}]=====", event.message());

            // 阶段结束
            case DISCOVER_END, RESOLVE_END ->
                    log.info("{}", event.message());

            case INSTALL_END ->
                    log.info("{}，当前共安装 {} 个插件", event.message(), event.payload());

            // 成功事件
            case INSTALLED ->
                    log.info("{}", event.message());

            case STARTED ->
                    log.info("{}", event.message());

            case STOPPED ->
                    log.debug("{}", event.message());

            case UNINSTALLED ->
                    log.info("{}", event.message());

            // 跳过事件
            case SKIPPED ->
                    log.debug("{}", event.message());

            // 失败事件
            case FAILED -> {
                if (event.payload() instanceof PluginLifecycleEvent.FailurePayload payload) {
                    log.error("{}", event.message(), payload.error());
                } else {
                    log.error("{}", event.message());
                }
            }

            // 进行中的事件
            case DISCOVERED, RESOLVED, STARTING, STOPPING, UNINSTALLING ->
                    log.debug("{}", event.message());
        }
    }
}
