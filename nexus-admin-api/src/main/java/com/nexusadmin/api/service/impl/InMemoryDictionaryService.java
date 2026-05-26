package com.nexusadmin.api.service.impl;

import com.nexusadmin.api.domain.dictionary.Dictionary;
import com.nexusadmin.api.domain.dictionary.DictionaryItem;
import com.nexusadmin.api.domain.result.PageResult;
import com.nexusadmin.api.service.DictionaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字典管理的默认内存实现。
 */
public class InMemoryDictionaryService implements DictionaryService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDictionaryService.class);

    private final ConcurrentHashMap<String, Dictionary> dictionaries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, DictionaryItem>> dictItems = new ConcurrentHashMap<>();

    // ==================== 字典管理 ====================

    @Override
    public PageResult<Dictionary> list(int page, int size) {
        List<Dictionary> all = new ArrayList<>(dictionaries.values());
        int total = all.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        return PageResult.of(total, page, size, all.subList(from, to));
    }

    @Override
    public Optional<Dictionary> get(String code) {
        return Optional.ofNullable(dictionaries.get(code));
    }

    @Override
    public Dictionary create(Dictionary dict) {
        dictionaries.put(dict.code(), dict);
        log.info("创建字典：{}", dict.code());
        return dict;
    }

    @Override
    public Dictionary update(String code, Dictionary dict) {
        dictionaries.put(code, dict);
        log.info("更新字典：{}", code);
        return dict;
    }

    @Override
    public void delete(String code) {
        dictionaries.remove(code);
        dictItems.remove(code);
        log.info("删除字典：{}", code);
    }

    // ==================== 字典项管理 ====================

    @Override
    public List<DictionaryItem> listItems(String dictCode) {
        Map<String, DictionaryItem> items = dictItems.get(dictCode);
        return items != null ? List.copyOf(items.values()) : List.of();
    }

    @Override
    public DictionaryItem createItem(String dictCode, DictionaryItem item) {
        dictItems.computeIfAbsent(dictCode, k -> new LinkedHashMap<>()).put(item.key(), item);
        log.info("创建字典项：{}/{}", dictCode, item.key());
        return item;
    }

    @Override
    public DictionaryItem updateItem(String dictCode, String itemCode, DictionaryItem item) {
        Map<String, DictionaryItem> items = dictItems.get(dictCode);
        if (items != null) {
            items.put(itemCode, item);
        }
        log.info("更新字典项：{}/{}", dictCode, itemCode);
        return item;
    }

    @Override
    public void deleteItem(String dictCode, String itemCode) {
        Map<String, DictionaryItem> items = dictItems.get(dictCode);
        if (items != null) {
            items.remove(itemCode);
        }
        log.info("删除字典项：{}/{}", dictCode, itemCode);
    }
}
