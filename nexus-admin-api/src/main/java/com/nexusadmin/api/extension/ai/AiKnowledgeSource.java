package com.nexusadmin.api.extension.ai;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.core.extension.ExtensionPoint;

import java.util.List;

/**
 * AI 知识源扩展点，提供可被 AI 检索的领域知识。
 *
 * <p>知识源为 AI 提供"背景信息"——平台文档、数据库 Schema、
 * API 契约等。AiProvider 在对话前检索相关知识源，将结果注入上下文。</p>
 *
 * <p>此接口只定义"检索"语义，不关心底层是向量数据库、全文搜索还是图谱查询。</p>
 */
public interface AiKnowledgeSource extends ExtensionPoint {

    /**
     * 按查询条件检索知识。
     *
     * @param query   自然语言查询
     * @param topK    最大返回条数
     * @param context 调用上下文
     * @return 相关知识条目列表
     */
    List<KnowledgeEntry> search(String query, int topK, InvocationContext context);

    /**
     * 知识源名称。
     *
     * @return 知识源名称
     */
    String name();

    /**
     * 知识条目。
     *
     * @param title     标题
     * @param content   内容
     * @param relevance 相关度
     * @param source    来源标识
     */
    record KnowledgeEntry(String title, String content,
                           double relevance, String source) {
    }
}
