# SPI注册中心设计

------

## 1. 概述

### 1.1 设计目标

SPI 注册中心用于统一管理系统中具备多实现能力的核心组件，构建结构清晰、职责明确、可扩展的组件管理体系。其目标包括：

- **职责清晰**：插件生命周期管理与组件注册管理分离
- **统一治理**：所有多实现核心组件由单一注册中心管理
- **可扩展性**：新增组件类型无需修改核心框架结构
- **组合标准化**：通过统一接口规范多实现组合行为
- **灵活配置**：支持优先级控制与应用层装配策略

### 1.2 核心原则

- **分层架构**：契约层与实现层分离，核心模块保持零框架依赖
- **接口标记机制**：通过标记接口识别可管理组件
- **优先级驱动选择**：数值越大优先级越高，同优先级后注册优先
- **构造函数注入**：所有依赖通过构造函数传递
- **注册集中化**：组件注册统一在应用层完成

------

## 2. 架构设计

### 2.1 分层结构

```mermaid
graph TD
    subgraph api模块[api 模块（契约层）]
        A[SpiRegistry<br/>（接口）]
        B[@SpiImplementation<br/>（注解支持）]
        C[CoreComponent<br/>（标记接口）]
    end

    subgraph core模块[core 模块（实现层）]
        D[ComponentRegistry<br/>（无框架依赖）]
        subgraph 复合体系[Composite 体系]
            E[Composite<br/>（组合接口）]
            F[Composable<br/>（可组合标记）]
        end
    end

    subgraph app模块[app 模块（配置层）]
        G[BootstrapConfig<br/>统一装配并注册核心组件，控制启用与优先级]
    end

    %% 定义样式以模拟原始框图中的分隔线
    style api模块 fill:#fff,stroke:#333,stroke-width:2px
    style core模块 fill:#fff,stroke:#333,stroke-width:2px
    style app模块 fill:#fff,stroke:#333,stroke-width:2px
    style 复合体系 stroke-dasharray: 5 5
```

### 2.2 核心抽象

#### 2.2.1 Composable（标记接口）

所有具备多实现能力的核心组件必须实现该接口，以表明其可被注册中心管理。

```java
public interface Composable {
}
```

典型实现类型包括：

- PluginDescriptorParser
- PluginDescriptorReader
- PluginDescriptorPathResolver
- PluginLoader
- PluginLifecycleListener
- PluginSource

------

#### 2.2.2 ComponentRegistry（组件注册中心）

用于统一管理所有 `Composable` 组件。

核心能力：

- 按类型注册组件实现（支持优先级）
- 获取单个最高优先级实现
- 获取所有实现（按优先级排序）
- 注销指定实现
- 查询实现数量与存在性

优先级规则：

- 数值越大优先级越高
- 相同优先级时后注册优先

------

#### 2.2.3 Composite（组合接口）

用于将多个组件整合为统一访问入口。

```java
public interface Composite<T extends Composable> extends Composable {

    List<T> getMembers();

    void addMember(T member);

    void removeMember(T member);

    boolean containsMember(T member);

    int memberCount();
}
```

Composite 统一规范多实现组件的组合行为，可嵌套组合。

------

#### 2.2.4 GenericComposite（通用组合器）

为避免为每个组件类型单独创建 Composite 实现类，系统提供了 `GenericComposite` 通用组合器。

**核心特性：**

- 适用于所有 `Composable` 组件类型
- 支持优先级排序（可选）
- 提供多种执行策略（短路、全执行、条件执行）
- 线程安全（使用 `CopyOnWriteArrayList`）

**使用示例：**

```java
// 创建带优先级排序的组合器
GenericComposite<PluginDescriptorPathResolver> pathResolvers = 
    new GenericComposite<>(PluginDescriptorPathResolver::priority);

pathResolvers.addMember(new PackedDirectoryResolver());
pathResolvers.addMember(new DevDirectoryResolver());
pathResolvers.addMember(new JarResolver());

// 执行第一个成功的解析器
Optional<Path> result = pathResolvers.executeFirst(
    resolver -> resolver.resolve(pluginPath)
);
```

**执行策略：**

- `executeFirst(executor)` - 短路策略，返回第一个非空结果
- `executeAll(executor)` - 全执行策略，返回所有结果
- `executeFirstSupported(supporter, executor)` - 条件短路，执行第一个支持的成员
- `executeAllSupported(supporter, executor)` - 条件全执行，执行所有支持的成员

**设计优势：**

- 消除重复代码：无需为每个组件类型创建专用 Composite 类
- 灵活组合：支持不同的执行策略和优先级配置
- 类型安全：通过泛型保证类型安全

------

## 3. 组件职责划分

| 组件               | 职责             | 说明                                                         |
| ------------------ | ---------------- | ------------------------------------------------------------ |
| ComponentRegistry  | 组件注册与查询   | 管理所有 Composable 类型组件                                 |
| PluginManager      | 插件生命周期管理 | 负责 Discover → Resolve → Load → Install → Start/Stop/Uninstall |
| BootstrapConfig    | 应用层装配       | 按核心组件大类配置启用策略与优先级                           |
| Composite          | 多实现聚合接口   | 统一规范多实现组件的组合行为                                 |
| GenericComposite   | 通用组合器实现   | 适用于所有 Composable 类型的通用组合实现                     |

### 3.1 PluginManager 设计

`PluginManager` 专注插件生命周期管理，所有依赖通过构造函数注入：

```java
public class PluginManager {

    private final List<PluginSource> sources;
    private final List<PluginLoader> loaders;
    private final List<PluginLifecycleListener> listeners;

    public PluginManager(SpiRegistry spiRegistry,
                         List<PluginSource> sources,
                         List<PluginLoader> loaders,
                         List<PluginLifecycleListener> listeners) {

        this.sources = List.copyOf(sources);
        this.loaders = List.copyOf(loaders);
        this.listeners = List.copyOf(listeners);
    }
}
```

------

## 4. 扩展机制

### 4.1 新增组件类型

步骤：

1. 定义接口（继承 Composable）

```java
public interface NewComponent extends Composable {
    void execute();
}
```

2. 实现具体类

```java
public class NewComponentImpl implements NewComponent {

    @Override
    public void execute() {
        // 实现逻辑
    }
}
```

3. 在配置层注册

```java
registry.register(NewComponent.class, newComponentImpl, 100);
```

4. 使用组件

```java
List<NewComponent> components = registry.getAll(NewComponent.class);
```

------

### 4.2 使用 GenericComposite 组合多实现

**推荐方式：使用 GenericComposite**

```java
// 在配置类中使用 GenericComposite
@Bean
public ComponentRegistry<Composable> descriptorComponents(
        ComponentRegistry<Composable> registry) {

    // 创建组合器
    GenericComposite<PluginDescriptorPathResolver> pathResolvers =
            new GenericComposite<>(PluginDescriptorPathResolver::priority);

    // 添加成员
    pathResolvers.addMember(new PackedDirectoryResolver());
    pathResolvers.addMember(new DevDirectoryResolver());
    pathResolvers.addMember(new JarResolver());

    // 使用组合器创建包装实现
    PluginDescriptorPathResolver resolver = new PluginDescriptorPathResolver() {
        @Override
        public Optional<Path> resolve(Path pluginPath) {
            return pathResolvers.executeFirst(r -> r.resolve(pluginPath));
        }

        @Override
        public int priority() {
            return pathResolvers.getMembers().stream()
                    .mapToInt(PluginDescriptorPathResolver::priority)
                    .max()
                    .orElse(0);
        }
    };

    return registry;
}
```

**特殊场景：自定义 Composite 实现**

仅在以下情况下创建专用 Composite 类：

- 需要复杂的组合逻辑（如状态管理、缓存）
- 需要实现组件接口本身（如 `CompositeLoader implements PluginLoader`）
- 需要特殊的线程安全保证

------

## 5. 配置方式

### 5.1 基础装配（按大类组织）

为避免配置类膨胀，`BootstrapConfig` 按核心组件大类组织配置方法：

```java
@Configuration
public class BootstrapConfig {

    // ==================== 注册中心初始化 ====================

    @Bean
    public SpiRegistry spiRegistry() {
        return new DefaultSpiRegistry();
    }

    @Bean
    public ComponentRegistry<Composable> componentRegistry(PlatformProperties properties) {
        ComponentRegistry<Composable> registry = new DefaultComponentRegistry();

        // 配置各类核心组件
        configureDescriptorComponents(registry, properties);
        configurePluginSourceComponents(registry, properties);
        configurePluginLoaderComponents(registry);
        configureLifecycleListenerComponents(registry);

        return registry;
    }

    // ==================== 插件管理器初始化 ====================

    @Bean
    public PluginManager pluginManager(
            SpiRegistry spiRegistry,
            ComponentRegistry<Composable> registry) {

        List<PluginSource> sources = registry.getAll(PluginSource.class);
        List<PluginLoader> loaders = registry.getAll(PluginLoader.class);
        List<PluginLifecycleListener> listeners = registry.getAll(PluginLifecycleListener.class);

        return new PluginManager(spiRegistry, sources, loaders, listeners);
    }

    // ==================== 各系统核心组件配置 ====================

    private void configureDescriptorComponents(
            ComponentRegistry<Composable> registry,
            PlatformProperties properties) {

        // 1. 配置路径解析器（使用 GenericComposite 组合多个解析器）
        GenericComposite<PluginDescriptorPathResolver> pathResolverComposite =
                new GenericComposite<>(PluginDescriptorPathResolver::priority);

        pathResolverComposite.addMember(new PackedDirectoryResolver());
        pathResolverComposite.addMember(new DevDirectoryResolver());
        pathResolverComposite.addMember(new JarResolver());

        // 2. 配置描述文件读取器（使用 GenericComposite 作为路径解析器）
        // 创建一个实现了 PluginDescriptorPathResolver 的包装类
        PluginDescriptorPathResolver pathResolverWrapper = new PluginDescriptorPathResolver() {
            @Override
            public Optional<Path> resolve(Path pluginPath) {
                return pathResolverComposite.executeFirst(r -> r.resolve(pluginPath));
            }

            @Override
            public int priority() {
                return pathResolverComposite.getMembers().stream()
                        .mapToInt(PluginDescriptorPathResolver::priority)
                        .max()
                        .orElse(0);
            }
        };

        JsonPluginDescriptorParser parser = new JsonPluginDescriptorParser();
        JsonDescriptorReader reader = new JsonDescriptorReader(parser, pathResolverWrapper);

        registry.register(PluginDescriptorReader.class, reader, 0);
    }
    
    private void configurePluginSourceComponents(
            ComponentRegistry<Composable> registry,
            PlatformProperties properties) {

        // 获取描述文件读取器
        PluginDescriptorReader reader = registry.get(PluginDescriptorReader.class)
                .orElseThrow(() -> new IllegalStateException("描述文件读取器未配置"));

        // 本地目录源
        String pluginPath = properties.getPlugin().getPath();
        LocalDirectorySource localSource = new LocalDirectorySource(
                Paths.get(pluginPath), reader);
        registry.register(PluginSource.class, localSource, 100);

        // 类路径源
        ClasspathPluginSource classpathSource =
                new ClasspathPluginSource(new JsonPluginDescriptorParser());
        registry.register(PluginSource.class, classpathSource, 50);
    }

    private void configurePluginLoaderComponents(
            ComponentRegistry<Composable> registry) {

        // JAR 加载器（高优先级）
        registry.register(PluginLoader.class, new JarPluginLoader(), 100);

        // 类路径加载器
        registry.register(PluginLoader.class, new ClasspathPluginLoader(), 50);
    }

    private void configureLifecycleListenerComponents(
            ComponentRegistry<Composable> registry) {

        registry.register(PluginLifecycleListener.class,
                new LoggingLifecycleListener(), 0);
    }
}
```

**设计原则：**

- 按核心组件大类（描述符解析、插件源、加载器、监听器）组织配置方法
- 每个大类一个配置方法，避免为每个具体实现类创建独立 Bean 方法
- 配置方法内部创建和注册组件，保持配置集中
- 方法间通过 `ComponentRegistry` 传递和共享组件

------

### 5.2 条件启用

```java
if (properties.getPlugin().isEnableLocalDirectory()) {
    registry.register(PluginSource.class, localDirectorySource, 100);
}
```

------

### 5.3 运行时动态注册

```java
registry.register(PluginLoader.class, customLoader, 200);
registry.unregister(PluginLoader.class, customLoader);
```

------

## 6. 线程安全设计

`DefaultComponentRegistry` 采用：

- `ConcurrentHashMap`：存储类型到实现集合映射
- `CopyOnWriteArrayList`：存储实现列表
- `AtomicLong`：生成全局注册序号

设计保证：

- 并发注册安全
- 读取过程无锁遍历
- 返回不可变集合防止外部修改

------

## 7. Spring Boot 集成

### 7.1 @SpiImplementation 注解

用于自动注册 SPI 实现。

```java
@SpiImplementation(value = StorageProvider.class, priority = 100)
public class S3StorageProvider implements StorageProvider {
}
```

------

### 7.2 自动注册器

```java
@Component
public class SpiImplementationRegistrar
        implements BeanPostProcessor {

    private final SpiRegistry spiRegistry;

    public SpiImplementationRegistrar(SpiRegistry spiRegistry) {
        this.spiRegistry = spiRegistry;
    }

    @Override
    public Object postProcessAfterInitialization(
            Object bean, String beanName) {

        SpiImplementation annotation =
                AnnotationUtils.findAnnotation(
                        bean.getClass(),
                        SpiImplementation.class);

        if (annotation != null) {
            Class<?> spiType = annotation.value();
            if (spiType == Void.class) {
                spiType = inferSpiType(bean.getClass());
            }

            if (spiType != null && spiType.isInstance(bean)) {
                spiRegistry.register(
                        spiType,
                        bean,
                        annotation.priority());
            }
        }

        return bean;
    }
}
```

------

## 8. 设计约束与实践规范

### 8.1 组件设计规范

- 所有多实现核心组件必须实现 `Composable`
- 接口职责单一，避免聚合多种语义
- 实现类应保持无状态或线程安全

### 8.2 优先级策略建议

- 默认实现：中等优先级（如 50）
- 覆盖实现：高优先级（如 100）
- 兜底实现：低优先级（如 0–25）
- 建议为每类组件定义优先级区间

### 8.3 注册规范

- 组件注册集中在应用配置层
- 禁止在组件内部自行完成注册
- 所有依赖通过构造函数注入
- 优先使用 `GenericComposite` 组合多实现，避免创建冗余的 Composite 类
- 配置类按核心组件大类组织，避免配置方法膨胀
- 组合逻辑通过 `Composite` 或 `GenericComposite` 实现，不得在调用方手写遍历逻辑

------

本架构形成了以 `ComponentRegistry` 为核心的统一组件管理体系，实现了插件生命周期管理与组件扩展机制的清晰分离，为系统提供可预测、可扩展、可治理的 SPI 基础设施能力。
