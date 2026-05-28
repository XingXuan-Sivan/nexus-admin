# ADR-009: MCP 客户端能力设计

---

## 背景与问题

Nexus Admin 平台已具备 **MCP 服务端**能力（`McpController` 将平台 `AiTool` 通过 JSON-RPC 2.0 暴露为 MCP Tool，可被 Claude Desktop、Cline 等外部 AI 工具调用），形成了"平台能力 → MCP 工具"的对外供给通道。

但当前存在以下能力缺口：

### 问题一：平台无法调用外部 MCP 服务

平台只能作为 MCP 服务端被外部调用，无法作为 MCP 客户端主动调用外部 MCP 服务。在以下场景中，这一能力缺失成为瓶颈：

- **工具聚合**：管理面板需要集成外部团队提供的 MCP 工具（如代码审查工具、数据查询服务），统一纳管并供平台 AI 调用
- **跨系统联动**：企业内部多个系统均通过 MCP 暴露能力，平台需要作为编排中枢统一调度
- **AI 代理网关**：平台内建 AI 需要能调用外部 MCP 工具来扩展自身能力边界

### 问题二：缺乏 MCP 连接的管理面板界面

即使未来实现了 MCP 客户端代码，前端管理面板也缺少配置和管理 MCP 客户端连接的界面能力，包括：

- 无 CRUD API 管理连接配置（添加/编辑/删除外部 MCP 服务地址）
- 无连接测试与健康检查能力
- 无远程工具浏览与手动调用测试能力

### 问题三：AI 方面能力不够模块化，难以赋能旧系统

当前 AI 相关功能（`AiTool`、`AiToolRegistry`、`McpController` 等）集中在 `nexus-admin-api` 模块中，但 MCP 客户端能力的缺失导致平台的 AI 能力体系不完整。引入 MCP 客户端后，应当可与现有 AI 能力一起整体打包为一个完整的能力单元，使得任何引入 `nexus-admin-api` 依赖的项目都能同时获得：

- MCP 服务端能力（对外暴露工具）
- MCP 客户端能力（调用外部工具）
- AI 对话与工具调用能力

---

## 决策目标

从 AI 体系完整性视角，设计平台的 MCP 客户端能力，使其：

1. **与 MCP 服务端对称**：形成 MCP 双向通信能力（Server + Client），补全 AI 体系功能闭环
2. **融入管理面板**：提供标准 CRUD 管理 API，与现有 Plugin/Config/User 等管理功能风格一致
3. **遵循扩展点体系**：MCP 客户端连接抽象为 `ExtensionPoint`，支持不同传输协议由插件提供
4. **可复用即开即用**：能力放置在 `nexus-admin-api` 模块，引入依赖即可自动装配
5. **预留桥接能力**：远程 MCP 工具可桥接为本地 `AiTool`，使平台 AI 能直接调用外部工具

---

## 决策方案

### 议题一：MCP Client 模块归属

#### 可选方案对比

| 维度 | A. 置于 nexus-admin-api（推荐） | B. 新建独立模块 nexus-admin-mcp | C. 置于 nexus-admin-app |
|------|-------------------------------|-------------------------------|------------------------|
| 与现有 AI 体系统一 | ✅ 与 AiProvider/AiTool/McpController 同层 | ⚠️ 新增模块增加依赖复杂度 | ❌ app 模块不可被外部依赖 |
| 引入即用 | ✅ 引入 api 依赖即获得全部 AI 能力 | ⚠️ 需额外引入 mcp 模块 | ❌ app 为启动层不可外引 |
| 架构简洁性 | ✅ 零新增模块，包级隔离 | ⚠️ 模块拆分过度 | ❌ 违反模块依赖方向 |
| 独立演进 | ⚠️ MCP 与 AI 耦合在同一模块 | ✅ MCP 可独立版本演进 | ❌ 与启动逻辑耦合 |
| 赋能旧系统 | ✅ 旧系统引入 api 即获完整 AI+MCP 能力 | ⚠️ 需同时引入两个依赖 | ❌ 旧系统无法引入 app |

#### 决策结论

**MCP Client 能力置于 `nexus-admin-api` 模块内**，在 `extension/ai/` 包下扩展。

**决策依据：**

- 现有 AI 体系（`AiProvider`、`AiTool`、`AiToolRegistry`、`McpController`、`AiAutoConfig`）全部在 `api` 模块，MCP Client 作为 AI 体系的对称补充应同层放置
- `api` 模块已是插件开发的"唯一依赖模块"，引入即获得平台全部能力接口
- 项目当前 AI 相关接口数量有限，包级隔离即可提供足够的模块边界，过早拆分为独立模块属于过度设计
- 遵循项目 ADR-008 确立的模块依赖方向：`core ← api ← app`

**注：** 若未来 MCP 能力体量显著增长（如支持 5+ 种传输协议、MCP 服务市场等），可将 `extension/ai/mcp/` 包独立为 `nexus-admin-mcp` 模块，当前设计已为此留有演进空间——所有 MCP Client 相关接口通过 `ExtensionPoint` 抽象，与 AI 体系的具体实现解耦。

---

### 议题二：MCP Client 连接抽象层次

#### 可选方案对比

| 维度 | A. McpClientConnection 作为 ExtensionPoint（推荐） | B. 内部 Service 直接实现 HTTP 调用 | C. 基于 LangChain4j McpClient |
|------|--------------------------------------------------|-----------------------------------|------------------------------|
| 传输协议可替换 | ✅ 插件可提供 stdio/SSE 等传输实现 | ❌ 仅支持 HTTP | ⚠️ 受限于 LangChain4j 支持 |
| 架构一致性 | ✅ 与 AiProvider/WebEndpointExtension 模式一致 | ❌ 不符合扩展点体系 | ⚠️ 引入外部框架耦合 |
| 零框架依赖 | ✅ 接口定义不依赖具体框架 | ✅ 不依赖框架 | ❌ 依赖 LangChain4j 版本 |
| 实现成本 | 中（需定义扩展点 + 默认 HTTP 实现） | 低（直接实现） | 低（复用已有实现） |
| 演进灵活性 | ✅ 可热替换传输实现 | ❌ 仅 HTTP | ⚠️ 跟随 LangChain4j 演进 |

#### 决策结论

**`McpClientConnection` 作为 `ExtensionPoint` 子类型**，默认提供基于 HTTP JSON-RPC 的实现。

```
┌─────────────────────────────────────────────────────────┐
│              MCP Client 体系三层设计                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │   第一层：McpClientConnection（连接抽象）            │  │
│  │   • ExtensionPoint 扩展点                         │  │
│  │   • 封装与远程 MCP 服务端的通信                     │  │
│  │   • 支持 HTTP JSON-RPC / stdio / SSE 等传输协议    │  │
│  └───────────────────────────────────────────────────┘  │
│                         │                                │
│                         ▼                                │
│  ┌───────────────────────────────────────────────────┐  │
│  │   第二层：McpClientRegistry（客户端注册表）          │  │
│  │   • 管理多个 MCP 客户端连接生命周期                 │  │
│  │   • 聚合所有远程工具列表                            │  │
│  │   • 支持连接健康检查                                │  │
│  └───────────────────────────────────────────────────┘  │
│                         │                                │
│                         ▼                                │
│  ┌───────────────────────────────────────────────────┐  │
│  │   第三层：McpClientController（管理面板 API）        │  │
│  │   • CRUD 管理 MCP 客户端连接配置                    │  │
│  │   • 浏览远程工具 / 手动调用测试                     │  │
│  │   • 连接测试 / 状态查询                             │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ═══════════════ 可选桥接层 ═══════════════              │
│  ┌───────────────────────────────────────────────────┐  │
│  │   McpRemoteToolAdapter implements AiTool           │  │
│  │   • 将远程 MCP Tool 包装为平台 AiTool               │  │
│  │   • 使平台 AI 能直接调用外部 MCP 工具               │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**与现有 AI 体系三层架构的对比呼应：**

| 层次 | AI 体系（已有） | MCP Client 体系（新增） |
|------|---------------|----------------------|
| 第一层（能力提供） | `AiProvider extends ExtensionPoint` | `McpClientConnection extends ExtensionPoint` |
| 第二层（注册表） | `AiToolRegistry` | `McpClientRegistry` |
| 第三层（对外端点） | `McpController`（MCP Server 端点） | `McpClientController`（管理面板 API） |
| 自动装配 | `AiAutoConfig` | `McpAutoConfig` |

---

### 议题三：连接配置持久化方式

#### 可选方案对比

| 维度 | A. 配置中心存储（推荐） | B. 独立文件存储 | C. 仅内存存储 |
|------|----------------------|---------------|-------------|
| 持久化 | ✅ 利用现有 ConfigFacade 持久化 | ✅ 文件持久化 | ❌ 重启丢失 |
| 复用基础设施 | ✅ 复用配置中心读写能力 | ⚠️ 需自行管理文件 | ❌ 无复用 |
| 管理面板集成 | ✅ 复用 ConfigController 管理 | ⚠️ 需独立管理 API | ⚠️ 需独立管理 |
| 与现有体系一致 | ✅ 与插件禁用配置存储模式一致 | ⚠️ 引入额外存储机制 | ❌ 不可接受 |

#### 决策结论

**MCP 客户端连接配置通过现有配置中心存储**，使用专用作用域 `mcp-clients`。

**存储模型：**

```yaml
# 配置中心 mcp-clients scope 中的连接配置
mcp-clients:
  connections:
    - id: "external-tool-service"
      name: "外部工具服务"
      url: "http://localhost:9090/mcp"
      protocol: "http"
      authToken: ""
      enabled: true
```

**决策依据：**

- 平台已有成熟的配置中心体系（多作用域 Schema 加载 + JSON Schema 验证），直接复用无需重复建设
- 配置中心的 `ConfigFacade` 已有完善的文件持久化能力
- `McpClientRegistry` 作为无状态组件，启动时从配置中心加载连接列表，运行时动态注册/注销
- 与插件禁用状态存储（`plugins-data/config/disabled.yml`）模式一致：文件为权威源，运行时状态在内存

---

### 议题四：远程工具桥接体系

#### 可选方案对比

| 维度 | A. 调度器桥接（推荐） | B. 直接注册 | C. 完全隔离 |
|------|---------------------|------------|-----------|
| 生命周期管理 | ✅ 自动绑定连接生命周期 | ❌ 手动管理注册/注销 | ❌ 无注册 |
| 桥接可控性 | ✅ 连接级开关 + 工具过滤器 | ⚠️ 全量或无 | ❌ 不可控 |
| 与本地工具等价性 | ✅ 统一在 AiToolRegistry 中 | ✅ 同在 Registry | ❌ 分离 |
| 故障隔离 | ✅ 单连接失败不影响其他 | ⚠️ 需自行处理 | ✅ 天然隔离 |
| 可观察性 | ✅ 可查询每个连接的桥接清单 | ⚠️ 需遍历 Registry | ❌ 不可见 |

#### 决策结论

**远程工具通过 `McpRemoteToolBridge` 调度器统一管理桥接生命周期**，桥接后的工具与本地 Spring Bean `AiTool` 在 `AiToolRegistry` 中完全等价——上层调用者无需区分来源。

**四层架构中的桥接层定位：**

```
┌─────────────────────────────────────────────────────────┐
│              MCP Client 体系四层 + 桥接层                  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │   第一层：McpClientConnection（连接扩展点）         │  │
│  │   • ExtensionPoint，支持 HTTP / stdio / SSE       │  │
│  └───────────────────────────────────────────────────┘  │
│                         │                                │
│                         ▼                                │
│  ┌───────────────────────────────────────────────────┐  │
│  │   第二层：McpClientRegistry（客户端注册表）         │  │
│  │   • 连接生命周期管理，ConfigFacade 持久化          │  │
│  │   • 连接变更时通知桥接调度器                        │  │
│  └───────────────────────────────────────────────────┘  │
│                         │                                │
│                         ▼                                │
│  ┌───────────────────────────────────────────────────┐  │
│  │   第三层：McpRemoteToolBridge（桥接调度器）★新增   │  │
│  │   • 监听注册表变更，自动桥接/解桥                   │  │
│  │   • 维护桥接清单 → 支撑 MCP Server 分类暴露        │  │
│  │   • 每连接失败隔离，部分桥接降级                    │  │
│  └───────────────┬───────────────────────────────────┘  │
│                  │                                       │
│         ┌────────▼────────┐                              │
│         │ McpRemoteTool   │                              │
│         │ Adapter         │  implements AiTool           │
│         │ • 适配器骨架    │                              │
│         │ • 命名: mcp.{server}.{tool}                    │
│         └────────┬────────┘                              │
│                  │                                       │
│                  ▼                                       │
│  ┌───────────────────────────────────────────────────┐  │
│  │         AiToolRegistry（工具注册表）               │  │
│  │  本地工具 + 桥接工具 → 完全等价                    │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │   第四层：McpClientController（管理面板 API）       │  │
│  │   • CRUD 连接 / 远程工具浏览 / 桥接刷新            │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**桥接调度器接口（`McpRemoteToolBridge`）：**

```java
package com.nexusadmin.api.extension.ai;

/**
 * MCP 远程工具桥接调度器。
 *
 * <p>监听 McpClientRegistry 的连接变更，将启用连接的远程工具
 * 通过 McpRemoteToolAdapter 注册到 AiToolRegistry，使平台 AI 透明调用。</p>
 *
 * <p>桥接行为由连接级配置控制——McpConnectionInfo.bridgeEnabled
 * 决定某连接的工具是否桥接为本地 AiTool。</p>
 */
public interface McpRemoteToolBridge {

    /** 将指定连接的远程工具桥接到 AiToolRegistry，返回成功数量 */
    int bridge(McpClientConnection connection, McpConnectionInfo info);

    /** 从 AiToolRegistry 移除指定连接的所有桥接工具，返回移除数量 */
    int unbridge(String connectionId);

    /** 刷新桥接（断开旧桥接，重新拉取并注册），返回新工具数量 */
    int refresh(McpClientConnection connection, McpConnectionInfo info);

    /** 获取指定连接已桥接的工具名称列表 */
    List<String> getBridgedTools(String connectionId);

    /** 获取所有已桥接的工具名称集合（用于 MCP Server 分类暴露） */
    Set<String> getAllBridgedToolNames();

    /** 判断指定连接是否可以桥接 */
    boolean canBridge(McpConnectionInfo info);
}
```

**桥接生命周期规则：**

| 事件 | 桥接行为 | 说明 |
|------|---------|------|
| 连接注册（已启用 + bridgeEnabled=true） | `bridge(conn, info)` | 自动拉取远程工具并注册 |
| 连接注册（已启用 + bridgeEnabled=false） | 不桥接 | 连接可用但不注入 AiToolRegistry |
| 连接注册失败（网络不通等） | 不桥接 | 连接状态 ERROR，不阻塞启动 |
| 连接更新（bridgeEnabled 变为 true） | `bridge(conn, info)` | 恢复桥接 |
| 连接更新（bridgeEnabled 变为 false） | `unbridge(connId)` | 移除桥接 |
| 连接注销 | `unbridge(connId)` | 自动清理 |
| tools/list 返回异常 | 跳过该连接，记录日志 | 部分桥接降级，不影响其他连接 |
| 单个适配器注册失败 | 跳过该工具，继续下一个 | 部分桥接降级 |

**桥接执行条件（全部满足才执行）：**
1. 连接处于 `CONNECTED` 状态
2. 连接配置 `enabled = true`
3. 连接配置 `bridgeEnabled = true`
4. 远程工具列表非空

**命名空间隔离机制：**

桥接工具统一使用 `mcp.{serverName}.{toolName}` 命名格式，其中 `serverName` 取自 `McpClientConnection.getServerInfo().name()`，`toolName` 取自 `McpRemoteTool.name()`。该前缀机制确保：
- 桥接工具绝不会与本地 Spring Bean AiTool 产生名称冲突
- 通过名称前缀即可识别工具来源（以 `mcp.` 开头为桥接工具）
- MCP Server 分类暴露时，通过名称前缀过滤即可区分本地/桥接

**McpConnectionInfo 新增桥接控制字段：**

```java
/** 是否将远程工具桥接为平台 AiTool（默认 true） */
private boolean bridgeEnabled = true;

/** 桥接模式：ALL（全部工具）/ SELECTED（仅选中）/ NONE（不桥接） */
private String bridgeMode = "ALL";

/** 当 bridgeMode=SELECTED 时，允许桥接的工具名称列表 */
private List<String> bridgeToolFilter;
```

**决策依据：**

- 调度器模式将桥接逻辑从 Registry 中解耦，两者各自独立演进
- `getAllBridgedToolNames()` 为 MCP Server 分类暴露提供权威数据源
- 连接级开关 + 工具级过滤器提供细粒度控制，满足"部分连接仅管理、不注入 AI"的场景
- 命名空间隔离使本地和桥接工具天然可分，支撑 MCP Server `tools/list` 按模式过滤
- 失败降级保证系统整体可用性——单个外部 MCP 服务故障不会影响平台正常运行

---

### 议题五：MCP Server 工具分类暴露

#### 背景

项目在作为 MCP Server 对外暴露工具时，对应三种典型使用场景：

- **全量模式**：外部 AI 客户端需要调用所有可用工具（本地 + 桥接），形成统一的工具视图
- **本地模式**：外部 AI 客户端仅需平台原生能力，不关心桥接的外部服务
- **桥接模式**：外部 AI 客户端仅需通过平台访问已桥接的外部 MCP 服务，平台作为聚合网关

当前 `McpController.handleToolsList()` 无条件返回 `toolRegistry.listAll()`，无法区分来源。

#### 可选方案对比

| 维度 | A. mode 参数过滤（推荐） | B. 分离端点 | C. 工具名称标注 |
|------|------------------------|------------|---------------|
| MCP 协议兼容 | ✅ 标准 tools/list + 扩展参数 | ⚠️ 非标准自定义方法 | ✅ 标准 tools/list |
| 客户端适配 | ✅ 传参即可，无需额外端点 | ❌ 需要多端点配置 | ✅ 无参即可 |
| 过滤精度 | ✅ 精确分类 | ✅ 精确分类 | ❌ 依赖客户端解析 |
| 向后兼容 | ✅ 不传 mode 默认 all | ⚠️ 需同时保持旧端点 | ✅ 完全兼容 |
| 实现复杂度 | 低 | 中 | 低 |

#### 决策结论

**`tools/list` 支持可选的 `mode` 参数进行工具分类过滤**，不传参数时默认返回全部工具（保持向后兼容）。

**JSON-RPC 请求格式：**

```json
// 获取全部工具（默认，向后兼容）
{"jsonrpc": "2.0", "method": "tools/list", "id": 1}

// 仅获取平台自带工具
{"jsonrpc": "2.0", "method": "tools/list", "params": {"mode": "local"}, "id": 2}

// 仅获取桥接远程工具
{"jsonrpc": "2.0", "method": "tools/list", "params": {"mode": "bridged"}, "id": 3}
```

**实现策略：**

| mode 值 | 数据来源 | 说明 |
|---------|---------|------|
| 不传 / `"all"` | `toolRegistry.listAll()` | 全部工具（本地 + 桥接），保持不变 |
| `"local"` | `toolRegistry.listAll()` − 桥接工具名集合 | 仅平台自带工具（Spring Bean + 非桥接运行时注册） |
| `"bridged"` | `bridge.getAllBridgedToolNames()` → `toolRegistry.get(name)` | 仅桥接的远程工具 |

`McpController` 注入 `McpRemoteToolBridge`，通过 `bridge.getAllBridgedToolNames()` 获取桥接工具名称集合，以此为过滤依据。桥接工具名以 `mcp.` 为前缀天然可区分，不需修改 `AiTool` 接口。

**决策依据：**

- 桥接工具的 `mcp.` 命名前缀提供天然的过滤依据，无需修改 `AiTool` 接口增加来源标记
- `mode` 参数为 JSON-RPC params 的合法扩展字段，不破坏 MCP 协议兼容性
- 默认保持 `listAll()` 行为，现有 MCP 客户端无需任何修改
- 平台的 MCP 代理网关定位由此得到清晰体现：对外既是工具提供者，也是工具聚合者

---

### 议题六：LangChain4j 工具无缝集成

#### 背景

项目已通过 `AiAutoConfig` 装配了 `ChatLanguageModel` 桥接 Bean（委托 `AiProvider`），可与 LangChain4j `AiServices` 结合使用。但当前开发者需要手动将 `AiTool` 逐一映射为 `ToolSpecification`，且需自行实现工具执行回调，操作繁琐且重复。

理想状态：开发者注入 `ChatLanguageModel` 和平台提供的 `ToolProvider` 后，`AiToolRegistry` 中所有工具（本地 + 桥接）自动对 LangChain4j 可见，无需手动映射。

#### 可选方案对比

| 维度 | A. 平台 ToolProvider（推荐） | B. @Tool 注解扫描 | C. 保持手动映射 |
|------|----------------------------|------------------|---------------|
| 与 AiToolRegistry 统一 | ✅ 以 Registry 为唯一权威来源 | ⚠️ 双套体系并行 | ✅ 以 Registry 为准 |
| 桥接工具可见性 | ✅ 自动可见 | ❌ 桥接工具无注解不可见 | ✅ 需手动 |
| 开发者体验 | ✅ 注入即用 | ✅ 注解即用 | ❌ 手动映射 |
| 与 ExtensionPoint 一致 | ✅ Registry 统一管理 | ❌ 绕过扩展点体系 | ✅ 但不统一 |
| 对现有体系侵入 | 低（新增 Bean） | 高（需修改 AiTool 接口） | 无 |

#### 决策结论

**提供 `AIToolProvider` 作为 AiToolRegistry 与 LangChain4j 之间的标准桥接**，使开发者可直接通过 LangChain4j `AiServices` 使用所有平台工具，无需手动映射。

**LangChain4j 集成架构：**

```
开发者代码:
  @Autowired ChatLanguageModel model;
  @Autowired AIToolProvider toolProvider;

  Assistant ai = AiServices.builder(Assistant.class)
      .chatLanguageModel(model)
      .toolProvider(toolProvider)         // ← 一行注入
      .build();

  String result = ai.chat("审查 Service.java");
  // AI 自动发现 mcp.enterprise.code.review 并调用

内部流程:
  AIToolProvider
      │
      │ 实现 LangChain4j ToolProvider 接口
      │ provideTools(ToolProviderRequest) → 构建 ToolSpecification ↔ ToolExecutor 映射
      │
      ├── 工具发现：toolRegistry.listAll() → 自动构建 ToolSpecification 列表
      │   • 本地工具：plugin.list、system.status、...
      │   • 桥接工具：mcp.enterprise.code.review、mcp.data.sql.query、...
      │   • 对 LangChain4j 无任何区别
      │
      └── 工具执行：ToolExecutor → toolRegistry.get(name)
          • 本地工具 → 直接 execute()
          • 桥接工具 → McpRemoteToolAdapter.execute() → HTTP → 远程 MCP 服务
          • 对 AIToolProvider 无任何区别
```

**`AIToolProvider` 接口定义：**

```java
package com.nexusadmin.api.extension.ai.langchain4j;

/**
 * 平台级 LangChain4j ToolProvider 桥接。
 *
 * <p>将 AiToolRegistry 中的所有工具（本地 + 桥接）无缝暴露给 LangChain4j 生态。
 * 开发者直接注入此 Bean，即可在 AiServices 中使用全部平台工具。</p>
 *
 * <p>工具发现以 AiToolRegistry 为唯一权威来源——
 * 任何注册到 Registry 的工具（无论来源）自动对 LangChain4j 可见。</p>
 */
public class AIToolProvider implements ToolProvider {

    private final AiToolRegistry toolRegistry;

    /**
     * 向 LangChain4j 提供所有可用工具的映射。
     * 每次调用时从 AiToolRegistry 获取最新工具列表，
     * 构建 ToolSpecification ↔ ToolExecutor 映射。
     */
    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
        for (AiTool tool : toolRegistry.listAll()) {
            ToolSpecification spec = ToolSpecification.builder()
                    .name(tool.getName())
                    .description(tool.getDescription())
                    .parameters(buildSchema(tool.getInputTypeSchema()))
                    .build();
            ToolExecutor executor = (toolRequest, memoryId) -> {
                Map<String, Object> args = JSON.parseObject(toolRequest.arguments(), Map.class);
                AiTool.ToolResult result = tool.execute(args, InvocationContext.builder()
                        .channelId("AI_ASSISTANT").build());
                return result.message();
            };
            tools.put(spec, executor);
        }
        return new ToolProviderResult(tools);
    }

    /** 获取所有工具规格（用于需要直接获取 ToolSpecification 列表的场景） */
    public List<ToolSpecification> getToolSpecifications() {
        return toolRegistry.listAll().stream()
                .map(tool -> ToolSpecification.builder()
                        .name(tool.getName())
                        .description(tool.getDescription())
                        .parameters(buildSchema(tool.getInputTypeSchema()))
                        .build())
                .toList();
    }
}
```

**装配策略：**

- `AIToolProvider` 由 `McpAutoConfig` 装配（因 MCP Client 桥接工具是其核心数据源之一），带 `@ConditionalOnClass("dev.langchain4j.service.AiServices")` 条件保护
- 若项目未引入 LangChain4j 依赖，此 Bean 自动跳过装配，不影响 MCP 基础能力
- 带 `@ConditionalOnMissingBean` 保护，插件可覆盖提供自定义 ToolProvider

**决策依据：**

- 以 `AiToolRegistry` 为唯一工具权威来源，桥接工具与本地工具对 LangChain4j 完全透明
- 开发者体验极简：注入 `ChatLanguageModel` + `AIToolProvider` 即可使用全部平台工具
- 不修改 `AiTool` 接口、不引入注解，保持扩展点体系的纯粹性
- LangChain4j 为可选依赖——不引入时平台 MCP 能力独立可用

---

### 二、核心接口定义概要

#### 2.1 McpClientConnection（第一层：连接抽象）

```java
package com.nexusadmin.api.extension.ai;

import com.nexusadmin.core.extension.ExtensionPoint;
import java.util.List;
import java.util.Map;

/**
 * MCP 客户端连接扩展点，封装与远程 MCP 服务端的通信。
 * 支持 HTTP JSON-RPC（默认）、stdio、SSE 等传输协议。
 * 插件可通过 @Extension 注解提供自定义传输实现。
 */
public interface McpClientConnection extends ExtensionPoint {

    /** 获取支持的传输协议名称，如 "http"、"stdio"、"sse" */
    String getProtocol();

    /** 建立连接，执行 MCP initialize 握手 */
    void connect(McpConnectionConfig config);

    /** 断开连接 */
    void disconnect();

    /** 是否已连接 */
    boolean isConnected();

    /** 列出远程服务器提供的所有工具（MCP tools/list） */
    List<McpRemoteTool> listTools();

    /** 调用远程工具（MCP tools/call） */
    McpToolResult callTool(String toolName, Map<String, Object> arguments);

    /** 获取远程服务器信息（来自 MCP initialize 响应） */
    McpServerInfo getServerInfo();

    /** 连接配置 */
    record McpConnectionConfig(String url, String authToken, Map<String, String> headers) {}

    /** 远程工具描述 */
    record McpRemoteTool(String name, String description, String inputSchema) {}

    /** 远程工具调用结果 */
    record McpToolResult(boolean success, String content, String error) {}

    /** 服务端信息 */
    record McpServerInfo(String name, String version) {}
}
```

#### 2.2 McpClientRegistry（第二层：注册表）

```java
package com.nexusadmin.api.extension.ai;

import com.nexusadmin.api.domain.ai.McpConnectionInfo;

import java.util.List;
import java.util.Optional;

/**
 * MCP 客户端注册表，管理多个 MCP 客户端连接的生命周期。
 * 聚合所有已注册连接，提供统一的外部工具调用入口。
 */
public interface McpClientRegistry {

    /** 注册并建立连接 */
    void register(McpConnectionInfo info);

    /** 注销并断开连接 */
    void unregister(String connectionId);

    /** 获取指定连接 */
    Optional<McpClientConnection> get(String connectionId);

    /** 列出所有已注册连接 */
    List<McpConnectionInfo> listAll();

    /** 获取所有远程工具（聚合所有连接） */
    List<McpClientConnection.McpRemoteTool> listAllRemoteTools();

    /** 调用远程工具（自动定位所属连接） */
    McpClientConnection.McpToolResult callRemoteTool(
            String toolName, Map<String, Object> arguments);

    /** 测试连接是否可用 */
    boolean testConnection(McpConnectionInfo info);
}
```

#### 2.3 McpConnectionInfo（连接配置 DTO）

```java
package com.nexusadmin.api.domain.ai;

/** MCP 客户端连接配置视图对象，用于管理面板 CRUD 和数据持久化。 */
public class McpConnectionInfo {
    private String id;          // 连接唯一标识
    private String name;        // 显示名称
    private String url;         // MCP 服务端地址
    private String protocol;    // 传输协议（默认 "http"）
    private String authToken;   // 认证令牌
    private boolean enabled;    // 是否启用
    private String status;      // 连接状态：CONNECTED / DISCONNECTED / ERROR
    private Long lastCheckTime; // 最后健康检查时间

    // getters / setters ...
}
```

#### 2.4 McpClientService（业务服务接口）

```java
package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.ai.McpConnectionInfo;
import com.nexusadmin.api.extension.ai.McpClientConnection;

import java.util.List;

/**
 * MCP 客户端业务服务，封装对注册表的管理操作。
 * 遵循平台 CRUD 方法命名统一规范。
 */
public interface McpClientService {

    /** 获取连接列表 */
    List<McpConnectionInfo> list();

    /** 获取连接详情 */
    McpConnectionInfo get(String id);

    /** 创建连接 */
    McpConnectionInfo create(McpConnectionInfo info);

    /** 更新连接 */
    McpConnectionInfo update(String id, McpConnectionInfo info);

    /** 删除连接 */
    void delete(String id);

    /** 测试连接是否可用 */
    boolean testConnection(McpConnectionInfo info);

    /** 列出指定连接的远程工具 */
    List<McpClientConnection.McpRemoteTool> listRemoteTools(String connectionId);

    /** 聚合所有连接的远程工具 */
    List<McpClientConnection.McpRemoteTool> listAllRemoteTools();

    /** 调用远程工具 */
    McpClientConnection.McpToolResult callRemoteTool(
            String toolName, Map<String, Object> arguments);

    /** 刷新指定连接的远程工具桥接 */
    int refreshBridge(McpClientConnection connection, McpConnectionInfo info);

    /** 获取指定连接已桥接的工具名称列表 */
    List<String> getBridgedTools(String connectionId);
}
```

#### 2.5 McpRemoteToolBridge（桥接调度器）

```java
package com.nexusadmin.api.extension.ai;

import com.nexusadmin.api.domain.ai.McpConnectionInfo;

import java.util.List;
import java.util.Set;

/**
 * MCP 远程工具桥接调度器。
 *
 * <p>监听 McpClientRegistry 的连接变更，将启用连接的远程工具
 * 通过 McpRemoteToolAdapter 注册到 AiToolRegistry。桥接后的远程工具
 * 与本地 Spring Bean AiTool 在 AiToolRegistry 中完全等价。</p>
 *
 * <p>同时维护桥接工具名称清单，为 MCP Server 分类暴露提供权威数据源。
 * McpController 通过 {@link #getAllBridgedToolNames()} 获取桥接工具集合，
 * 实现 tools/list 的 local/bridged/all 分类过滤。</p>
 */
public interface McpRemoteToolBridge {

    /** 将指定连接的远程工具桥接到 AiToolRegistry，返回成功数量 */
    int bridge(McpClientConnection connection, McpConnectionInfo info);

    /** 从 AiToolRegistry 移除指定连接的所有桥接工具，返回移除数量 */
    int unbridge(String connectionId);

    /** 刷新桥接（断开旧桥接，重新拉取并注册） */
    int refresh(McpClientConnection connection, McpConnectionInfo info);

    /** 获取指定连接已桥接的工具名称列表 */
    List<String> getBridgedTools(String connectionId);

    /** 获取所有已桥接的工具名称集合（供 MCP Server 分类暴露） */
    Set<String> getAllBridgedToolNames();

    /** 判断指定连接是否可以桥接 */
    boolean canBridge(McpConnectionInfo info);
}
```

**实现约束：**

- `bridge()`：先调用 `unbridge(connectionId)` 清理旧桥接（防止重复注册），再拉取远程工具列表，为每个工具创建 `McpRemoteToolAdapter` 并注册到 `AiToolRegistry`。单个工具失败不影响同连接其他工具。
- `getAllBridgedToolNames()`：返回所有连接已桥接工具的 `getName()` 名称集合，供 `McpController` 按模式过滤。
- 桥接条件检查：连接 `isConnected()` 为 true + `McpConnectionInfo.enabled` 为 true + `McpConnectionInfo.bridgeEnabled` 为 true + 远程工具列表非空。任一项不满足返回 0。
```

---

### 三、管理面板 API 设计

遵循 ADR-008 确立的 CRUD 命名规范，`McpClientController` 提供统一风格的 REST API：

**MCP 客户端管理端点：**

| 操作 | HTTP 方法 | URL 路径 | Controller 方法 | 权限 |
|------|----------|---------|----------------|------|
| 连接列表 | `GET` | `/admin/v1/mcp/clients` | `list()` | `mcp-clients.view` |
| 连接详情 | `GET` | `/admin/v1/mcp/clients/{id}` | `get(String id)` | `mcp-clients.view` |
| 创建连接 | `POST` | `/admin/v1/mcp/clients` | `create(McpConnectionInfo)` | `mcp-clients.manage` |
| 更新连接 | `PUT` | `/admin/v1/mcp/clients/{id}` | `update(String, McpConnectionInfo)` | `mcp-clients.manage` |
| 删除连接 | `DELETE` | `/admin/v1/mcp/clients/{id}` | `delete(String)` | `mcp-clients.manage` |
| 测试连接 | `POST` | `/admin/v1/mcp/clients/{id}/test` | `testConnection(String)` | `mcp-clients.view` |
| 远程工具列表 | `GET` | `/admin/v1/mcp/clients/{id}/tools` | `listRemoteTools(String)` | `mcp-clients.view` |
| 调用远程工具 | `POST` | `/admin/v1/mcp/clients/{id}/tools/call` | `callRemoteTool(String, Map)` | `mcp-clients.view` |
| 刷新桥接 | `POST` | `/admin/v1/mcp/clients/{id}/bridge/refresh` | `refreshBridge(String)` | `mcp-clients.manage` |
| 已桥接工具 | `GET` | `/admin/v1/mcp/clients/{id}/bridged-tools` | `getBridgedTools(String)` | `mcp-clients.view` |

**MCP Server 端点（增强后）：**

| 操作 | MCP Method | params | 说明 |
|------|-----------|--------|------|
| 全部工具列表 | `tools/list` | 无参 或 `{"mode":"all"}` | 返回平台全部 AiTool（本地 + 桥接），**向后兼容** |
| 本地工具列表 | `tools/list` | `{"mode":"local"}` | 仅返回平台自带工具（Spring Bean + 非桥接运行时注册） |
| 桥接工具列表 | `tools/list` | `{"mode":"bridged"}` | 仅返回已桥接的远程 MCP 工具 |
| 工具调用 | `tools/call` | `{"name":"...", "arguments":{...}}` | 不变，无论如何来源的工具统一通过 `AiToolRegistry.get()` 执行 |

`tools/call` 无需 `mode` 参数——工具发现和工具调用解耦，调用时通过 `name` 直接定位。

**MCP API 路径体系：**

```text
/admin/v1/mcp              → MCP Server 端点（JSON-RPC：tools/list + tools/call）
                               tools/list 支持 mode=all|local|bridged 分类过滤
/admin/v1/mcp/clients      → MCP Client 管理 API（CRUD + 测试 + 浏览 + 桥接管理）
```

整体 MCP 相关 API 统一收敛在 `/admin/v1/mcp` 前缀下，Server 与 Client 形成层次化的路径命名空间。

---

### 四、自动装配设计

#### 4.1 配置类职责重新划分

现有 `AiAutoConfig` 中包含部分 MCP 相关的隐式装配（`McpController` 依赖 `AiToolRegistry`），为保持职责分明，对配置类进行如下划分：

| 配置类 | 职责范围 | 装配内容 |
|--------|---------|---------|
| `AiAutoConfig` | **仅 AI 核心能力** | `AiToolRegistry`、`AiProvider` 消费者、`ChatLanguageModel` 桥接 |
| `McpAutoConfig`（新增） | **全部 MCP 相关装配** | MCP Server 端（`McpController` + 分类过滤）+ MCP Client 端（连接/注册表/桥接/服务）+ LangChain4j 集成（`AIToolProvider`） |

**设计理由：**

- `McpController` 虽然消费 `AiToolRegistry`，但其本质是 MCP 协议端点，属于 MCP 关注点而非 AI 关注点
- MCP Server 与 MCP Client 同属 MCP 协议范畴，应集中在同一配置类中统一管理
- `AIToolProvider` 虽然服务于 LangChain4j 生态，但其核心职责是将 MCP Client 桥接后的工具暴露给 LangChain4j，且依赖于 MCP 桥接能力，归属于 MCP 关注点
- `AiAutoConfig` 职责收缩为纯 AI 能力装配（模型 + 工具注册表），不再感知 MCP 协议和 LangChain4j 集成
- 未来若提取独立 `nexus-admin-mcp` 模块，仅需迁移 `McpAutoConfig` 及其关联类，`AiAutoConfig` 不受影响

#### 4.2 McpAutoConfig 装配清单

新增 `McpAutoConfig` 配置类，统一装配全部 MCP 相关 Bean，所有 Bean 带 `@ConditionalOnMissingBean` 保护：

```
McpAutoConfig
│
├── MCP Server 端
│   └── McpController（显式装配，注入 AiToolRegistry + McpRemoteToolBridge）
│         @ConditionalOnMissingBean(McpController.class)
│         tools/list 支持 mode=all|local|bridged 分类过滤
│
├── MCP Client 端
│   ├── McpClientConnection (default: HttpMcpClientConnection)
│   │     @ConditionalOnMissingBean(McpClientConnection.class)
│   ├── McpRemoteToolBridge (default: DefaultMcpRemoteToolBridge)
│   │     @ConditionalOnMissingBean(McpRemoteToolBridge.class)
│   │     注入 AiToolRegistry，维护桥接清单
│   ├── McpClientRegistry (default: DefaultMcpClientRegistry)
│   │     @ConditionalOnMissingBean(McpClientRegistry.class)
│   │     注入 McpRemoteToolBridge，注册/注销时自动桥接/解桥
│   └── McpClientService (default: DefaultMcpClientService)
│         @ConditionalOnMissingBean(McpClientService.class)
│
├── LangChain4j 集成（可选，条件装配）
│   └── AIToolProvider
│         @ConditionalOnClass("dev.langchain4j.service.AiServices")
│         @ConditionalOnMissingBean(AIToolProvider.class)
│         注入 AiToolRegistry，桥接全部工具到 LangChain4j 生态
│
└── 配置属性
    └── @ConfigurationProperties(prefix = "panel.mcp")
```

**设计要点：**

- `McpController` 同时注入 `AiToolRegistry` 和 `McpRemoteToolBridge`，实现 `tools/list` 的 mode 过滤
- `DefaultMcpClientRegistry` 注入 `McpRemoteToolBridge`，在 `register()` 成功后自动调用 `bridge.bridge()`，在 `unregister()` 前自动调用 `bridge.unbridge()`
- `AIToolProvider` 包路径为 `extension/ai/langchain4j/`，在包级隔离 LangChain4j 依赖，同时保持在 AI 扩展包体系内
- `@ConditionalOnClass` 保证不引入 LangChain4j 的项目零影响——MCP Server + Client 核心能力独立可用
- 所有 Bean 均带 `@ConditionalOnMissingBean`，插件可通过声明同类型 Bean 覆盖任意环节

---

### 五、目录结构规划

所有新增文件在 `nexus-admin-api` 模块内：

```
nexus-admin-api/src/main/java/com/nexusadmin/api/
├── extension/ai/
│   ├── McpClientConnection.java              ← 新增：连接扩展点接口
│   ├── McpClientRegistry.java                ← 新增：注册表接口
│   ├── McpRemoteToolBridge.java              ← 新增：桥接调度器接口
│   ├── langchain4j/
│   │   └── AIToolProvider.java         ← 新增：LangChain4j ToolProvider 桥接
│   └── impl/
│       ├── HttpMcpClientConnection.java       ← 新增：HTTP JSON-RPC 默认实现
│       ├── DefaultMcpClientRegistry.java      ← 新增：注册表默认实现（集成桥接）
│       ├── DefaultMcpRemoteToolBridge.java    ← 新增：桥接调度器默认实现
│       └── McpRemoteToolAdapter.java          ← 新增：远程工具→AiTool 桥接适配器
├── domain/mcp/
│   └── McpConnectionInfo.java                ← 新增：连接配置 DTO（含桥接控制字段）
├── service/
│   ├── McpClientService.java                 ← 新增：业务服务接口
│   └── impl/
│       └── DefaultMcpClientService.java      ← 新增：业务服务默认实现
├── controller/
│   ├── McpController.java                    ← 修改：注入 McpRemoteToolBridge，
│   │                                              tools/list 支持 mode 过滤
│   └── McpClientController.java              ← 新增：管理面板 API（含桥接管理端点）
└── config/
    ├── McpAutoConfig.java                    ← 新增：MCP 全量装配
    └── McpProperties.java                    ← 新增：MCP 配置属性
```

---

### 六、与现有 AI 体系的集成

MCP Client 与现有 AI 体系的集成形成完整的 **MCP 双向通信能力**，并通过 `AIToolProvider` 无缝融入 LangChain4j 生态：

```
                     ┌──────────────────┐
外部 AI 工具 ──MCP──▶│  McpController   │──▶ AiToolRegistry ──▶ AiTool（本地）
(Claude/Cline等)     │  (MCP Server)    │         │
                     │  mode=all/local/ │         ├── AiTool（桥接）
                     │  bridged 分类过滤│         │
                     └──────────────────┘         │
                                                  │
                     ┌──────────────────┐         │
                     │McpClientController│        │
                     │  (管理面板 API)   │        │
                     └────────┬─────────┘        │
                              │                   │
                              ▼                   │
                     McpClientRegistry            │
                              │                   │
                    ┌─────────▼─────────┐        │
                    │McpRemoteToolBridge│        │
                    │  (桥接调度器)      │        │
                    └─────────┬─────────┘        │
                              │                   │
              ┌───────────────┼───────────────┐   │
              │               │               │   │
              ▼               ▼               ▼   │
         McpRemote      外部 MCP 服务    AiToolRegistry
         ToolAdapter    (HTTP JSON-RPC)       │
              │                               │
              └───────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
         McpController   平台 AI 调用    LangChain4j
         (对外暴露)    (直接execute)   AiServices
                                            │
                              ┌─────────────▼─────────────┐
                              │  AIToolProvider      │
                              │  • 所有 AiTool 自动可见     │
                              │  • 本地+桥接 无差别调用     │
                              │  • 一行注入即用             │
                              └───────────────────────────┘
```

**数据流：**

1. **对外暴露**（已有，增强）：`AiTool` → `AiToolRegistry` → `McpController` → 外部 MCP 客户端。`tools/list` 支持 `mode` 分类过滤。
2. **对内引入**（新增）：外部 MCP 服务 → `McpClientConnection` → `McpClientRegistry` → 管理面板查看/调用。
3. **桥接回路**（新增）：`McpRemoteToolBridge` 自动将启用连接的远程工具 → `McpRemoteToolAdapter` → `AiToolRegistry`，本地与桥接工具完全等价。
4. **LangChain4j 集成**（新增）：`AIToolProvider` 将 `AiToolRegistry` 全部工具暴露给 LangChain4j `AiServices`，开发者注入即用。

**两层等价性保证：**

| 层面 | 桥接工具与本地工具的等价性 | 实现机制 |
|------|--------------------------|---------|
| API 层 | `McpController.handleToolsCall()` 统一通过 `AiToolRegistry.get(name)` 执行，不区分来源 | `AiTool` 接口统一 |
| LangChain4j 层 | `AIToolProvider.provideTools()` 统一通过 `AiToolRegistry.listAll()` 遍历所有工具，构建 ToolSpecification ↔ ToolExecutor 映射 | `ToolProvider` 接口统一 |
| 管理面板层 | `McpClientController` 管理远程连接，不涉及本地工具 | 职责分离 |
| MCP Server 暴露层 | `tools/list` 通过 `mode` 参数可区分来源，满足不同客户端需求 | `McpRemoteToolBridge.getAllBridgedToolNames()` |

---

## 与现有架构的关系

```
┌─────────────────────────────────────────────────────────────────┐
│                        ADR-009 决策体系                          │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  延续核心原则                                               │  │
│  │  • 微内核核心不动摇（core 模块零修改）                       │  │
│  │  • 模块依赖方向不变：core ← api ← app                       │  │
│  │  • ExtensionPoint 注册/消费机制不变                          │  │
│  │  • @ConditionalOnMissingBean 覆盖机制不变                   │  │
│  │  • CRUD 命名遵循 ADR-008 统一规范                           │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  与现有 AI 体系的对称设计                                    │  │
│  │  • McpController（Server） ←→ McpClientConnection（Client） │  │
│  │  • AiToolRegistry（本地工具） ←→ McpClientRegistry（远程）  │  │
│  │  • AiAutoConfig（AI核心）与 McpAutoConfig（MCP全量）职责分离  │  │
│  │  • McpRemoteToolAdapter 桥接远程工具到本地 AiTool            │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  遵循规范要求                                               │  │
│  │  • 配置抽象化：使用 panel.mcp.client 前缀                   │  │
│  │  • 路径收敛：/admin/v1/mcp 前缀统一管理 MCP 全部 API         │  │
│  │  • 注释规范：仅描述意图，不含元数据标签                     │  │
│  │  • 国际化：系统提示及日志信息使用中文                       │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 影响范围评估

| 模块 | 影响说明 | 风险等级 |
|------|---------|---------|
| nexus-admin-core | 无修改 | 无 |
| nexus-admin-api | 新增 10 个文件（1 连接扩展点 + 1 注册表 + 1 桥接调度器 + 1 AIToolProvider + 1 Service + 1 Controller + 1 DTO + 1 AutoConfig + 2 默认实现），修改 2 个文件（McpController 注入桥接 + McpConnectionInfo 新增字段） | 低 |
| nexus-admin-app | 无强制修改（McpAutoConfig 自动装配） | 无 |
| plugins | 现有插件无影响（新接口为增量，不修改现有接口）。插件可选实现 `McpClientConnection` 提供自定义传输协议 | 无 |

**不修改的现有接口：** `AiTool`、`AiToolRegistry`、`AiProvider`、`ExtensionPoint`、`ConfigFacade` 均无修改。

**有修改的现有文件（仅增量，不破坏现有行为）：** `McpController`（注入 `McpRemoteToolBridge`，`tools/list` 增加 mode 过滤，不传 mode 时行为不变）、`McpConnectionInfo`（新增 3 个桥接控制字段，向后兼容）。

---

## 总结

### 核心决策一览

| 决策项 | 结论 | 关键依据 |
|-------|------|---------|
| 模块归属 | 置于 `nexus-admin-api` 模块 | 与现有 AI 体系统一，引入即用，架构简洁 |
| 连接抽象 | `McpClientConnection extends ExtensionPoint` | 支持多传输协议插件化扩展，与 AiProvider 模式一致 |
| 配置持久化 | 配置中心专用作用域 `mcp-clients` | 复用现有基础设施，遵循平台统一配置管理 |
| 远程工具桥接 | `McpRemoteToolBridge` 调度器 + `McpRemoteToolAdapter` 适配器 | 调度器管理生命周期，适配器保证 AiTool 接口等价；命名空间 `mcp.{server}.{tool}` 隔离 |
| MCP Server 分类暴露 | `tools/list` 扩展 `mode` 参数（all/local/bridged） | 不传参保持向后兼容；桥接工具的 `mcp.` 前缀提供天然过滤依据 |
| LangChain4j 集成 | `AIToolProvider` 桥接 AiToolRegistry 到 LangChain4j 生态 | 以 Registry 为唯一权威来源，本地+桥接工具自动可见；`@ConditionalOnClass` 可选装配 |
| 默认传输实现 | HTTP JSON-RPC | MCP 协议最通用的传输方式 |
| API 路径 | `/admin/v1/mcp/clients` | MCP 全部 API 收敛于 `/admin/v1/mcp` 前缀，Server 与 Client 层次化命名 |
| JSON 解析 | 复用 `fastjson2` | 与现有 `McpController` 实现一致 |
| 不引入外部 MCP 框架 | 自建轻量 JSON-RPC 客户端 | 项目已有手写 MCP 服务端实现，客户端保持对称；避免引入额外框架依赖 |

### 设计原则贯彻

| 原则 | 体现 |
|------|------|
| **桥接工具与本地工具完全等价** | `McpRemoteToolAdapter implements AiTool`，统一通过 `AiToolRegistry` 管理，MCP Server / LangChain4j / 代码调用三层均不区分来源 |
| **优雅融入现有体系** | 不修改 `AiTool`、`AiToolRegistry`、`AiProvider` 等现有接口；桥接调度器作为独立层插入，不污染现有组件 |
| **可演进性** | `McpRemoteToolBridge` 独立接口，可替换实现；`AIToolProvider` 通过 `@ConditionalOnMissingBean` 可覆盖；包级隔离为未来模块独立预留空间 |
| **整体性** | MCP Server + Client + Bridge + LangChain4j 集成统一由 `McpAutoConfig` 装配，形成完整闭环 |
| **零侵入** | LangChain4j 为可选依赖（`@ConditionalOnClass`），不引入时空平台 MCP 能力独立可用 |

### 长期演进方向

1. **stdio 传输支持**：插件提供 `StdioMcpClientConnection`，通过子进程通信实现本地 MCP 服务对接
2. **SSE 传输支持**：插件提供 `SseMcpClientConnection`，支持流式工具调用与实时推送
3. **MCP 工具市场**：管理面板可浏览/安装外部 MCP 服务工具，平台成为 MCP 能力发现中心
4. **MCP 代理网关**：平台成为统一的 MCP 能力聚合层，对外暴露所有已接入的外部工具
5. **独立模块提取**：若未来 MCP 能力体量显著增长，可将 `extension/ai/` 中 MCP 相关接口独立为 `nexus-admin-mcp` 模块（当前设计已预留演进空间）
