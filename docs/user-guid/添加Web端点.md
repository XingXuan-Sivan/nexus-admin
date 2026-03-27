# 添加 Web 端点

------

## 1. Web 接入机制概述

Web 接入机制允许插件通过实现 `WebControllerProvider` 接口，将自定义的 Web 控制器暴露为 HTTP 端点。平台负责将插件的控制器动态注册到 Web 框架（如 Spring MVC），并在插件生命周期内自动管理端点的注册与卸载。

------

## 2. 实现 WebControllerProvider

### 2.1 基本用法

插件通过实现 `WebControllerProvider` 接口提供控制器：

```java
public class MyPlugin extends AbstractPlugin implements WebControllerProvider {

    @Override
    public List<Object> getControllers() {
        return List.of(new MyController());
    }
}
```

### 2.2 使用 Spring MVC 注解

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

## 3. 路径隔离机制

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

## 4. 依赖注入

### 4.1 构造函数注入

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

### 4.2 在插件中创建控制器

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

## 5. 延迟初始化

### 5.1 解决依赖就绪问题

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

## 6. 完整示例

### 6.1 插件实现

```java
package com.nexusadmin.plugin.demo;

import com.nexusadmin.api.extension.web.WebControllerProvider;
import com.nexusadmin.core.AbstractPlugin;
import com.nexusadmin.plugin.demo.controller.DemoController;
import com.nexusadmin.plugin.demo.service.DemoService;

import java.util.List;

/**
 * 演示插件。
 */
public class DemoPlugin extends AbstractPlugin implements WebControllerProvider {

    private DemoService demoService;

    @Override
    protected void initialize() throws Exception {
        demoService = new DemoService(config());
    }

    @Override
    protected void start() throws Exception {
        demoService.start();
    }

    @Override
    protected void stop() throws Exception {
        demoService.stop();
    }

    @Override
    public List<Object> getControllers() {
        return List.of(new DemoController(demoService));
    }
}
```

### 6.2 控制器实现

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

### 6.3 访问端点

插件启动后，可通过以下路径访问：

```
GET    /api/demo/hello
GET    /api/demo/users
POST   /api/demo/users
GET    /api/demo/users/{id}
```

------

## 7. 管理面板端点示例

### 7.1 实现管理 API

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

### 7.2 访问管理端点

```
GET /admin/stats
```

**注意**：管理面板端点需要 Basic 认证，默认用户名密码为 `admin` / `admin123`。

------

## 8. 注意事项

- **路径冲突**：避免与其他插件的路径重复，建议使用插件特有的路径前缀
- **依赖注入**：控制器依赖应在 `getControllers()` 中注入，确保依赖已就绪
- **生命周期**：端点会在插件激活时自动注册，停止时自动卸载
- **线程安全**：控制器应设计为无状态或线程安全
- **异常处理**：建议统一处理控制器异常，返回标准错误响应

------

## 9. 参考文档

- [Web 接入机制](../developer-guid/设计文档/Web接入机制.md)
- [开发插件](开发插件.md)
