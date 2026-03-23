package com.nexusadmin.core.event;

import java.util.Objects;

/**
 * 事件作用域，标识事件所属的逻辑空间。
 * <p>作用域由分类(category)和标识符(identifier)两部分组成，
 * 用于实现事件的隔离与路由。</p>
 * <p>分类取值与配置中心作用域保持一致，确保跨模块概念统一。</p>
 *
 * <h3>标准作用域分类</h3>
 * <ul>
 *   <li><b>global</b> - 全局作用域，所有订阅者可见</li>
 *   <li><b>platform</b> - 平台作用域，平台核心功能使用</li>
 *   <li><b>plugin</b> - 插件作用域，特定插件的私有事件</li>
 *   <li><b>tenant</b> - 租户作用域，多租户场景使用</li>
 * </ul>
 */
public final class EventScope {

    /**
     * 作用域分类，如 global、platform、plugin、tenant。
     */
    private final String category;

    /**
     * 作用域标识符，如插件ID、租户ID等。
     * 对于 global 作用域可为空。
     */
    private final String identifier;

    private EventScope(String category, String identifier) {
        this.category = Objects.requireNonNull(category, "作用域分类不能为空").toLowerCase();
        this.identifier = identifier;
    }

    /**
     * 获取全局作用域。
     * <p>全局事件对所有订阅者可见。</p>
     *
     * @return 全局作用域实例
     */
    public static EventScope global() {
        return new EventScope("global", null);
    }

    /**
     * 获取平台作用域。
     * <p>平台级事件，用于平台核心功能、平台配置变更等场景。</p>
     *
     * @return 平台作用域实例
     */
    public static EventScope platform() {
        return new EventScope("platform", "platform");
    }

    /**
     * 获取插件作用域。
     * <p>插件私有事件，仅对订阅了该插件作用域的监听器可见。</p>
     *
     * @param pluginId 插件唯一标识
     * @return 插件作用域实例
     * @throws IllegalArgumentException 如果 pluginId 为空或空白
     */
    public static EventScope plugin(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("插件ID不能为空");
        }
        return new EventScope("plugin", pluginId);
    }

    /**
     * 获取租户作用域。
     * <p>租户级事件，用于多租户场景。</p>
     *
     * @param tenantId 租户唯一标识
     * @return 租户作用域实例
     * @throws IllegalArgumentException 如果 tenantId 为空或空白
     */
    public static EventScope tenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        return new EventScope("tenant", tenantId);
    }

    /**
     * 创建自定义作用域。
     * <p>用于扩展标准分类之外的场景。</p>
     *
     * @param category   作用域分类
     * @param identifier 作用域标识符
     * @return 自定义作用域实例
     * @throws IllegalArgumentException 如果 category 为空或空白
     */
    public static EventScope custom(String category, String identifier) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("作用域分类不能为空");
        }
        return new EventScope(category, identifier);
    }

    /**
     * 获取作用域分类。
     *
     * @return 分类字符串，如 global、platform、plugin、tenant
     */
    public String category() {
        return category;
    }

    /**
     * 获取作用域标识符。
     *
     * @return 标识符字符串，global 作用域可能返回 null
     */
    public String identifier() {
        return identifier;
    }

    /**
     * 判断是否为全局作用域。
     *
     * @return 如果是 global 分类返回 true
     */
    public boolean isGlobal() {
        return "global".equals(category);
    }

    /**
     * 判断是否为平台作用域。
     *
     * @return 如果是 platform 分类返回 true
     */
    public boolean isPlatform() {
        return "platform".equals(category);
    }

    /**
     * 判断是否为插件作用域。
     *
     * @return 如果是 plugin 分类返回 true
     */
    public boolean isPlugin() {
        return "plugin".equals(category);
    }

    /**
     * 判断是否为租户作用域。
     *
     * @return 如果是 tenant 分类返回 true
     */
    public boolean isTenant() {
        return "tenant".equals(category);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventScope that = (EventScope) o;
        return category.equals(that.category) && Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, identifier);
    }

    @Override
    public String toString() {
        if (identifier == null) {
            return category;
        }
        return category + ":" + identifier;
    }
}
