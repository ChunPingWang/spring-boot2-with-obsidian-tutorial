# 自動配置（Auto Configuration）

## 什麼是自動配置？

Spring Boot 的**自動配置**是它最強大的特性之一。當你在 `pom.xml` 加入依賴時，Spring Boot 會自動偵測 classpath 上的類別，並幫你建立對應的 Bean，省去手動編寫大量配置程式碼。

舉例：
- 加入 `spring-boot-starter-web` → 自動配置 DispatcherServlet、Tomcat
- 加入 `spring-boot-starter-data-jpa` → 自動配置 EntityManagerFactory、DataSource
- 加入 `spring-boot-starter-security` → 自動配置 Spring Security 的登入機制

---

## 自動配置的運作原理

### 1. @SpringBootApplication 的秘密

```java
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

`@SpringBootApplication` 包含了 `@EnableAutoConfiguration`，這個注解告訴 Spring Boot 開始自動配置流程。

### 2. spring.factories 檔案

Spring Boot 的自動配置類別清單儲存在：

```
META-INF/spring.factories
```

或（Spring Boot 2.7+）：

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

這個檔案列出所有自動配置類別，例如：

```properties
# spring.factories 內容片段
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration,\
  org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,\
  org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,\
  ...（共超過 100 個）
```

### 3. @Conditional 條件判斷

每個自動配置類別都用 `@Conditional` 系列注解來判斷是否生效：

```java
@Configuration
// 只有當 classpath 上有 DataSource 類別時，才啟用此配置
@ConditionalOnClass(DataSource.class)
// 只有當沒有自定義 DataSource Bean 時，才啟用此配置
@ConditionalOnMissingBean(DataSource.class)
public class DataSourceAutoConfiguration {

    @Bean
    public DataSource dataSource() {
        // 建立預設的 H2 記憶體資料庫連線
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }
}
```

---

## 常用的 @Conditional 注解

| 注解 | 說明 |
|------|------|
| `@ConditionalOnClass` | classpath 上存在指定類別時生效 |
| `@ConditionalOnMissingClass` | classpath 上不存在指定類別時生效 |
| `@ConditionalOnBean` | Spring 容器中存在指定 Bean 時生效 |
| `@ConditionalOnMissingBean` | Spring 容器中不存在指定 Bean 時生效 |
| `@ConditionalOnProperty` | 指定屬性有特定值時生效 |
| `@ConditionalOnWebApplication` | 是 Web 應用程式時生效 |
| `@ConditionalOnNotWebApplication` | 不是 Web 應用程式時生效 |
| `@ConditionalOnResource` | 存在指定資源檔案時生效 |
| `@ConditionalOnExpression` | SpEL 表達式為 true 時生效 |

---

## 查看已啟用的自動配置

### 方法一：啟動時加上 debug 模式

在 `application.properties` 加入：

```properties
debug=true
```

啟動後 console 會顯示所有配置的報告：

```
============================
CONDITIONS EVALUATION REPORT
============================

Positive matches:（已啟用）
-----------------
   DispatcherServletAutoConfiguration matched:
      - @ConditionalOnClass found required class 'javax.servlet.Servlet' (OnClassCondition)
      - @ConditionalOnWebApplication (required) found 'session' scope (OnWebApplicationCondition)

Negative matches:（未啟用）
-----------------
   ActiveMQAutoConfiguration:
      - @ConditionalOnClass did not find required class 'javax.jms.ConnectionFactory' (OnClassCondition)
```

### 方法二：使用 Actuator

加入 Actuator 依賴後，存取 `/actuator/conditions` 端點。

---

## 覆寫自動配置：自定義 Bean

當你定義了自己的 Bean，Spring Boot 的 `@ConditionalOnMissingBean` 就會讓自動配置跳過：

```java
package com.example.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // 自定義 ObjectMapper Bean
    // 由於我們定義了 ObjectMapper，Spring Boot 不會再自動建立預設的
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 設定日期序列化格式（不使用時間戳記）
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

---

## 透過 application.properties 控制自動配置

許多自動配置都可以透過屬性來調整：

```properties
# 資料庫連線設定（覆寫 DataSourceAutoConfiguration 的預設值）
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=secret
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# 關閉 Spring Security 的預設登入（覆寫 SecurityAutoConfiguration）
spring.security.user.name=admin
spring.security.user.password=admin123

# 設定 Jackson 日期格式（覆寫 JacksonAutoConfiguration）
spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
spring.jackson.time-zone=Asia/Taipei
```

---

## 排除特定自動配置

有時你需要完全停用某個自動配置：

### 方法一：在注解上排除

```java
@SpringBootApplication(exclude = {
    // 排除資料庫自動配置（例如：應用程式不需要資料庫）
    DataSourceAutoConfiguration.class,
    // 排除 Security 自動配置
    SecurityAutoConfiguration.class
})
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

### 方法二：在 application.properties 排除

```properties
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

---

## 撰寫自己的自動配置

初學者可以了解其原理，進階開發者可以為自己的 Library 撰寫自動配置：

### 1. 建立配置類別

```java
package com.example.mylib.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// classpath 上有 MyService 類別才啟用
@ConditionalOnClass(MyService.class)
// 啟用設定屬性類別
@EnableConfigurationProperties(MyProperties.class)
public class MyServiceAutoConfiguration {

    @Bean
    // 沒有自定義 MyService Bean 才建立
    @ConditionalOnMissingBean
    // 屬性 mylib.enabled=true 才建立（預設 true）
    @ConditionalOnProperty(prefix = "mylib", name = "enabled", matchIfMissing = true)
    public MyService myService(MyProperties properties) {
        return new MyService(properties.getMessage());
    }
}
```

### 2. 建立屬性類別

```java
package com.example.mylib.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 對應 application.properties 中 mylib.* 的設定
@ConfigurationProperties(prefix = "mylib")
public class MyProperties {

    private String message = "Hello from MyLib!";  // 預設值
    private boolean enabled = true;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
```

### 3. 在 spring.factories 中宣告

```properties
# src/main/resources/META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.example.mylib.autoconfigure.MyServiceAutoConfiguration
```

---

## 自動配置運作流程圖

```
應用程式啟動
     ↓
@EnableAutoConfiguration 觸發
     ↓
掃描 spring.factories / AutoConfiguration.imports
     ↓
逐一檢查每個自動配置類別的 @Conditional 條件
     ↓
條件成立 → 建立 Bean，加入 Spring 容器
條件不成立 → 跳過
     ↓
應用程式就緒
```

---

## 重點整理

- 自動配置是 Spring Boot 的核心機制，讓你「加依賴就能用」
- 透過 `@Conditional` 系列注解決定是否啟用
- 自定義 Bean 會優先於自動配置（`@ConditionalOnMissingBean`）
- 使用 `debug=true` 可以查看配置報告
- 可透過 `exclude` 或 `spring.autoconfigure.exclude` 停用不需要的配置

---

## 相關文章

- [[00-Spring-Boot-2-簡介與快速入門]]
- [[02-Spring-Boot-Starter]]
- [[03-應用程式配置-Properties-and-YAML]]
