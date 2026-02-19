package com.nexusadmin.api.extension.ai;

import com.nexusadmin.api.context.CoreContext;
import com.nexusadmin.api.extension.ExtensionPoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 检索增强生成（RAG）扩展点，用于按查询条件检索相关文档并返回摘要等信息。
 *
 * @author NexusAdmin
 * @since 1.0.0
 */
public interface RagProvider extends ExtensionPoint {

    /**
     * 执行 RAG 检索，根据查询请求和上下文返回相关文档及摘要。
     *
     * @param request RAG 请求
     * @param context 平台上下文
     * @return RAG 响应
     */
    RagResponse retrieve(RagRequest request, CoreContext context);

    /**
     * 构造 RAG 检索请求参数。
     *
     * @param query   查询条件
     * @param topK    返回结果数量
     * @param filters 过滤条件
     */
    record RagRequest(String query,
                      int topK,
                      Map<String, String> filters) {
        public RagRequest {
            filters = filters == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(filters));
        }
    }

    /**
     * 构造 RAG 检索响应结果。
     *
     * @param documents 相关文档列表
     * @param summary   摘要内容
     * @param metadata  元数据
     */
    record RagResponse(List<RagDocument> documents,
                       String summary,
                       Map<String, String> metadata) {
        public RagResponse {
            documents = documents == null ? List.of() : List.copyOf(documents);
            metadata = metadata == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(metadata));
        }
    }

    /**
     * 构造 RAG 返回的单条文档。
     *
     * @param id        文档 ID
     * @param title     文档标题
     * @param content   文档内容
     * @param score     相关度分数
     * @param metadata  元数据
     */
    record RagDocument(String id,
                       String title,
                       String content,
                       double score,
                       Map<String, String> metadata) {
        public RagDocument {
            metadata = metadata == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(metadata));
        }
    }
}
