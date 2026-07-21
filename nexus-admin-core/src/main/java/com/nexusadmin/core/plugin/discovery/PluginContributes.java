package com.nexusadmin.core.plugin.discovery;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件贡献声明，描述插件向管理面板提供的菜单、路由、挂载点和权限。
 * <p>contributes 字段为可选，缺失时默认为空对象，确保旧插件兼容。</p>
 */
public final class PluginContributes {

    /**
     * 空贡献对象，用于未声明 contributes 的插件。
     */
    public static final PluginContributes EMPTY = new PluginContributes(
            List.of(), List.of(), List.of(), List.of()
    );

    /**
     * 菜单贡献列表。
     */
    private final List<MenuContribution> menus;

    /**
     * 路由贡献列表。
     */
    private final List<RouteContribution> routes;

    /**
     * 挂载点贡献列表。
     */
    private final List<MountPointContribution> mountPoints;

    /**
     * 权限声明列表。
     */
    private final List<PermissionContribution> permissions;

    /**
     * 构造插件贡献声明。
     *
     * @param menus       菜单贡献列表，可为 null
     * @param routes      路由贡献列表，可为 null
     * @param mountPoints 挂载点贡献列表，可为 null
     * @param permissions 权限声明列表，可为 null
     */
    public PluginContributes(List<MenuContribution> menus,
                             List<RouteContribution> routes,
                             List<MountPointContribution> mountPoints,
                             List<PermissionContribution> permissions) {
        this.menus = menus != null ? List.copyOf(menus) : List.of();
        this.routes = routes != null ? List.copyOf(routes) : List.of();
        this.mountPoints = mountPoints != null ? List.copyOf(mountPoints) : List.of();
        this.permissions = permissions != null ? List.copyOf(permissions) : List.of();
    }

    /**
     * 获取菜单贡献列表。
     *
     * @return 菜单列表，不可变
     */
    public List<MenuContribution> menus() {
        return menus;
    }

    /**
     * 获取路由贡献列表。
     *
     * @return 路由列表，不可变
     */
    public List<RouteContribution> routes() {
        return routes;
    }

    /**
     * 获取挂载点贡献列表。
     *
     * @return 挂载点列表，不可变
     */
    public List<MountPointContribution> mountPoints() {
        return mountPoints;
    }

    /**
     * 获取权限声明列表。
     *
     * @return 权限列表，不可变
     */
    public List<PermissionContribution> permissions() {
        return permissions;
    }

    /**
     * 判断是否为空贡献（所有子列表均为空）。
     *
     * @return 如果没有任何贡献返回 true
     */
    public boolean isEmpty() {
        return menus.isEmpty() && routes.isEmpty()
                && mountPoints.isEmpty() && permissions.isEmpty();
    }

    /**
     * 菜单贡献项。
     *
     * @param id          菜单唯一标识
     * @param label       菜单显示文本
     * @param icon        菜单图标标识
     * @param parentId    父级菜单ID
     * @param order       排序权重
     * @param route       关联路由路径
     * @param permissions 访问菜单所需权限列表（AND 关系）
     */
    public record MenuContribution(
            String id,
            String label,
            String icon,
            String parentId,
            int order,
            String route,
            List<String> permissions
    ) {
        /**
         * 紧凑构造器，处理空值默认。
         */
        public MenuContribution {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("菜单ID不能为空");
            }
            label = label != null ? label : "";
            icon = icon != null ? icon : "";
            parentId = parentId != null ? parentId : "";
            route = route != null ? route : "";
            permissions = permissions != null ? List.copyOf(permissions) : List.of();
        }
    }

    /**
     * 路由贡献项。
     *
     * @param path        路由路径
     * @param component   组件路径，如 pluginId/pages/Dashboard
     * @param title       路由标题
     * @param icon        路由图标
     * @param permissions 所需权限列表
     */
    public record RouteContribution(
            String path,
            String component,
            String title,
            String icon,
            List<String> permissions
    ) {
        /**
         * 紧凑构造器，处理空值默认。
         */
        public RouteContribution {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("路由路径不能为空");
            }
            component = component != null ? component : "";
            title = title != null ? title : "";
            icon = icon != null ? icon : "";
            permissions = permissions != null ? List.copyOf(permissions) : List.of();
        }
    }

    /**
     * 挂载点贡献项。
     *
     * @param target    挂载目标，如 dashboard.widgets
     * @param component 组件路径
     * @param order     排序权重
     * @param props     传递给组件的属性
     */
    public record MountPointContribution(
            String target,
            String component,
            int order,
            Map<String, Object> props
    ) {
        /**
         * 紧凑构造器，处理空值默认。
         */
        public MountPointContribution {
            if (target == null || target.isBlank()) {
                throw new IllegalArgumentException("挂载目标不能为空");
            }
            component = component != null ? component : "";
            props = props != null
                    ? Collections.unmodifiableMap(new HashMap<>(props))
                    : Map.of();
        }
    }

    /**
     * 权限声明项。
     *
     * @param id          权限唯一标识，如 analytics.view
     * @param label       权限显示名称
     * @param description 权限描述
     */
    public record PermissionContribution(
            String id,
            String label,
            String description
    ) {
        /**
         * 紧凑构造器，处理空值默认。
         */
        public PermissionContribution {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("权限ID不能为空");
            }
            label = label != null ? label : "";
            description = description != null ? description : "";
        }
    }
}
