# 日誌管理（Logging）

## 本章已驗證範例

- 對應專案：`examples/boot2-tutorial-app`
- 主要程式：`CheckoutService`
- 驗證測試：`CheckoutServiceTest`
- 校正重點：日誌章節使用 SLF4J + Logback 的實際服務範例，並透過測試捕捉輸出確認 log 內容。


## 概述

Spring Boot 預設使用 **Logback** 作為日誌框架，並透過 **SLF4J（Simple Logging Facade for Java）** 作為抽象層。這讓你可以在不修改程式碼的情況下切換底層日誌實作。

---

## 日誌等級（由低到高）

| 等級 | 使用情境 |
|------|---------|
| `TRACE` | 最詳細，追蹤程式執行流程（幾乎不用於生產） |
| `DEBUG` | 除錯資訊，開發時使用 |
| `INFO` | 一般資訊（預設等級），記錄重要事件 |
| `WARN` | 警告，可能有問題但不影響執行 |
| `ERROR` | 錯誤，需要關注和處理 |
| `OFF` | 關閉所有日誌 |

---

## 在程式碼中使用日誌

### 方法一：使用 SLF4J Logger（推薦）

```java
package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    // 建立 Logger（通常用當前類別作為名稱）
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public User createUser(String name, String email) {
        // INFO：記錄重要操作
        log.info("建立使用者：name={}, email={}", name, email);

        try {
            User user = doCreateUser(name, email);
            log.info("使用者建立成功：id={}", user.getId());
            return user;
        } catch (DuplicateEmailException e) {
            // WARN：Email 重複是預期的業務情況
            log.warn("嘗試建立重複的 Email：{}", email);
            throw e;
        } catch (Exception e) {
            // ERROR：記錄例外（第二個參數是 Throwable，會印出 Stack Trace）
            log.error("建立使用者時發生未預期錯誤：name={}", name, e);
            throw e;
        }
    }

    public void processData(List<String> items) {
        // DEBUG：詳細的處理過程（只在 DEBUG 等級以上才輸出）
        log.debug("開始處理 {} 筆資料", items.size());

        for (String item : items) {
            log.trace("處理項目：{}", item);  // 最詳細的追蹤
        }

        log.debug("資料處理完成");
    }
}
```

### 方法二：使用 Lombok @Slf4j 注解（更簡潔）

```xml
<!-- 加入 Lombok 依賴 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j  // 自動生成 private static final Logger log = ...
@Service
public class OrderService {

    public void placeOrder(Order order) {
        log.info("收到訂單：orderId={}, userId={}, amount={}",
            order.getId(), order.getUserId(), order.getTotalAmount());

        log.debug("訂單詳情：{}", order);
    }
}
```

---

## application.properties 設定

```properties
# 根 Logger 等級（影響所有套件）
logging.level.root=INFO

# 設定特定套件的等級
logging.level.com.example=DEBUG
logging.level.com.example.demo.repository=TRACE

# 關閉 Spring 框架的詳細日誌
logging.level.org.springframework=WARN
logging.level.org.hibernate=WARN

# 顯示 SQL 語句
logging.level.org.hibernate.SQL=DEBUG
# 顯示 SQL 綁定參數
logging.level.org.hibernate.type.descriptor.sql=TRACE

# 設定日誌輸出到檔案
logging.file.name=logs/application.log

# 或設定目錄（檔名預設 spring.log）
logging.file.path=logs/

# 設定日誌格式（Console）
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# 設定日誌格式（File）
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# 設定單一日誌檔案最大大小（超過會 rotate）
logging.file.max-size=100MB

# 保留多少個備份檔
logging.file.max-history=30
```

---

## 自定義 Logback 設定（logback-spring.xml）

建立 `src/main/resources/logback-spring.xml`，可實現更複雜的設定：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- 引入 Spring Boot 預設設定 -->
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- 定義屬性 -->
    <property name="LOG_PATH" value="logs"/>
    <property name="APP_NAME" value="spring-boot-app"/>

    <!-- Console Appender（輸出到終端） -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) [%blue(%thread)] %cyan(%logger{36}) - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- File Appender（輸出到檔案，自動輪轉） -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 每天輪轉，保留 30 天 -->
            <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy
                    class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 錯誤日誌單獨輸出 -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <file>${LOG_PATH}/${APP_NAME}-error.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}-error.%d{yyyy-MM-dd}.log.gz</fileNamePattern>
            <maxHistory>90</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 依 Profile 使用不同設定 -->
    <springProfile name="dev">
        <!-- 開發環境：DEBUG 等級輸出到 Console -->
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
        <logger name="com.example" level="DEBUG"/>
    </springProfile>

    <springProfile name="prod">
        <!-- 生產環境：INFO 等級，同時輸出到 Console 和 File -->
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
            <appender-ref ref="ERROR_FILE"/>
        </root>
        <logger name="com.example" level="INFO"/>
    </springProfile>

    <!-- 預設設定（未指定 Profile 時） -->
    <springProfile name="!dev,!prod">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

</configuration>
```

---

## MDC（Mapped Diagnostic Context）：追蹤請求

MDC 讓你在日誌中帶入請求相關的資訊（如 requestId、userId），方便追蹤整個請求的日誌：

```java
package com.example.demo.filter;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 設定請求 ID（放入 MDC）
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);
        MDC.put("method", request.getMethod());
        MDC.put("uri", request.getRequestURI());

        // 在回應標頭中加入 requestId（方便前端追蹤）
        response.setHeader("X-Request-Id", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 請求結束後清除 MDC（避免記憶體洩漏）
            MDC.clear();
        }
    }
}
```

在 logback 格式中使用 MDC 變數：

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%X{requestId}] [%X{method} %X{uri}] %-5level %logger - %msg%n</pattern>
```

日誌輸出：
```
2024-01-01 10:00:00 [abc12345] [POST /api/users] INFO  c.e.demo.service.UserService - 建立使用者：name=Alice
2024-01-01 10:00:00 [abc12345] [POST /api/users] INFO  c.e.demo.service.UserService - 使用者建立成功：id=1
```

---

## 請求/回應日誌記錄

```java
package com.example.demo.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// 方法一：繼承 CommonsRequestLoggingFilter
@Component
public class RequestLoggingFilter extends CommonsRequestLoggingFilter {

    public RequestLoggingFilter() {
        setIncludeQueryString(true);
        setIncludePayload(true);
        setMaxPayloadLength(10000);
        setIncludeHeaders(false);
        setAfterMessagePrefix("REQUEST DATA : ");
    }
}

// 同時在 application.properties 設定等級：
// logging.level.org.springframework.web.filter.CommonsRequestLoggingFilter=DEBUG
```

---

## 結構化日誌（JSON 格式）

生產環境中，結構化日誌（JSON）更方便被日誌系統（ELK、Splunk）解析：

```xml
<!-- 加入 Logstash Logback Encoder -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

```xml
<!-- logback-spring.xml -->
<appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <!-- 加入自定義欄位 -->
        <customFields>{"app":"spring-boot-app","env":"prod"}</customFields>
    </encoder>
</appender>
```

JSON 輸出：
```json
{
  "@timestamp": "2024-01-01T10:00:00.000Z",
  "@version": "1",
  "message": "建立使用者：name=Alice",
  "logger_name": "com.example.demo.service.UserService",
  "thread_name": "http-nio-8080-exec-1",
  "level": "INFO",
  "requestId": "abc12345",
  "app": "spring-boot-app",
  "env": "prod"
}
```

---

## 換成 Log4j2

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
```

建立 `src/main/resources/log4j2-spring.xml` 進行設定。

---

## 日誌最佳實踐

1. **使用佔位符**，而非字串拼接：
   ```java
   // 好：只有需要輸出時才會建立字串
   log.debug("處理使用者：{}", userId);

   // 不好：即使 DEBUG 關閉，仍會拼接字串（浪費資源）
   log.debug("處理使用者：" + userId);
   ```

2. **記錄例外時傳入 Throwable**：
   ```java
   log.error("發生錯誤", e);  // 會印出完整 Stack Trace
   ```

3. **不要記錄敏感資訊**：密碼、信用卡號、個人資料

4. **使用 MDC** 追蹤跨方法的請求鏈路

5. **生產環境設定 INFO 等級**，開發環境設定 DEBUG

---

## 重點整理

- Spring Boot 預設使用 SLF4J + Logback，無需額外設定
- 日誌等級：TRACE < DEBUG < INFO < WARN < ERROR
- 使用 `LoggerFactory.getLogger()` 或 Lombok `@Slf4j`
- 可用 `application.properties` 或 `logback-spring.xml` 設定
- MDC 讓日誌帶上請求追蹤資訊，方便排查問題
- 使用佔位符 `{}` 而非字串拼接，避免效能浪費

---

## 相關文章

- [[07-Spring-Boot-Actuator]]（Loggers 端點可動態調整等級）
- [[16-Spring-Boot-DevTools]]
- [[09-測試-Testing]]
