# 電商系統：Spring Modulith 版本

> **版本標記**：本文會提到 **Spring Boot 3 / Spring Modulith** 的正式版脈絡；但本倉庫的可執行範例已統一為 **Spring Boot 2.7 + JDK 8 + legacy Moduliths 1.3**。

## 本章已驗證範例

- 對應專案：`examples/commerce-modulith`
- 主要程式：`OrderService`、`OrderPlacedEvent`、`NotificationTracker`
- 驗證測試：`ApplicationModulesTest`、`OrderServiceIntegrationTest`
- 校正重點：正式版 Spring Modulith 需要 Spring Boot 3 / Java 17；為了與本倉庫一致，本章範例改以 Spring Boot 2.7 / JDK 8 可用的 legacy Moduliths 1.3 呈現。


## 概述

本文以[[19-電商微服務系統-實戰總結]]的**同一個電商系統**為例，改用 **Spring Modulith** 架構（模組化單體 / Modular Monolith）實作。

透過比較兩種架構，你可以理解各自的優缺點，以及什麼情況下該選擇哪種架構。

> **前提知識**：建議先閱讀 [[19-電商微服務系統-實戰總結]] 後再閱讀本文，才能有效比較兩種架構的差異。

---

## 什麼是 Spring Modulith？

**Spring Modulith** 是 Spring 官方提供的框架，讓你在**單一應用程式（單體）**中，以嚴格的模組邊界組織程式碼，享有以下好處：

- **模組隔離**：每個模組有清晰的 API 邊界，模組間不能直接存取彼此的內部類別
- **模組間事件**：透過 Spring Application Events 實現模組間鬆耦合通訊
- **測試支援**：可以單獨測試某個模組，不需要啟動完整應用程式
- **架構文件**：自動生成模組依賴圖
- **漸進式遷移**：未來可以將模組拆成微服務

### 核心思想

> 「先以模組化單體（Modular Monolith）啟動，等真的需要時再拆分成微服務。」 — Spring Modulith 設計哲學

---

## 加入依賴

```xml
<!-- 若專案需維持 Spring Boot 2.7 / JDK 8，請使用 legacy Moduliths -->
<dependency>
    <groupId>org.moduliths</groupId>
    <artifactId>moduliths-core</artifactId>
    <version>1.3.0</version>
</dependency>
<dependency>
    <groupId>org.moduliths</groupId>
    <artifactId>moduliths-test</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>

<!-- 一般 Spring Boot 依賴 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 專案結構（對應微服務版本）

```
ecommerce/
├── src/main/java/
│   └── com/example/ecommerce/
│       ├── EcommerceApplication.java           ← 主程式
│       │
│       ├── user/                               ← User 模組（對應 User Service）
│       │   ├── User.java                       ← 模組內部（package-private）
│       │   ├── UserRepository.java             ← 模組內部
│       │   ├── UserService.java                ← 模組內部
│       │   ├── UserController.java             ← 模組內部
│       │   └── UserAPI.java                    ← 模組公開 API（public）
│       │
│       ├── catalog/                            ← Product 模組（對應 Product Service）
│       │   ├── internal/                       ← 明確標記內部
│       │   │   ├── Product.java
│       │   │   ├── ProductRepository.java
│       │   │   └── ProductService.java
│       │   ├── CatalogAPI.java                 ← 公開 API
│       │   └── ProductDTO.java                 ← 公開 DTO
│       │
│       ├── order/                              ← Order 模組（對應 Order Service）
│       │   ├── internal/
│       │   │   ├── Order.java
│       │   │   ├── OrderItem.java
│       │   │   ├── OrderRepository.java
│       │   │   └── OrderService.java
│       │   ├── OrderController.java
│       │   ├── OrderAPI.java
│       │   └── events/
│       │       ├── OrderPlacedEvent.java       ← 對外發布的事件
│       │       └── OrderCancelledEvent.java
│       │
│       ├── payment/                            ← Payment 模組
│       │   ├── internal/
│       │   └── PaymentAPI.java
│       │
│       └── notification/                       ← Notification 模組
│           └── internal/
│               └── NotificationListener.java
│
└── src/main/resources/
    └── application.yml
```

### 模組邊界規則

- **public 類別**：可被其他模組存取（模組的 API）
- **package-private 類別**（預設）：只能在模組內使用（實作細節）
- `internal/` 子包：明確標記不對外暴露的類別

---

## 模組間通訊：Application Events

微服務版本用 Kafka 傳遞事件；Modulith 版本用 **Spring Application Events**，但效果相同：

```java
// 定義事件（公開，其他模組可訂閱）
package com.example.ecommerce.order.events;

public class OrderPlacedEvent {
    private final Long orderId;
    private final Long userId;
    private final String userEmail;
    private final BigDecimal totalAmount;
    private final List<OrderItemInfo> items;

    public OrderPlacedEvent(Order order) {
        this.orderId = order.getId();
        this.userId = order.getUserId();
        this.userEmail = order.getUserEmail();
        this.totalAmount = order.getTotalAmount();
        this.items = order.getItems().stream()
            .map(i -> new OrderItemInfo(i.getProductId(), i.getQuantity()))
            .collect(Collectors.toList());
    }
    // Getters...
}
```

```java
// Order 模組：發佈事件
package com.example.ecommerce.order.internal;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CatalogAPI catalogAPI;              // 使用模組公開 API
    private final ApplicationEventPublisher events;

    public Order createOrder(Long userId, CreateOrderRequest request) {
        Order order = new Order();
        order.setUserId(userId);

        for (OrderItemRequest item : request.getItems()) {
            // 呼叫 Catalog 模組的公開 API（同一個 JVM，不需要 HTTP！）
            ProductDTO product = catalogAPI.getProductById(item.getProductId());
            catalogAPI.decreaseStock(item.getProductId(), item.getQuantity());

            // ... 建立訂單項目
        }

        Order saved = orderRepository.save(order);

        // 發佈事件（Notification 模組會訂閱）
        events.publishEvent(new OrderPlacedEvent(saved));

        return saved;
    }
}
```

```java
// Notification 模組：訂閱事件（鬆耦合）
package com.example.ecommerce.notification.internal;

@Component
public class NotificationListener {

    private final EmailService emailService;

    // 訂閱 Order 模組的事件
    // @Async 讓通知在背景執行，不阻塞訂單建立流程（見 [[14-非同步處理-Async]]）
    @Async
    @ApplicationModuleListener  // Spring Modulith 的增強版 @EventListener
    public void handleOrderPlaced(OrderPlacedEvent event) {
        emailService.sendOrderConfirmation(event.getUserEmail(), event.getOrderId());
    }

    @Async
    @ApplicationModuleListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        emailService.sendOrderCancellation(event.getUserEmail(), event.getOrderId());
    }
}
```

### @ApplicationModuleListener vs @EventListener

| | `@EventListener` | `@ApplicationModuleListener` |
|--|-----------------|------------------------------|
| 事務 | 與發布者同一事務 | 發布者事務提交後才執行 |
| 失敗處理 | 影響主流程 | 不影響主流程 |
| 重試 | 否 | 可搭配持久化事件 |

---

## 各模組詳細實作

### User 模組

```java
// 公開 API（可被其他模組呼叫）
package com.example.ecommerce.user;

import com.example.ecommerce.user.internal.User;
import com.example.ecommerce.user.internal.UserService;
import org.springframework.stereotype.Service;

@Service  // 這個類別是 public，其他模組可以注入使用
public class UserAPI {

    private final UserService userService;

    public UserAPI(UserService userService) {
        this.userService = userService;
    }

    // 對外提供的方法（使用 DTO，不暴露 Entity）
    public UserDTO findById(Long id) {
        User user = userService.findById(id);
        return new UserDTO(user.getId(), user.getUsername(), user.getEmail());
    }

    public boolean isUserActive(Long userId) {
        return userService.isActive(userId);
    }
}
```

```java
// 模組內部（package-private，外部無法直接存取）
package com.example.ecommerce.user.internal;

@Entity
@Table(name = "users")
class User {  // 注意：package-private（無 public 修飾）
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String email;
    @Column(nullable = false)
    private String password;
    private boolean active = true;
    // ...
}

@Repository
interface UserRepository extends JpaRepository<User, Long> {  // package-private
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}

@Service
@Transactional
class UserService {  // package-private
    // 實作細節，外部不需知道
}
```

---

### Catalog（商品）模組

```java
// 公開 DTO
package com.example.ecommerce.catalog;

public class ProductDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private int availableStock;

    // Constructor, Getters...
}

// 公開 API
package com.example.ecommerce.catalog;

@Service
public class CatalogAPI {

    private final ProductService productService;

    public ProductDTO getProductById(Long id) {
        return productService.findById(id);
    }

    public void decreaseStock(Long productId, int quantity) {
        productService.decreaseStock(productId, quantity);
    }

    public Page<ProductDTO> searchProducts(String keyword, Long categoryId,
                                            int page, int size) {
        return productService.search(keyword, categoryId, page, size);
    }
}

// 內部實作（帶快取，見 [[12-快取-Caching]]）
package com.example.ecommerce.catalog.internal;

@Service
@Transactional(readOnly = true)
class ProductService {

    @Cacheable(value = "products", key = "#id")
    public ProductDTO findById(Long id) { ... }

    @Transactional
    @CacheEvict(value = "products", key = "#productId")
    public void decreaseStock(Long productId, int quantity) { ... }
}
```

---

## 模組測試

在正式版 Spring Modulith 中，可以用 `@ApplicationModuleTest` **單獨測試某個模組**；若專案維持 Spring Boot 2.7 / JDK 8，則通常改以 `@SpringBootTest`、Mock 與封裝良好的模組 API 來達成相同目標：

```java
package com.example.ecommerce.order;

import org.springframework.modulith.test.ApplicationModuleTest;

// 只啟動 Order 模組（其他模組 Mock 掉）
@ApplicationModuleTest
class OrderModuleTest {

    @Autowired
    private OrderService orderService;

    @MockBean
    private CatalogAPI catalogAPI;  // Mock 掉 Catalog 模組

    @Test
    void createOrder_ShouldPublishEvent() {
        // Arrange
        ProductDTO product = new ProductDTO(1L, "商品A", new BigDecimal("100"), 10);
        when(catalogAPI.getProductById(1L)).thenReturn(product);

        CreateOrderRequest request = new CreateOrderRequest(1L, 2);

        // Act
        Order order = orderService.createOrder(1L, request);

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("200");
    }
}
```

---

## 架構驗證測試

無論是正式版 Spring Modulith 或 legacy Moduliths，都可以**自動驗證模組規則**（防止意外跨模組存取）：

```java
package com.example.ecommerce;

import org.moduliths.model.Modules;

class ModularityTests {

    Modules modules = Modules.of(EcommerceApplication.class);

    @Test
    void verifiesModularStructure() {
        // 驗證模組邊界沒有被違反（如 notification 直接存取 order 的 Entity）
        modules.verify();
    }
}
```

---

## 持久化事件（Event Persistence）

防止事件在處理過程中遺失（類似微服務的 Outbox Pattern）：

> **版本提醒**：下列 `spring-modulith-events-jpa` 屬於正式 Spring Modulith / Spring Boot 3 生態的做法。若專案固定在 Spring Boot 2.7 / JDK 8，可保留一般 Spring Event，或自行實作 outbox / 重試機制。

```xml
<dependency>
    <groupId>org.springframework.experimental</groupId>
    <artifactId>spring-modulith-events-jpa</artifactId>
    <version>0.6.0</version>
</dependency>
```

```java
// 使用 @ApplicationModuleListener 搭配持久化，
// 即使處理失敗也不會遺失事件（可重試）
@ApplicationModuleListener
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void handleOrderPlaced(OrderPlacedEvent event) {
    // 如果這裡拋出例外，Spring Modulith 會在稍後重試
    emailService.sendOrderConfirmation(event.getUserEmail(), event.getOrderId());
}
```

---

## 與微服務版本的架構對比

### 相同點

| 面向 | 微服務版 | Modulith 版 |
|------|---------|------------|
| 模組邊界 | 服務邊界（網路隔離） | 包邊界（編譯器隔離） |
| 模組間通訊 | Kafka 事件 | Spring Application Events |
| 鬆耦合 | 是（事件驅動） | 是（事件驅動） |
| 各模組獨立 DB | 是（各有 MySQL） | 否（共用一個 DB，但可用 Schema 分隔） |

### 技術差異

| 功能 | 微服務版 | Modulith 版 |
|------|---------|------------|
| 服務間呼叫 | HTTP（OpenFeign） | 直接方法呼叫（同 JVM） |
| 事件傳遞 | Kafka | Spring ApplicationEvent |
| 部署單元 | 5個 JAR | 1個 JAR |
| 資料庫 | 5個 MySQL | 1個 MySQL（多個 Schema） |
| 測試 | 需要 Testcontainers 或 Mock | `@ApplicationModuleTest` |
| 分散式追蹤 | Zipkin + Sleuth | 不需要 |
| 服務發現 | Eureka | 不需要 |

---

## 何時選擇 Spring Modulith vs 微服務？

### 選擇 Spring Modulith 的時機

```
✅ 團隊人數少（< 20 人）
✅ 系統剛起步，需求未穩定
✅ 不想花太多時間在基礎設施上
✅ 想先驗證商業模式
✅ 資料一致性要求高
✅ 預算有限
```

### 選擇微服務的時機

```
✅ 不同部分有不同的擴展需求
   （商品查詢流量大，但訂單相對少）
✅ 不同服務需要不同的技術堆疊
✅ 多個獨立團隊開發不同服務
✅ 需要獨立部署和升級特定功能
✅ 各服務有不同的 SLA（可用性要求）
```

---

## 完整設定

```yaml
# application.yml（Modulith 版，單一服務）

server:
  port: 8080

spring:
  application:
    name: ecommerce
  datasource:
    url: jdbc:mysql://localhost:3306/ecommerce
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root}
  jpa:
    hibernate:
      ddl-auto: validate
  cache:
    type: redis
  redis:
    host: ${REDIS_HOST:localhost}

# Spring Modulith 設定
spring.modulith:
  events:
    # 使用 JPA 持久化事件（防止遺失）
    republish-outstanding-events-on-restart: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

logging:
  level:
    com.example.ecommerce: INFO
    org.springframework.modulith: DEBUG  # 開發時查看模組事件流程
```

---

## 本文與其他文章的關係

本文綜合應用了本系列的所有核心技術，並以「單體 vs 微服務」的架構對比，幫助你選擇合適的設計方向：

| 技術 | 應用之處 | 對應文章 |
|------|---------|---------|
| 自動配置 | Spring Modulith 自動設定 | [[01-自動配置-Auto-Configuration]] |
| Starter | Modulith Starter | [[02-Spring-Boot-Starter]] |
| 多環境配置 | dev/prod 設定 | [[03-應用程式配置-Properties-and-YAML]] |
| REST API | 各模組 Controller | [[04-Spring-MVC-RESTful-API]] |
| JPA | 各模組 Entity/Repository | [[05-Spring-Data-JPA]] |
| Security | User 模組 JWT 認證 | [[06-Spring-Security]] |
| Actuator | 健康監控 | [[07-Spring-Boot-Actuator]] |
| Logging | 模組事件追蹤 | [[08-日誌管理-Logging]] |
| Testing | `@ApplicationModuleTest` | [[09-測試-Testing]] |
| 異常處理 | 全域 + 模組級別 | [[10-異常處理-Exception-Handling]] |
| Validation | DTO 驗證 | [[11-驗證-Validation]] |
| Caching | Catalog 模組快取 | [[12-快取-Caching]] |
| Scheduling | 庫存監控排程 | [[13-排程任務-Scheduling]] |
| Async | 通知非同步執行 | [[14-非同步處理-Async]] |
| AOP | 稽核日誌 | [[15-AOP-切面導向程式設計]] |
| DevTools | 開發時熱重載 | [[16-Spring-Boot-DevTools]] |
| 部署 | Docker 單一容器 | [[17-打包與部署]] |
| 微服務版本 | 對比參考 | [[19-電商微服務系統-實戰總結]] |
