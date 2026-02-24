package com.nexusadmin.app.listener;

import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.plugin.PluginState;
import com.nexusadmin.core.plugin.event.PluginStateChangedEvent;
import com.nexusadmin.core.plugin.event.PluginProcessEvent;
import com.nexusadmin.core.plugin.event.PluginUpgradeEvent;
import com.nexusadmin.core.plugin.event.PluginFailureEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * 记录各阶段开始时间，用于计算耗时。
     */
    private final Map<PluginProcessEvent.Phase, Instant> phaseStartTimes =
            new ConcurrentHashMap<>();

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
     * 插件状态变更事件。
     *
     * INFO：语义化生命周期提示
     * DEBUG：原始状态迁移轨迹
     */
    private void onStateChanged(PluginStateChangedEvent event) {

        String pluginId = event.plugin().getPluginId();
        PluginState from = event.from();
        PluginState to = event.to();

        // 语义化提示（INFO）
        switch (to) {

            case INITIALIZED ->
                    log.info("插件 [{}] 初始化完成", pluginId);

            case STARTING ->
                    log.info("插件 [{}] 启动中...", pluginId);

            case ACTIVE -> {
                // 区分是首次启动还是重新启用
                if (from == PluginState.STARTING) {
                    log.info("插件 [{}] 启动完成", pluginId);
                }
            }

            case STOPPING ->
                    log.info("插件 [{}] 停止中...", pluginId);

            case STOPPED ->
                    log.info("插件 [{}] 停止完成", pluginId);

            case DISABLED ->
                    log.info("插件 [{}] 已禁用", pluginId);

            case UNLOADED ->
                    log.info("插件 [{}] 已卸载", pluginId);

            default -> {
                // 其他状态不输出语义提示
            }
        }

        // 底层状态轨迹（DEBUG）
        log.debug("插件 [{}] 状态迁移: {} → {}",
                pluginId,
                from,
                to);
    }

    /**
     * 批处理（平台流程）阶段事件。
     */
    private void onProcess(PluginProcessEvent event) {

        String phaseName = phaseName(event.phase());

        if (event.stage() == PluginProcessEvent.Stage.START) {

            phaseStartTimes.put(event.phase(), event.occurredAt());

            log.info("▶ [{}...]", phaseName);
            log.info("开始    | 数量 : {}", event.count());

        } else {

            Instant start = phaseStartTimes.remove(event.phase());
            long cost = 0L;

            if (start != null) {
                cost = Duration.between(start, event.occurredAt()).toMillis();
            }

            log.info("完成    | 数量 : {} | 耗时 : {} ms",
                    event.count(),
                    cost);
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
        log.error("插件 [{}] 处理失败",
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
