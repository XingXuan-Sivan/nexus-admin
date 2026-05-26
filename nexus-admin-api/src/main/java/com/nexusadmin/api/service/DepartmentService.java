package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.org.Department;
import com.nexusadmin.api.domain.result.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * 部门管理接口。
 *
 * <p>CRUD 方法命名遵循平台统一规范：list / get / create / update / delete。</p>
 */
public interface DepartmentService {

    /** 获取部门列表（分页） */
    PageResult<Department> list(int page, int size);

    /** 获取部门详情 */
    Optional<Department> get(String id);

    /** 创建部门 */
    Department create(Department dept);

    /** 更新部门 */
    Department update(String id, Department dept);

    /** 删除部门 */
    void delete(String id);

    /** 获取子部门列表 */
    List<Department> getChildren(String parentId);
}
