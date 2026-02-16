package com.nexusadmin.api.domain.dictionary;

import java.util.List;

/**
 * 数据字典领域对象，表示一组相关的字典项集合。
 *
 * @param code  字典编码，唯一标识字典
 * @param name  字典名称
 * @param items 字典项列表
 */
public record Dictionary(String code,
                         String name,
                         List<DictionaryItem> items) {
    public Dictionary {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
