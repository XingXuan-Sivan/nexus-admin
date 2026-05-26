package com.nexusadmin.api.service.impl;

import com.nexusadmin.api.domain.org.Department;
import com.nexusadmin.api.domain.result.PageResult;
import com.nexusadmin.api.service.DepartmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 部门管理的默认内存实现。
 */
public class InMemoryDepartmentService implements DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDepartmentService.class);

    private final ConcurrentHashMap<String, Department> departments = new ConcurrentHashMap<>();

    @Override
    public PageResult<Department> list(int page, int size) {
        List<Department> all = new ArrayList<>(departments.values());
        int total = all.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        return PageResult.of(total, page, size, all.subList(from, to));
    }

    @Override
    public Optional<Department> get(String id) {
        return Optional.ofNullable(departments.get(id));
    }

    @Override
    public Department create(Department dept) {
        departments.put(dept.id(), dept);
        log.info("创建部门：{}", dept.name());
        return dept;
    }

    @Override
    public Department update(String id, Department dept) {
        departments.put(id, dept);
        log.info("更新部门：{}", id);
        return dept;
    }

    @Override
    public void delete(String id) {
        departments.remove(id);
        log.info("删除部门：{}", id);
    }

    @Override
    public List<Department> getChildren(String parentId) {
        return departments.values().stream()
                .filter(d -> Objects.equals(d.parentId(), parentId))
                .collect(Collectors.toList());
    }
}
