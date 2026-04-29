# 添加 Web 端点

------

## 1. Web 接入机制概述

Web 接入机制允许插件将自定义的 Web 控制器暴露为 HTTP 端点。平台负责将插件的控制器动态注册到 Web 框架（如 Spring MVC），并在插件生命周期内自动管理端点的注册与卸载。

支持两种控制器提供方式：

- **自动扫描**（推荐）：通过 `@EnablePluginWebEndpoints` 注解启用，平台自动发现并注册控制器
- **显式提供**：通过实现 `WebControllerProvider` 接口手动提供控制器

------

## 2. 自动扫描模式（推荐）

### 2.1 基本用法

插件通过在主类上添加 `@EnablePluginWebEndpoints` 注解启用自动扫描：

```java
import com.nexusadmin.api.extension.web.EnableWebEndpoints;
import com.nexusadmin.core.AbstractPlugin;

@EnablePluginWebEndpoints
public class MyPlugin extends AbstractPlugin {
    // 平台会自动扫描主类所在包及其子包下的控制器
}
```

### 2.2 指定扫描包

可以通过 `basePackages` 或 `basePackageClasses` 指定扫描路径：

```java
@EnablePluginWebEndpoints(basePackages = "com.example.plugin.web")
public class MyPlugin extends AbstractPlugin {
    // 扫描指定包及其子包
}
```

或使用类推导：

```java
@EnablePluginWebEndpoints(basePackageClasses = MyController.class)
public class MyPlugin extends AbstractPlugin {
    // 扫描 MyController 所在包及其子包
}
```

### 2.3 控制器示例

使用标准的 Spring MVC 注解定义控制器：

```java
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> list() {
        return userService.listAll();
    }

    @GetMapping("/{id}")
    public User get(@PathVariable String id) {
        return userService.findById(id);
    }
}
```

**特点**：

- 无需实现 `WebControllerProvider` 接口
- 平台自动扫描并实例化控制器
- 支持构造器注入，依赖可从平台 ApplicationContext 获取

------

## 3. 显式提供模式

### 3.1 基本用法

插件通过实现 `WebControllerProvider` 接口显式提供控制器：

```java
public class MyPlugin extends AbstractPlugin implements WebControllerProvider {

    @Override
    public List<Object> getControllers() {
        return List.of(new MyController());
    }
}
```

### 3.2 使用场景

- 需要完全控制控制器的创建过程
- 控制器依赖需要特殊处理
- 需要动态决定提供哪些控制器

### 3.3 使用 Spring MVC 注解

控制器可以使用标准的 Spring MVC 注解定义路由：

```java
@RestController
@RequestMapping("/api")
public class MyController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from plugin!";
    }

    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        return userService.save(user);
    }
}
```

------

## 4. 路径隔离机制

### 3.1 默认路径前缀

插件的 Web 端点自动添加路径前缀，避免不同插件之间的路径冲突：

| 类型 | 路径前缀 | 示例 |
|------|----------|------|
| 普通插件 API | `/api/**` | `/api/my-plugin/api/hello` |
| 管理面板 API | `/admin/**` | `/admin/plugins` |

### 3.2 使用 @AdminApi 注解

标记控制器为管理面板 API，使其映射到 `/admin` 路径前缀：

```java
@RestController
@AdminApi
@RequestMapping("/plugins")
public class PluginManageController {

    @GetMapping
    public List<PluginView> listAll() {
        return pluginAdminFacade.listAll();
    }
}
```

**实际访问路径**：`/admin/plugins`

### 3.3 路径计算示例

```java
// 插件ID: order-plugin
@RestController
@RequestMapping("/orders")
public class OrderController {

    @GetMapping("/list")
    public List<Order> list() { ... }
}
```

**实际访问路径**：`/api/orders/list`

------

## 5. 依赖注入

### 5.1 构造函数注入

控制器通过构造函数接收依赖：

```java
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable String id) {
        return userService.findById(id);
    }
}
```

### 5.2 在插件中创建控制器

在 `getControllers()` 方法中注入依赖：

```java
public class MyPlugin extends AbstractPlugin implements WebControllerProvider {

    private UserService userService;

    @Override
    protected void start() throws Exception {
        // 初始化服务
        userService = new UserService(config());
    }

    @Override
    public List<Object> getControllers() {
        return List.of(
            new UserController(userService),
            new OrderController(orderService)
        );
    }
}
```

------

## 6. 延迟初始化

### 6.1 解决依赖就绪问题

如果插件初始化时依赖尚未就绪，可以延迟获取：

```java
public class MyPlugin extends AbstractPlugin implements WebControllerProvider {

    private AdminFacade adminFacade;

    @Override
    protected void initialize() throws Exception {
        // 初始化阶段可能无法获取依赖
        adminFacade = service(AdminFacade.class).orElse(null);
    }

    @Override
    public List<Object> getControllers() {
        // 延迟获取依赖
        if (adminFacade == null) {
            adminFacade = service(AdminFacade.class).orElse(null);
        }
        
        if (adminFacade == null) {
            return List.of();  // 依赖未就绪，返回空列表
        }
        
        return List.of(new MyController(adminFacade));
    }
}
```

------

## 7. 完整示例

### 7.1 自动扫描模式示例

```java
package com.nexusadmin.plugin.demo;

import com.nexusadmin.api.extension.web.EnableWebEndpoints;
import com.nexusadmin.core.AbstractPlugin;

/**
 * 演示插件。
 */
@EnablePluginWebEndpoints
public class DemoPlugin extends AbstractPlugin {
    // 平台自动扫描 com.nexusadmin.plugin.demo 包及其子包
}
```

### 7.2 控制器实现

```java
package com.nexusadmin.plugin.demo.controller;

import com.nexusadmin.plugin.demo.entity.User;
import com.nexusadmin.plugin.demo.service.DemoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 演示控制器。
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello from DemoPlugin!";
    }

    @GetMapping("/users")
    public List<User> listUsers() {
        return demoService.listUsers();
    }

    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        return demoService.createUser(user);
    }

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable String id) {
        return demoService.getUser(id);
    }
}
```

### 7.3 访问端点

插件启动后，可通过以下路径访问：

```
GET    /api/demo/hello
GET    /api/demo/users
POST   /api/demo/users
GET    /api/demo/users/{id}
```

------

## 8. 管理面板端点示例

### 8.1 实现管理 API

```java
@RestController
@AdminApi
@RequestMapping("/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public Stats getStats() {
        return statsService.getCurrentStats();
    }
}
```

### 8.2 访问管理端点

```
GET /admin/stats
```

**注意**：管理面板端点需要认证。默认使用引导认证（配置 `platform.auth.bootstrap.username/password`），也可通过浏览器访问 `/admin/login` 登录。

------

## 9. 注意事项

- **路径冲突**：避免与其他插件的路径重复，建议使用插件特有的路径前缀
- **依赖注入**：自动扫描模式下支持构造器注入；显式提供模式下应在 `getControllers()` 中注入依赖
- **生命周期**：端点会在插件激活时自动注册，停止时自动卸载
- **线程安全**：控制器应设计为无状态或线程安全
- **异常处理**：建议统一处理控制器异常，返回标准错误响应
- **推荐模式**：优先使用自动扫描模式，代码更简洁，接近 Spring Boot 开发体验

------

## 10. 参考文档

- [Web 接入机制](../developer-guid/设计文档/Web接入机制.md)
- [开发插件](开发插件.md)
