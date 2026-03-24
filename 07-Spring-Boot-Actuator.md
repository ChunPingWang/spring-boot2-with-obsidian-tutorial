# Spring Boot Actuator

## 什麼是 Actuator？

Spring Boot Actuator 提供一系列**生產環境就緒的監控端點（Endpoint）**，讓你可以：

- 查看應用程式的健康狀態
- 監控效能指標（CPU、記憶體、HTTP 請求次數）
- 查看和修改應用程式設定
- 查看 Bean 與依賴關係
- 管理日誌等級

---

## 加入依賴

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

---

## 常用端點一覽

加入依賴後，以下端點預設可用（前綴 `/actuator`）：

| 端點 | 說明 | 預設啟用 |
|------|------|---------|
| `/actuator/health` | 應用程式健康狀態 | 是 |
| `/actuator/info` | 應用程式資訊 | 是 |
| `/actuator/metrics` | 應用程式指標 | 否（需開放） |
| `/actuator/env` | 環境屬性 | 否（需開放） |
| `/actuator/beans` | 所有 Spring Bean | 否（需開放） |
| `/actuator/mappings` | 所有 URL 映射 | 否（需開放） |
| `/actuator/loggers` | 日誌設定 | 否（需開放） |
| `/actuator/threaddump` | 執行緒狀態 | 否（需開放） |
| `/actuator/heapdump` | Heap dump 檔案 | 否（需開放） |
| `/actuator/conditions` | 自動配置條件報告 | 否（需開放） |
| `/actuator/scheduledtasks` | 排程任務清單 | 否（需開放） |
| `/actuator/caches` | 快取資訊 | 否（需開放） |

---

## 開放端點設定

```properties
# application.properties

# 開放所有端點（開發時使用，生產環境謹慎使用）
management.endpoints.web.exposure.include=*

# 開放指定端點
management.endpoints.web.exposure.include=health,info,metrics,loggers

# 排除特定端點
management.endpoints.web.exposure.exclude=env,beans

# 修改端點路徑前綴（預設 /actuator）
management.endpoints.web.base-path=/monitor

# 修改 Actuator 的 Port（與應用程式分開）
management.server.port=8081
```

---

## Health 端點

```bash
curl http://localhost:8080/actuator/health
```

預設回傳簡單狀態：
```json
{
  "status": "UP"
}
```

開啟詳細資訊：
```properties
management.endpoint.health.show-details=always
# 選項：never（預設）、when-authorized、always
```

詳細回傳：
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500107862016,
        "free": 200107862016,
        "threshold": 10485760,
        "path": "."
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### 自定義 Health 指標

```java
package com.example.demo.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ExternalApiHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // 檢查外部服務是否正常
            boolean isApiAvailable = checkExternalApi();

            if (isApiAvailable) {
                return Health.up()
                    .withDetail("api", "External API is available")
                    .withDetail("responseTime", "150ms")
                    .build();
            } else {
                return Health.down()
                    .withDetail("api", "External API is unavailable")
                    .build();
            }
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }

    private boolean checkExternalApi() {
        // 實際的健康檢查邏輯
        return true;
    }
}
```

結果（端點名稱自動從類別名稱截取）：
```json
{
  "status": "UP",
  "components": {
    "externalApi": {
      "status": "UP",
      "details": {
        "api": "External API is available",
        "responseTime": "150ms"
      }
    }
  }
}
```

---

## Info 端點

```bash
curl http://localhost:8080/actuator/info
```

設定資訊：
```properties
# application.properties

info.app.name=My Spring Boot App
info.app.version=1.0.0
info.app.description=Spring Boot 2 Tutorial
info.contact.email=admin@example.com
```

也可以注入 Git 和 Build 資訊：

```xml
<!-- pom.xml：加入 git-commit-id-plugin -->
<plugin>
    <groupId>pl.project13.maven</groupId>
    <artifactId>git-commit-id-plugin</artifactId>
</plugin>
```

```properties
# 啟用 build 資訊
management.info.build.enabled=true
management.info.git.enabled=true
management.info.git.mode=full
```

```json
{
  "app": {
    "name": "My Spring Boot App",
    "version": "1.0.0"
  },
  "build": {
    "version": "1.0.0",
    "artifact": "demo",
    "time": "2024-01-01T10:00:00.000Z"
  },
  "git": {
    "branch": "main",
    "commit": {
      "id": "abc1234",
      "time": "2024-01-01T09:00:00.000Z"
    }
  }
}
```

---

## Metrics 端點

```bash
# 列出所有可用指標
curl http://localhost:8080/actuator/metrics

# 查看特定指標
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# 查詢篩選條件（tag 過濾）
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=status:200"
```

常用指標：

| 指標名稱 | 說明 |
|---------|------|
| `jvm.memory.used` | JVM 記憶體使用量 |
| `jvm.memory.max` | JVM 最大記憶體 |
| `jvm.gc.pause` | GC 暫停時間 |
| `process.cpu.usage` | CPU 使用率 |
| `system.cpu.count` | CPU 核心數 |
| `http.server.requests` | HTTP 請求統計 |
| `spring.data.repository.invocations` | Repository 呼叫次數 |
| `tomcat.threads.current` | 目前執行緒數 |
| `hikaricp.connections` | 資料庫連線池狀態 |

---

## 自定義指標（Micrometer）

Spring Boot 2 使用 **Micrometer** 作為指標門面（Facade），可以整合 Prometheus、Grafana 等。

```java
package com.example.demo.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class OrderService {

    private final Counter orderCounter;
    private final Counter failedOrderCounter;
    private final Timer orderProcessingTimer;

    public OrderService(MeterRegistry meterRegistry) {
        // 計數器：記錄訂單總數
        this.orderCounter = Counter.builder("orders.created")
            .description("Total orders created")
            .tag("service", "order")
            .register(meterRegistry);

        // 計數器：記錄失敗訂單
        this.failedOrderCounter = Counter.builder("orders.failed")
            .description("Total failed orders")
            .register(meterRegistry);

        // 計時器：記錄訂單處理時間
        this.orderProcessingTimer = Timer.builder("orders.processing.time")
            .description("Order processing duration")
            .register(meterRegistry);
    }

    public Order createOrder(OrderRequest request) {
        return orderProcessingTimer.record(() -> {
            try {
                // 業務邏輯
                Order order = processOrder(request);
                orderCounter.increment();  // 成功計數
                return order;
            } catch (Exception e) {
                failedOrderCounter.increment();  // 失敗計數
                throw e;
            }
        });
    }
}
```

---

## Loggers 端點（動態修改日誌等級）

```bash
# 查看所有 logger
curl http://localhost:8080/actuator/loggers

# 查看特定 logger
curl http://localhost:8080/actuator/loggers/com.example

# 動態修改日誌等級（無需重啟！）
curl -X POST http://localhost:8080/actuator/loggers/com.example \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'

# 重置回預設等級
curl -X POST http://localhost:8080/actuator/loggers/com.example \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":null}'
```

---

## 整合 Prometheus + Grafana

```xml
<!-- 加入 Micrometer Prometheus Registry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```properties
# 開放 Prometheus 端點
management.endpoints.web.exposure.include=prometheus,health,info

# 設定應用程式名稱（會出現在 Prometheus 標籤中）
spring.application.name=my-app
```

```bash
# Prometheus 會定期抓取這個端點
curl http://localhost:8080/actuator/prometheus
```

輸出格式：
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space"} 2.0971520E7
jvm_memory_used_bytes{area="heap",id="G1 Old Gen"} 1.8388992E7
http_server_requests_seconds_count{method="GET",status="200",uri="/api/users"} 42.0
```

---

## 保護 Actuator 端點

```java
@Configuration
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http
            .requestMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeRequests(auth -> auth
                // health 和 info 公開
                .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
                // 其他端點需要 ACTUATOR 角色
                .anyRequest().hasRole("ACTUATOR")
            )
            .httpBasic();  // 使用 HTTP Basic Auth

        return http.build();
    }
}
```

```properties
# 設定 Actuator 的帳號密碼
spring.security.user.name=actuator-admin
spring.security.user.password=secure-password
spring.security.user.roles=ACTUATOR
```

---

## 重點整理

- Actuator 提供生產環境監控能力，加入依賴即可使用
- 預設只開放 `health` 和 `info`，其他端點需在設定中開放
- `health` 顯示服務健康狀態，可自定義 `HealthIndicator`
- `metrics` 顯示效能指標，整合 Micrometer 可對接 Prometheus
- `loggers` 可在不重啟的情況下動態調整日誌等級
- 生產環境務必保護 Actuator 端點，避免敏感資訊外洩

---

## 相關文章

- [[08-日誌管理-Logging]]
- [[06-Spring-Security]]
- [[17-打包與部署]]
