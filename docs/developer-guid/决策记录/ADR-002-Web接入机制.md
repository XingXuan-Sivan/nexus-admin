# admin-panel 与插件 Web 接入机制设计文档（兼容现有架构）

------

# 一、设计前提（必须遵守）

本设计严格遵循现有系统约束：

```text
1. 已存在完整插件生命周期状态机（不可修改）
2. 已存在 PluginManager / PluginContext / ConfigManager / Facade
3. admin-panel 为普通插件（无特权）
4. 不允许破坏已有核心结构
```

------

# 二、问题定义

当前系统：

```text
插件运行在独立 PluginContext（子容器）
```

但：

```text
Spring MVC 仅识别 Root ApplicationContext 中的 Controller
```

导致：

```text
插件中的 @RestController 无法生效
```

------

# 三、设计目标

```text
1. 让插件中的 Controller 可被访问
2. 不污染主容器（Root Context）
3. 支持插件卸载（必须）
4. 不修改 Plugin 生命周期模型
5. 与现有 PluginSpringManager 对齐（扩展而非重构）
```

------

# 四、总体方案

采用：

```text
子容器隔离 + HandlerMapping 动态注册（桥接模式）
```

------

## 4.1 架构结构

```text
PluginContext（子容器）
    ↓
扫描 Controller
    ↓
WebEndpointBridge（桥接层）
    ↓
RequestMappingHandlerMapping（主容器）
```

------

## 4.2 关键思想

```text
不把 Controller 放进主容器
而是注册 HandlerMapping
```

------

# 五、模块设计

------

# 5.1 新增模块：web-bridge（或放入 integration 层）

```text
plugin-integration
└─ web
   ├─ WebEndpointRegistrar
   ├─ SpringWebEndpointRegistrar
   ├─ MappingResolver
   └─ PluginWebRegistry
```

------

# 5.2 组件说明

------

## 5.2.1 WebEndpointRegistrar（核心接口）

```java
public interface WebEndpointRegistrar {

    void register(String pluginId, Object controller);

    void unregister(String pluginId);
}
```

------

## 5.2.2 PluginWebRegistry（状态存储）

```java
public class PluginWebRegistry {

    private final Map<String, List<RequestMappingInfo>> mappings = new ConcurrentHashMap<>();

    public void add(String pluginId, RequestMappingInfo info);

    public List<RequestMappingInfo> get(String pluginId);

    public void remove(String pluginId);
}
```

------

## 5.2.3 MappingResolver（注解解析器）

```java
public interface MappingResolver {

    List<ResolvedMapping> resolve(Object controller);
}
```

------

## 5.2.4 SpringMappingResolver（实现）

职责：

```text
解析：
- @RequestMapping
- @GetMapping / @PostMapping / ...
```

------

## 5.2.5 SpringWebEndpointRegistrar（实现类）

```java
public class SpringWebEndpointRegistrar implements WebEndpointRegistrar {

    private final RequestMappingHandlerMapping handlerMapping;
    private final MappingResolver resolver;
    private final PluginWebRegistry registry;

    @Override
    public void register(String pluginId, Object controller) {

        List<ResolvedMapping> mappings = resolver.resolve(controller);

        for (ResolvedMapping rm : mappings) {

            handlerMapping.registerMapping(
                rm.getInfo(),
                controller,
                rm.getMethod()
            );

            registry.add(pluginId, rm.getInfo());
        }
    }

    @Override
    public void unregister(String pluginId) {

        List<RequestMappingInfo> infos = registry.get(pluginId);

        if (infos == null) return;

        for (RequestMappingInfo info : infos) {
            handlerMapping.unregisterMapping(info);
        }

        registry.remove(pluginId);
    }
}
```

------

# 六、与 PluginSpringManager 集成（关键）

------

## 6.1 扩展点（不修改核心逻辑）

在 PluginSpringManager 中增加**可选扩展调用**：

```java
public interface PluginSpringManager {

    // 已存在
    void start(PluginContext context);

    void stop(PluginContext context);

    // 新增（扩展点）
    default void afterStart(PluginContext context) {}

    default void beforeStop(PluginContext context) {}
}
```

------

## 6.2 Web 注册挂载点

```text
afterStart → 注册 Web
beforeStop → 卸载 Web
```

------

## 6.3 实现示例

```java
public class DefaultPluginSpringManager implements PluginSpringManager {

    private WebEndpointRegistrar registrar;

    @Override
    public void afterStart(PluginContext context) {
        registerControllers(context);
    }

    @Override
    public void beforeStop(PluginContext context) {
        registrar.unregister(context.getPluginId());
    }

    private void registerControllers(PluginContext context) {

        Map<String, Object> controllers =
            context.getBeansWithAnnotation(RestController.class);

        controllers.values().forEach(controller ->
            registrar.register(context.getPluginId(), controller)
        );
    }
}
```

------

# 七、路径隔离机制

------

## 7.1 设计目标

```text
避免不同插件 API 冲突
```

------

## 7.2 方案

统一前缀：

```text
/api/**
```

------

## 7.3 实现方式

在 MappingResolver 中处理：

```java
String prefix = "/api";

newPath = prefix + originalPath;
```

------

# 八、admin-panel 插件接入

------

## 8.1 Controller 正常编写

```java
@RestController
@RequestMapping("/admin/plugins")
public class PluginController {}
```

------

## 8.2 实际访问路径

```text
/api/admin/plugins
```

------

## 8.3 优化（可选）

允许声明：

```java
@AdminApi
```

自动映射为：

```text
/admin/**
```

------

# 九、认证集成

------

## 9.1 Filter 注册位置

```text
Root Context（主容器）
```

------

## 9.2 插件提供能力

```text
AuthProvider（来自 api）
```

------

## 9.3 自动聚合

```java
List<AuthProvider> providers
```

------

## 9.4 生命周期

```text
插件加载 → provider 生效
插件卸载 → provider 自动移除
```

------

# 十、错误处理

------

## 10.1 Mapping 冲突检测

```java
handlerMapping.getHandlerMethods()
```

------

## 10.2 注册失败策略

```text
记录日志 + 跳过该 Controller
```

------

## 10.3 卸载安全

```text
必须保证 mapping 全部移除
```

------

# 十一、扩展能力（未来）

------

## 11.1 WebEndpointContributor

```java
public interface WebEndpointContributor {
    Object handler();
}
```

------

## 11.2 非 Spring Controller 支持

```text
直接注册 HandlerMethod
```

------

## 11.3 UI 扩展（admin-panel）

```text
AdminExtension（已有设计）
```

------

# 十二、设计优势

------

## ✔ 完整插件隔离

```text
插件不进入主容器
```

------

## ✔ 支持卸载

```text
通过 registry 精确移除 mapping
```

------

## ✔ 与现有系统完全兼容

```text
不修改状态机 / PluginManager
```

------

## ✔ 可演进

```text
支持：
- 网关化
- 分布式 control plane
```

------

# 十三、最终结论

```text
本方案本质是：

“在不破坏现有插件架构的前提下，
通过 WebEndpointRegistrar 将插件能力桥接到 Spring MVC”
```