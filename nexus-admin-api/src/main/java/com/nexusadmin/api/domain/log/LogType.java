package com.nexusadmin.api.domain.log;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 日志类型，标识日志的业务分类。
 *
 * <p>采用 class 而非 enum 设计，参照 {@link com.nexusadmin.core.event.EventScope}
 * 的扩展模式，支持插件通过 {@link #of(String, String)} 自由创建自定义类型。</p>
 *
 * <h3>内置分类体系</h3>
 * <ul>
 *   <li><b>system</b> — 平台运行日志（插件生命周期、API请求、配置变更、AI调用、
 *       定时任务、错误堆栈等全部运行时日志）</li>
 *   <li><b>audit</b> — 安全审计日志（登录、权限检查、敏感操作等需留痕的安全事件）</li>
 * </ul>
 */
public final class LogType {

    /** 主分类：system / audit */
    private final String category;

    /** 子类型名称：如 login、plugin-lifecycle */
    private final String name;

    private LogType(String category, String name) {
        this.category = Objects.requireNonNull(category, "category").toLowerCase();
        this.name = Objects.requireNonNull(name, "name").toLowerCase();
    }

    // ═══════════════ 内置标准常量 ═══════════════

    /** 系统通用日志 */
    public static final LogType SYSTEM = new LogType("system", "general");

    /** 审计通用日志 */
    public static final LogType AUDIT = new LogType("audit", "general");

    // ═══════════════ 内置常用子类型工厂方法 ═══════════════

    /** 系统子类型：插件生命周期事件 */
    public static LogType systemPluginLifecycle() {
        return new LogType("system", "plugin-lifecycle");
    }

    /** 系统子类型：API 请求/响应 */
    public static LogType systemApiRequest() {
        return new LogType("system", "api-request");
    }

    /** 系统子类型：配置变更 */
    public static LogType systemConfigChange() {
        return new LogType("system", "config-change");
    }

    /** 系统子类型：AI 调用 */
    public static LogType systemAiCall() {
        return new LogType("system", "ai-call");
    }

    /** 审计子类型：登录事件 */
    public static LogType auditLogin() {
        return new LogType("audit", "login");
    }

    /** 审计子类型：权限检查 */
    public static LogType auditPermission() {
        return new LogType("audit", "permission");
    }

    /** 审计子类型：用户敏感操作 */
    public static LogType auditOperation() {
        return new LogType("audit", "operation");
    }

    // ═══════════════ 插件扩展入口 ═══════════════

    /**
     * 创建自定义日志类型（供插件使用）。
     * <p>插件可自由组合 category 和 name 来定义领域专属日志类型，
     * 管理面板通过 category 和 typeName 字符串参数进行过滤。</p>
     *
     * @param category 主分类，如 "system"、"audit"
     * @param name     子类型名称，如 "order-created"、"data-export"
     * @return 日志类型实例
     */
    public static LogType of(String category, String name) {
        return new LogType(category, name);
    }

    // ═══════════════ getters ═══════════════

    public String category() {
        return category;
    }

    public String name() {
        return name;
    }

    /** 判断是否属于指定分类 */
    public boolean isCategory(String cat) {
        return category.equalsIgnoreCase(cat);
    }

    // ═══════════════ JSON 序列化 ═══════════════

    @JsonValue
    @Override
    public String toString() {
        return category + "/" + name;
    }

    @JsonCreator
    public static LogType fromString(String value) {
        if (value == null || value.isBlank()) {
            return SYSTEM;
        }
        int idx = value.indexOf('/');
        if (idx < 0) {
            return new LogType(value, "general");
        }
        return new LogType(value.substring(0, idx), value.substring(idx + 1));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LogType that)) {
            return false;
        }
        return category.equals(that.category) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, name);
    }
}
