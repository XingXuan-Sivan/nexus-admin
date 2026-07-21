package com.nexusadmin.api.ui;

import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.PluginState;
import com.nexusadmin.core.plugin.discovery.PluginContributes.MenuContribution;
import com.nexusadmin.core.plugin.discovery.PluginContributes.RouteContribution;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultUIContributionRegistryTest {

    @Test
    void platformProtectedMenus_shouldMatchTheirRoutePermissions() {
        PluginManager pluginManager = mock(PluginManager.class);
        when(pluginManager.listByState(PluginState.ACTIVE)).thenReturn(List.of());
        DefaultUIContributionRegistry registry = new DefaultUIContributionRegistry(pluginManager);

        Map<String, MenuContribution> menusById = registry.getMenus().stream()
                .collect(Collectors.toMap(MenuContribution::id, Function.identity()));
        Map<String, List<String>> permissionsByRoute = registry.getRoutes().stream()
                .collect(Collectors.toMap(RouteContribution::path, RouteContribution::permissions));

        registry.getMenus().stream()
                .filter(menu -> !menu.permissions().isEmpty())
                .forEach(menu -> assertEquals(
                        permissionsByRoute.get(resolveRoute(menu, menusById)),
                        menu.permissions(),
                        () -> "菜单与路由权限不一致: " + menu.id()
                ));
    }

    private String resolveRoute(MenuContribution menu, Map<String, MenuContribution> menusById) {
        if (menu.route().startsWith("/") || menu.parentId().isBlank()) {
            return menu.route();
        }
        MenuContribution parent = menusById.get(menu.parentId());
        return parent.route().replaceAll("/+$", "") + "/" + menu.route().replaceAll("^/+", "");
    }
}
