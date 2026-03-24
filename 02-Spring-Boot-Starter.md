# Spring Boot Starter

## 本章已驗證範例

- 對應專案：`examples/demo-greeting-spring-boot-starter` + `examples/boot2-tutorial-app`
- 主要程式：`demo-greeting-spring-boot-starter/pom.xml`、`GreetingController`
- 驗證測試：`GreetingAutoConfigurationTest`、`GreetingControllerTest`
- 校正重點：Starter 章節已拆成 `autoconfigure` 與 `starter` 兩個模組，並由主應用程式實際引用與驗證。


## 什麼是 Starter？

Spring Boot Starter 是一組**預先整合好的依賴集合**（Dependency Descriptor）。

傳統 Spring 開發需要手動管理數十個依賴的版本相容性，而 Starter 讓你只需加入一個依賴，就能獲得該功能所需的所有函式庫，且版本已預先測試過相容。

---

## Starter 命名規則

| 類型 | 命名格式 | 範例 |
|------|---------|------|
| 官方 Starter | `spring-boot-starter-{功能}` | `spring-boot-starter-web` |
| 第三方 Starter | `{名稱}-spring-boot-starter` | `mybatis-spring-boot-starter` |

---

## 常用官方 Starter 一覽

### Web 開發

```xml
<!-- Spring MVC Web 開發（含 Tomcat） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- WebFlux 響應式 Web（Reactor） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- WebSocket 支援 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 資料存取

```xml
<!-- Spring Data JPA（Hibernate） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Spring Data MongoDB -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>

<!-- Spring Data Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Spring Data Elasticsearch -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>

<!-- JDBC 模板 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

### 安全性

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- OAuth2 Client -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

### 模板引擎

```xml
<!-- Thymeleaf 模板引擎 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- FreeMarker 模板引擎 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-freemarker</artifactId>
</dependency>
```

### 訊息傳遞

```xml
<!-- ActiveMQ 訊息佇列 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-activemq</artifactId>
</dependency>

<!-- RabbitMQ -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- Apache Kafka -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-kafka</artifactId>
</dependency>
```

### 快取與驗證

```xml
<!-- 快取支援（EhCache、Redis 等） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Bean Validation（Hibernate Validator） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### 監控與測試

```xml
<!-- Actuator 監控端點 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- 測試（JUnit 5、Mockito） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Starter 包含的內容

以 `spring-boot-starter-web` 為例，它實際上包含：

```
spring-boot-starter-web
├── spring-boot-starter                    ← 核心 Starter
│   ├── spring-boot                        ← Spring Boot 核心
│   ├── spring-boot-autoconfigure          ← 自動配置
│   └── spring-boot-starter-logging        ← 日誌（Logback）
├── spring-boot-starter-json               ← JSON 支援（Jackson）
├── spring-boot-starter-tomcat             ← 內嵌 Tomcat
├── spring-web                             ← Spring Web 核心
└── spring-webmvc                          ← Spring MVC
```

你只需要宣告一個依賴，就自動取得以上所有函式庫！

---

## 更換內嵌伺服器

`spring-boot-starter-web` 預設使用 **Tomcat**，可以替換成其他伺服器：

### 換成 Jetty

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- 排除預設的 Tomcat -->
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 加入 Jetty -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

### 換成 Undertow

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

---

## 換成 Log4j2 日誌

預設使用 Logback，更換成 Log4j2：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <!-- 排除預設的 Logback -->
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 加入 Log4j2 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
```

---

## spring-boot-starter-parent 的作用

在 `pom.xml` 繼承 `spring-boot-starter-parent`：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>
```

它提供：

1. **依賴版本管理（BOM）**：所有常用依賴版本預先定義，不需要寫 `<version>`
2. **Maven 插件預設設定**：compiler plugin、resource plugin 等
3. **資源過濾**：`application.properties` 的 `@...@` 佔位符支援
4. **Java 版本設定**：透過 `<java.version>` 屬性設定

### 不使用 parent POM（企業環境常見）

如果公司已有自己的 parent POM，改用 `dependencyManagement`：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>2.7.18</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 建立自定義 Starter

當你有一段常用的功能（如：公司內部的日誌格式、統一回傳格式），可以封裝成 Starter。

### 專案結構

```
my-spring-boot-starter/
├── my-spring-boot-starter-autoconfigure/  ← 自動配置模組
│   └── src/main/java/.../
│       ├── MyService.java
│       ├── MyProperties.java
│       └── MyAutoConfiguration.java
└── my-spring-boot-starter/               ← Starter 模組（只有 pom.xml）
    └── pom.xml
```

### 自動配置模組的 pom.xml

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-autoconfigure</artifactId>
</dependency>
<!-- 可選：讓 IDE 能夠提示屬性 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

### 使用者在自己的 pom.xml 加入

```xml
<dependency>
    <groupId>com.mycompany</groupId>
    <artifactId>my-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 查看依賴樹

```bash
# Maven：查看完整依賴樹
mvn dependency:tree

# 只查看特定 scope
mvn dependency:tree -Dscope=compile

# Gradle：查看依賴樹
./gradlew dependencies
```

---

## 重點整理

- Starter 是「預打包的依賴集合」，解決版本相容性問題
- 命名規則：官方用 `spring-boot-starter-*`，第三方用 `*-spring-boot-starter`
- 可以用 `<exclusions>` 排除 Starter 內的某個依賴（如換伺服器、換日誌）
- `spring-boot-starter-parent` 提供版本管理，建議繼承它
- 可以自己撰寫 Starter，封裝公司共用功能

---

## 相關文章

- [[01-自動配置-Auto-Configuration]]
- [[03-應用程式配置-Properties-and-YAML]]
- [[04-Spring-MVC-RESTful-API]]
