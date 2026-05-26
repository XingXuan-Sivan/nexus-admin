package com.nexusadmin.api.service.impl;

import com.nexusadmin.api.domain.org.Position;
import com.nexusadmin.api.domain.result.PageResult;
import com.nexusadmin.api.service.PositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 岗位管理的默认内存实现。
 */
public class InMemoryPositionService implements PositionService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryPositionService.class);

    private final ConcurrentHashMap<String, Position> positions = new ConcurrentHashMap<>();

    @Override
    public PageResult<Position> list(int page, int size) {
        List<Position> all = new ArrayList<>(positions.values());
        int total = all.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        return PageResult.of(total, page, size, all.subList(from, to));
    }

    @Override
    public Optional<Position> get(String id) {
        return Optional.ofNullable(positions.get(id));
    }

    @Override
    public Position create(Position position) {
        positions.put(position.id(), position);
        log.info("创建岗位：{}", position.name());
        return position;
    }

    @Override
    public Position update(String id, Position position) {
        positions.put(id, position);
        log.info("更新岗位：{}", id);
        return position;
    }

    @Override
    public void delete(String id) {
        positions.remove(id);
        log.info("删除岗位：{}", id);
    }
}
