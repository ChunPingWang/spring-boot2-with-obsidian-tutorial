# Spring MVC 與 RESTful API

## REST 是什麼？

**REST（Representational State Transfer）** 是一種 API 設計風格，遵循以下原則：

- 使用 **HTTP 方法** 表示操作（GET、POST、PUT、DELETE）
- 使用 **URL** 表示資源（名詞，而非動詞）
- 回傳 **JSON** 或 XML 格式資料
- **無狀態**：每次請求都包含完整資訊，伺服器不保存 Session

### RESTful API 設計範例

| HTTP 方法 | URL | 操作說明 |
|-----------|-----|---------|
| GET | `/api/users` | 取得所有使用者 |
| GET | `/api/users/1` | 取得 ID=1 的使用者 |
| POST | `/api/users` | 新增使用者 |
| PUT | `/api/users/1` | 更新 ID=1 的使用者（完整更新） |
| PATCH | `/api/users/1` | 更新 ID=1 的使用者（部分更新） |
| DELETE | `/api/users/1` | 刪除 ID=1 的使用者 |

---

## 加入依賴

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

---

## 建立 REST API：完整範例

### 1. 模型類別（Model）

```java
package com.example.demo.model;

public class User {
    private Long id;
    private String name;
    private String email;
    private int age;

    // 無參數建構子（JSON 反序列化需要）
    public User() {}

    public User(Long id, String name, String email, int age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}
```

### 2. Service 層（業務邏輯）

```java
package com.example.demo.service;

import com.example.demo.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {

    // 模擬資料庫（實際應使用 JPA）
    private final Map<Long, User> userDb = new HashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public UserService() {
        // 初始化測試資料
        User u1 = new User(1L, "Alice", "alice@example.com", 28);
        User u2 = new User(2L, "Bob",   "bob@example.com",   32);
        userDb.put(1L, u1);
        userDb.put(2L, u2);
        idCounter.set(3);
    }

    public List<User> findAll() {
        return new ArrayList<>(userDb.values());
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userDb.get(id));
    }

    public User save(User user) {
        user.setId(idCounter.getAndIncrement());
        userDb.put(user.getId(), user);
        return user;
    }

    public Optional<User> update(Long id, User updatedUser) {
        if (!userDb.containsKey(id)) {
            return Optional.empty();
        }
        updatedUser.setId(id);
        userDb.put(id, updatedUser);
        return Optional.of(updatedUser);
    }

    public boolean delete(Long id) {
        return userDb.remove(id) != null;
    }
}
```

### 3. Controller 層（接收請求）

```java
package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // 建構子注入（推薦方式）
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/users → 取得所有使用者
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    // GET /api/users/1 → 取得指定使用者
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.findById(id)
            // 找到 → 回傳 200 OK 與使用者資料
            .map(ResponseEntity::ok)
            // 找不到 → 回傳 404 Not Found
            .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/users → 新增使用者
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userService.save(user);
        // 回傳 201 Created 與新建的使用者
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // PUT /api/users/1 → 更新使用者
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User user) {

        return userService.update(id, user)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/users/1 → 刪除使用者
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.delete(id)) {
            // 成功 → 回傳 204 No Content
            return ResponseEntity.noContent().build();
        }
        // 找不到 → 回傳 404 Not Found
        return ResponseEntity.notFound().build();
    }
}
```

---

## 常用注解說明

### 路由注解

```java
@RequestMapping("/api")          // 設定路徑前綴，可用於類別或方法
@GetMapping("/users")            // HTTP GET
@PostMapping("/users")           // HTTP POST
@PutMapping("/users/{id}")       // HTTP PUT
@PatchMapping("/users/{id}")     // HTTP PATCH
@DeleteMapping("/users/{id}")    // HTTP DELETE
```

### 參數注解

```java
// 路徑變數：/users/1 → id = 1
@GetMapping("/{id}")
public User get(@PathVariable Long id) { ... }

// 查詢參數：/users?page=0&size=10
@GetMapping
public List<User> list(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size) { ... }

// 請求體（JSON）
@PostMapping
public User create(@RequestBody User user) { ... }

// 請求標頭
@GetMapping
public String header(@RequestHeader("Authorization") String token) { ... }

// Cookie
@GetMapping
public String cookie(@CookieValue("sessionId") String sessionId) { ... }
```

---

## ResponseEntity 使用指南

`ResponseEntity` 讓你完全控制 HTTP 回應（狀態碼、標頭、Body）：

```java
// 200 OK + Body
ResponseEntity.ok(data);

// 200 OK + 自定義 Header + Body
ResponseEntity.ok()
    .header("X-Custom-Header", "value")
    .body(data);

// 201 Created + Location Header
ResponseEntity.created(URI.create("/api/users/1"))
    .body(newUser);

// 204 No Content
ResponseEntity.noContent().build();

// 400 Bad Request
ResponseEntity.badRequest().body("Invalid input");

// 404 Not Found
ResponseEntity.notFound().build();

// 500 Internal Server Error
ResponseEntity.internalServerError().body("Server error");

// 自定義狀態碼
ResponseEntity.status(HttpStatus.CONFLICT).body("Resource already exists");
ResponseEntity.status(422).body("Unprocessable entity");
```

---

## 查詢參數進階用法

```java
// GET /api/users?name=alice&age=28&sort=name,asc&page=0&size=10
@GetMapping
public List<User> searchUsers(
    @RequestParam(required = false) String name,
    @RequestParam(required = false) Integer age,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "asc") String sortDir,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size) {

    // 業務邏輯...
    return userService.search(name, age, sortBy, sortDir, page, size);
}
```

---

## 統一回傳格式

實際專案通常會統一 API 回傳格式：

```java
package com.example.demo.common;

// 統一回傳結構
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功回傳
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "Success", data);
    }

    // 成功（無資料）
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "Success", null);
    }

    // 失敗回傳
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // Getters...
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
```

使用統一格式的 Controller：

```java
@GetMapping
public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
    List<User> users = userService.findAll();
    return ResponseEntity.ok(ApiResponse.success(users));
}

@PostMapping
public ResponseEntity<ApiResponse<User>> createUser(@RequestBody User user) {
    User saved = userService.save(user);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(saved));
}
```

回傳 JSON：
```json
{
  "code": 200,
  "message": "Success",
  "data": [
    {"id": 1, "name": "Alice", "email": "alice@example.com"}
  ]
}
```

---

## 跨域（CORS）設定

```java
// 方法一：在方法或類別上加注解
@CrossOrigin(origins = "http://localhost:3000")
@RestController
public class UserController { ... }

// 方法二：全域設定（推薦）
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000", "http://localhost:4200")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);  // 預檢請求快取時間（秒）
    }
}
```

---

## 設定訊息轉換器（Jackson）

```properties
# application.properties

# 日期格式
spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
spring.jackson.time-zone=Asia/Taipei

# 忽略 null 欄位
spring.jackson.default-property-inclusion=non_null

# 美化輸出（開發時使用）
spring.jackson.serialization.indent-output=true

# 未知欄位不拋例外
spring.jackson.deserialization.fail-on-unknown-properties=false
```

---

## 常用 HTTP 狀態碼

| 狀態碼 | 說明 | 使用情境 |
|--------|------|---------|
| 200 OK | 成功 | GET、PUT 成功 |
| 201 Created | 建立成功 | POST 成功新增資源 |
| 204 No Content | 無內容 | DELETE 成功、PUT 無需回傳 |
| 400 Bad Request | 請求格式錯誤 | 參數驗證失敗 |
| 401 Unauthorized | 未認證 | 未登入 |
| 403 Forbidden | 無權限 | 登入但無存取權限 |
| 404 Not Found | 找不到 | 資源不存在 |
| 409 Conflict | 衝突 | 資源已存在（重複新增） |
| 422 Unprocessable | 無法處理 | 業務邏輯驗證失敗 |
| 500 Internal Error | 伺服器錯誤 | 未預期的例外 |

---

## 用 curl 測試 API

```bash
# GET 所有使用者
curl http://localhost:8080/api/users

# GET 單一使用者
curl http://localhost:8080/api/users/1

# POST 新增使用者
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Charlie","email":"charlie@example.com","age":25}'

# PUT 更新使用者
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice Updated","email":"alice@example.com","age":29}'

# DELETE 刪除使用者
curl -X DELETE http://localhost:8080/api/users/1
```

---

## 重點整理

- `@RestController` = `@Controller` + `@ResponseBody`
- 使用 `ResponseEntity` 完整控制 HTTP 回應
- `@PathVariable` 取路徑參數，`@RequestParam` 取查詢參數，`@RequestBody` 取請求體
- 遵循 REST 語義：GET 查詢、POST 新增、PUT 更新、DELETE 刪除
- 全域 CORS 設定使用 `WebMvcConfigurer`
- 統一回傳格式（ApiResponse）讓前端更容易處理

---

## 相關文章

- [[03-應用程式配置-Properties-and-YAML]]
- [[05-Spring-Data-JPA]]
- [[10-異常處理-Exception-Handling]]
- [[11-驗證-Validation]]
