package com.nexusadmin.api.domain.dictionary;

/**
 * 字典项领域对象，表示字典中的单个键值对。
 *
 * @param key   字典项键，在代码中使用的标识
 * @param value 字典项值，实际存储的数据
 * @param label 字典项标签，用于界面显示
 * @param order 字典项排序序号
 */
public record DictionaryItem(String key,
                             String value,
                             String label,
                             int order) {
}
