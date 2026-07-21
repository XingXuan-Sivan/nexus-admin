# 管理面板 API 规范

> Nexus Admin 微内核插件化平台 — 管理面板 RESTful API 规范文档

---

## 1. API 总览与设计原则

### 1.1 RESTful 设计原则

| 原则 | 说明 |
|------|------|
| 资源导向 | URL 表示资源名词，HTTP 方法表示操作语义 |
| 统一接口 | 遵循 GET / POST / PUT / DELETE 标准语义 |
| 无状态 | 每次请求需携带完整认证信息，服务端不依赖 Session 状态（管理面板 API） |
| 统一响应 | 所有 API 返回统一的 `Result` / `DataResult` 信封格式 |
| 幂等性 | GET、PUT、DELETE 天然幂等；POST 非幂等操作需明确标注 |

### 1.2 API 分区说明

Nexus Admin 的 API 按职责分为两个独立分区：

| 分区 | 路径前缀 | 说明 | 实现层 |
|------|----------|------|--------|
| 管理面板 API | `/admin/v1/**` | 平台管理操作（插件管理、配置、系统状态、认证等） | nexus-admin-api 内建 Controller |
| 插件业务 API | `/plugins/{pluginId}/**` | 各插件暴露的业务端点 | 插件自身实现 |

- **管理面板 API**：由平台统一管控，经 Service 层调用 core 门面（`PluginFacade`、`ConfigFacade` 等）后暴露。
- **插件业务 API**：由插件通过 `WebControllerProvider` 或 `@EnableWebEndpoints` 动态注册，平台负责生命周期绑定。

### 1.3 版本策略

- 采用 **URL 路径版本化**：`/admin/v1/...`、`/admin/v2/...`
- 当前版本：**v1**
- 版本升级时旧版本至少保留一个大版本周期的兼容期
- 插件业务 API 不纳入平台版本管理，由插件自行治理

### 1.4 内容协商

- 默认且唯一支持的响应格式：**JSON**
- 请求头：`Content-Type: application/json`
- 响应头：`Content-Type: application/json; charset=utf-8`
- 文件上传端点使用 `multipart/form-data`，单独标注

---

## 2. 认证与授权规范

### 2.1 认证方式

管理面板 API 采用 **Bearer Token** 认证：

```
Authorization: Bearer {accessToken}
```

平台通过 `AuthProvider` 拓展点 + 责任链模式（`CompositeAuthProvider`）支持多认证源，
默认内建 `BootstrapAuthProvider` 提供初始认证能力，插件可注入自定义 `AuthProvider` 实现。

### 2.2 Token 生命周期

| Token 类型 | 有效期 | 用途 |
|------------|--------|------|
| Access Token | 30 分钟 | 请求鉴权，放于 `Authorization` 头 |
| Refresh Token | 7 天 | 刷新 Access Token，放于请求体 |

```
登录 → 获取 accessToken + refreshToken
       ↓
请求 API（携带 accessToken）
       ↓ accessToken 过期
使用 refreshToken 刷新 → 获取新 accessToken
       ↓ refreshToken 过期
重新登录
```

### 2.3 公开端点列表

以下端点无需认证即可访问：

| 端点 | 方法 | 说明 |
|------|------|------|
| `/admin/v1/auth/login` | POST | 登录 |
| `/admin/v1/system/info` | GET | 平台基本信息（仅含名称、版本等公开信息） |
| `/admin/v1/system/status` | GET | 系统运行状态（健康检查） |
| `/admin/v1/docs` | GET | Knife4j（交互式 API 文档） |

### 2.4 权限模型

平台通过 `PermissionResolver` 拓展点实现权限解析与授权决策。

**权限标识格式**：`{module}.{action}`

| 模块 | 权限标识 | 说明 |
|------|----------|------|
| 插件管理 | `plugins.view` | 查看插件列表与详情 |
| 插件管理 | `plugins.manage` | 启停、启用禁用、卸载插件 |
| 插件管理 | `plugins.upload` | 上传安装插件 |
| 配置管理 | `config.view` | 查看配置值与 Schema |
| 配置管理 | `config.manage` | 修改与重置配置 |
| 配置密钥 | `config.secret.rotate` | 替换或清除敏感配置值；不授予读取明文能力 |
| 配置文档 | `config.document.view` | 查看受控原始配置文档；敏感域只返回脱敏状态 |
| 配置文档 | `config.document.manage` | 校验、保存不含敏感字段的原始配置文档 |
| 系统状态 | `system.view` | 查看系统状态与端点列表 |
| UI 元数据 | `*`（仅认证） | 查看菜单、路由等 UI 资源 |
| 用户管理 | `users.view` | 查看用户列表 |
| 用户管理 | `users.manage` | 创建、修改、删除用户 |
| 角色管理 | `roles.view` | 查看角色列表 |
| 角色管理 | `roles.manage` | 创建、修改、删除角色及分配权限 |
| 权限管理 | `permissions.view` | 查看所有权限定义 |

> 权限定义来源包括平台内建权限和插件通过 `plugin.yaml` 声明的权限，
> 均由 `PermissionResolver` 统一解析为 `Permission` 领域对象（code / name / description / resource / action）。

---

## 3. 统一响应格式

### 3.1 成功响应（无数据）

```json
{
  "code": 200,
  "message": "操作成功"
}
```

对应 Java 类型：`Result`（`code` + `message`）

### 3.2 成功响应（有数据）

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

对应 Java 类型：`DataResult<T>`（继承 `Result`，增加 `data` 字段）

### 3.3 分页响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      { "pluginId": "demo-plugin", "name": "示例插件", "version": "1.0.0", "state": "ACTIVE", "description": "...", "provider": "Nexus" }
    ],
    "total": 100,
    "page": 1,
    "size": 20,
    "totalPages": 5
  }
}
```

分页数据结构：

| 字段 | 类型 | 说明 |
|------|------|------|
| `items` | `T[]` | 当前页数据列表 |
| `total` | `long` | 总记录数 |
| `page` | `int` | 当前页码（从 1 开始） |
| `size` | `int` | 每页条数 |
| `totalPages` | `int` | 总页数 |

### 3.4 错误响应

错误响应使用 `application/problem+json`，字段遵循 Problem Details 语义。`errorCode` 的 JSON 类型始终为字符串；配置中心使用稳定符号码，尚未迁移的模块把原数字码序列化为字符串。`traceId` 始终存在，客户端可用于日志关联。

```json
{
  "type": "urn:nexus-admin:problem:config-validation",
  "title": "配置校验失败",
  "status": 422,
  "detail": "配置值不符合 Schema",
  "instance": "/admin/v1/config/demo-plugin",
  "errorCode": "CONFIG_VALIDATION_FAILED",
  "traceId": "6d0cc6bb-9340-49aa-bc0a-f805eddb9a52",
  "scopeId": "demo-plugin",
  "fieldErrors": [{
    "path": "/maxItems",
    "keyword": "maximum",
    "messageKey": "configuration.validation.maximum",
    "params": { "limit": 1000 },
    "line": null,
    "column": null
  }]
}
```

`fieldErrors` 仅包含 `path/keyword/messageKey/params/line/column`，不得返回 rejected value 或把服务端原始异常消息作为字段值。revision 冲突额外返回 `currentRevision`。

### 3.5 错误码体系

平台错误码基于 `StatusCode` 接口，按模块分段管理：

| 范围 | 模块 | 示例 |
|------|------|------|
| 200-299 | 通用成功 | 200 操作成功、201 创建成功 |
| 400-499 | 客户端错误 | 400 参数错误、401 未认证、403 无权限、404 资源不存在 |
| 500-599 | 服务端错误 | 500 服务器内部错误 |
| 1000-1999 | 认证授权模块 | 1001 认证失败、1002 Token 已过期、1004 权限不足 |
| 2000-2999 | 插件管理模块 | 2001 插件不存在、2002 插件状态不合法、2004 插件加载失败 |
| 稳定字符串 | 配置管理模块 | `CONFIG_VALIDATION_FAILED`、`CONFIG_DOMAIN_NOT_FOUND`、`CONFIG_REVISION_CONFLICT`、`CONFIG_DOCUMENT_TOO_LARGE` |
| 4000-4999 | 系统模块 | 4001 系统不可用、4002 系统维护中 |
| 5000-5999 | 业务对象模块 | 5001 用户不存在、5003 角色不存在 |
| 10000+ | 插件自定义 | 由各插件自行定义，需在 `plugin.yaml` 中声明 |

**通用状态码**（`StatusCodes` 枚举）：

| 状态码 | 枚举值 | 消息 |
|--------|--------|------|
| 200 | SUCCESS | 操作成功 |
| 201 | CREATED | 创建成功 |
| 400 | BAD_REQUEST | 请求参数错误 |
| 401 | UNAUTHORIZED | 未认证 |
| 403 | FORBIDDEN | 无权限 |
| 404 | NOT_FOUND | 资源不存在 |
| 500 | INTERNAL_ERROR | 服务器内部错误 |

> 自定义业务状态码实现 `StatusCode` 接口即可，无需扩展 `StatusCodes` 枚举。

---

## 4. 通用查询参数规范

### 4.1 分页参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | `int` | 1 | 页码，从 1 开始 |
| `size` | `int` | 20 | 每页条数，最大 100 |

**示例**：`GET /admin/v1/plugins?page=2&size=50`

### 4.2 排序参数

格式：`sort={field}:{direction}`，多字段以逗号分隔。

| 值 | 说明 |
|----|------|
| `asc` | 升序 |
| `desc` | 降序 |

**示例**：`GET /admin/v1/plugins?sort=name:asc,createdAt:desc`

### 4.3 过滤参数

格式：`filter[{field}]={value}`，支持多字段并列。

**示例**：`GET /admin/v1/plugins?filter[state]=ACTIVE&filter[name]=demo`

### 4.4 关键字搜索

| 参数 | 类型 | 说明 |
|------|------|------|
| `keyword` | `string` | 在名称、描述等主要文本字段中模糊搜索 |

**示例**：`GET /admin/v1/plugins?keyword=demo`

---

## 5. 各模块端点详细定义

### 5.1 认证 API

基础路径：`/admin/v1/auth`

#### POST /admin/v1/auth/login — 登录

权限：**公开**

**请求体**：

```json
{
  "username": "admin",
  "password": "<bootstrap password>"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | `string` | 是 | 用户名 |
| `password` | `string` | 是 | 密码 |

**响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g...",
    "expiresIn": 1800,
    "tokenType": "Bearer"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `accessToken` | `string` | 访问令牌 |
| `refreshToken` | `string` | 刷新令牌 |
| `expiresIn` | `int` | Access Token 有效时长（秒） |
| `tokenType` | `string` | 令牌类型，固定为 `Bearer` |

**错误响应**：

| 状态码 | 说明 |
|--------|------|
| 3001 | 用户名或密码错误 |
| 3003 | 账户已锁定 |

---

#### POST /admin/v1/auth/logout — 登出

权限：**需认证**

**请求头**：`Authorization: Bearer {accessToken}`

**响应**：

```json
{
  "code": 200,
  "message": "操作成功"
}
```

---

#### POST /admin/v1/auth/refresh — 刷新 Token

权限：**需认证**

**请求体**：

```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g..."
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `refreshToken` | `string` | 是 | 刷新令牌 |

**响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 1800,
    "tokenType": "Bearer"
  }
}
```

**错误响应**：

| 状态码 | 说明 |
|--------|------|
| 3002 | Token 已过期或无效 |

---

#### GET /admin/v1/auth/me — 获取当前用户信息

权限：**需认证**

**请求头**：`Authorization: Bearer {accessToken}`

**响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": "admin",
    "username": "admin",
    "displayName": "管理员",
    "roles": ["ADMIN"],
    "permissions": ["plugins.view", "plugins.manage", "config.view", "config.manage", "system.view", "ui.view", "users.view", "users.manage", "roles.view", "roles.manage", "permissions.view"]
  }
}
```

---

### 5.2 插件管理 API

基础路径：`/admin/v1/plugins`

#### GET /admin/v1/plugins — 获取插件列表

权限：`plugins.view`

当前端点不接收查询参数，返回全部已注册插件摘要。

**响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [{
    "pluginId": "demo-plugin",
    "version": "1.0.0",
    "name": "示例插件",
    "description": "用于演示插件开发流程",
    "state": "ACTIVE",
    "provider": "Nexus",
    "configuration": {
      "scopeId": "demo-plugin",
      "configurable": true,
      "hasSchema": true,
      "schemaStatus": "valid",
      "canView": true,
      "canEdit": true
    }
  }]
}
```

**PluginView 字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `pluginId` | `string` | 插件唯一标识 |
| `version` | `string` | 插件版本 |
| `name` | `string` | 插件名称 |
| `description` | `string` | 插件描述 |
| `state` | `string` | 插件状态（PluginStateView 枚举值） |
| `provider` | `string` | 提供者信息 |
| `configuration` | `PluginConfigurationView` | 插件配置域与可视化配置能力；scopeId 为原始 pluginId |

---

#### GET /admin/v1/plugins/{pluginId} — 获取插件详情

权限：`plugins.view`

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `pluginId` | `string` | 插件标识 |

**响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pluginId": "demo-plugin",
    "version": "1.0.0",
    "name": "示例插件",
    "description": "用于演示插件开发流程",
    "state": "ACTIVE",
    "provider": "Nexus",
    "mainClass": "com.nexusadmin.plugin.demo.DemoPlugin",
    "dependencies": ["system-user-plugin"],
    "extensions": [
      {
        "extensionPoint": "AuthProvider",
        "className": "com.nexusadmin.plugin.demo.DemoAuthProvider",
        "priority": 100
      }
    ],
    "loadedAt": "2026-04-01T08:30:00Z",
    "startedAt": "2026-04-01T08:30:05Z",
    "attributes": {
      "author": "Nexus Team",
      "homepage": "https://nexusadmin.com"
    },
    "configuration": {
      "scopeId": "demo-plugin",
      "configurable": true,
      "hasSchema": true,
      "schemaStatus": "valid",
      "canView": true,
      "canEdit": true
    }
  }
}
```

**PluginDetailView 字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `pluginId` | `string` | 插件唯一标识 |
| `version` | `string` | 插件版本 |
| `name` | `string` | 插件名称 |
| `description` | `string` | 插件描述 |
| `state` | `string` | 插件状态 |
| `provider` | `string` | 提供者信息 |
| `mainClass` | `string` | 主类名 |
| `dependencies` | `string[]` | 依赖插件标识集合 |
| `extensions` | `ExtensionView[]` | 扩展点信息列表 |
| `loadedAt` | `string` | 加载时间（ISO 8601） |
| `startedAt` | `string` | 启动时间（ISO 8601） |
| `attributes` | `object` | 扩展属性 |
| `configuration` | `PluginConfigurationView` | 与插件列表相同的配置能力元数据 |

`PluginConfigurationView.configurable` 表示服务端存在有效 Schema；`canView/canEdit` 还会结合当前认证用户的 `config.view/config.manage` 有效权限。前端必须使用返回的 `scopeId`，不能自行拼接配置域，也不能把 capability 当作平台静态能力缓存到其他用户会话。

**ExtensionView 字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `extensionPoint` | `string` | 扩展点接口名 |
| `className` | `string` | 实现类名 |
| `priority` | `int` | 优先级 |

**错误响应**：

| 状态码 | 说明 |
|--------|------|
| 404 | 插件不存在 |
| 1001 | 插件不存在 |

---

#### POST /admin/v1/plugins/{pluginId}/start — 启动插件

权限：`plugins.manage`

将插件从 INITIALIZED 或 STOPPED 状态迁移到 ACTIVE 状态。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `pluginId` | `string` | 插件标识 |

**响应**：

```json
{
  "code": 200,
  "message": "操作成功"
}
```

**错误响应**：

| 状态码 | 说明 |
|--------|------|
| 1001 | 插件不存在 |
| 1002 | 插件当前状态不允许启动（如已为 ACTIVE） |
| 1003 | 插件启动失败 |

---

#### POST /admin/v1/plugins/{pluginId}/stop — 停止插件

权限：`plugins.manage`

将插件从 ACTIVE 状态迁移到 STOPPED 状态。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `pluginId` | `string` | 插件标识 |

**响应**：

```json
{
  "code": 200,
  "message": "操作成功"
}
```

**错误响应**：

| 状态码 | 说明 |
|--------|------|
| 1001 | 插件不存在 |
| 1002 | 插件当前状态不允许停止 |

---

#### POST /admin/v1/plugins/{pluginId}/enable — 启用插件

权限：`plugins.manage`

将插件从 DISABLED 状态恢复，并持久化启用状态。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `pluginId` | `string` | 插件标识 |

**响应**：

```json
{
  "code": 200,
  "message": "操作成功"
}
```

**错误响应**：

| 状态码 | 说明 |
|--------|------|
| 1001 | 插件不存在 |

---

#### POST /admin/v1/plugins/{pluginId}/disable — 禁用插件

权限：`plugins.manage`

将插件设为 DISABLED 状态，并持久化禁用状态。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `pluginId` | `string` | 插件标识 |

**响应**：

```json
{
  "code": 200,
  "message": "操作成功"
}
```

**错误响应**：

| 状态码 | 说明 |
|--------|------|
| 1001 | 插件不存在 |

---

#### DELETE /admin/v1/plugins/{pluginId} — 卸载插件

权限：`plugins.manage`

停止并卸载插件，释放资源，从注册中心移除。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `pluginId` | `string` | 插件标识 |

**响应**：

```json
{
  "code": 200,
  "message": "操作成功"
}
```

**错误响应**：

| 状态码 | 说明 |
|--------|------|
| 1001 | 插件不存在 |
| 1003 | 插件卸载失败 |

---

#### POST /admin/v1/plugins/upload — 上传安装插件

权限：`plugins.upload`

**Content-Type**：`multipart/form-data`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | `file` | 是 | 插件 JAR 文件 |

**响应**：

```json
{
  "code": 201,
  "message": "创建成功",
  "data": {
    "pluginId": "new-plugin",
    "version": "1.0.0",
    "name": "新插件",
    "description": "通过上传安装的插件",
    "state": "DISCOVERED",
    "provider": "External"
  }
}
```

**错误响应**：

| 状态码 | 说明 |
|--------|------|
| 400 | 上传文件为空或格式不正确 |
| 1003 | 插件安装失败（JAR 解析错误、依赖缺失等） |

---

### 5.3 配置中心 API

基础路径：`/admin/v1/config`。`scopeId` 是后端返回的 URL-safe opaque ID，规范为 `[a-z][a-z0-9_-]*(?:\.[a-z0-9_-]+)*` 且最长 128 个字符；插件域直接使用原始 `pluginId`，客户端不得添加 `plugin.`、`plugin:` 等前缀，也不得解析 ID 推导层级。

所有读取 snapshot 或 document 的响应同时返回 `ETag: "{revision}"`。所有 validate、update、reset 和 document save 请求必须携带读取时得到的 `If-Match`；revision 过期返回 HTTP 409 和 `currentRevision`，服务端不会静默覆盖。

#### GET /domains — 获取配置域目录

权限：`config.view`

响应 `Catalog`：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "catalogRevision": "catalog-a12f",
    "domains": [{
      "id": "demo-plugin",
      "parentId": null,
      "kind": "plugin",
      "pluginId": "demo-plugin",
      "displayName": "演示插件",
      "description": "演示插件配置",
      "hasSchema": true,
      "schemaStatus": "valid",
      "documentFormat": "yaml",
      "capabilities": {
        "viewValues": true,
        "editValues": true,
        "resetValues": true,
        "viewSchema": true,
        "viewDocument": true,
        "editDocument": true
      },
      "hotReload": true,
      "restartRequired": false,
      "revision": "cfg-8c3f...",
      "updatedAt": null
    }]
  }
}
```

明确保留的平台域为 `platform` 和 `platform.disabled`；其他 ID 一律按 opaque 插件域处理。例如插件 ID `platformer` 不属于平台域。

`capabilities` 是当前认证用户的有效能力，并同时受 Schema、存储实现和敏感域安全策略约束。例如用户没有 `config.manage` 时 `editValues/resetValues=false`；没有 `config.document.manage` 或域含敏感字段时 `editDocument=false`。客户端不得跨用户会话缓存该对象，服务端也不会因为 capability 为 true 而跳过端点权限校验。

#### GET /{scopeId} — 获取类型化配置快照

权限：`config.view`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "scopeId": "demo-plugin",
    "revision": "cfg-8c3f...",
    "effectiveValues": { "enabled": true, "maxItems": 100 },
    "persistedValues": { "maxItems": 100 },
    "defaultValues": { "enabled": true, "maxItems": 50 },
    "fieldStates": {
      "/maxItems": {
        "source": "file",
        "hasEffectiveValue": true,
        "hasPersistedValue": true,
        "hasDefaultValue": true,
        "persistedWritable": true,
        "effectiveValueOverridden": false,
        "sensitive": false,
        "restartRequired": false,
        "readOnlyReason": null
      }
    }
  }
}
```

来源枚举为 `environment | file | default | none`，优先级为 Environment > File > Default。值保持 JSON 原生类型。敏感路径不出现在三个 values 对象中，只通过 `fieldStates.has*Value` 表示是否已设置。

#### GET /{scopeId}/schema — 获取无损 JSON Schema

权限：`config.view`

响应字段为 `scopeId`、`schemaRevision`、`dialect` 和原始 `schema`。插件 Schema 位于 `META-INF/schema.json`，方言固定为 JSON Schema Draft 2020-12；嵌套 object/array、条件、`$defs`、本地 `$ref` 和 `x-ui-*` 扩展不会被压平。远程 `$ref` 被拒绝。

敏感字段使用标准 `writeOnly: true` 或 `x-ui-options.sensitive: true`。Schema 不得包含可用的敏感 default。若 array 的 `items/prefixItems` 包含敏感字段，服务端将整个 array 视为原子敏感路径并完整省略；客户端对数组路径使用 `replace-sensitive/clear-sensitive`，不提交带数字索引的敏感子路径。

#### POST /{scopeId}/validate — 校验结构化变更

权限：`config.manage`

```json
{
  "changes": [
    { "op": "set", "path": "/maxItems", "value": 120 },
    { "op": "unset", "path": "/legacyFlag" },
    { "op": "replace-sensitive", "path": "/token", "value": "new-secret" }
  ],
  "reason": "调整插件策略"
}
```

操作为 `set | unset | replace-sensitive | clear-sensitive`，路径必须是 JSON Pointer。敏感路径只接受 `replace-sensitive/clear-sensitive`；使用普通 `set/unset` 或写入包含敏感后代的父对象会返回 400，避免绕过专用敏感操作。敏感替换和清除除 `config.manage` 外还要求 `config.secret.rotate`。响应包含 `valid`、`issues` 及 `effectivePreview`。issue 字段包括 `path/keyword/message/line/column/messageKey/params/source/severity`；当前 Schema 语义校验的 line/column 可为空，messageKey 采用 `configuration.validation.{keyword}`，message 只由 keyword 生成，params 仅含 Schema 约束且绝不含被校验值，source 为 `server`，severity 为 `error`。

#### PUT /{scopeId} — 原子保存结构化变更

权限：`config.manage`

请求体与 validate 相同。服务端分别校验 `default + persisted` 候选配置和应用外部来源后的完整有效配置，外部来源不能掩盖非法文件值；以 scope 为单次事务原子写入。任一 change 失败时不写文件、不刷新缓存、不发布成功事件。成功返回与该次提交 revision 一致的完整 snapshot 和 ETag。

#### POST /{scopeId}/reset — 移除持久化覆盖

权限：`config.manage`

重置指定字段：

```json
{ "paths": ["/maxItems"], "all": false, "reason": "恢复默认值" }
```

整域重置必须显式提交 `{ "paths": [], "all": true }`。空 paths 不代表整域重置。reset 只移除 File 层覆盖，使值回退到 Environment 或 Default，不会把默认值写回文件；敏感路径由服务端转换为受控 `clear-sensitive`，无需且不允许客户端借助普通 `unset` 绕过专用语义。包含敏感路径的 reset 还要求 `config.secret.rotate`。成功返回新 snapshot。

#### GET /{scopeId}/document — 读取受控配置文档

权限：`config.document.view`

响应包含 `scopeId/displayName/format/revision/content/writable/redacted/requiresReauthentication/maxBytes`。`format` 根据当前文档内容返回 `yaml | json`，`displayName` 仅为逻辑文件名，不暴露绝对路径。

若 Schema 含敏感字段，当前实现返回 `content=""`、`redacted=true`、`writable=false`。平台尚无近期重新认证机制，因此不会开放包含敏感字段的原始文档读写；敏感值只能通过结构化 replace-sensitive 操作替换且不会回显。

#### POST /{scopeId}/document/validate — 校验配置文档

权限：`config.document.manage`

```json
{ "format": "yaml", "content": "maxItems: 120\n", "reason": "调整插件策略" }
```

支持 YAML 与 JSON，根节点必须是对象，最大 524288 字节。响应模型与结构化 validate 相同；语法问题 keyword 为 `syntax`，Schema 问题使用 JSON Pointer 定位。

#### PUT /{scopeId}/document — 原子保存配置文档

权限：`config.document.manage`

请求体与 document validate 相同。服务端依次执行权限与域白名单、大小限制、revision、Safe YAML/JSON 解析、Draft 2020-12 校验、同目录临时文件强制落盘、备份及原子替换。YAML/JSON 内容按提交文本原样保存，因此 YAML 注释不会因文档模式保存而丢失。成功返回 `{ document, snapshot }` 和新 ETag。

#### 配置错误响应

| HTTP | errorCode | 场景 |
|---:|---|---|
| 400 | `"400"` | 请求结构、scopeId、JSON Pointer 或操作非法 |
| 403 | `CONFIG_PERMISSION_DENIED` 或 `"1004"` | 缺少敏感操作权限或端点权限 |
| 404 | `CONFIG_DOMAIN_NOT_FOUND` | 配置域或 Schema 不存在 |
| 409 | `CONFIG_REVISION_CONFLICT` | If-Match revision 冲突，响应包含 `currentRevision` |
| 413 | `CONFIG_DOCUMENT_TOO_LARGE` | 文档超过 524288 字节 |
| 422 | `CONFIG_VALIDATION_FAILED` | Schema 或配置语义校验失败 |
| 423 | `CONFIG_DOCUMENT_LOCKED` | 敏感配置文档因缺少重新认证能力而锁定 |

错误响应使用 `application/problem+json`，并包含字符串 `errorCode`、`traceId` 和适用时的 `scopeId/currentRevision/fieldErrors`。敏感值不会进入响应、日志、事件、校验错误或 revision 冲突信息。

---

### 5.4 系统状态 API

基础路径：`/admin/v1/system`

#### GET /admin/v1/system/info — 平台信息

权限：**公开**

**响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "name": "Nexus Admin",
    "version": "1.0.0",
    "description": "微内核插件化业务拓展平台",
    "buildInfo": {
      "javaVersion": "21",
      "springBootVersion": "3.4.x",
      "buildTime": "2026-04-01T00:00:00Z"
    },
    "attributes": {}
  }
}
```

**PlatformInfoView 字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | `string` | 平台名称 |
| `version` | `string` | 平台版本 |
| `description` | `string` | 平台描述 |
| `buildInfo` | `object` | 构建信息键值对 |
| `attributes` | `object` | 扩展属性 |

---

#### GET /admin/v1/system/status — 系统状态

权限：**公开**

**响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "UP",
    "totalPlugins": 5,
    "activePlugins": 3,
    "disabledPlugins": 1,
    "failedPlugins": 0,
    "uptimeMillis": 86400000,
    "jvmInfo": {
      "maxMemory": "1024 MB",
      "totalMemory": "512 MB",
      "freeMemory": "256 MB",
      "availableProcessors": "8",
      "javaVersion": "21.0.1"
    },
    "attributes": {}
  }
}
```

**SystemStatusView 字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | `string` | 系统状态：`UP` / `DOWN` / `DEGRADED` |
| `totalPlugins` | `int` | 插件总数 |
| `activePlugins` | `int` | 活跃插件数 |
| `disabledPlugins` | `int` | 禁用插件数 |
| `failedPlugins` | `int` | 失败插件数 |
| `uptimeMillis` | `long` | 运行时长（毫秒） |
| `jvmInfo` | `object` | JVM 信息键值对 |
| `attributes` | `object` | 扩展属性 |

---

#### GET /admin/v1/system/endpoints — 已注册 API 端点列表

权限：`system.view`

> 此端点提供轻量级的端点发现服务。如需完整的 API 描述（含参数、响应格式等），请使用 OpenAPI 文档端点 `/admin/v1/openapi.json`。

**响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "endpoints": [
      {
        "method": "GET",
        "path": "/admin/v1/plugins",
        "handler": "PluginManageController.listAll",
        "source": "platform"
      },
      {
        "method": "GET",
        "path": "/api/demo-plugin/hello",
        "handler": "DemoController.hello",
        "source": "demo-plugin"
      }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `endpoints` | `object[]` | 端点列表 |
| `endpoints[].method` | `string` | HTTP 方法 |
| `endpoints[].path` | `string` | 请求路径 |
| `endpoints[].handler` | `string` | 处理器标识 |
| `endpoints[].source` | `string` | 来源（`platform` 或 `pluginId`） |

---

### 5.5 UI 元数据 API

基础路径：`/admin/v1/ui`

#### GET /admin/v1/ui/manifest — 聚合 UI 贡献声明

权限：**需认证**

返回所有 ACTIVE 插件的原始 UI 贡献，以 `pluginId` 为键分组。此端点不包含平台内建菜单，也不按当前用户权限预过滤。

**响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "demo-plugin": {
      "menus": [{
        "id": "demo-menu",
        "label": "示例功能",
        "icon": "flask",
        "parentId": "",
        "order": 100,
        "route": "/demo",
        "permissions": ["demo.view"]
      }],
      "routes": [{
        "path": "/demo",
        "component": "demo-plugin/pages/Index",
        "title": "示例页面",
        "icon": "flask",
        "permissions": ["demo.view"]
      }],
      "mountPoints": [],
      "permissions": [{
        "id": "demo.view",
        "label": "查看示例",
        "description": "查看示例插件页面"
      }]
    }
  }
}
```

`data` 的每个值都是 `PluginContributes`，其 `menus/routes/mountPoints/permissions` 字段始终为数组。插件未声明 `contributes` 或四类贡献均为空时，不会出现在该映射中。

---

#### GET /admin/v1/ui/menus — 获取聚合菜单列表

权限：**需认证**

获取平台内建菜单 + ACTIVE 插件贡献菜单的扁平列表，已按 `order` 升序排序。客户端使用 `parentId` 组装层级树；顶级菜单的 `parentId` 为空字符串。

**响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [{
    "id": "dashboard",
    "label": "仪表盘",
    "icon": "ri:dashboard-line",
    "parentId": "",
    "order": 0,
    "route": "/dashboard",
    "permissions": []
  }, {
    "id": "plugins.list",
    "label": "插件列表",
    "icon": "",
    "parentId": "plugins",
    "order": 1,
    "route": "list",
    "permissions": ["plugins.view"]
  }, {
    "id": "configuration",
    "label": "配置中心",
    "icon": "ri:settings-3-line",
    "parentId": "",
    "order": 15,
    "route": "/configuration",
    "permissions": ["config.view"]
  }]
}
```

**菜单项字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `string` | 菜单项唯一标识 |
| `label` | `string` | 显示标题 |
| `icon` | `string` | 图标标识（可选） |
| `parentId` | `string` | 父菜单 ID；空字符串表示顶级 |
| `order` | `int` | 排序权重，值越小越靠前 |
| `route` | `string` | 关联路由；子菜单可使用相对父菜单的路径段 |
| `permissions` | `string[]` | 菜单可见所需权限，列表为 AND 关系；空列表表示无额外 UI 权限限制 |

`/menus` 未内嵌来源字段。如需判断插件来源，使用 `/manifest` 的 `pluginId` 分组，不得根据菜单 ID 猜测。

---

#### GET /admin/v1/ui/routes — 获取路由表

权限：**需认证**

**响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [{
    "path": "/dashboard",
    "component": "DashboardPage",
    "title": "仪表盘",
    "icon": "ri:dashboard-line",
    "permissions": []
  }, {
    "path": "/plugins/list",
    "component": "PluginListPage",
    "title": "插件列表",
    "icon": "",
    "permissions": ["plugins.view"]
  }]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `path` | `string` | 路由路径 |
| `component` | `string` | 前端组件标识 |
| `title` | `string` | 路由标题 |
| `icon` | `string` | 图标标识（可选） |
| `permissions` | `string[]` | 进入路由所需权限，列表为 AND 关系 |

菜单权限只决定导航可见性，不是安全边界。可达页面应在菜单和关联路由上声明一致权限，后端业务 API 仍必须独立执行权限校验。`/manifest`、`/menus` 和 `/routes` 都返回未按当前用户权限过滤的元数据，由前端结合当前用户的有效权限列表处理。

---

### 5.6 核心业务对象 API（拓展点驱动）

> **说明**：以下 API 的实际实现由插件提供（如 `system-user-plugin`），平台通过 Service 层暴露统一端点。
> 不同部署环境下可能由不同的插件提供实现，但 API 契约保持一致。

#### 用户管理

基础路径：`/admin/v1/users`

| 端点 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/admin/v1/users` | GET | 获取用户列表（分页） | `users.view` |
| `/admin/v1/users/{userId}` | GET | 获取用户详情 | `users.view` |
| `/admin/v1/users` | POST | 创建用户 | `users.manage` |
| `/admin/v1/users/{userId}` | PUT | 更新用户 | `users.manage` |
| `/admin/v1/users/{userId}` | DELETE | 删除用户 | `users.manage` |
| `/admin/v1/users/{userId}/roles` | PUT | 分配角色 | `users.manage` |

**GET /admin/v1/users — 获取用户列表**

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | `int` | 否 | 页码 |
| `size` | `int` | 否 | 每页条数 |
| `keyword` | `string` | 否 | 在用户名、显示名中搜索 |
| `filter[status]` | `string` | 否 | 按状态过滤：`ACTIVE` / `DISABLED` |
| `filter[roleId]` | `string` | 否 | 按角色 ID 过滤 |

**响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "userId": "u001",
        "username": "admin",
        "displayName": "管理员",
        "email": "admin@nexusadmin.com",
        "status": "ACTIVE",
        "roles": ["ADMIN"],
        "createdAt": "2026-01-01T00:00:00Z",
        "updatedAt": "2026-04-01T00:00:00Z"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20,
    "totalPages": 1
  }
}
```

**POST /admin/v1/users — 创建用户**

**请求体**：

```json
{
  "username": "newuser",
  "displayName": "新用户",
  "email": "newuser@nexusadmin.com",
  "password": "Pass123!",
  "roleIds": ["ADMIN"]
}
```

**错误响应**：

| 状态码 | 说明 |
|--------|------|
| 4001 | 用户已存在 |
| 400 | 参数校验失败 |

---

#### 角色管理

基础路径：`/admin/v1/roles`

| 端点 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/admin/v1/roles` | GET | 获取角色列表（分页） | `roles.view` |
| `/admin/v1/roles/{roleId}` | GET | 获取角色详情 | `roles.view` |
| `/admin/v1/roles` | POST | 创建角色 | `roles.manage` |
| `/admin/v1/roles/{roleId}` | PUT | 更新角色 | `roles.manage` |
| `/admin/v1/roles/{roleId}` | DELETE | 删除角色 | `roles.manage` |
| `/admin/v1/roles/{roleId}/permissions` | PUT | 分配权限 | `roles.manage` |

**GET /admin/v1/roles — 获取角色列表**

**响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "roleId": "ADMIN",
        "name": "管理员",
        "description": "系统管理员，拥有所有权限",
        "permissions": ["plugins.view", "plugins.manage", "config.view", "config.manage"],
        "userCount": 1,
        "createdAt": "2026-01-01T00:00:00Z"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20,
    "totalPages": 1
  }
}
```

---

#### 权限管理

基础路径：`/admin/v1/permissions`

| 端点 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/admin/v1/permissions` | GET | 查询所有权限定义 | `permissions.view` |
| `/admin/v1/permissions/{code}` | GET | 获取权限详情 | `permissions.view` |

**GET /admin/v1/permissions — 查询所有权限定义**

包含平台内建权限和插件通过 `plugin.yaml` 声明的权限。

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `filter[source]` | `string` | 否 | 按来源过滤：`platform` 或 `pluginId` |
| `filter[resource]` | `string` | 否 | 按资源类型过滤 |

**响应**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "code": "plugins.view",
        "name": "查看插件",
        "description": "查看插件列表与详情",
        "resource": "plugins",
        "action": "view",
        "source": "platform"
      },
      {
        "code": "users.manage",
        "name": "管理用户",
        "description": "创建、修改、删除用户",
        "resource": "users",
        "action": "manage",
        "source": "system-user-plugin"
      }
    ]
  }
}
```

---

## 6. 插件自定义 API 接入规范

### 6.1 插件 API 统一前缀

所有插件暴露的业务 API 统一使用前缀 `/plugins/{pluginId}/**`，与平台管理 API（`/admin/v1/**`）隔离。

**示例**：

| 插件 | API 前缀 | 示例端点 |
|------|----------|----------|
| demo-plugin | `/plugins/demo-plugin/` | `/plugins/demo-plugin/hello` |
| system-user-plugin | `/plugins/system-user-plugin/` | `/plugins/system-user-plugin/users` |

### 6.2 插件 Controller 开发规范

插件 Controller 使用标准 Spring MVC 注解：

```java
@RestController
@RequestMapping("/plugins/demo-plugin")
public class DemoController {

    @GetMapping("/hello")
    public DataResult<String> hello() {
        return DataResult.success("Hello from demo-plugin!");
    }
}
```

**规范要求**：

- `@RequestMapping` 路径必须以 `/plugins/{pluginId}` 开头
- 返回值必须使用平台统一的 `Result` / `DataResult` 包装
- 插件 API 不受平台 `AuthFilter` 拦截，需自行处理认证鉴权（如需要）

### 6.3 两种接入方式

#### 方式一：WebControllerProvider 显式提供

插件实现 `WebControllerProvider` 接口，在 `getControllers()` 方法中返回控制器实例：

```java
public class DemoPlugin extends AbstractPlugin implements WebControllerProvider {

    @Override
    public List<Object> getControllers() {
        return List.of(new DemoController());
    }
}
```

**适用场景**：需要精细控制注册时机的插件。

#### 方式二：@EnableWebEndpoints 自动扫描

在插件主类上添加 `@EnableWebEndpoints` 注解，平台自动扫描并注册插件包下的 `@RestController`：

```java
@EnableWebEndpoints
public class DemoPlugin extends AbstractPlugin {
    // 自动注册 DemoController
}
```

**适用场景**：大多数常规插件，简化配置。

### 6.4 插件 API 命名规范

| 规则 | 说明 | 示例 |
|------|------|------|
| 路径前缀 | 必须为 `/plugins/{pluginId}/` | `/plugins/demo-plugin/` |
| 资源命名 | 使用小写 kebab-case | `/plugins/demo-plugin/user-logs` |
| 版本管理 | 如需版本化，在插件前缀后追加版本 | `/plugins/demo-plugin/v1/logs` |
| 操作语义 | 遵循 RESTful 动词 | GET 查询、POST 创建、PUT 更新、DELETE 删除 |

### 6.5 响应格式要求

插件 API 必须使用平台统一响应格式：

```java
// 有数据
DataResult.success(data);

// 无数据（操作成功）
Result.success();

// 错误
Result.of(StatusCodes.BAD_REQUEST, "参数错误");
```

禁止直接返回裸对象或非标准格式，确保前端统一解析。

---

### 5.7 API 文档端点

基础路径：`/admin/v1`

> **说明**：平台通过 [Knife4j 4.x](https://doc.xiaominfo.com/)（基于 SpringDoc）集成 OpenAPI 文档支持。
>
> - 平台管理 API 通过 Spring MVC 注解自动纳入 OpenAPI 文档
> - 插件通过 `WebEndpointExtension` 注册的 API 会在注册时同步追加到 OpenAPI paths 中

---

#### GET /admin/v1/openapi.json — OpenAPI 文档（JSON 格式）

权限：`system.view`

返回符合 OpenAPI 3.0 规范的完整 API 文档，包含平台管理 API 和当前所有活跃插件注册的业务 API。

**响应**：标准 OpenAPI 3.0 JSON 文档（不使用 `Result` 信封包装，直接返回 OpenAPI spec）。

**响应头**：`Content-Type: application/json`

---

#### GET /admin/v1/openapi.yaml — OpenAPI 文档（YAML 格式）

权限：`system.view`

同上，返回 YAML 格式的 OpenAPI 3.0 文档。

**响应**：标准 OpenAPI 3.0 YAML 文档（不使用 `Result` 信封包装，直接返回 OpenAPI spec）。

**响应头**：`Content-Type: application/vnd.oai.openapi`

---

#### GET /admin/v1/docs — Knife4j 文档

权限：**公开**

提供交互式 API 文档界面（Knife4j），方便开发者浏览和测试 API。

**响应**：HTML 页面（Knife4j 渲染的交互式 API 文档）。

---

## 7. 实时推送规范（SSE）

### 7.1 SSE 端点

**GET /admin/v1/events/stream** — 服务端推送事件流

权限：**需认证**

**请求头**：

```
Authorization: Bearer {accessToken}
Accept: text/event-stream
```

### 7.2 事件类型定义

| 事件类型 | 触发条件 | 数据示例 |
|----------|----------|----------|
| `plugin.stateChanged` | 插件状态变更（启停、启用禁用等） | `{"pluginId":"demo-plugin","oldState":"STOPPED","newState":"ACTIVE"}` |
| `config.changed` | 配置值变更 | `{"scope":"platform","keys":["logging.level"]}` |
| `system.alert` | 系统告警（内存不足、插件失败等） | `{"level":"WARN","message":"插件 demo-plugin 启动失败","source":"plugin-manager"}` |

### 7.3 事件消息格式

SSE 消息遵循标准 `text/event-stream` 协议，每条消息为 JSON 格式：

```
event: plugin.stateChanged
id: 1
data: {"pluginId":"demo-plugin","oldState":"STOPPED","newState":"ACTIVE","timestamp":"2026-04-01T08:30:05Z"}

event: config.changed
id: 2
data: {"scope":"platform","keys":["logging.level"],"timestamp":"2026-04-01T08:35:00Z"}

event: system.alert
id: 3
data: {"level":"WARN","message":"插件 demo-plugin 启动失败","source":"plugin-manager","timestamp":"2026-04-01T08:40:00Z"}
```

**通用字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `timestamp` | `string` | 事件发生时间（ISO 8601） |

**plugin.stateChanged 字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `pluginId` | `string` | 插件标识 |
| `oldState` | `string` | 变更前状态 |
| `newState` | `string` | 变更后状态 |

**config.changed 字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `scope` | `string` | 配置作用域 |
| `keys` | `string[]` | 变更的配置键列表 |

**system.alert 字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `level` | `string` | 告警级别：`INFO` / `WARN` / `ERROR` |
| `message` | `string` | 告警消息 |
| `source` | `string` | 告警来源 |

### 7.4 重连与心跳机制

- **心跳**：服务端每 30 秒发送一次心跳注释（`: heartbeat`），保持连接活跃
- **重连**：客户端断开后自动重连，通过 `Last-Event-ID` 请求头传递上次接收的事件 ID，实现断点续传
- **事件 ID**：每条事件的 `id` 字段为全局递增整数，用于断点续传定位

```
: heartbeat

event: plugin.stateChanged
id: 42
data: {"pluginId":"demo-plugin","oldState":"ACTIVE","newState":"STOPPED","timestamp":"2026-04-01T09:00:00Z"}

: heartbeat
```
