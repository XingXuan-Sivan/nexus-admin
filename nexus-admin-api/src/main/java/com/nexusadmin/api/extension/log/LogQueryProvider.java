package com.nexusadmin.api.extension.log;

import com.nexusadmin.api.context.CoreContext;
import com.nexusadmin.api.domain.log.LogEntry;
import com.nexusadmin.api.domain.log.LogLevel;
import com.nexusadmin.api.domain.log.LogType;
import com.nexusadmin.api.extension.ExtensionPoint;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日志查询扩展点，用于按条件检索平台产生的日志记录。
 *
 * @author NexusAdmin
 * @since 1.0.0
 */
public interface LogQueryProvider extends ExtensionPoint {

    /**
     * 按查询条件检索日志记录。
     *
     * @param query   日志查询请求
     * @param context 平台上下文
     * @return 匹配的日志条目列表
     */
    List<LogEntry> query(LogQuery query, CoreContext context);

    /**
     * 日志查询请求。
     *
     * @param type     日志类型
     * @param level    日志级别
     * @param keyword  关键字
     * @param from     查询开始时间
     * @param to       查询结束时间
     * @param limit    查询结果限制
     * @param filters  查询过滤条件
     */
    record LogQuery(LogType type,
                    LogLevel level,
                    String keyword,
                    Instant from,
                    Instant to,
                    int limit,
                    Map<String, String> filters) {
        public LogQuery {
            filters = filters == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(filters));
        }
    }
}
