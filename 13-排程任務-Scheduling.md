# 排程任務（Scheduling）

## 本章已驗證範例

- 對應專案：`examples/boot2-tutorial-app`
- 主要程式：`ReportScheduler`
- 驗證測試：`ReportSchedulerTest`
- 校正重點：排程章節已對齊 `@Scheduled` 範例，測試會驗證排程註解與核心邏輯。


## 概述

Spring Boot 提供簡單且強大的排程機制，只需一個注解就能讓方法定期自動執行。常見應用：

- 每天凌晨清理過期資料
- 每小時同步外部資料
- 每分鐘檢查待處理的訂單
- 定期發送報表郵件

---

## 啟用排程

```java
@SpringBootApplication
@EnableScheduling  // 啟用排程功能
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

---

## @Scheduled 的三種設定方式

### 1. fixedRate：固定頻率

每隔固定時間就執行一次，不管上一次是否已完成：

```java
@Component
public class DataSyncTask {

    private static final Logger log = LoggerFactory.getLogger(DataSyncTask.class);

    // 每 5 秒執行一次（毫秒）
    @Scheduled(fixedRate = 5000)
    public void syncData() {
        log.info("開始同步資料：{}", LocalDateTime.now());
        // 同步邏輯...
    }

    // 從設定檔讀取頻率（更靈活）
    @Scheduled(fixedRateString = "${app.sync.interval:10000}")
    public void syncDataConfigurable() {
        log.info("從設定檔讀取頻率的同步任務");
    }

    // 延遲 10 秒後才開始，之後每 5 秒執行一次
    @Scheduled(initialDelay = 10000, fixedRate = 5000)
    public void syncDataWithDelay() {
        log.info("延遲啟動的同步任務");
    }
}
```

### 2. fixedDelay：固定延遲

上一次執行**完成後**，等待固定時間再執行（確保不重疊）：

```java
@Component
public class CleanupTask {

    // 上一次執行完成後，等待 30 秒再執行
    // 適合執行時間不固定的任務
    @Scheduled(fixedDelay = 30000)
    public void cleanup() {
        log.info("開始清理工作");
        // 可能執行 1~30 秒的清理邏輯...
        log.info("清理完成");
    }
}
```

### 3. cron：Cron 表達式（最靈活）

```java
@Component
public class ReportTask {

    // Cron 格式：秒 分 時 日 月 星期
    // *    = 任意值
    // ?    = 不指定（只用於日和星期）
    // ,    = 多個值（1,3,5）
    // -    = 範圍（1-5）
    // /    = 步進（*/5 = 每 5 個單位）
    // L    = 最後（只用於日和星期）
    // W    = 最近的工作日
    // #    = 第幾個星期幾（如 2#3 = 第三個星期二）

    // 每天早上 8 點執行
    @Scheduled(cron = "0 0 8 * * ?")
    public void dailyMorningReport() {
        log.info("發送每日早報");
    }

    // 每週一早上 9 點執行
    @Scheduled(cron = "0 0 9 ? * MON")
    public void weeklyReport() {
        log.info("發送每週報告");
    }

    // 每月 1 日凌晨 1 點執行
    @Scheduled(cron = "0 0 1 1 * ?")
    public void monthlyReport() {
        log.info("發送月報");
    }

    // 每分鐘執行（等同 fixedRate = 60000）
    @Scheduled(cron = "0 * * * * ?")
    public void everyMinute() {
        log.info("每分鐘任務");
    }

    // 每個工作日（週一到週五）中午 12 點
    @Scheduled(cron = "0 0 12 ? * MON-FRI")
    public void workdayLunch() {
        log.info("工作日中午任務");
    }

    // 從設定檔讀取 cron 表達式
    @Scheduled(cron = "${app.report.cron:0 0 8 * * ?}")
    public void configurableCron() {
        log.info("可設定的排程任務");
    }
}
```

---

## 常用 Cron 表達式參考

| Cron 表達式 | 說明 |
|------------|------|
| `0 * * * * ?` | 每分鐘 |
| `0 */5 * * * ?` | 每 5 分鐘 |
| `0 0 * * * ?` | 每小時 |
| `0 0 8 * * ?` | 每天 08:00 |
| `0 0 0 * * ?` | 每天凌晨 |
| `0 0 8 ? * MON-FRI` | 每個工作日 08:00 |
| `0 0 0 1 * ?` | 每月 1 號凌晨 |
| `0 0 0 ? * 1` | 每週日凌晨 |
| `0 0 8,12,18 * * ?` | 每天 08:00、12:00、18:00 |

> 使用 [crontab.guru](https://crontab.guru) 可以線上測試 Cron 表達式

---

## 實際範例：訂單處理系統

```java
package com.example.demo.scheduler;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStatus;
import com.example.demo.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderScheduler.class);
    private final OrderRepository orderRepository;

    public OrderScheduler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // 每 10 分鐘處理待付款的超時訂單
    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void cancelExpiredOrders() {
        log.info("開始檢查超時訂單");

        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(30);

        List<Order> expiredOrders = orderRepository
            .findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, expireTime);

        if (expiredOrders.isEmpty()) {
            log.info("沒有超時訂單");
            return;
        }

        expiredOrders.forEach(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelReason("付款超時自動取消");
            log.info("取消超時訂單：orderId={}", order.getId());
        });

        orderRepository.saveAll(expiredOrders);
        log.info("共取消 {} 筆超時訂單", expiredOrders.size());
    }

    // 每天凌晨 2 點統計前一天的訂單數據
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyStatistics() {
        log.info("開始生成每日統計報告");
        // 統計邏輯...
        log.info("每日統計報告完成");
    }

    // 每隔 1 小時同步庫存
    @Scheduled(cron = "0 0 * * * ?")
    public void syncInventory() {
        log.info("開始同步庫存資料");
        // 庫存同步邏輯...
    }
}
```

---

## 多執行緒排程

預設情況下，所有排程任務**共用一個單執行緒**，任務會按順序執行，不會同時進行。若某個任務耗時過長，會影響其他任務。

### 方法一：設定執行緒池

```java
@Configuration
public class SchedulingConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 設定排程任務的執行緒池（10 個執行緒）
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.initialize();
        taskRegistrar.setTaskScheduler(scheduler);
    }
}
```

### 方法二：搭配 @Async（非同步執行）

```java
@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig { ... }

@Component
public class AsyncTask {

    @Scheduled(fixedRate = 5000)
    @Async  // 每次觸發都在新的執行緒執行，不等待上次完成
    public void asyncScheduledTask() {
        // 這個任務會在獨立的執行緒中執行
        log.info("非同步排程任務執行在執行緒：{}", Thread.currentThread().getName());
    }
}
```

---

## 動態排程（程式化設定）

有時需要在執行期間動態調整排程頻率：

```java
@Component
public class DynamicTask implements SchedulingConfigurer {

    private volatile String cronExpression = "0 * * * * ?"; // 預設每分鐘

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
            // 任務本體
            this::executeTask,
            // 觸發器：動態計算下一次執行時間
            triggerContext -> {
                CronTrigger trigger = new CronTrigger(cronExpression);
                return trigger.nextExecutionTime(triggerContext);
            }
        );
    }

    private void executeTask() {
        log.info("動態排程任務執行，當前 cron：{}", cronExpression);
    }

    // 外部呼叫此方法更新 cron 表達式
    public void updateCron(String newCron) {
        this.cronExpression = newCron;
        log.info("排程頻率已更新為：{}", newCron);
    }
}
```

---

## 在不同環境啟用/停用排程

```java
@Component
// 只在非 test Profile 執行排程
@Profile("!test")
public class ProductionScheduler {

    @Scheduled(cron = "0 0 2 * * ?")
    public void nightlyTask() {
        // 只在生產/開發環境執行，測試時不執行
    }
}
```

也可以用條件屬性控制：

```java
@Component
@ConditionalOnProperty(
    name = "app.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = true  // 預設啟用
)
public class ConditionalScheduler {

    @Scheduled(fixedRate = 60000)
    public void scheduledTask() { ... }
}
```

```properties
# application-test.properties
app.scheduler.enabled=false  # 測試環境停用排程
```

---

## 排程任務監控

```java
@Component
public class MonitoredTask {

    private final AtomicInteger executionCount = new AtomicInteger(0);
    private LocalDateTime lastExecutionTime;
    private Duration lastExecutionDuration;

    @Scheduled(fixedRate = 60000)
    public void scheduledTask() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("任務開始執行，第 {} 次", executionCount.incrementAndGet());

        try {
            // 業務邏輯...
            Thread.sleep(1000);  // 模擬耗時
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        lastExecutionTime = startTime;
        lastExecutionDuration = Duration.between(startTime, LocalDateTime.now());
        log.info("任務執行完成，耗時：{}", lastExecutionDuration);
    }

    // Actuator 可以取得此資訊（見 [[07-Spring-Boot-Actuator]]）
    public int getExecutionCount() { return executionCount.get(); }
    public LocalDateTime getLastExecutionTime() { return lastExecutionTime; }
}
```

---

## 重點整理

- `@EnableScheduling` 啟用排程，`@Scheduled` 設定執行頻率
- `fixedRate`：固定頻率（不等上次完成）
- `fixedDelay`：固定延遲（等上次完成後）
- `cron`：Cron 表達式（最彈性，格式：秒 分 時 日 月 星期）
- 預設單執行緒，需要並行執行時設定 `ThreadPoolTaskScheduler`
- 用 `@Profile` 或 `@ConditionalOnProperty` 在特定環境停用排程

---

## 相關文章

- [[14-非同步處理-Async]]（搭配 @Async 讓排程任務非同步執行）
- [[07-Spring-Boot-Actuator]]（`/actuator/scheduledtasks` 查看所有排程）
- [[03-應用程式配置-Properties-and-YAML]]（cron 表達式存放在設定檔）
