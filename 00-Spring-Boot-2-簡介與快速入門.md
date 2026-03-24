# Spring Boot 2 簡介與快速入門

## 本章已驗證範例

- 對應專案：`examples/boot2-tutorial-app`
- 主要程式：`TutorialApplication`、`GreetingController`
- 驗證測試：`GreetingControllerTest`
- 校正重點：快速入門段落現在對齊實際可執行的 Boot 2 範例專案，入口程式與 `/api/greetings/{name}` 端點都已測試。


## 什麼是 Spring Boot？

Spring Boot 是由 Pivotal 團隊開發的框架，建立在 Spring Framework 之上，目標是讓開發者能夠**快速建立可獨立執行的 Spring 應用程式**，不需要複雜的 XML 設定。

### Spring Boot 的核心優勢

- **開箱即用（Opinionated Defaults）**：提供合理的預設設定，減少樣板程式碼
- **內嵌伺服器**：內建 Tomcat、Jetty 或 Undertow，無需部署 WAR 檔案
- **自動配置（Auto-configuration）**：根據 classpath 自動設定 Spring
- **獨立執行**：打包成可執行的 JAR 檔案，直接用 `java -jar` 執行
- **豐富的 Actuator**：提供生產環境監控與管理功能

---

## 環境需求

| 工具 | 版本需求 |
|------|---------|
| Java | 8 或 11（Spring Boot 2.x 支援） |
| Maven | 3.3+ |
| Gradle | 4.4+ |
| Spring Boot | 2.x |

---

## 建立第一個 Spring Boot 專案

### 方法一：使用 Spring Initializr（推薦初學者）

1. 開啟瀏覽器，前往 [https://start.spring.io](https://start.spring.io)
2. 填寫專案資訊：
   - **Project**：Maven Project
   - **Language**：Java
   - **Spring Boot**：2.7.x
   - **Group**：`com.example`
   - **Artifact**：`demo`
   - **Packaging**：Jar
   - **Java**：8
3. 加入依賴（Dependencies）：搜尋並加入 **Spring Web**
4. 點選 **GENERATE** 下載壓縮檔
5. 解壓縮並用 IDE（IntelliJ IDEA 或 Eclipse）開啟

### 方法二：Maven pom.xml 手動建立

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 繼承 Spring Boot 父 POM，取得預設依賴管理 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>demo</name>
    <description>Spring Boot 2 入門範例</description>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <!-- Web 開發 Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 測試 Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven Plugin，用於打包可執行 JAR -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

---

## 專案結構說明

```
demo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/demo/
│   │   │       └── DemoApplication.java      ← 主程式入口
│   │   └── resources/
│   │       ├── application.properties        ← 應用程式設定檔
│   │       ├── static/                       ← 靜態資源（CSS、JS、圖片）
│   │       └── templates/                    ← 模板檔案（Thymeleaf 等）
│   └── test/
│       └── java/
│           └── com/example/demo/
│               └── DemoApplicationTests.java ← 測試類別
├── pom.xml                                   ← Maven 設定檔
└── HELP.md
```

---

## 主程式入口：DemoApplication.java

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication 是三個注解的組合：
// @SpringBootConfiguration - 標記為 Spring 配置類
// @EnableAutoConfiguration - 啟用自動配置
// @ComponentScan           - 掃描當前包及子包的 Bean
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        // 啟動 Spring 應用程式
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

---

## 撰寫第一個 REST API

建立一個 Controller 類別：

```java
package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// @RestController = @Controller + @ResponseBody
// 表示此類別的所有方法都直接回傳資料（JSON/文字），不走視圖解析
@RestController
public class HelloController {

    // 對應 GET /hello 請求
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot 2!";
    }

    // 對應 GET /greet?name=xxx 請求
    @GetMapping("/greet")
    public String greet(@RequestParam(defaultValue = "World") String name) {
        return "Hello, " + name + "!";
    }
}
```

---

## 啟動應用程式

### 使用 Maven 啟動
```bash
./mvnw spring-boot:run
```

### 使用 IDE 啟動
直接執行 `DemoApplication.java` 的 `main()` 方法

### 啟動成功後的 Console 輸出
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v2.7.18)

2024-01-01 10:00:00.000  INFO --- [main] c.e.demo.DemoApplication : Starting DemoApplication
2024-01-01 10:00:02.000  INFO --- [main] o.s.b.w.e.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080
2024-01-01 10:00:02.000  INFO --- [main] c.e.demo.DemoApplication : Started DemoApplication in 2.5 seconds
```

---

## 測試 API

開啟瀏覽器或使用 curl：

```bash
# 測試 /hello
curl http://localhost:8080/hello
# 輸出：Hello, Spring Boot 2!

# 測試 /greet
curl http://localhost:8080/greet?name=Java
# 輸出：Hello, Java!
```

---

## 回傳 JSON 物件

實際開發中，API 通常回傳 JSON 格式的資料。建立一個 User 類別：

```java
package com.example.demo.model;

// 普通的 Java 類別（POJO）
public class User {
    private Long id;
    private String name;
    private String email;

    // 建構子
    public User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    // Getter 和 Setter（Spring 需要這些來序列化成 JSON）
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

在 Controller 中使用：

```java
package com.example.demo.controller;

import com.example.demo.model.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/users")  // 設定此 Controller 的共用路徑前綴
public class UserController {

    // GET /api/users
    @GetMapping
    public List<User> getAllUsers() {
        return Arrays.asList(
            new User(1L, "Alice", "alice@example.com"),
            new User(2L, "Bob",   "bob@example.com")
        );
    }

    // GET /api/users/1
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return new User(id, "Alice", "alice@example.com");
    }
}
```

存取 `http://localhost:8080/api/users` 會得到：

```json
[
  {"id": 1, "name": "Alice", "email": "alice@example.com"},
  {"id": 2, "name": "Bob",   "email": "bob@example.com"}
]
```

---

## application.properties 基本設定

```properties
# 修改伺服器埠號（預設 8080）
server.port=8081

# 設定應用程式名稱
spring.application.name=demo-app

# 設定時區
spring.jackson.time-zone=Asia/Taipei
```

---

## 重點整理

| 注解 | 說明 |
|------|------|
| `@SpringBootApplication` | 主程式入口，組合了三個注解 |
| `@RestController` | 標記 REST API 控制器 |
| `@GetMapping` | 對應 HTTP GET 請求 |
| `@PostMapping` | 對應 HTTP POST 請求 |
| `@RequestParam` | 取得 URL 查詢參數 |
| `@PathVariable` | 取得 URL 路徑變數 |
| `@RequestMapping` | 設定路由前綴 |

---

## 下一步

- [[01-自動配置-Auto-Configuration]]：了解 Spring Boot 如何自動配置元件
- [[02-Spring-Boot-Starter]]：深入了解 Starter 依賴機制
- [[03-應用程式配置-Properties-and-YAML]]：學習如何管理應用程式設定
