# 應用程式配置（Properties & YAML）

## 本章已驗證範例

- 對應專案：`examples/boot2-tutorial-app`
- 主要程式：`application.properties`、`TutorialProperties`
- 驗證測試：`TutorialPropertiesTest`
- 校正重點：本章已對齊外部化設定的實際綁定範例，示範 `properties` 讀入 POJO；若要示範 YAML，可沿用相同屬性結構。


## 設定檔概述

Spring Boot 的應用程式設定儲存在 `src/main/resources/` 目錄，支援兩種格式：

| 格式         | 檔案名稱                     | 特點                   |
| ---------- | ------------------------ | -------------------- |
| Properties | `application.properties` | 簡單的 key=value，適合簡單設定 |
| YAML       | `application.yml`        | 階層式結構，可讀性高，適合複雜設定    |

兩種格式功能相同，可依個人喜好選擇。本文會同時展示兩種寫法。

---

## 基本設定範例

### application.properties

```properties
# 伺服器設定
server.port=8080
server.servlet.context-path=/api

# 應用程式名稱
spring.application.name=my-app

# 資料庫設定
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=secret

# JPA 設定
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# 日誌等級
logging.level.root=INFO
logging.level.com.example=DEBUG
```

### application.yml（等效設定）

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  application:
    name: my-app
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: secret
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect

logging:
  level:
    root: INFO
    com.example: DEBUG
```

---

## 自定義設定屬性

### 1. 在 application.properties 定義

```properties
# 自定義設定
app.name=My Spring Boot App
app.version=1.0.0
app.max-retry=3
app.timeout=5000
app.feature.enabled=true
```

### 2. 用 @Value 注入單一屬性

```java
package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AppInfoService {

    // 注入 app.name 的值
    @Value("${app.name}")
    private String appName;

    // 注入時提供預設值（屬性不存在時使用）
    @Value("${app.version:0.0.1}")
    private String version;

    // 注入整數
    @Value("${app.max-retry:3}")
    private int maxRetry;

    // 注入布林值
    @Value("${app.feature.enabled:false}")
    private boolean featureEnabled;

    // 注入 Spring EL 表達式
    @Value("#{${app.timeout} * 1000}")
    private long timeoutMs;

    public String getAppInfo() {
        return String.format("App: %s v%s (retry=%d, feature=%b)",
            appName, version, maxRetry, featureEnabled);
    }
}
```

### 3. 用 @ConfigurationProperties 注入一組屬性（推薦）

對於多個相關屬性，使用 `@ConfigurationProperties` 更加優雅：

```properties
# application.properties
app.mail.host=smtp.example.com
app.mail.port=587
app.mail.username=noreply@example.com
app.mail.password=mailpass
app.mail.from=noreply@example.com
app.mail.use-tls=true
```

```java
package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 對應 application.properties 中 app.mail.* 的所有屬性
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    private String host;
    private int port;
    private String username;
    private String password;
    private String from;
    private boolean useTls;

    // 必須提供 Getter 和 Setter
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public boolean isUseTls() { return useTls; }
    public void setUseTls(boolean useTls) { this.useTls = useTls; }
}
```

在 Service 中使用：

```java
@Service
public class MailService {

    private final MailProperties mailProperties;

    // 建構子注入（推薦）
    public MailService(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    public void sendMail(String to, String subject, String body) {
        System.out.println("Sending mail via " + mailProperties.getHost()
            + ":" + mailProperties.getPort());
        // 實際發送郵件邏輯...
    }
}
```

---

## 多環境配置 Profile

實際專案通常有多個環境（開發、測試、生產），使用 Profile 管理各環境設定。

### 設定檔命名規則

```
application.properties          ← 所有環境共用
application-dev.properties      ← 開發環境
application-test.properties     ← 測試環境
application-prod.properties     ← 生產環境
```

### 各環境設定範例

**application.properties（共用設定）**
```properties
spring.application.name=my-app
server.port=8080
```

**application-dev.properties（開發環境）**
```properties
# 開發環境使用 H2 記憶體資料庫
spring.datasource.url=jdbc:h2:mem:devdb
spring.datasource.driver-class-name=org.h2.Driver
spring.h2.console.enabled=true

spring.jpa.show-sql=true
logging.level.com.example=DEBUG
```

**application-prod.properties（生產環境）**
```properties
# 生產環境使用 MySQL
spring.datasource.url=jdbc:mysql://prod-db:3306/mydb
spring.datasource.username=${DB_USER}       ← 從環境變數讀取
spring.datasource.password=${DB_PASSWORD}   ← 從環境變數讀取

spring.jpa.show-sql=false
logging.level.com.example=WARN
```

### 啟用 Profile

**方法一：在 application.properties 設定**
```properties
spring.profiles.active=dev
```

**方法二：啟動時加上 JVM 參數**
```bash
java -jar app.jar --spring.profiles.active=prod
```

**方法三：設定環境變數**
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar
```

**方法四：在程式碼中設定**
```java
SpringApplication app = new SpringApplication(DemoApplication.class);
app.setAdditionalProfiles("dev");
app.run(args);
```

### YAML 格式的多 Profile（單一檔案）

```yaml
# application.yml

# 共用設定
spring:
  application:
    name: my-app

---
# 開發環境（使用 --- 分隔不同 Profile）
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:h2:mem:devdb

logging:
  level:
    com.example: DEBUG

---
# 生產環境
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:mysql://prod-db:3306/mydb
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

---

## 設定屬性的優先順序

Spring Boot 的屬性有明確的優先順序（數字越小，優先級越高）：

```
1. 命令列參數                    java -jar app.jar --server.port=9090
2. 作業系統環境變數              SERVER_PORT=9090
3. JVM 系統屬性                  java -Dserver.port=9090 -jar app.jar
4. application-{profile}.properties
5. application.properties
6. @PropertySource 注解指定的檔案
7. 預設屬性（SpringApplication.setDefaultProperties）
```

範例：

```bash
# 啟動時覆寫 port（最高優先）
java -jar app.jar --server.port=9090

# 多個屬性
java -jar app.jar --server.port=9090 --spring.datasource.url=jdbc:mysql://other-db/mydb
```

---

## 使用環境變數

在生產環境中，敏感資訊不應寫在設定檔中，使用環境變數替代：

```properties
# application.properties
spring.datasource.password=${DB_PASSWORD}
spring.datasource.username=${DB_USER:root}  ← 若沒有環境變數，預設為 root
```

Spring Boot 也支援屬性名稱的轉換，以下三種方式等效：

| 格式 | 範例 |
|------|------|
| Properties | `spring.datasource.url` |
| 環境變數 | `SPRING_DATASOURCE_URL` |
| JVM 屬性 | `-Dspring.datasource.url` |

---

## 屬性型別轉換

Spring Boot 支援自動型別轉換：

```properties
# 時間單位（Duration）
app.connection-timeout=30s     ← 30 秒
app.read-timeout=5000ms        ← 5000 毫秒
app.session-timeout=30m        ← 30 分鐘

# 資料大小（DataSize）
app.max-file-size=10MB
app.max-upload-size=1GB

# 清單（List）
app.allowed-origins=http://localhost:3000,http://localhost:4200

# 陣列
app.cors.allowed-methods=GET,POST,PUT,DELETE
```

```java
@ConfigurationProperties(prefix = "app")
@Component
public class AppProperties {

    private Duration connectionTimeout;  // 自動轉換為 Duration
    private DataSize maxFileSize;        // 自動轉換為 DataSize
    private List<String> allowedOrigins; // 自動轉換為 List

    // Getters and Setters...
}
```

---

## 設定屬性驗證

使用 `@Validated` 確保設定值符合規範：

```java
@Component
@ConfigurationProperties(prefix = "app")
@Validated  // 啟用 Bean Validation
public class AppProperties {

    @NotEmpty(message = "app.name 不可為空")
    private String name;

    @Min(value = 1, message = "app.max-retry 最小為 1")
    @Max(value = 10, message = "app.max-retry 最大為 10")
    private int maxRetry;

    @NotNull
    @Valid  // 巢狀物件也要驗證
    private MailConfig mail;

    // ...

    public static class MailConfig {
        @NotEmpty
        private String host;

        @Min(1)
        @Max(65535)
        private int port;

        // Getters and Setters...
    }
}
```

如果屬性不合法，應用程式**啟動時**就會拋出異常，避免執行期間才發現問題。

---

## H2 Console 設定（開發用）

```properties
# 開啟 H2 記憶體資料庫（開發環境）
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

啟動後瀏覽 `http://localhost:8080/h2-console` 可進入資料庫管理介面。

---

## 重點整理

- Properties 和 YAML 格式功能等效，YAML 更適合複雜的階層設定
- `@Value` 注入單一屬性，`@ConfigurationProperties` 注入一組屬性
- 使用 Profile（`application-{env}.properties`）管理多環境設定
- 命令列參數優先順序最高，可在部署時動態覆寫設定
- 敏感資訊（密碼、金鑰）應使用環境變數，不寫在設定檔中
- 加上 `@Validated` 可在啟動時驗證設定值的合法性

---

## 相關文章

- [[01-自動配置-Auto-Configuration]]
- [[02-Spring-Boot-Starter]]
- [[04-Spring-MVC-RESTful-API]]
