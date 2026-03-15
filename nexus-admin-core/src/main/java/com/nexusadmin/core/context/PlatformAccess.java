package com.nexusadmin.core.context;

import com.nexusadmin.core.config.ConfigManager;
import com.nexusadmin.core.event.EventPublisher;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.plugin.RuntimeMode;

import java.util.Objects;
import java.util.Optional;

/**
 * 平台能力访问接口，提供插件与平台交互的统一入口。
 * <p>通过此接口插件可以访问扩展注册中心、事件发布、配置服务等核心能力。</p>
 */
public final class PlatformAccess {

    private final ExtensionRegistry extensionRegistry;
    private final EventPublisher eventPublisher;
    private final RuntimeMode runtimeMode;
    private final String coreVersion;
    private final ConfigManager configManager;

    /**
     * 构造平台访问对象。
     *
     * @param extensionRegistry 扩展注册中心
     * @param eventPublisher    事件发布者
     * @param runtimeMode       运行模式
     * @param coreVersion       核心版本号
     * @param configManager     配置管理器
     */
    public PlatformAccess(ExtensionRegistry extensionRegistry,
                          EventPublisher eventPublisher,
                          RuntimeMode runtimeMode,
                          String coreVersion,
                          ConfigManager configManager) {
        this.extensionRegistry = Objects.requireNonNull(extensionRegistry, "扩展注册中心不能为空");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "事件发布者不能为空");
        this.runtimeMode = Objects.requireNonNull(runtimeMode, "运行模式不能为空");
        this.coreVersion = Objects.requireNonNull(coreVersion, "核心版本号不能为空");
        this.configManager = configManager;
    }

    /**
     * 获取扩展注册中心。
     *
     * @return 扩展注册中心
     */
    public ExtensionRegistry extensions() {
        return extensionRegistry;
    }

    /**
     * 获取事件发布者。
     *
     * @return 事件发布者
     */
    public EventPublisher events() {
        return eventPublisher;
    }

    /**
     * 获取当前运行模式。
     *
     * @return 运行模式
     */
    public RuntimeMode runtimeMode() {
        return runtimeMode;
    }

    /**
     * 获取平台核心版本号。
     *
     * @return 核心版本号
     */
    public String coreVersion() {
        return coreVersion;
    }

    /**
     * 检查当前是否为开发模式。
     *
     * @return 如果是开发模式返回 true
     */
    public boolean isDevelopment() {
        return runtimeMode == RuntimeMode.DEVELOPMENT;
    }

    /**
     * 检查当前是否为部署模式（生产模式）。
     *
     * @return 如果是部署模式返回 true
     */
    public boolean isDeployment() {
        return runtimeMode == RuntimeMode.DEPLOYMENT;
    }

    /**
     * 获取配置管理器。
     *
     * @return 配置管理器，如果未配置可能返回 null
     */
    public ConfigManager config() {
        return configManager;
    }

    /**
     * 获取配置管理器（可选形式）。
     *
     * @return 配置管理器 Optional
     */
    public Optional<ConfigManager> configOpt() {
        return Optional.ofNullable(configManager);
    }
}
