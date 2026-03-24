# 快取（Caching）

## 本章已驗證範例

- 對應專案：`examples/boot2-tutorial-app`
- 主要程式：`CachedCatalogService`
- 驗證測試：`CachedCatalogServiceTest`
- 校正重點：快取章節已對齊 `@Cacheable` 的可執行範例，透過重複查詢只命中一次實際方法。


## 什麼是快取？

快取（Cache）是將**常用且不常變動的資料**暫存在記憶體中，避免重複查詢資料庫或進行昂貴的運算，從而提升系統效能。

典型場景：
- 商品分類清單（不常變動）
- 使用者資料（讀多寫少）
- 計算密集的運算結果
- 外部 API 呼叫結果

---

## 加入依賴

```xml
<!-- Spring 快取抽象層 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- 使用 Redis 作為快取（生產環境推薦） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- 或使用 Caffeine（本地快取，適合單機） -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

---

## 啟用快取

```java
@SpringBootApplication
@EnableCaching  // 啟用 Spring 快取功能
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

---

## 核心注解

### @Cacheable：查詢快取

第一次呼叫會執行方法並將結果存入快取；後續相同參數的呼叫直接從快取取得。

```java
@Service
public class ProductService {

    // 快取名稱："products"，key 為 productId
    @Cacheable(value = "products", key = "#productId")
    public Product findById(Long productId) {
        // 只有快取 miss 時才會執行這段程式碼
        System.out.println("從資料庫查詢 productId=" + productId);
        return productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
    }

    // 快取整個清單
    @Cacheable("product-categories")
    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    // 條件性快取：只有當結果不為 null 時才快取
    @Cacheable(value = "products", key = "#name", unless = "#result == null")
    public Product findByName(String name) {
        return productRepository.findByName(name).orElse(null);
    }

    // 帶條件的快取：只有 id > 0 才快取
    @Cacheable(value = "products", key = "#id", condition = "#id > 0")
    public Product findByIdConditional(Long id) {
        return productRepository.findById(id).orElse(null);
    }
}
```

### @CachePut：更新快取

每次都執行方法，並用結果更新快取（適用於更新操作）：

```java
// 更新商品後，同步更新快取
@CachePut(value = "products", key = "#product.id")
public Product updateProduct(Product product) {
    return productRepository.save(product);
}
```

### @CacheEvict：清除快取

清除快取中的資料（適用於刪除或重大更新操作）：

```java
// 刪除單一快取項目
@CacheEvict(value = "products", key = "#productId")
public void deleteProduct(Long productId) {
    productRepository.deleteById(productId);
}

// 清除整個快取（allEntries = true）
@CacheEvict(value = "products", allEntries = true)
public void clearProductCache() {
    // 可以是空方法，只是用來觸發清除
}

// 方法執行前清除（beforeInvocation = true）
// 適用於方法可能拋出例外的情況
@CacheEvict(value = "products", key = "#id", beforeInvocation = true)
public void deleteWithPreEvict(Long id) {
    productRepository.deleteById(id);
}
```

### @Caching：組合多個快取操作

```java
@Caching(
    cacheable = {
        @Cacheable(value = "products", key = "#id")
    },
    evict = {
        @CacheEvict(value = "product-list", allEntries = true)
    }
)
public Product findAndRefreshList(Long id) {
    return productRepository.findById(id).orElseThrow(...);
}
```

---

## 使用 Caffeine（本地快取）

適合單機應用或不需要跨服務共享快取的場景：

```properties
# application.properties

spring.cache.type=caffeine

# 設定快取規格：最多 500 筆，10 分鐘後過期
spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=10m
```

或用 Java Config 做更細緻的設定：

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 為不同快取設定不同的策略
        cacheManager.registerCustomCache("products",
            Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats()  // 啟用統計（方便監控）
                .build());

        cacheManager.registerCustomCache("product-categories",
            Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofHours(1))
                .build());

        // 預設設定（其他未指定的快取）
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(10))
        );

        return cacheManager;
    }
}
```

---

## 使用 Redis（分散式快取）

適合多服務共享快取或快取需要持久化的場景：

```properties
# application.properties

spring.cache.type=redis

# Redis 連線設定
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=
spring.redis.database=0

# 連線池設定
spring.redis.lettuce.pool.max-active=8
spring.redis.lettuce.pool.max-idle=8
spring.redis.lettuce.pool.min-idle=0
```

```java
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // 預設 TTL：30 分鐘
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()));

        // 為特定快取設定不同 TTL
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("products",
            defaultConfig.entryTtl(Duration.ofMinutes(60)));
        cacheConfigs.put("product-categories",
            defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
```

---

## 快取 Key 的寫法

```java
// 使用 SpEL 表達式自訂 key
@Cacheable(value = "users", key = "#userId")
public User findById(Long userId) { ... }

// 使用方法名稱（預設 key 包含所有參數）
@Cacheable("users")
public User findByNameAndEmail(String name, String email) { ... }
// key = SimpleKey[name, email]

// 自訂複合 key
@Cacheable(value = "users", key = "#name + '_' + #email")
public User findByNameAndEmail(String name, String email) { ... }

// 使用物件的屬性
@Cacheable(value = "users", key = "#user.email")
public User findByUser(User user) { ... }

// 使用方法名稱（keyGenerator）
@Cacheable(value = "users", keyGenerator = "customKeyGenerator")
public User findUser(Long id) { ... }
```

---

## 快取使用範例：商品服務

```java
@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 查詢單一商品（快取 60 分鐘）
    @Cacheable(value = "products", key = "#id")
    public ProductDTO findById(Long id) {
        log.debug("快取未命中，從資料庫查詢 id={}", id);
        return productRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    // 查詢分類下的商品
    @Cacheable(value = "products-by-category", key = "#categoryId")
    public List<ProductDTO> findByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // 更新商品（更新快取）
    @Transactional
    @CachePut(value = "products", key = "#id")
    @CacheEvict(value = "products-by-category", allEntries = true)
    public ProductDTO updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        return toDTO(productRepository.save(product));
    }

    // 刪除商品（清除快取）
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "products-by-category", allEntries = true)
    })
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    private ProductDTO toDTO(Product product) {
        return new ProductDTO(product.getId(), product.getName(), product.getPrice());
    }
}
```

---

## 注意事項

### 同類別方法呼叫快取失效

```java
@Service
public class UserService {

    // 問題：從同一類別內部呼叫 findById，快取不會生效！
    // 因為 Spring 的快取是透過 AOP Proxy 實作的，
    // 類別內部直接呼叫繞過了 Proxy
    public void doSomething(Long id) {
        User user = findById(id);  // ❌ 不會觸發快取
    }

    @Cacheable("users")
    public User findById(Long id) { ... }

    // 解法：從 Spring 容器取得自身的 Proxy
    @Autowired
    private UserService self;

    public void doSomethingFixed(Long id) {
        User user = self.findById(id);  // ✅ 透過 Proxy，快取生效
    }
}
```

### 快取不適合的場景

- 頻繁更新的資料
- 使用者個人化資料（每個使用者資料不同，快取 key 需包含 userId）
- 需要強一致性的場景

---

## 重點整理

- `@EnableCaching` 啟用快取功能
- `@Cacheable` 查詢快取、`@CachePut` 更新快取、`@CacheEvict` 清除快取
- 本地快取用 Caffeine，分散式快取用 Redis
- 可用 SpEL 表達式自訂 cache key
- 類別內部方法呼叫不走 Proxy，快取注解無效

---

## 相關文章

- [[05-Spring-Data-JPA]]
- [[07-Spring-Boot-Actuator]]（`/actuator/caches` 端點）
- [[15-AOP-切面導向程式設計]]（快取底層使用 AOP）
