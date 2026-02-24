package com.nexusadmin.app.listener;

import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.plugin.event.PluginStateChangedEvent;
import com.nexusadmin.core.plugin.event.PluginProcessEvent;
import com.nexusadmin.core.plugin.event.PluginUpgradeEvent;
import com.nexusadmin.core.plugin.event.PluginFailureEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 插件生命周期日志监听器。
 * <p>
 * 订阅插件生命周期事件并输出到日志。
 * 支持多行结构化提示信息。
 * </p>
 */
@Component
public class PluginLifecycleLogger {

    private static final Logger log =
            LoggerFactory.getLogger(PluginLifecycleLogger.class);

    private final EventBus eventBus;

    public PluginLifecycleLogger(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * 初始化时订阅生命周期事件。
     */
    @PostConstruct
    public void init() {
        eventBus.subscribe(PluginStateChangedEvent.class, this::onStateChanged);
        eventBus.subscribe(PluginProcessEvent.class, this::onProcess);
        eventBus.subscribe(PluginUpgradeEvent.class, this::onUpgrade);
        eventBus.subscribe(PluginFailureEvent.class, this::onFailure);
    }

    /**
     * 处理状态变更事件。
     */
    private void onStateChanged(PluginStateChangedEvent event) {
        log.debug("插件 [{}] {} → {}",
                event.plugin().getPluginId(),
                event.from(),
                event.to());
    }

    /**
     * 处理批处理阶段事件。
     */
    private void onProcess(PluginProcessEvent event) {
        if (event.stage() == PluginProcessEvent.Stage.START) {
            log.info("▶ [{}..]", phaseName(event.phase()));
            log.info("  数量 : {}", event.count());
        } else {
            log.info("{}完成", phaseName(event.phase()));
            log.info("  结果数量 : {}", event.count());
        }
    }

    /**
     * 处理升级事件。
     */
    private void onUpgrade(PluginUpgradeEvent event) {
        log.info("插件升级 [{}]，模式 : {}",
                event.pluginId(),
                event.mode() == PluginUpgradeEvent.Mode.HOT ? "热升级" : "冷升级");
    }

    /**
     * 处理失败事件。
     */
    private void onFailure(PluginFailureEvent event) {
        log.error("插件处理失败，插件ID : {}",
                event.pluginId(),
                event.error());
    }

    /**
     * 获取阶段名称。
     */
    private String phaseName(PluginProcessEvent.Phase phase) {
        return switch (phase) {
            case DISCOVER -> "插件扫描";
            case RESOLVE -> "插件解析";
            case LOAD -> "插件加载";
            case DELETE -> "插件删除";
        };
    }
}
