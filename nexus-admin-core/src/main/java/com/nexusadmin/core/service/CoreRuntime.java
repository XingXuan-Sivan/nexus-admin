package com.nexusadmin.core.service;

import com.nexusadmin.core.context.CoreContext;
import com.nexusadmin.core.domain.log.LogContext;
import com.nexusadmin.core.domain.log.LogEntry;
import com.nexusadmin.core.domain.log.LogLevel;
import com.nexusadmin.core.domain.log.LogType;
import com.nexusadmin.core.exception.SpiNotFoundException;
import com.nexusadmin.core.spi.SpiRegistry;
import com.nexusadmin.core.spi.ai.AgentPolicy;
import com.nexusadmin.core.spi.ai.ChatProvider;
import com.nexusadmin.core.spi.ai.RagProvider;
import com.nexusadmin.core.spi.ai.ToolExecutor;
import com.nexusadmin.core.spi.auth.AuthProvider;
import com.nexusadmin.core.spi.cache.CacheProvider;
import com.nexusadmin.core.spi.log.LogWriter;
import com.nexusadmin.core.spi.permission.PermissionResolver;
import com.nexusadmin.core.spi.routing.RoutingPolicy;
import com.nexusadmin.core.spi.storage.StorageProvider;

import java.util.Optional;

/**
 * 平台核心运行时门面，封装认证、授权、路由、存储、缓存、AI 能力等统一入口。
 * <p>所有对外能力均依赖 {@link com.nexusadmin.core.spi.SpiRegistry} 提供的 SPI 实现。</p>
 */
public class CoreRuntime {
    private final SpiRegistry spiRegistry;

    /**
     * 构造核心运行时门面。
     *
     * @param spiRegistry SPI 注册中心，用于查找各类能力的具体实现
     */
    public CoreRuntime(SpiRegistry spiRegistry) {
        this.spiRegistry = spiRegistry;
    }

    /**
     * 执行认证操作，根据提供的认证请求和平台上下文调用 {@link AuthProvider}。
     *
     * @param request 认证请求，包含主体标识和凭证等信息
     * @param context 平台上下文
     * @return 认证结果
     */
    public AuthProvider.AuthResult authenticate(AuthProvider.AuthRequest request, CoreContext context) {
        AuthProvider provider = requireSpi(AuthProvider.class);
        AuthProvider.AuthResult result = provider.authenticate(request, context);
        writeAuditLog("auth", "auth result: " + result.status(), context);
        return result;
    }

    /**
     * 执行授权判断，根据权限检查请求和平台上下文调用 {@link PermissionResolver}。
     *
     * @param check   权限检查请求
     * @param context 平台上下文
     * @return 授权决策结果
     */
    public PermissionResolver.PermissionDecision authorize(PermissionResolver.PermissionCheck check,
                                                           CoreContext context) {
        PermissionResolver resolver = requireSpi(PermissionResolver.class);
        PermissionResolver.PermissionDecision decision = resolver.decide(check, context);
        writeAuditLog("permission", "permission result: " + decision.allowed(), context);
        return decision;
    }

    /**
     * 执行路由决策，根据路由请求和平台上下文调用 {@link RoutingPolicy}。
     *
     * @param request 路由请求
     * @param context 平台上下文
     * @return 路由决策结果
     */
    public RoutingPolicy.RouteDecision route(RoutingPolicy.RouteRequest request, CoreContext context) {
        RoutingPolicy policy = requireSpi(RoutingPolicy.class);
        RoutingPolicy.RouteDecision decision = policy.decide(request, context);
        writeAuditLog("route", "route result: " + decision.allowed(), context);
        return decision;
    }

    /**
     * 从存储中加载对象。
     *
     * @param key     存储键
     * @param context 平台上下文
     * @return 存储对象，可为空
     */
    public Optional<StorageProvider.StorageObject> load(StorageProvider.StorageKey key,
                                                        CoreContext context) {
        StorageProvider provider = requireSpi(StorageProvider.class);
        return provider.load(key, context);
    }

    /**
     * 将对象保存到存储中。
     *
     * @param object  存储对象
     * @param context 平台上下文
     */
    public void save(StorageProvider.StorageObject object, CoreContext context) {
        StorageProvider provider = requireSpi(StorageProvider.class);
        provider.save(object, context);
    }

    /**
     * 从缓存中读取数据。
     *
     * @param key     缓存键
     * @param context 平台上下文
     * @return 缓存值，可为空
     */
    public Optional<CacheProvider.CacheValue> cacheGet(CacheProvider.CacheKey key, CoreContext context) {
        CacheProvider provider = requireSpi(CacheProvider.class);
        return provider.get(key, context);
    }

    /**
     * 向缓存写入数据。
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param context 平台上下文
     */
    public void cachePut(CacheProvider.CacheKey key, CacheProvider.CacheValue value, CoreContext context) {
        CacheProvider provider = requireSpi(CacheProvider.class);
        provider.put(key, value, context);
    }

    /**
     * 从缓存中删除数据。
     *
     * @param key     缓存键
     * @param context 平台上下文
     */
    public void cacheEvict(CacheProvider.CacheKey key, CoreContext context) {
        CacheProvider provider = requireSpi(CacheProvider.class);
        provider.evict(key, context);
    }

    /**
     * 调用对话模型进行对话。
     *
     * @param request 对话请求
     * @param context 平台上下文
     * @return 对话响应
     */
    public ChatProvider.ChatResponse chat(ChatProvider.ChatRequest request, CoreContext context) {
        ChatProvider provider = requireSpi(ChatProvider.class);
        ChatProvider.ChatResponse response = provider.chat(request, context);
        writeAiLog("chat", "chat completed", context);
        return response;
    }

    /**
     * 调用 RAG 能力进行检索增强生成。
     *
     * @param request RAG 请求
     * @param context 平台上下文
     * @return RAG 响应
     */
    public RagProvider.RagResponse rag(RagProvider.RagRequest request, CoreContext context) {
        RagProvider provider = requireSpi(RagProvider.class);
        RagProvider.RagResponse response = provider.retrieve(request, context);
        writeAiLog("rag", "rag completed", context);
        return response;
    }

    /**
     * 执行工具调用。
     *
     * @param call    工具调用请求
     * @param context 平台上下文
     * @return 工具执行结果
     */
    public ToolExecutor.ToolResult executeTool(ToolExecutor.ToolCall call, CoreContext context) {
        ToolExecutor executor = requireSpi(ToolExecutor.class);
        ToolExecutor.ToolResult result = executor.execute(call, context);
        writeAiLog("tool", "tool completed: " + call.toolName(), context);
        return result;
    }

    /**
     * 执行智能体策略决策。
     *
     * @param request 智能体决策请求
     * @param context 平台上下文
     * @return 智能体决策结果
     */
    public AgentPolicy.AgentDecision decideAgent(AgentPolicy.AgentRequest request, CoreContext context) {
        AgentPolicy policy = requireSpi(AgentPolicy.class);
        AgentPolicy.AgentDecision decision = policy.decide(request, context);
        writeAiLog("agent", "agent decision completed", context);
        return decision;
    }

    /**
     * 写入一条日志到日志系统。
     *
     * @param entry   日志条目
     * @param context 平台上下文
     */
    public void writeLog(LogEntry entry, CoreContext context) {
        spiRegistry.get(LogWriter.class).ifPresent(writer -> writer.write(entry, context));
    }

    /**
     * 写入一条审计日志，内部封装为 {@link LogEntry} 并委托给日志 SPI。
     * 
     * @param action  日志动作
     * @param message 日志消息
     * @param context 平台上下文
     */
    private void writeAuditLog(String action, String message, CoreContext context) {
        LogEntry entry = new LogEntry(
                null,
                null,
                LogType.AUDIT,
                LogLevel.INFO,
                action + ": " + message,
                toLogContext(context),
                null
        );
        writeLog(entry, context);
    }

    /**
     * 写入一条 AI 日志，内部封装为 {@link LogEntry} 并委托给日志 SPI。
     * 
     * @param action  日志动作
     * @param message 日志消息
     * @param context 平台上下文
     */
    private void writeAiLog(String action, String message, CoreContext context) {
        LogEntry entry = new LogEntry(
                null,
                null,
                LogType.AI,
                LogLevel.INFO,
                action + ": " + message,
                toLogContext(context),
                null
        );
        writeLog(entry, context);
    }

    /**
     * 将 {@link CoreContext} 转换为日志上下文对象。
     *
     * @param context 平台上下文
     * @return 日志上下文
     */
    private LogContext toLogContext(CoreContext context) {
        if (context == null) {
            return new LogContext(null, null, null, null, null);
        }
        return new LogContext(
                context.userId(),
                context.tenantId(),
                context.traceId(),
                context.aiSessionId(),
                context.attributes()
        );
    }

    /**
     * 从 SPI 注册中心获取指定类型的实现，若不存在则抛出异常。
     *
     * @param spiType SPI 接口类型
     * @param <T>     SPI 接口泛型
     * @return SPI 实现
     */
    private <T> T requireSpi(Class<T> spiType) {
        return spiRegistry.get(spiType).orElseThrow(() -> new SpiNotFoundException(spiType));
    }
}
