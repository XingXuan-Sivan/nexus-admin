# ADR-003: 配置治理与SPI热替换方案

------

## 背景与问题

Nexus Admin 采用微内核架构，通过 ExtensionRegistry 管理扩展点实现，通过 ConfigManager 管理运行时配置。但在实际演进中，出现了四个相互关联的架构问题：

1. **配置治理不统一**：部分配置通过 YML 静态管理（无法热更新），部分通过 ConfigManager 动态管理（可热更新），两者缺乏统一的归属划分标准。`BootstrapAuthProperties` 通过 `@ConfigurationProperties` 绑定，与 ConfigManager 完全脱节。

2. **SPI 消费方式陈旧**：`CompositeAuthProvider` 通过 `List<AuthProvider>` Spring 注入，在启动时固化。运行时插件注册新 `AuthProvider` 到 ExtensionRegistry 后，CompositeAuthProvider 无法感知。而 `WebEndpointExtension` 已正确注册到 ExtensionRegistry，可作为正面参考。

3. **AuthChallengeHandler 未纳入扩展点体系**：不继承 `ExtensionPoint`，在 `AdminAutoConfig` 中硬编码选择 `BootstrapAuthChallengeHandler` 或 `DefaultAuthChallengeHandler`，运行时无法切换。

4. **装配职责不清晰**：`AdminAutoConfig`（api模块）同时负责认证组件装配和管理面板基础设施装配，缺少专门的扩展点注册配置类。

------

## 议题一：配置归属划分

### 划分原则

配置项归属的判断标准：

```text
YML 保留（静态配置）：
  - Spring 框架级配置（server.port、spring.*、logging.level）
  - 启动后不可变更的配置（文件路径、运行模式）
  - 安全敏感的引导配置（引导认证凭据，仅启动时读取一次）

配置中心承载（热修改配置）：
  - 运行时需要动态调整的配置
  - 面向运维可观测/可修改的配置
  - 多值/列表类配置（如禁用插件列表）
  - 平台级策略配置（日志级别、并发限制）
```

### 具体划分方案

| 配置项 | 当前归属 | 目标归属 | 判断理由 |
|--------|----------|----------|----------|
| `spring.application.name` | YML | YML | Spring 框架级，启动后不可变 |
| `spring.profiles.active` | YML | YML | Spring 框架级，决定配置源加载 |
| `server.port` | YML | YML | Spring 框架级，启动后不可变 |
| `platform.info.name` | YML | 配置中心 | 运维可观测，可能动态调整平台名称 |
| `platform.info.version` | YML | 配置中心 | 运维可观测，版本展示 |
| `platform.info.description` | YML | 配置中心 | 运维可观测，描述可能更新 |
| `platform.plugin.path` | YML | YML | 文件系统路径，启动后不可变 |
| `platform.plugin.core-version` | YML | 配置中心 | 从 YML 移除，统一由配置中心管理（Schema 已定义 coreVersion） |
| `platform.plugin.runtime-mode` | YML | YML | 影响插件加载策略，运行时切换有风险 |
| `platform.plugin.auto-start` | YML | 配置中心 | 运维可能需要动态开关自动启动 |
| `platform.auth.bootstrap.username` | YML | 配置中心 | 需支持热修改引导凭据 |
| `platform.auth.bootstrap.password` | YML | 配置中心 | 需支持热修改引导凭据 |
| `logging.level.root` | YML | YML | Spring 日志框架级，YML 管理 |

**补充说明**：

- `platform.plugin.core-version` 同时存在于 YML 和 Schema，移除 YML 中的定义，统一由配置中心管理
- `platform.auth.bootstrap.*` 从 YML 迁入配置中心后，`BootstrapAuthProperties` 不再通过 `@ConfigurationProperties` 绑定，改为从 `ConfigManager` 读取

### 配置中心 Schema 扩展

当前 `platform.yml` Schema 仅定义了 3 个键，需要扩展以覆盖所有配置中心管理的配置项：

```yaml
pluginId: platform
properties:
  # === 已有 ===
  coreVersion:
    type: string
    title: 核心版本
    description: 平台核心版本号，只读展示
    default: "1.0.0"
  maxConcurrentPlugins:
    type: integer
    title: 最大并发插件数
    description: 平台允许同时运行的插件数量上限
    default: 50
    minimum: 1
    maximum: 500
  logLevel:
    type: enum
    title: 日志级别
    description: 平台全局日志级别
    enum: [DEBUG, INFO, WARN, ERROR]
    default: INFO

  # === 新增：平台信息 ===
  infoName:
    type: string
    title: 平台名称
    description: 平台显示名称
    default: "Nexus Admin"
  infoVersion:
    type: string
    title: 平台版本
    description: 平台版本号
    default: "0.1.0-SNAPSHOT"
  infoDescription:
    type: string
    title: 平台描述
    description: 平台功能描述
    default: "插件化系统拓展平台"

  # === 新增：插件策略 ===
  autoStart:
    type: boolean
    title: 自动启动插件
    description: 是否在加载后自动启动具有入口点的插件
    default: true

  # === 新增：引导认证 ===
  bootstrapUsername:
    type: string
    title: 引导认证用户名
    description: 系统引导认证的管理员用户名
    default: "admin"
  bootstrapPassword:
    type: string
    title: 引导认证密码
    description: 系统引导认证的管理员密码（敏感配置）
    default: "admin123"
    sensitive: true
```

------

## 议题二：SPI 热替换通用方案

### 核心原则

```text
所有 SPI 消费方必须在运行时从 ExtensionRegistry 动态解析，禁止启动时固化。
```

这意味着：

1. **禁止** `List<ExtensionPoint>` Spring 注入作为消费手段
2. **禁止** 在 `@Bean` 方法中根据当前扩展点快照做一次性决策
3. **必须** 在每次调用时从 ExtensionRegistry 获取最新实现列表

### 方案设计

#### 2.2.1 默认实现的注册规范

所有默认实现（平台内置的扩展点实现）统一通过 `ExtensionRegistry.register()` 注册，而非仅作为 Spring Bean 存在：

```java
// 改造前：仅注册为 Spring Bean，扩展点体系不可见
@Bean
public BootstrapAuthProvider bootstrapAuthProvider(BootstrapAuthProperties properties) {
    return new BootstrapAuthProvider(properties);
}

// 改造后：同时注册到 ExtensionRegistry
@Bean
public BootstrapAuthProvider bootstrapAuthProvider(
        BootstrapAuthProperties properties,
        ExtensionRegistry extensionRegistry) {
    BootstrapAuthProvider provider = new BootstrapAuthProvider(properties);
    // 注册为最低优先级兜底实现
    extensionRegistry.register(AuthProvider.class, provider, 25);
    return provider;
}
```

**注册规范**：

| 实现类型 | 优先级范围 | 说明 |
|----------|-----------|------|
| 平台引导/兜底实现 | 0 ~ 49 | 最低优先级，作为 fallback |
| 平台增强实现 | 50 ~ 99 | 平台标准实现 |
| 插件实现 | 100+ | 插件注册的实现在此范围 |
| 人工指定高优先级 | 200+ | 显式指定优先级的实现 |

#### 2.2.2 消费方的动态解析模式

消费方每次调用时从 ExtensionRegistry 动态获取实现列表，而非持有启动时的快照：

```java
// 改造前：启动时固化
public class CompositeAuthProvider implements AuthProvider {
    private final List<AuthProvider> providers; // 构造时固化

    public CompositeAuthProvider(List<AuthProvider> providers) {
        this.providers = filterProviders(providers);
    }
}

// 改造后：运行时动态解析
public class CompositeAuthProvider implements AuthProvider {
    private final ExtensionRegistry registry;

    public CompositeAuthProvider(ExtensionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public AuthResult authenticate(AuthRequest request, InvocationContext context) {
        List<AuthProvider> providers = resolveProviders();
        // ... 按优先级依次尝试
    }

    private List<AuthProvider> resolveProviders() {
        List<AuthProvider> all = registry.getAll(AuthProvider.class);
        // 自动降级：当存在非引导认证时，过滤掉引导认证
        boolean hasNonBootstrap = all.stream()
                .anyMatch(p -> !(p instanceof BootstrapAuthProvider));
        if (hasNonBootstrap) {
            return all.stream()
                    .filter(p -> !(p instanceof BootstrapAuthProvider))
                    .toList();
        }
        return all;
    }
}
```

#### 2.2.3 扩展点变更的事件通知机制

当 ExtensionRegistry 发生注册/注销变更时，发布事件通知消费方：

```java
// ExtensionRegistry 变更事件
public record ExtensionRegistryChangeEvent(
    Class<? extends ExtensionPoint> pointType,
    ChangeType changeType,       // REGISTERED / UNREGISTERED
    ExtensionPoint implementation,
    int priority
) {}

// 消费方监听变更，刷新缓存
public class CompositeAuthProvider implements AuthProvider {

    private volatile List<AuthProvider> cachedProviders;
    private final ExtensionRegistry registry;

    public CompositeAuthProvider(ExtensionRegistry registry, EventBus eventBus) {
        this.registry = registry;
        // 监听扩展点变更，失效缓存
        eventBus.subscribe(ExtensionRegistryChangeEvent.class, this::onRegistryChange);
    }

    private void onRegistryChange(ExtensionRegistryChangeEvent event) {
        if (AuthProvider.class.equals(event.pointType())) {
            this.cachedProviders = null; // 失效缓存
        }
    }

    private List<AuthProvider> resolveProviders() {
        List<AuthProvider> cached = this.cachedProviders;
        if (cached != null) {
            return cached;
        }
        List<AuthProvider> all = registry.getAll(AuthProvider.class);
        cached = applyDegradationPolicy(all);
        this.cachedProviders = cached;
        return cached;
    }
}
```

#### 2.2.4 扩展点通用消费者

对于所有扩展点的消费，统一通过 `ExtensionConsumer` 获取实现。`ExtensionConsumer<T extends ExtensionPoint>` 是 core 模块提供的通用泛型模板类，提供 `get()` 获取最高优先级单实现和 `getAll()` 获取所有实现，内置缓存 + 事件驱动失效机制。

归属：**core 模块** `com.nexusadmin.core.extension.ExtensionConsumer`（依赖 ExtensionRegistry 和 EventBus，均为 core 组件；作为通用基础设施，不依赖 Spring）。

```java
package com.nexusadmin.core.extension;

/**
 * 扩展点通用消费者模板 -- 提供缓存 + 事件驱动失效的动态解析能力
 * 所有需要消费扩展点的组件都应通过此类获取扩展实现，而非直接调用 ExtensionRegistry
 */
public class ExtensionConsumer<T extends ExtensionPoint> {
    private final Class<T> pointType;
    private final ExtensionRegistry registry;
    private volatile List<T> cachedAll;
    private volatile T cachedTop;

    public ExtensionConsumer(Class<T> pointType, ExtensionRegistry registry, EventBus eventBus) {
        this.pointType = pointType;
        this.registry = registry;
        // 监听扩展注册变更事件，自动失效缓存
        eventBus.subscribe(ExtensionChangedEvent.class, event -> {
            if (event.pointType().equals(pointType)) {
                invalidateCache();
            }
        });
    }

    /** 获取最高优先级实现 */
    public Optional<T> get() {
        T top = cachedTop;
        if (top == null) {
            synchronized (this) {
                top = cachedTop;
                if (top == null) {
                    top = registry.get(pointType).orElse(null);
                    cachedTop = top;
                }
            }
        }
        return Optional.ofNullable(top);
    }

    /** 获取所有实现（按优先级降序） */
    public List<T> getAll() {
        List<T> all = cachedAll;
        if (all == null) {
            synchronized (this) {
                all = cachedAll;
                if (all == null) {
                    all = List.copyOf(registry.getAll(pointType));
                    cachedAll = all;
                }
            }
        }
        return all;
    }

    private void invalidateCache() {
        cachedAll = null;
        cachedTop = null;
    }
}
```

**缓存策略对比**：

| 策略 | 适用场景 | 一致性 | 性能 |
|------|---------|--------|------|
| 无缓存（直接调用 Registry） | 极低频扩展点 | 强一致 | 低 |
| ExtensionConsumer | 所有扩展点（统一模板，按需缓存） | 最终一致 | 高 |

**使用示例**：在 api 模块的 `CompositeAuthProvider` 中使用：

```java
public class CompositeAuthProvider implements AuthProvider {
    private final ExtensionConsumer<AuthProvider> consumer;

    public CompositeAuthProvider(ExtensionRegistry registry, EventBus eventBus) {
        this.consumer = new ExtensionConsumer<>(AuthProvider.class, registry, eventBus);
    }

    @Override
    public AuthResult authenticate(AuthRequest request, InvocationContext context) {
        List<AuthProvider> providers = consumer.getAll();
        // ... 按优先级依次尝试
    }
}
```

### 改造示例：CompositeAuthProvider

```java
// ==================== 改造前 ====================

// AdminAutoConfig.java
@Bean
public CompositeAuthProvider compositeAuthProvider(List<AuthProvider> providers) {
    return new CompositeAuthProvider(providers);  // 启动时固化
}

// CompositeAuthProvider.java
public class CompositeAuthProvider implements AuthProvider {
    private final List<AuthProvider> providers;  // 不可变列表

    public CompositeAuthProvider(List<AuthProvider> providers) {
        this.providers = filterProviders(providers);
    }

    @Override
    public AuthResult authenticate(AuthRequest request, InvocationContext context) {
        for (AuthProvider provider : providers) {  // 永远使用启动时的列表
            // ...
        }
    }
}

// 问题：运行时插件注册新 AuthProvider 到 ExtensionRegistry，此处置若罔闻


// ==================== 改造后 ====================

// AuthExtensionConfig.java（新配置类）
@Configuration
public class AuthExtensionConfig {

    @Bean
    public BootstrapAuthProvider bootstrapAuthProvider(
            ConfigManager configManager,
            ExtensionRegistry extensionRegistry) {
        // 从 ConfigManager 读取凭据（支持热更新）
        String username = configManager.getString("platform", "bootstrapUsername").orElse("admin");
        String password = configManager.getString("platform", "bootstrapPassword").orElse("admin123");
        BootstrapAuthProvider provider = new BootstrapAuthProvider(username, password);
        // 注册到 ExtensionRegistry，最低优先级
        extensionRegistry.register(AuthProvider.class, provider, 25);
        return provider;
    }

    @Bean
    public CompositeAuthProvider compositeAuthProvider(
            ExtensionRegistry extensionRegistry,
            EventBus eventBus) {
        return new CompositeAuthProvider(extensionRegistry, eventBus);
    }
}

// CompositeAuthProvider.java（改造后）
public class CompositeAuthProvider implements AuthProvider {

    private final ExtensionRegistry registry;
    private volatile List<AuthProvider> cachedProviders;

    public CompositeAuthProvider(ExtensionRegistry registry, EventBus eventBus) {
        this.registry = registry;
        eventBus.subscribe(ExtensionRegistryChangeEvent.class, this::onRegistryChange);
    }

    @Override
    public AuthResult authenticate(AuthRequest request, InvocationContext context) {
        List<AuthProvider> providers = resolveProviders();

        if (providers.isEmpty()) {
            return AuthResult.builder()
                    .status(AuthStatus.FAILED)
                    .message("系统未配置认证服务")
                    .build();
        }

        AuthResult result = AuthResult.builder()
                .status(AuthStatus.FAILED)
                .message("认证失败")
                .build();

        for (AuthProvider provider : providers) {
            try {
                result = provider.authenticate(request, context);
                if (result.status() == AuthStatus.SUCCESS) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("认证提供者执行异常: {} - {}",
                        provider.getClass().getSimpleName(), e.getMessage());
            }
        }
        return result;
    }

    private List<AuthProvider> resolveProviders() {
        List<AuthProvider> cached = this.cachedProviders;
        if (cached != null) {
            return cached;
        }
        List<AuthProvider> all = registry.getAll(AuthProvider.class);
        cached = applyDegradationPolicy(all);
        this.cachedProviders = cached;
        return cached;
    }

    private void onRegistryChange(ExtensionRegistryChangeEvent event) {
        if (AuthProvider.class.equals(event.pointType())) {
            this.cachedProviders = null;  // 失效缓存
        }
    }

    /**
     * 降级策略：当存在非引导认证时，自动排除引导认证。
     */
    private List<AuthProvider> applyDegradationPolicy(List<AuthProvider> all) {
        boolean hasNonBootstrap = all.stream()
                .anyMatch(p -> !(p instanceof BootstrapAuthProvider));
        if (hasNonBootstrap) {
            return all.stream()
                    .filter(p -> !(p instanceof BootstrapAuthProvider))
                    .toList();
        }
        return all;
    }
}
```

### 各扩展点消费方式对照

所有扩展点消费方统一通过 `ExtensionConsumer` 获取实现，不再单独维护缓存逻辑：

| 扩展点 | 消费方法 | 说明 |
|--------|---------|------|
| AuthProvider | `getAll()` | 责任链依次尝试，高频调用 |
| WebEndpointExtension | `get()` | 插件生命周期调用 |
| StorageProvider | `get()` | 主实现切换 |
| CacheProvider | `get()` | 主实现切换 |
| PermissionResolver | `getAll()` | 每请求可能调用 |
| RoutingPolicy | `get()` | 请求路由 |
| LogWriter | `getAll()` | 写操作责任链 |
| LogQueryProvider | `get()` | 低频查询 |
| LogRetentionPolicy | `get()` | 策略类 |
| ChatProvider | `get()` | 中频 |
| RagProvider | `get()` | 低频 |
| ToolExecutor | `getAll()` | 低频 |
| AgentPolicy | `get()` | 策略类 |

------

## 议题三：认证提供者组合策略

### 方案对比

| 维度 | 单一实现（最高优先级） | 多实现组合（责任链） |
|------|----------------------|---------------------|
| 认证方式 | 仅使用优先级最高的 AuthProvider | 按优先级依次尝试所有 AuthProvider |
| 扩展性 | 插件替换认证，完全接管 | 插件增加认证方式，与现有方式共存 |
| 降级能力 | 无降级，最高优先级失败即失败 | 某个 Provider 失败可尝试下一个 |
| 复杂度 | 低 | 中 |
| 典型场景 | 企业SSO完全替换引导认证 | 多种认证共存（LDAP + 本地账号） |

### 推荐方案

推荐采用 **"责任链 + 自动降级"** 模式：

```text
多个 AuthProvider 按优先级依次尝试
  → 高优先级 Provider 成功 → 直接返回
  → 高优先级 Provider 失败 → 尝试下一个
  → 所有 Provider 均失败 → 返回认证失败

自动降级：
  → 当存在非引导认证 Provider 时，引导认证自动禁用
  → BootstrapAuthProvider 仅作为最低优先级兜底
```

**设计要点**：

1. **BootstrapAuthProvider 作为最低优先级兜底**：priority=25，确保在没有其他认证方式时系统仍可使用
2. **当有非引导认证存在时，引导认证自动降级/禁用**：`CompositeAuthProvider.resolveProviders()` 中过滤掉 `BootstrapAuthProvider` 实例
3. **AuthChallengeHandler 也应跟随动态切换**：当前引导认证活跃时显示登录页，非引导认证活跃时返回 401 JSON

**运行时行为示意**：

```text
场景 1：仅 BootstrapAuthProvider（初始状态）
  ┌─ AuthProvider 列表 ─────────────────┐
  │ BootstrapAuthProvider (priority=25) │
  └─────────────────────────────────────┘
  → 引导认证活跃，显示 HTML 登录页
  → 用户可通过 admin/admin123 登录

场景 2：安装了 LDAP 认证插件后
  ┌─ AuthProvider 列表 ─────────────────┐
  │ LdapAuthProvider    (priority=100)  │
  │ BootstrapAuthProvider (priority=25) │  ← 自动过滤掉
  └─────────────────────────────────────┘
  → 引导认证自动禁用，显示 401 JSON 响应
  → 用户通过 LDAP 凭证登录

场景 3：LDAP 插件被卸载
  ┌─ AuthProvider 列表 ─────────────────┐
  │ BootstrapAuthProvider (priority=25) │  ← 自动恢复
  └─────────────────────────────────────┘
  → 引导认证自动恢复，显示 HTML 登录页
  → 回退到 admin/admin123 登录
```

### 优先级与降级策略

**优先级分配规则**：

| AuthProvider 实现 | 优先级 | 说明 |
|-------------------|--------|------|
| BootstrapAuthProvider | 25 | 平台兜底，最低优先级 |
| 平台增强认证 | 50~99 | 如未来内置的 Token 认证 |
| 插件认证实现 | 100~199 | 插件注册的认证方式 |
| 显式高优先级 | 200+ | 管理员手动指定的优先实现 |

**降级逻辑伪代码**：

```java
/**
 * 解析当前有效的认证提供者列表。
 * <p>
 * 降级策略：当 Registry 中存在非 BootstrapAuthProvider 时，
 * 自动排除 BootstrapAuthProvider，实现引导认证的自动禁用。
 * 当非引导认证被卸载后，引导认证自动恢复。
 */
private List<AuthProvider> resolveProviders() {
    List<AuthProvider> all = registry.getAll(AuthProvider.class);

    boolean hasNonBootstrap = all.stream()
            .anyMatch(p -> !(p instanceof BootstrapAuthProvider));

    if (hasNonBootstrap) {
        log.debug("检测到其他认证提供者，引导认证已自动禁用");
        return all.stream()
                .filter(p -> !(p instanceof BootstrapAuthProvider))
                .toList();
    }

    return all;
}
```

------

## 议题四：规范性改进

### 4.1 AuthChallengeHandler 扩展点化

**当前问题**：

- `AuthChallengeHandler` 不继承 `ExtensionPoint`，无法通过 ExtensionRegistry 管理
- 在 `AdminAutoConfig.authChallengeHandler()` 中硬编码选择实现：

```java
// 当前：硬编码选择
@Bean
public AuthChallengeHandler authChallengeHandler(CompositeAuthProvider compositeAuthProvider) {
    boolean bootstrapActive = compositeAuthProvider.getProviders().stream()
            .anyMatch(p -> p instanceof BootstrapAuthProvider);
    if (bootstrapActive) {
        return new BootstrapAuthChallengeHandler();  // HTML 登录页
    }
    return new DefaultAuthChallengeHandler();         // 401 JSON
}
```

**改造方案**：

1. 让 `AuthChallengeHandler` 继承 `ExtensionPoint`
2. 将 `BootstrapAuthChallengeHandler` 和 `DefaultAuthChallengeHandler` 注册到 ExtensionRegistry
3. 消费方（AuthFilter）运行时动态解析

```java
// 改造后：AuthChallengeHandler 继承 ExtensionPoint
public interface AuthChallengeHandler extends ExtensionPoint {
    void handleChallenge(HttpServletRequest request,
                         HttpServletResponse response,
                         String message) throws IOException;
}

// AuthExtensionConfig 中注册默认实现
@Bean
public BootstrapAuthChallengeHandler bootstrapAuthChallengeHandler(
        ExtensionRegistry extensionRegistry) {
    BootstrapAuthChallengeHandler handler = new BootstrapAuthChallengeHandler();
    extensionRegistry.register(AuthChallengeHandler.class, handler, 25);
    return handler;
}

@Bean
public DefaultAuthChallengeHandler defaultAuthChallengeHandler(
        ExtensionRegistry extensionRegistry) {
    DefaultAuthChallengeHandler handler = new DefaultAuthChallengeHandler();
    extensionRegistry.register(AuthChallengeHandler.class, handler, 10);
    return handler;
}

// AuthFilter 中动态解析
public class AuthFilter implements Filter {
    private final ExtensionRegistry registry;

    private AuthChallengeHandler resolveChallengeHandler() {
        // 根据当前认证状态动态选择
        List<AuthProvider> providers = registry.getAll(AuthProvider.class);
        boolean hasNonBootstrap = providers.stream()
                .anyMatch(p -> !(p instanceof BootstrapAuthProvider));

        if (hasNonBootstrap) {
            // 非引导认证活跃 → 跳过低优先级的引导处理器
            return registry.getAll(AuthChallengeHandler.class).stream()
                    .filter(h -> !(h instanceof BootstrapAuthChallengeHandler))
                    .findFirst()
                    .orElseGet(() -> registry.get(AuthChallengeHandler.class).orElseThrow());
        }
        // 仅引导认证 → 使用引导处理器
        return registry.getAll(AuthChallengeHandler.class).stream()
                .filter(h -> h instanceof BootstrapAuthChallengeHandler)
                .findFirst()
                .orElseGet(() -> registry.get(AuthChallengeHandler.class).orElseThrow());
    }
}
```

### 4.2 BootstrapAuthProvider 配置中心集成

**当前问题**：

- `BootstrapAuthProperties` 通过 `@ConfigurationProperties(prefix = "platform.auth.bootstrap")` 绑定 YML
- 构造 `BootstrapAuthProvider` 时从 Properties 读取，凭据在启动后不可变更
- 与 ConfigManager 完全脱节

**改造方案**：

1. 将引导认证凭据迁入配置中心（Schema 中增加 `bootstrapUsername` / `bootstrapPassword`）
2. `BootstrapAuthProvider` 改为从 `ConfigManager` 读取凭据，支持热更新
3. 监听配置变更事件，自动刷新凭据

```java
// 改造前：从 @ConfigurationProperties 读取，启动时固化
public class BootstrapAuthProvider implements AuthProvider {
    private final String username;
    private final String password;

    public BootstrapAuthProvider(BootstrapAuthProperties properties) {
        this.username = properties.getUsername();  // 不可变
        this.password = properties.getPassword();  // 不可变
    }
}


// 改造后：从 ConfigManager 读取，支持热更新
public class BootstrapAuthProvider implements AuthProvider {
    private static final String SCOPE = "platform";
    private static final String KEY_USERNAME = "bootstrapUsername";
    private static final String KEY_PASSWORD = "bootstrapPassword";

    private final ConfigManager configManager;
    private volatile String username;
    private volatile String password;

    public BootstrapAuthProvider(ConfigManager configManager, EventBus eventBus) {
        this.configManager = configManager;
        this.username = configManager.getString(SCOPE, KEY_USERNAME).orElse("admin");
        this.password = configManager.getString(SCOPE, KEY_PASSWORD).orElse("admin123");

        // 监听配置变更，热更新凭据
        eventBus.subscribe(ConfigChangeEvent.class, this::onConfigChange);
    }

    private void onConfigChange(ConfigChangeEvent event) {
        if (!SCOPE.equals(event.scope())) return;
        if (KEY_USERNAME.equals(event.key())) {
            this.username = configManager.getString(SCOPE, KEY_USERNAME).orElse("admin");
            log.info("引导认证用户名已更新");
        }
        if (KEY_PASSWORD.equals(event.key())) {
            this.password = configManager.getString(SCOPE, KEY_PASSWORD).orElse("admin123");
            log.info("引导认证密码已更新");
        }
    }

    @Override
    public AuthResult authenticate(AuthRequest request, InvocationContext context) {
        // 使用 volatile 字段，每次读取最新值
        if (username.equals(request.principal()) && password.equals(request.credential())) {
            return AuthResult.builder()
                    .status(AuthStatus.SUCCESS)
                    .userId(username)
                    .message("认证成功")
                    .attribute("authType", "bootstrap")
                    .attribute("role", "admin")
                    .build();
        }
        return AuthResult.builder()
                .status(AuthStatus.FAILED)
                .message("用户名或密码错误")
                .build();
    }
}
```

**配置迁移步骤**：

1. 在 `platform.yml` Schema 中新增 `bootstrapUsername` 和 `bootstrapPassword` 定义
2. `BootstrapAuthProperties` 保留但标记为 `@Deprecated`，过渡期仍从 YML 读取默认值写入配置中心
3. YML 中的 `platform.auth.bootstrap.*` 保留作为初始值来源，但运行时修改走配置中心
4. 后续版本移除 YML 中的引导认证配置和 `BootstrapAuthProperties` 类

### 4.3 装配层职责划分

**当前问题**：

- `ApiAutoConfig`（api模块）虽已拆分出 `AuthExtensionConfig`，但装配职责描述需进一步明确
- 缺少专门的扩展点注册配置类

**改造方案**：

将 `AdminAutoConfig` 拆分为 `ApiAutoConfig`（负责组件扫描）和 `ApiInfraAutoConfig`（负责管理面板基础设施装配）：

```text
ApiAutoConfig（api模块）
  └─ @ComponentScan("com.nexusadmin.api")  — 组件扫描

ApiInfraAutoConfig（api模块）
  ├─ UIContributionRegistry 装配
  ├─ PermissionInterceptor 装配
  └─ PluginStaticResourceController 装配

AuthExtensionConfig（api模块）
  ├─ BootstrapAuthProvider 注册到 ExtensionRegistry
  ├─ CompositeAuthProvider 装配（注入 ExtensionRegistry）
  ├─ BootstrapAuthChallengeHandler 注册到 ExtensionRegistry
  └─ DefaultAuthChallengeHandler 注册到 ExtensionRegistry
```

**改造后的配置类**：

```java
// ==================== AuthExtensionConfig（新增） ====================

/**
 * 认证扩展点配置。
 * <p>
 * 专责认证扩展点的注册与装配，职责边界：
 * <ul>
 *   <li>将认证相关默认实现注册到 ExtensionRegistry</li>
 *   <li>装配 CompositeAuthProvider（消费 ExtensionRegistry）</li>
 * </ul>
 */
@Configuration
public class AuthExtensionConfig {

    @Bean
    public BootstrapAuthProvider bootstrapAuthProvider(
            ConfigManager configManager,
            ExtensionRegistry extensionRegistry,
            EventBus eventBus) {
        BootstrapAuthProvider provider = new BootstrapAuthProvider(configManager, eventBus);
        extensionRegistry.register(AuthProvider.class, provider, 25);
        return provider;
    }

    @Bean
    public BootstrapAuthChallengeHandler bootstrapAuthChallengeHandler(
            ExtensionRegistry extensionRegistry) {
        BootstrapAuthChallengeHandler handler = new BootstrapAuthChallengeHandler();
        extensionRegistry.register(AuthChallengeHandler.class, handler, 25);
        return handler;
    }

    @Bean
    public DefaultAuthChallengeHandler defaultAuthChallengeHandler(
            ExtensionRegistry extensionRegistry) {
        DefaultAuthChallengeHandler handler = new DefaultAuthChallengeHandler();
        extensionRegistry.register(AuthChallengeHandler.class, handler, 10);
        return handler;
    }

    @Bean
    public CompositeAuthProvider compositeAuthProvider(
            ExtensionRegistry extensionRegistry,
            EventBus eventBus) {
        return new CompositeAuthProvider(extensionRegistry, eventBus);
    }
}


// ==================== ApiAutoConfig（精简后） ====================

/**
 * API 模块自动配置。
 * <p>
 * 通过组件扫描发现 Controller、Service 等 Spring 组件。
 */
@Configuration
@ComponentScan("com.nexusadmin.api")
public class ApiAutoConfig {
}
```

### 4.4 扩展点注册规范

**统一规范**：所有默认实现通过 `ExtensionRegistry.register()` 注册。

```java
/**
 * 扩展点注册规范（所有默认实现必须遵守）：
 *
 * 1. 注册时机：在 @Bean 方法中同时注册到 ExtensionRegistry
 * 2. 优先级规范：
 *    - 0~49：  平台兜底/引导实现
 *    - 50~99： 平台标准实现
 *    - 100+：  插件实现
 * 3. Spring Bean 保留：@Bean 方法仍返回实例，供其他组件直接注入使用
 * 4. 插件卸载清理：通过 extensionRegistry.unregisterByPluginId() 批量清理
 */

// 正面参考：WebEndpointExtension 的注册方式（app 模块）
@Bean
public WebEndpointExtension webEndpointExtension(
        ApplicationContext applicationContext,
        ExtensionRegistry extensionRegistry) {
    SpringWebEndpointExtension extension = new SpringWebEndpointExtension(applicationContext);
    extensionRegistry.register(WebEndpointExtension.class, extension, 50);
    return extension;
}
```

**当前各扩展点注册状态**：

| 扩展点 | 是否继承 ExtensionPoint | 是否注册到 ExtensionRegistry | 是否符合规范 |
|--------|------------------------|----------------------------|-------------|
| WebEndpointExtension | 是 | 是（priority=50） | 符合 |
| AuthProvider | 是 | 否（仅 Spring Bean） | 不符合 |
| AuthChallengeHandler | 否 | 否（硬编码选择） | 不符合 |
| StorageProvider | 是 | 否 | 待确认 |
| CacheProvider | 是 | 否 | 待确认 |
| PermissionResolver | 是 | 否 | 待确认 |
| RoutingPolicy | 是 | 否 | 待确认 |
| LogWriter | 是 | 否 | 待确认 |
| LogQueryProvider | 是 | 否 | 待确认 |
| LogRetentionPolicy | 是 | 否 | 待确认 |
| ChatProvider | 是 | 否 | 待确认 |
| RagProvider | 是 | 否 | 待确认 |
| ToolExecutor | 是 | 否 | 待确认 |
| AgentPolicy | 是 | 否 | 待确认 |

------

## 议题五：模块依赖单向化

### 背景

当前 app 模块的 POM 同时依赖了 `nexus-admin-core` 和 `nexus-admin-api`。由于 api 模块已经依赖 core，app 对 core 的直接依赖是冗余的。目标改为严格的 `core ← api ← app` 单向链式依赖。

### 现状分析

| 模块 | 当前依赖 | 问题 |
|------|---------|------|
| nexus-admin-app | core + api | api 已传递依赖 core，直接依赖 core 冗余 |
| nexus-admin-api | core | 符合单向依赖 |
| nexus-admin-core | 无 | 根模块 |

app 模块中直接使用了 core 的类（如 `DefaultPluginManager`、`DefaultExtensionRegistry` 等），共 17 个 Bean 在 `BootstrapConfig` 中装配。

### 目标

app 仅依赖 api，通过 api 的传递依赖获得 core 的能力，形成 `core ← api ← app` 的严格单向链。

### 实施方案

**POM 调整**：

app 移除对 `nexus-admin-core` 的直接依赖声明，通过 api 的传递依赖获得 core。

**代码调整**：

app 中对 core 类的直接引用需要评估：

| 引用场景 | 处理方式 | 目标模块 |
|---------|---------|---------|
| 通用组件装配（ExtensionRegistry、EventBus 等） | 下沉到 api 模块 `CoreAutoConfig` | api |
| 平台特定装配（PluginManager、ConfigManager 等） | 保留在 app 模块，通过传递依赖仍可访问 core 类 | app |
| 配置类（BootstrapConfig）中引用 Default* 实现 | 保留，传递依赖不影响运行时类访问 | app |

**注意**：这里的"单向依赖"指的是 POM 声明层面的依赖关系简化，app 通过传递依赖仍能使用 core 类。

### 插件依赖策略

| 插件类型 | 依赖方式 | 说明 |
|---------|---------|------|
| 基础插件（如 demo-plugin） | 仅依赖 core（scope: provided） | 最小依赖，纯扩展点实现 |
| 业务插件（如 system-user-plugin） | 依赖 api（scope: compile）+ core（scope: provided） | 需要 api 门面和控制器支持 |

此策略保持不变。

------

## 议题六：装配职责重新划分

### 核心思想

在 api 模块中对 core 的核心组件进行基本自动装配（确保系统开箱即用），app 模块负责平台特定的装配和覆盖。

### 划分原则

| 配置类 | 归属模块 | 职责 | 装配内容 |
|--------|---------|------|---------|
| `CoreAutoConfig`（新增） | api | 装配 core 默认实现，确保"毛坯房"基本可用 | ExtensionRegistry (DefaultExtensionRegistry)、EventBus (SyncEventBus)、PluginRegistry (DefaultPluginRegistry)、VersionManager (DefaultVersionManager)、DependenceManager (DefaultDependenceManager)、PluginLoader (DefaultPluginLoader) |
| `AuthExtensionConfig`（从 AdminAutoConfig 拆分） | api | 认证扩展点注册与装配 | BootstrapAuthProvider → 注册到 ExtensionRegistry、CompositeAuthProvider → 使用 ExtensionConsumer、AuthChallengeHandler → BootstrapAuthChallengeHandler 注册到 ExtensionRegistry、AuthFilter |
| `AdminAutoConfig`（精简后） | api | 组件扫描 | @ComponentScan |
| `BootstrapConfig`（精简后） | app | 平台启动装配 | PlatformProperties 绑定、RuntimeMode、PluginDescriptorFinder / PluginDescriptorParser / PluginSource、PluginManager（DefaultPluginManager，聚合所有组件）、ConfigManager（从 PluginManager 获取）、BootstrapRunner / StartupNotifier |
| `WebEndpointAutoConfig` | app | Web 端点相关 | 保持不变 |

所有 api 中的 `@Bean` 使用 `@ConditionalOnMissingBean`，确保可被上层覆盖。

### 覆盖机制

```
优先级：插件注册（ExtensionRegistry）> app @Bean > api @ConditionalOnMissingBean
```

- api 中所有 `@Bean` 使用 `@ConditionalOnMissingBean`
- app 或插件可通过声明同类型 Bean 覆盖 api 的默认装配

### 装配顺序保证

使用 `@AutoConfiguration` + `@AutoConfigureBefore` / `@AutoConfigureAfter` 控制，或使用 `@Order` + `@DependsOn` 组合确保依赖顺序。

### 代码示例

```java
// api 模块新增 CoreAutoConfig
@Configuration
public class CoreAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public ExtensionRegistry extensionRegistry() {
        return new DefaultExtensionRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventBus eventBus() {
        return new SyncEventBus();
    }

    // ... 其他 core 默认实现
}
```

------

## 议题七：API 层门面规范化

### 核心思想

api 层对 core 核心实现进行门面包装，业务层应尽量与门面交互而非直接与 core 交互。

### 现状评价

| 维度 | 评价 | 合规度 |
|------|------|--------|
| Service 层架构 | 设计良好 | 100% |
| 控制器层访问方式 | 完全通过门面访问 core | 95%+ |
| View 类（Record VO） | 设计规范，防御性拷贝到位 | 100% |
| ExtensionRegistry / EventBus 等核心组件 | 缺乏门面包装，直接暴露 | 不符合 |

### Service 层架构方案

Controller 通过 Service 层调用 core 门面，保持标准三层架构（Controller → Service → Core Facade）：

| 维度 | 处理方式 |
|------|---------|
| 管理操作 | 走 Service 层（PluginService、ConfigService、SystemStatusService 等） |
| 运行时能力（扩展点消费） | 通过 ExtensionConsumer 直接使用，不需要额外 Service 包装 |

**明确边界**：管理面板通过 Service 层暴露操作，Service 负责 View 转换与异常翻译，直接调用 core 门面（ConfigFacade、PluginFacade 等）。

### Service 层规范

| 规范项 | 要求 |
|--------|------|
| 包位置 | api 模块 `service/` 包下 |
| 参数/返回值 | 使用 domain 中的 View 记录，不暴露 core 类型 |
| 异常处理 | Service 负责异常转换（core 异常 → api 层 PluginOperationException） |
| 内部交互 | 通过 core 门面接口（而非 Default* 实现类）交互 |

### 未来演进方向

| 需求场景 | 方案 |
|---------|------|
| 暴露扩展点管理能力（查询已注册扩展、动态启用/禁用） | 新增 `ExtensionService` |
| 事件管理能力（查询监听器、发布事件审计） | 新增 `EventService` |
| 保持 Controller → Service → Core Facade 三层架构 | 所有管理操作统一走 Service 层 |

------

## 实施计划

### P0 - 紧急（核心链路改造）

| 阶段 | 任务 | 涉及模块 | 风险 |
|------|------|---------|------|
| 1 | CompositeAuthProvider 改造为动态解析模式 | api | 中（认证链路） |
| 2 | BootstrapAuthProvider 注册到 ExtensionRegistry | api | 低 |
| 3 | AuthChallengeHandler 继承 ExtensionPoint 并注册 | api | 低 |
| 4 | 拆分 AdminAutoConfig → AuthExtensionConfig | api | 低 |
| 5 | app POM 依赖清理（移除对 core 的直接依赖） | app | 低 |

### P1 - 重要（配置治理与装配下沉）

| 阶段 | 任务 | 涉及模块 | 风险 |
|------|------|---------|------|
| 6 | 扩展 platform.yml Schema（新增平台信息、插件策略、引导认证键） | core | 低 |
| 7 | BootstrapAuthProvider 集成 ConfigManager（支持凭据热更新） | api | 中（安全敏感） |
| 8 | PlatformInfoView 从 ConfigManager 读取平台信息 | api | 低 |
| 9 | 迁移 platform.plugin.auto-start 到配置中心 | api/app | 低 |
| 10 | api 模块新增 4 个 AutoConfig（EventBus/Extension/Config/Plugin） | api | 低 |
| 11 | AdminAutoConfig 拆分为 ApiAutoConfig + ApiInfraAutoConfig + AuthExtensionConfig | api | 低 |
| 12 | BootstrapConfig 精简（通用组件下沉到 api） | app | 低 |

### P2 - 增强（事件通知、缓存与门面规范化）

| 阶段 | 任务 | 涉及模块 | 风险 |
|------|------|---------|------|
| 13 | ExtensionRegistry 发布变更事件（注册/注销时） | core | 中（核心组件） |
| 14 | 实现 ExtensionConsumer 缓存策略（替代 ExtensionPointProxy） | core | 低 |
| 15 | CompositeAuthProvider 启用 ExtensionConsumer | api | 低 |
| 16 | Service 层架构审查（三层架构规范化评估） | api | 低 |

### P3 - 通用化（其他扩展点）

| 阶段 | 任务 | 涉及模块 | 风险 |
|------|------|---------|------|
| 17 | StorageProvider / CacheProvider 注册到 ExtensionRegistry | api | 低 |
| 18 | Log 系列扩展点注册到 ExtensionRegistry | api | 低 |
| 19 | AI 系列扩展点注册到 ExtensionRegistry | api | 低 |
| 20 | 清理 BootstrapAuthProperties（过渡期后移除） | api | 低 |

------

## 影响范围

### 模块影响

| 模块 | 影响说明 |
|------|---------|
| nexus-admin-core | ExtensionRegistry 需增加变更事件发布能力；platform.yml Schema 扩展；新增 ExtensionConsumer |
| nexus-admin-api | CompositeAuthProvider、BootstrapAuthProvider、AuthChallengeHandler 改造；新增 AuthExtensionConfig、4 个 AutoConfig；AdminAutoConfig 精简；Service 层架构审查 |
| nexus-admin-app | application.yml 配置项调整；POM 移除对 core 的直接依赖；BootstrapConfig 精简 |

### 文件变更清单

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `core/extension/ExtensionRegistry.java` | 修改 | 增加变更事件发布接口 |
| `core/extension/DefaultExtensionRegistry.java` | 修改 | 实现变更事件发布 |
| `core/extension/ExtensionConsumer.java` | 新增 | 扩展点通用消费者模板 |
| `core/resources/META-INF/schema/platform.yml` | 修改 | Schema 扩展 |
| `api/auth/CompositeAuthProvider.java` | 修改 | 改为动态解析模式 |
| `api/auth/BootstrapAuthProvider.java` | 修改 | 从 ConfigManager 读取凭据 |
| `api/auth/AuthChallengeHandler.java` | 修改 | 继承 ExtensionPoint |
| `api/auth/BootstrapAuthChallengeHandler.java` | 无修改 | 仅注册方式变化 |
| `api/auth/DefaultAuthChallengeHandler.java` | 无修改 | 仅注册方式变化 |
| `api/auth/AuthFilter.java` | 修改 | 改为从 ExtensionRegistry 动态解析 |
| `api/config/AdminAutoConfig.java` | 修改 | 精简，移除认证组件装配 |
| `api/config/AuthExtensionConfig.java` | 新增 | 认证扩展点配置类 |
| `api/config/CoreAutoConfig.java` | 新增 | core 默认实现自动装配 |
| `app/pom.xml` | 修改 | 移除对 core 的直接依赖 |
| `app/config/BootstrapConfig.java` | 修改 | 精简，通用组件下沉到 api |
| `app/resources/application.yml` | 修改 | 调整配置项归属 |

------

## 风险与缓解

| 风险 | 影响 | 可能性 | 缓解措施 |
|------|------|--------|---------|
| 认证链路改造导致登录功能中断 | 高 | 低 | 逐步改造，每步充分测试；P0 阶段编写集成测试覆盖全场景 |
| ExtensionRegistry 变更事件引入性能开销 | 中 | 低 | 事件异步发布；CopyOnWriteArrayList 写时复制，读无锁 |
| 引导认证凭据热更新存在安全风险 | 高 | 中 | 敏感配置变更需二次认证；配置变更审计日志；password 标记 sensitive |
| 插件卸载时AuthProvider注销导致在线用户Session失效 | 中 | 中 | 卸载前检查活跃Session；提供优雅降级提示；引导认证自动恢复 |
| 列表缓存 + 事件失效存在短暂不一致窗口 | 低 | 高 | 最终一致可接受；认证失败后立即刷新缓存重试 |
| 配置中心与YML配置源优先级冲突 | 中 | 中 | 明确优先级：环境变量(10) > 文件配置(20) > 默认配置(30)；过渡期YML值作为初始默认值 |
| AuthFilter 从 ExtensionRegistry 解析增加每次请求开销 | 中 | 低 | 使用 ExtensionConsumer 缓存最高优先级实现；读操作无锁 |
