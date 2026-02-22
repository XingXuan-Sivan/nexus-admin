package com.nexusadmin.core.plugin;

/**
 * 运行模式枚举，用于区分开发和生产环境。
 * <p>替代硬编码的 dev 标志判断，提供更清晰的语义。</p>
 */
public enum RuntimeMode {

    /**
     * 开发模式。
     * <p>启用热加载、详细日志、调试功能等。</p>
     */
    DEVELOPMENT,

    /**
     * 部署模式（生产模式）。
     * <p>启用性能优化、缓存、安全限制等，禁用开发专用策略。</p>
     */
    DEPLOYMENT
}
