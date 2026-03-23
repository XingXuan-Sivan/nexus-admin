package com.nexusadmin.core.event;

import java.util.Objects;

/**
 * 事件作用域匹配器，用于订阅时指定接收哪些作用域的事件。
 * <p>通过组合匹配器可以实现复杂的作用域过滤逻辑。</p>
 */
@FunctionalInterface
public interface EventScopeMatcher {

    /**
     * 判断给定作用域是否匹配。
     *
     * @param scope 要匹配的作用域
     * @return 如果匹配返回 true
     */
    boolean matches(EventScope scope);

    /**
     * 匹配所有作用域。
     *
     * @return 匹配所有作用域的匹配器
     */
    static EventScopeMatcher all() {
        return scope -> true;
    }

    /**
     * 精确匹配指定作用域。
     *
     * @param expected 期望的作用域
     * @return 精确匹配器
     */
    static EventScopeMatcher exact(EventScope expected) {
        Objects.requireNonNull(expected, "期望作用域不能为空");
        return scope -> expected.equals(scope);
    }

    /**
     * 匹配指定分类的所有作用域。
     *
     * @param category 作用域分类，如 global、platform、plugin、tenant
     * @return 分类匹配器
     */
    static EventScopeMatcher category(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("作用域分类不能为空");
        }
        String normalized = category.toLowerCase();
        return scope -> normalized.equals(scope.category());
    }

    /**
     * 匹配指定插件的作用域。
     *
     * @param pluginId 插件唯一标识
     * @return 插件作用域匹配器
     */
    static EventScopeMatcher plugin(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("插件ID不能为空");
        }
        return scope -> scope.isPlugin() && pluginId.equals(scope.identifier());
    }

    /**
     * 匹配所有插件作用域。
     *
     * @return 所有插件作用域匹配器
     */
    static EventScopeMatcher anyPlugin() {
        return EventScope::isPlugin;
    }

    /**
     * 匹配平台作用域。
     *
     * @return 平台作用域匹配器
     */
    static EventScopeMatcher platform() {
        return EventScope::isPlatform;
    }

    /**
     * 匹配全局作用域。
     *
     * @return 全局作用域匹配器
     */
    static EventScopeMatcher global() {
        return EventScope::isGlobal;
    }

    /**
     * 匹配指定租户的作用域。
     *
     * @param tenantId 租户唯一标识
     * @return 租户作用域匹配器
     */
    static EventScopeMatcher tenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        return scope -> scope.isTenant() && tenantId.equals(scope.identifier());
    }

    /**
     * 匹配所有租户作用域。
     *
     * @return 所有租户作用域匹配器
     */
    static EventScopeMatcher anyTenant() {
        return EventScope::isTenant;
    }

    /**
     * 组合匹配器：当前匹配器与另一个匹配器的逻辑与。
     *
     * @param other 另一个匹配器
     * @return 逻辑与组合匹配器
     */
    default EventScopeMatcher and(EventScopeMatcher other) {
        Objects.requireNonNull(other, "组合匹配器不能为空");
        return scope -> this.matches(scope) && other.matches(scope);
    }

    /**
     * 组合匹配器：当前匹配器与另一个匹配器的逻辑或。
     *
     * @param other 另一个匹配器
     * @return 逻辑或组合匹配器
     */
    default EventScopeMatcher or(EventScopeMatcher other) {
        Objects.requireNonNull(other, "组合匹配器不能为空");
        return scope -> this.matches(scope) || other.matches(scope);
    }

    /**
     * 取反匹配器。
     *
     * @return 逻辑非匹配器
     */
    default EventScopeMatcher negate() {
        return scope -> !this.matches(scope);
    }
}
