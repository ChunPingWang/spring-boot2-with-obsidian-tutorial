# 非同步處理（Async）

## 什麼是非同步處理？

在同步（Synchronous）模式下，方法呼叫會**阻塞**當前執行緒，等待結果返回才繼續。

在非同步（Asynchronous）模式下，方法在**獨立的執行緒**中執行，呼叫方不需要等待，可以繼續做其他事情。

### 適合使用非同步的場景

- 發送郵件、簡訊（不需要等待結果）
- 寫入日誌或稽核紀錄
- 呼叫耗時的外部 API
- 大量資料處理
- 同時執行多個獨立的查詢操作

---

## 啟用非同步

```java
@SpringBootApplication
@EnableAsync  // 啟用非同步功能
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

---

## 基本用法：@Async

### 不需要回傳值

```java
package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    // 非同步方法：呼叫後立即返回，郵件在背景發送
    @Async
    public void sendWelcomeEmail(String to, String name) {
        log.info("開始發送歡迎郵件到：{}", to);

        // 模擬耗時操作
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("郵件發送完成：{}", to);
    }
}
```

```java
@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    // 建立使用者後，非同步發送郵件（不阻塞主流程）
    public User register(RegisterRequest request) {
        User user = new User(request.getName(), request.getEmail());
        User saved = userRepository.save(user);

        // 這行呼叫立即返回，郵件在背景執行緒中發送
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getName());

        log.info("使用者註冊完成，郵件發送中（背景）");
        return saved;  // 不需要等待郵件發送完成
    }
}
```

### 有回傳值：CompletableFuture

```java
@Service
public class ReportService {

    @Async
    public CompletableFuture<List<SalesReport>> getSalesReport(int year, int month) {
        log.info("開始生成 {}/{} 銷售報告", year, month);

        // 耗時的報告生成...
        List<SalesReport> reports = generateReport(year, month);

        return CompletableFuture.completedFuture(reports);
    }

    @Async
    public CompletableFuture<UserStats> getUserStats(Long userId) {
        UserStats stats = calculateStats(userId);
        return CompletableFuture.completedFuture(stats);
    }
}
```

### 並行執行多個非同步任務

```java
@Service
public class DashboardService {

    private final ReportService reportService;
    private final UserService userService;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public DashboardData getDashboardData(Long userId) throws Exception {
        // 同時發起三個非同步查詢（並行執行，而非依序）
        CompletableFuture<UserStats> userStatsFuture = userService.getUserStats(userId);
        CompletableFuture<List<Order>> recentOrdersFuture = orderService.getRecentOrders(userId);
        CompletableFuture<List<Notification>> notificationsFuture =
            notificationService.getUnreadNotifications(userId);

        // 等待所有任務完成
        CompletableFuture.allOf(userStatsFuture, recentOrdersFuture, notificationsFuture).join();

        // 取得各任務結果
        return new DashboardData(
            userStatsFuture.get(),
            recentOrdersFuture.get(),
            notificationsFuture.get()
        );

        // 若三個任務各需 1 秒，並行只需約 1 秒，而非依序的 3 秒
    }
}
```

### 錯誤處理

```java
@Service
public class AsyncTaskService {

    @Async
    public CompletableFuture<String> processData(String input) {
        try {
            // 可能失敗的操作
            String result = doHeavyProcessing(input);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            // 回傳失敗的 CompletableFuture
            return CompletableFuture.failedFuture(e);
        }
    }
}

// 呼叫端處理錯誤
CompletableFuture<String> future = asyncTaskService.processData("test");

future.thenAccept(result -> log.info("結果：{}", result))
      .exceptionally(ex -> {
          log.error("處理失敗：{}", ex.getMessage());
          return null;
      });
```

---

## 設定執行緒池

預設情況下，`@Async` 使用 Spring 的 `SimpleAsyncTaskExecutor`，它不重用執行緒，每次都新建，不適合高並發場景。應改用 `ThreadPoolTaskExecutor`：

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    // 方法一：覆寫 getAsyncExecutor（設定預設執行器）
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心執行緒數（長期保持的執行緒）
        executor.setCorePoolSize(5);

        // 最大執行緒數
        executor.setMaxPoolSize(20);

        // 等待佇列大小（超過此數量的任務會拒絕或阻塞）
        executor.setQueueCapacity(100);

        // 執行緒名稱前綴（方便日誌識別）
        executor.setThreadNamePrefix("async-task-");

        // 執行緒閒置多久後回收（秒）
        executor.setKeepAliveSeconds(60);

        // 應用程式關閉時等待任務完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    // 方法二：定義多個命名執行器
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-task-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "reportExecutor")
    public Executor reportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("report-task-");
        executor.initialize();
        return executor;
    }
}
```

### 指定執行器

```java
@Service
public class EmailService {

    // 使用指定的 emailExecutor 執行器
    @Async("emailExecutor")
    public void sendEmail(String to) { ... }
}

@Service
public class ReportService {

    // 使用指定的 reportExecutor 執行器
    @Async("reportExecutor")
    public CompletableFuture<Report> generateReport(int year) { ... }
}
```

---

## 非同步異常處理

非同步方法中的例外不會傳播到呼叫者，需要額外設定：

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("非同步方法 {} 發生未捕獲例外：{}", method.getName(), ex.getMessage(), ex);
            // 可以加入告警邏輯（發送 Slack、郵件等）
        };
    }
}
```

---

## 非同步與事務

```java
// 問題：非同步方法和呼叫者在不同執行緒，不共享事務
@Service
public class OrderService {

    @Transactional
    public void placeOrder(Order order) {
        orderRepository.save(order);

        // 這個方法在新執行緒中執行，不在父事務中
        // 如果 saveOrder 失敗，inventoryService 的動作仍然會提交
        inventoryService.reduceStockAsync(order.getItems());  // ❌ 事務不共享
    }
}

// 解法：確保不依賴父事務，或使用事件機制（見下方）
@Async
@Transactional  // 非同步方法有自己的獨立事務
public void reduceStockAsync(List<OrderItem> items) {
    // 這是獨立的事務
}
```

---

## 搭配 Spring Events（事件驅動）

非同步處理常與事件機制配合使用：

```java
// 1. 定義事件
public class OrderPlacedEvent extends ApplicationEvent {
    private final Order order;

    public OrderPlacedEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }

    public Order getOrder() { return order; }
}

// 2. 發佈事件
@Service
public class OrderService {

    private final ApplicationEventPublisher eventPublisher;

    public Order placeOrder(OrderRequest request) {
        Order order = createAndSaveOrder(request);

        // 發佈事件（同步，但監聽器可以非同步處理）
        eventPublisher.publishEvent(new OrderPlacedEvent(this, order));

        return order;
    }
}

// 3. 非同步監聽事件
@Component
public class OrderEventListener {

    @Async  // 在獨立執行緒中處理事件
    @EventListener
    public void handleOrderPlaced(OrderPlacedEvent event) {
        Order order = event.getOrder();

        // 發送確認郵件（非同步）
        emailService.sendOrderConfirmation(order.getUserEmail(), order.getId());

        // 通知倉庫
        warehouseService.notifyNewOrder(order);

        log.info("訂單事件處理完成：orderId={}", order.getId());
    }
}
```

---

## CompletableFuture 常用操作

```java
// 建立 CompletableFuture
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello");

// 轉換結果
CompletableFuture<Integer> lengthFuture = future.thenApply(s -> s.length());

// 消費結果（無回傳值）
future.thenAccept(s -> System.out.println("結果：" + s));

// 組合兩個 Future
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "World");
CompletableFuture<String> combined = f1.thenCombine(f2, (s1, s2) -> s1 + " " + s2);

// 等待所有完成
CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2);
all.join();  // 阻塞直到全部完成

// 取第一個完成的結果
CompletableFuture<Object> any = CompletableFuture.anyOf(f1, f2);

// 錯誤處理
future.exceptionally(ex -> "預設值")
      .thenAccept(System.out::println);
```

---

## 注意事項

1. **類別內部呼叫不生效**：`@Async` 透過 AOP Proxy，類別內部直接呼叫不走 Proxy，同 `@Cacheable`
2. **`@Async` 方法不能是 private**：必須是 public（或 protected）
3. **執行緒池設定很重要**：預設的 `SimpleAsyncTaskExecutor` 不適合生產環境
4. **事務不跨執行緒共享**：非同步方法需要獨立的事務

---

## 重點整理

- `@EnableAsync` + `@Async` 讓方法在獨立執行緒執行
- 無回傳值的方法直接加 `@Async`
- 有回傳值的方法回傳 `CompletableFuture<T>`
- `CompletableFuture.allOf()` 可以並行執行多個非同步任務
- 生產環境務必設定自定義的 `ThreadPoolTaskExecutor`
- 搭配 Spring Events 實現鬆耦合的非同步處理

---

## 相關文章

- [[13-排程任務-Scheduling]]（@Async + @Scheduled 搭配使用）
- [[15-AOP-切面導向程式設計]]（@Async 底層使用 AOP）
- [[19-電商微服務系統-實戰總結]]（事件驅動架構）
