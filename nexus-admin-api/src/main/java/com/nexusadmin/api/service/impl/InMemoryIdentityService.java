package com.nexusadmin.api.service.impl;

import com.nexusadmin.api.domain.identity.Permission;
import com.nexusadmin.api.domain.identity.Role;
import com.nexusadmin.api.domain.identity.User;
import com.nexusadmin.api.domain.result.PageResult;
import com.nexusadmin.api.service.IdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 身份管理的默认内存实现，预置 admin 用户和内置权限码。
 *
 * <p>预置数据：
 * <ul>
 *   <li>admin 用户（id=admin，拥有 admin 角色）</li>
 *   <li>admin 角色（id=admin，拥有全部内置权限码）</li>
 *   <li>10 个内置权限码</li>
 * </ul>
 */
public class InMemoryIdentityService implements IdentityService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryIdentityService.class);

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Role> roles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Permission> permissions = new ConcurrentHashMap<>();

    public InMemoryIdentityService() {
        initBuiltinPermissions();
        initBuiltinRoles();
        initBuiltinUsers();
        log.info("已初始化 InMemoryIdentityService，预置 admin 用户及 10 个权限码");
    }

    private void initBuiltinPermissions() {
        String[][] permDefs = {
                {"plugins.view", "查看插件", "允许查看插件列表与详情", "plugins", "view"},
                {"plugins.manage", "管理插件", "允许启动/停止/启用/禁用/卸载插件", "plugins", "manage"},
                {"plugins.upload", "上传插件", "允许上传插件 JAR 包", "plugins", "upload"},
                {"users.view", "查看用户", "允许查看用户列表与详情", "users", "view"},
                {"users.manage", "管理用户", "允许创建/更新/删除用户", "users", "manage"},
                {"roles.view", "查看角色", "允许查看角色列表与详情", "roles", "view"},
                {"roles.manage", "管理角色", "允许创建/更新/删除角色", "roles", "manage"},
                {"permissions.view", "查看权限", "允许查看权限列表与权限树", "permissions", "view"},
                {"config.view", "查看配置", "允许查看平台及插件配置", "config", "view"},
                {"config.manage", "管理配置", "允许修改平台及插件配置", "config", "manage"},
                {"system.view", "查看系统", "允许查看系统状态与平台信息", "system", "view"},
                {"ai.view", "查看AI", "允许查看 AI 管理相关功能", "ai", "view"},
                {"storage.view", "查看存储", "允许查看存储管理相关功能", "storage", "view"},
                {"logs.view", "查看日志", "允许查看各类系统日志", "logs", "view"},
        };
        for (String[] def : permDefs) {
            Permission perm = new Permission(def[0], def[1], def[2], def[3], def[4], Map.of());
            permissions.put(perm.code(), perm);
        }
    }

    private void initBuiltinRoles() {
        Set<String> allPermCodes = permissions.keySet();
        Role adminRole = new Role("admin", "管理员", allPermCodes, Map.of());
        roles.put(adminRole.id(), adminRole);
    }

    private void initBuiltinUsers() {
        User adminUser = new User("admin", "admin", "系统管理员",
                Set.of("admin"), Set.of(), Map.of());
        users.put(adminUser.id(), adminUser);
    }

    // ==================== 用户管理 ====================

    @Override
    public PageResult<User> listUsers(int page, int size) {
        List<User> all = new ArrayList<>(users.values());
        int total = all.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        List<User> pageItems = all.subList(from, to);
        return PageResult.of(total, page, size, pageItems);
    }

    @Override
    public Optional<User> getUser(String id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return users.values().stream()
                .filter(u -> u.username().equals(username))
                .findFirst();
    }

    @Override
    public User createUser(User user) {
        users.put(user.id(), user);
        log.info("创建用户：{}", user.username());
        return user;
    }

    @Override
    public User updateUser(String id, User user) {
        users.put(id, user);
        log.info("更新用户：{}", id);
        return user;
    }

    @Override
    public void deleteUser(String id) {
        users.remove(id);
        log.info("删除用户：{}", id);
    }

    // ==================== 角色管理 ====================

    @Override
    public PageResult<Role> listRoles(int page, int size) {
        List<Role> all = new ArrayList<>(roles.values());
        int total = all.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        List<Role> pageItems = all.subList(from, to);
        return PageResult.of(total, page, size, pageItems);
    }

    @Override
    public Optional<Role> getRole(String id) {
        return Optional.ofNullable(roles.get(id));
    }

    @Override
    public Set<String> getUserRoleIds(String userId) {
        User user = users.get(userId);
        return user != null ? user.roleIds() : Set.of();
    }

    @Override
    public Role createRole(Role role) {
        roles.put(role.id(), role);
        log.info("创建角色：{}", role.name());
        return role;
    }

    @Override
    public Role updateRole(String id, Role role) {
        roles.put(id, role);
        log.info("更新角色：{}", id);
        return role;
    }

    @Override
    public void deleteRole(String id) {
        roles.remove(id);
        log.info("删除角色：{}", id);
    }

    // ==================== 权限管理 ====================

    @Override
    public List<Permission> listPermissions() {
        return List.copyOf(permissions.values());
    }

    @Override
    public Set<String> getUserPermissionCodes(String userId) {
        User user = users.get(userId);
        if (user == null) {
            return Set.of();
        }
        return user.roleIds().stream()
                .map(roles::get)
                .filter(Objects::nonNull)
                .flatMap(role -> role.permissionCodes().stream())
                .collect(Collectors.toSet());
    }
}
