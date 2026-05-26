package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.org.Position;
import com.nexusadmin.api.domain.result.PageResult;

import java.util.Optional;

/**
 * 岗位管理接口。
 *
 * <p>CRUD 方法命名遵循平台统一规范：list / get / create / update / delete。</p>
 */
public interface PositionService {

    /** 获取岗位列表（分页） */
    PageResult<Position> list(int page, int size);

    /** 获取岗位详情 */
    Optional<Position> get(String id);

    /** 创建岗位 */
    Position create(Position position);

    /** 更新岗位 */
    Position update(String id, Position position);

    /** 删除岗位 */
    void delete(String id);
}
