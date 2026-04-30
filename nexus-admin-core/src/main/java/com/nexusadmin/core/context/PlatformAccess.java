package com.nexusadmin.core.context;

import com.nexusadmin.core.config.ConfigManager;
import com.nexusadmin.core.event.EventPublisher;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.plugin.RuntimeMode;

import java.util.Objects;
import java.util.Optional;

/**
 * 平台能力访问接口，提供插件与平台交互的统一入口。
 * <p>通过此接口插件可以访问扩展注册中心、事件发布、配置服务、平台服务等核心能力。</p>
 */
public final class PlatformAccess {

    private final ExtensionRegistry extensionRegistry;
    private final EventPublisher eventPublisher;
    private final RuntimeMode runtimeMode;
    private final String coreVersion;
    private final ConfigManager configManager;
    private final PlatformServices platformServices;

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
        this(extensionRegistry, eventPublisher, runtimeMode, coreVersion, configManager, new PlatformServices());
    }

    /**
     * 构造平台访问对象。
     *
     * @param extensionRegistry 扩展注册中心
     * @param eventPublisher    事件发布者
     * @param runtimeMode       运行模式
     * @param coreVersion       核心版本号
     * @param configManager     配置管理器
     * @param platformServices  平台服务注册中心
     */
    public PlatformAccess(ExtensionRegistry extensionRegistry,
                          EventPublisher eventPublisher,
                          RuntimeMode runtimeMode,
                          String coreVersion,
                          ConfigManager configManager,
                          PlatformServices platformServices) {
        this.extensionRegistry = Objects.requireNonNull(extensionRegistry, "扩展注册中心不能为空");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "事件发布者不能为空");
        this.runtimeMode = Objects.requireNonNull(runtimeMode, "运行模式不能为空");
        this.coreVersion = Objects.requireNonNull(coreVersion, "核心版本号不能为空");
        this.configManager = configManager;
        this.platformServices = platformServices != null ? platformServices : new PlatformServices();
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
        return runtimeMode == RuntimeMode.DEV;
    }

    /**
     * 检查当前是否为部署模式（生产模式）。
     *
     * @return 如果是部署模式返回 true
     */
    public boolean isDeployment() {
        return runtimeMode == RuntimeMode.PROD;
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

    /**
     * 获取平台服务注册中心。
     *
     * @return 平台服务注册中心
     */
    public PlatformServices services() {
        return platformServices;
    }

    /**
     * 获取指定类型的服务实例。
     * <p>这是 {@link #services()} 的便捷方法。</p>
     *
     * @param <T>  服务类型
     * @param type 服务接口类型
     * @return 服务实例的 Optional，如果未注册则返回空
     */
    public <T> Optional<T> service(Class<T> type) {
        return platformServices.get(type);
    }
}
