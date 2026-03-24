# AOP 切面導向程式設計（Aspect-Oriented Programming）

## 本章已驗證範例

- 對應專案：`examples/boot2-tutorial-app`
- 主要程式：`ExecutionTimeAspect`、`MonitoredBusinessService`、`MonitoringRecorder`
- 驗證測試：`ExecutionTimeAspectTest`
- 校正重點：AOP 章節已對齊 `@Aspect` + `@Around` 的可執行範例，測試會驗證切面確實攔截並記錄方法。


## 什麼是 AOP？

AOP 是一種程式設計範式，用於將**橫切關注點（Cross-cutting Concerns）**從業務邏輯中分離。

橫切關注點是指那些分散在多個模組中的通用功能，如：

- **日誌記錄**：每個方法都要記錄執行時間
- **效能監控**：計算方法執行耗時
- **事務管理**：`@Transactional` 底層就是 AOP
- **安全檢查**：`@PreAuthorize` 底層也是 AOP
- **快取**：`@Cacheable` 底層也是 AOP
- **異常統一處理**
- **稽核日誌（Audit Log）**

---

## AOP 核心概念

| 術語 | 說明 | 比喻 |
|------|------|------|
| **Aspect（切面）** | 包含通用邏輯的類別 | 日誌切面、效能切面 |
| **JoinPoint（連接點）** | 程式執行的某個點（方法呼叫、異常拋出等） | 可以插入邏輯的地方 |
| **Pointcut（切入點）** | 定義「在哪些」JoinPoint 上執行 Advice | 所有 Service 層的方法 |
| **Advice（通知）** | 在 JoinPoint 上執行的「額外邏輯」 | 記錄日誌的程式碼 |
| **Weaving（織入）** | 將 Aspect 應用到目標物件的過程 | Spring 透過 Proxy 實作 |

### Advice 的類型

| 類型 | 執行時機 |
|------|---------|
| `@Before` | 方法執行前 |
| `@After` | 方法執行後（無論成功或失敗） |
| `@AfterReturning` | 方法正常返回後 |
| `@AfterThrowing` | 方法拋出異常後 |
| `@Around` | 包圍方法執行（最強大，可以控制是否執行目標方法） |

---

## 加入依賴

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

---

## Pointcut 表達式

Pointcut 表達式定義攔截的目標：

```
execution(修飾符? 返回型別 類別路徑?方法名稱(參數) 例外?)
```

```java
// 攔截所有 public 方法
execution(public * *(..))

// 攔截所有返回 String 的方法
execution(String *(..))

// 攔截特定類別的所有方法
execution(* com.example.demo.service.UserService.*(..))

// 攔截 service 包下所有類別的所有方法
execution(* com.example.demo.service.*.*(..))

// 攔截 service 包及其子包下所有類別的所有方法（..)
execution(* com.example.demo.service..*.*(..))

// 攔截方法名稱以 find 開頭的方法
execution(* find*(..))

// 攔截第一個參數為 Long 的方法
execution(* *(Long, ..))

// 攔截有 @Transactional 注解的方法
@annotation(org.springframework.transaction.annotation.Transactional)

// 攔截有 @Service 注解的類別的所有方法
within(@org.springframework.stereotype.Service *)
```

---

## 實作日誌切面

```java
package com.example.demo.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect   // 標記為切面類別
@Component // 讓 Spring 管理此 Bean
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    // 定義可重用的 Pointcut（方便在多個 Advice 中引用）
    @Pointcut("execution(* com.example.demo.service..*(..))")
    public void serviceLayer() {}

    @Pointcut("execution(* com.example.demo.controller..*(..))")
    public void controllerLayer() {}

    // @Before：方法執行前記錄
    @Before("serviceLayer()")
    public void logBefore(JoinPoint joinPoint) {
        log.debug("執行：{}.{}，參數：{}",
            joinPoint.getTarget().getClass().getSimpleName(),
            joinPoint.getSignature().getName(),
            Arrays.toString(joinPoint.getArgs())
        );
    }

    // @AfterReturning：正常返回後記錄（可取得返回值）
    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.debug("完成：{}.{}，返回：{}",
            joinPoint.getTarget().getClass().getSimpleName(),
            joinPoint.getSignature().getName(),
            result
        );
    }

    // @AfterThrowing：拋出例外後記錄
    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Exception ex) {
        log.error("例外：{}.{}，錯誤：{}",
            joinPoint.getTarget().getClass().getSimpleName(),
            joinPoint.getSignature().getName(),
            ex.getMessage()
        );
    }
}
```

---

## 效能監控切面（@Around）

`@Around` 是最強大的 Advice，可以在方法前後都執行邏輯，甚至可以決定是否呼叫目標方法：

```java
@Aspect
@Component
public class PerformanceAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceAspect.class);

    // 攔截所有 Service 和 Controller 層的方法
    @Around("execution(* com.example.demo.service..*(..)) || " +
            "execution(* com.example.demo.controller..*(..))")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();

        try {
            // 呼叫目標方法（必須呼叫，否則目標方法不會執行）
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;

            if (duration > 1000) {
                // 超過 1 秒警告
                log.warn("慢速方法：{}.{}，耗時：{}ms", className, methodName, duration);
            } else {
                log.debug("方法執行：{}.{}，耗時：{}ms", className, methodName, duration);
            }

            return result;

        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("方法異常：{}.{}，耗時：{}ms，錯誤：{}",
                className, methodName, duration, e.getMessage());
            throw e;  // 重新拋出，不要吞掉例外
        }
    }
}
```

---

## 自定義注解 + AOP

結合自定義注解讓切面更精確：

### 1. 定義注解

```java
package com.example.demo.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    String action() default "";      // 操作名稱
    String module() default "";      // 所屬模組
    boolean logParameters() default true;  // 是否記錄參數
}
```

### 2. 在方法上使用注解

```java
@Service
public class UserService {

    @AuditLog(action = "建立使用者", module = "用戶管理")
    public User createUser(CreateUserRequest request) { ... }

    @AuditLog(action = "刪除使用者", module = "用戶管理", logParameters = true)
    public void deleteUser(Long id) { ... }
}
```

### 3. 實作 AOP 切面

```java
@Aspect
@Component
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;

    public AuditLogAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // 攔截有 @AuditLog 注解的方法
    @Around("@annotation(auditLog)")
    public Object auditLog(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        // 取得當前登入使用者
        String username = getCurrentUsername();

        AuditLogEntry entry = new AuditLogEntry();
        entry.setUsername(username);
        entry.setAction(auditLog.action());
        entry.setModule(auditLog.module());
        entry.setMethod(joinPoint.getSignature().toShortString());
        entry.setTimestamp(LocalDateTime.now());

        if (auditLog.logParameters()) {
            entry.setParameters(Arrays.toString(joinPoint.getArgs()));
        }

        try {
            Object result = joinPoint.proceed();
            entry.setSuccess(true);
            return result;
        } catch (Exception e) {
            entry.setSuccess(false);
            entry.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            // 無論成功或失敗都儲存稽核日誌
            auditLogRepository.save(entry);
        }
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }
}
```

---

## 限流切面（Rate Limiting）

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit() default 100;          // 允許次數
    int duration() default 60;        // 時間窗口（秒）
}

@Aspect
@Component
public class RateLimitAspect {

    // 簡單的記憶體計數器（生產環境應使用 Redis）
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private final Map<String, Long> resetTimes = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit)
            throws Throwable {

        String key = getCurrentUsername() + ":" + joinPoint.getSignature().toShortString();
        long now = System.currentTimeMillis();
        long windowMs = rateLimit.duration() * 1000L;

        // 檢查是否需要重置計數器
        resetTimes.compute(key, (k, resetTime) -> {
            if (resetTime == null || now - resetTime > windowMs) {
                counters.put(k, new AtomicInteger(0));
                return now;
            }
            return resetTime;
        });

        AtomicInteger counter = counters.computeIfAbsent(key, k -> new AtomicInteger(0));

        if (counter.incrementAndGet() > rateLimit.limit()) {
            throw new RateLimitExceededException("請求過於頻繁，請稍後再試");
        }

        return joinPoint.proceed();
    }
}
```

使用：
```java
@GetMapping("/api/data")
@RateLimit(limit = 10, duration = 60)  // 每分鐘最多 10 次
public ResponseEntity<?> getData() { ... }
```

---

## AOP 的限制

1. **只能攔截 Spring 管理的 Bean**：直接 `new Object()` 的物件無法被攔截
2. **類別內部呼叫不走 Proxy**：同 `@Cacheable`、`@Async`
3. **final 方法無法被攔截**（CGlib 代理限制）
4. **private 方法無法被攔截**

---

## 執行順序

```
請求進入
    ↓
@Around (before part)
    ↓
@Before
    ↓
目標方法執行
    ↓
@AfterReturning / @AfterThrowing
    ↓
@After（always）
    ↓
@Around (after part)
    ↓
回傳
```

多個切面的順序可用 `@Order` 控制：

```java
@Aspect
@Component
@Order(1)  // 數字越小，越外層（先執行 before，後執行 after）
public class SecurityAspect { ... }

@Aspect
@Component
@Order(2)
public class LoggingAspect { ... }
```

---

## 重點整理

- AOP 讓橫切關注點（日誌、事務、安全）與業務邏輯分離
- `@Around` 最強大，可以控制目標方法的執行
- Pointcut 表達式定義攔截目標，可組合使用
- 結合自定義注解讓切面更有語義（`@AuditLog`、`@RateLimit`）
- 類別內部呼叫不走 Proxy，切面不生效
- Spring 的 `@Transactional`、`@Cacheable`、`@Async` 都是 AOP 的應用

---

## 相關文章

- [[12-快取-Caching]]（`@Cacheable` 底層是 AOP）
- [[14-非同步處理-Async]]（`@Async` 底層是 AOP）
- [[06-Spring-Security]]（`@PreAuthorize` 底層是 AOP）
- [[08-日誌管理-Logging]]（AOP 是實現統一日誌的最佳方式）
