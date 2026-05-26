package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.dictionary.Dictionary;
import com.nexusadmin.api.domain.dictionary.DictionaryItem;
import com.nexusadmin.api.domain.result.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * 字典管理接口，提供数据字典的增删改查能力。
 *
 * <p>字典用于管理平台中的枚举值、下拉选项等常量数据，
 * 如"用户状态"、"日志级别"等。平台提供 InMemoryDictionaryService
 * 作为默认内存实现，插件可通过声明 DictionaryService 类型的 Bean 整体替换。</p>
 *
 * <p><strong>CRUD 方法命名遵循平台统一规范：</strong>list / get / create / update / delete。</p>
 */
public interface DictionaryService {

    /** 获取字典列表（分页） */
    PageResult<Dictionary> list(int page, int size);

    /** 获取字典详情 */
    Optional<Dictionary> get(String code);

    /** 创建字典 */
    Dictionary create(Dictionary dict);

    /** 更新字典 */
    Dictionary update(String code, Dictionary dict);

    /** 删除字典 */
    void delete(String code);

    // ==================== 字典项管理 ====================

    /** 获取指定字典的字典项列表 */
    List<DictionaryItem> listItems(String dictCode);

    /** 创建字典项 */
    DictionaryItem createItem(String dictCode, DictionaryItem item);

    /** 更新字典项 */
    DictionaryItem updateItem(String dictCode, String itemCode, DictionaryItem item);

    /** 删除字典项 */
    void deleteItem(String dictCode, String itemCode);
}
