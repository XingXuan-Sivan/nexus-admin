package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.identity.Permission;
import com.nexusadmin.api.exception.ExtensionNotImplementedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 权限查询服务（骨架实现）。
 *
 * <p>提供权限列表与权限树的查询能力，当前为骨架实现，实际功能由拓展点驱动。</p>
 * <p>支持通过声明同类型 Bean 覆盖，插件可提供完整的权限查询实现。</p>
 */
@Service
public class PermissionService {

    /**
     * 获取权限列表。
     *
     * @return 权限视图列表，不为空
     */
    public List<Permission> list() {
        throw new ExtensionNotImplementedException("权限查询由拓展点实现");
    }

    /**
     * 获取按模块分组的权限树。
     *
     * @return 模块-权限映射，不为空
     */
    public Map<String, List<Permission>> tree() {
        throw new ExtensionNotImplementedException("权限查询由拓展点实现");
    }
}
