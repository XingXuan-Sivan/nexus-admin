package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.identity.Permission;
import com.nexusadmin.api.domain.identity.Role;
import com.nexusadmin.api.domain.identity.User;
import com.nexusadmin.api.domain.result.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 统一身份管理接口，聚合用户、角色、权限的查询与管理能力。
 *
 * <p>管理面板的身份管理界面通过此接口获取数据。平台提供 InMemoryIdentityService
 * 作为默认内存实现（预置 admin 用户），插件可通过声明 IdentityService 类型的 Bean
 * 整体替换为数据库实现。</p>
 *
 * <p><strong>命名说明：</strong>由于本接口聚合了用户、角色、权限三个实体，
 * CRUD 方法使用实体前缀命名（如 listUsers / getUser）以区分不同实体。
 * 单一实体 Service（如 DictionaryService、DepartmentService）则遵循平台统一规范，
 * 使用简短命名（list / get / create / update / delete）。</p>
 *
 * <p><strong>替换语义：</strong>替换此接口意味着同时替换用户、角色、权限三者的底层数据源，
 * 确保关联一致性。不允许部分替换。</p>
 */
public interface IdentityService {

    // ==================== 用户管理 ====================
    PageResult<User> listUsers(int page, int size);
    Optional<User> getUser(String id);
    Optional<User> getUserByUsername(String username);
    User createUser(User user);
    User updateUser(String id, User user);
    void deleteUser(String id);

    // ==================== 角色管理 ====================
    PageResult<Role> listRoles(int page, int size);
    Optional<Role> getRole(String id);
    Set<String> getUserRoleIds(String userId);
    Role createRole(Role role);
    Role updateRole(String id, Role role);
    void deleteRole(String id);

    // ==================== 权限管理 ====================
    List<Permission> listPermissions();
    Set<String> getUserPermissionCodes(String userId);
}
