# Spring Data JPA

## 什麼是 JPA 和 Spring Data JPA？

**JPA（Java Persistence API）** 是 Java 的 ORM（物件關聯對映）標準規範，讓你用 Java 物件操作資料庫，不需要手寫大量 SQL。

**Hibernate** 是最常用的 JPA 實作。

**Spring Data JPA** 在 JPA/Hibernate 之上再加一層抽象，只需定義 Interface，Spring 自動實作 CRUD 操作，大幅減少樣板程式碼。

---

## 加入依賴

```xml
<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- 開發時使用 H2 記憶體資料庫 -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- 或使用 MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## 設定資料庫連線

```properties
# application.properties（H2 記憶體資料庫）
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true

spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# none: 不做任何事
# validate: 驗證 schema，不修改
# update: 自動更新 schema（開發時常用）
# create: 每次啟動都重建 schema
# create-drop: 啟動時建立，關閉時刪除
spring.jpa.hibernate.ddl-auto=update
```

---

## 定義 Entity（實體類別）

Entity 對應資料庫的一張表，每個物件對應一筆記錄：

```java
package com.example.demo.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity                        // 標記為 JPA 實體
@Table(name = "users")         // 對應資料庫的 "users" 表（省略則用類別名稱）
public class User {

    @Id                        // 主鍵
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 自動遞增
    private Long id;

    @Column(nullable = false, length = 50)  // 欄位設定：不可為 null，最長 50
    private String name;

    @Column(unique = true, nullable = false)  // 唯一值
    private String email;

    @Column(name = "phone_number")  // 對應不同欄位名稱
    private String phoneNumber;

    private int age;

    @Enumerated(EnumType.STRING)  // 枚舉儲存為字串（不建議用 ORDINAL）
    private UserStatus status;

    @Column(updatable = false)   // 建立後不可修改
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 生命週期回調
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 無參數建構子（JPA 要求）
    public User() {}

    public User(String name, String email, int age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // Getters and Setters...
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

```java
// 枚舉類型
public enum UserStatus {
    ACTIVE, INACTIVE, SUSPENDED
}
```

---

## 定義 Repository

繼承 `JpaRepository`，免費獲得所有 CRUD 方法：

```java
package com.example.demo.repository;

import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// JpaRepository<Entity類型, 主鍵類型>
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ====== 方法命名查詢（Spring Data 自動實作）======

    // 根據 email 查詢（SELECT * FROM users WHERE email = ?）
    Optional<User> findByEmail(String email);

    // 根據名稱查詢（模糊搜尋）
    List<User> findByNameContaining(String name);

    // 根據年齡範圍查詢
    List<User> findByAgeBetween(int minAge, int maxAge);

    // 根據狀態查詢並排序
    List<User> findByStatusOrderByNameAsc(UserStatus status);

    // 根據年齡大於某值查詢
    List<User> findByAgeGreaterThan(int age);

    // 判斷 email 是否存在
    boolean existsByEmail(String email);

    // 計算特定狀態的使用者數量
    long countByStatus(UserStatus status);

    // ====== JPQL 自定義查詢 ======

    // 使用 JPQL（面向物件的查詢語言）
    @Query("SELECT u FROM User u WHERE u.age >= :minAge AND u.status = :status")
    List<User> findActiveUsersOlderThan(
        @Param("minAge") int minAge,
        @Param("status") UserStatus status);

    // ====== 原生 SQL 查詢 ======

    @Query(value = "SELECT * FROM users WHERE name LIKE %:keyword%",
           nativeQuery = true)
    List<User> searchByName(@Param("keyword") String keyword);

    // ====== 更新語句 ======

    @Modifying  // 宣告這是更新/刪除操作（必要）
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") UserStatus status);
}
```

---

## JpaRepository 內建方法

繼承 `JpaRepository` 後，自動擁有：

```java
// 查詢
List<User> findAll();                           // 取得全部
Optional<User> findById(Long id);              // 根據 ID 查詢
boolean existsById(Long id);                   // 判斷是否存在
long count();                                  // 計算總數

// 分頁與排序
Page<User> findAll(Pageable pageable);
List<User> findAll(Sort sort);

// 新增/更新（有 ID 則更新，無 ID 則新增）
User save(User user);
List<User> saveAll(List<User> users);

// 刪除
void deleteById(Long id);
void delete(User user);
void deleteAll();
```

---

## Service 層範例

```java
package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)  // 類別層級設為唯讀，節省資源
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 查詢不需要 @Transactional，使用類別預設的 readOnly=true
    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // 分頁查詢
    public Page<User> findAllPaged(int page, int size, String sortBy) {
        PageRequest pageable = PageRequest.of(
            page, size, Sort.by(sortBy).ascending()
        );
        return userRepository.findAll(pageable);
    }

    // 寫入操作需要 @Transactional（覆寫類別的 readOnly=true）
    @Transactional
    public User createUser(User user) {
        // 檢查 email 是否重複
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email 已存在：" + user.getEmail());
        }
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));

        existing.setName(updatedUser.getName());
        existing.setEmail(updatedUser.getEmail());
        existing.setAge(updatedUser.getAge());

        // save() 在同一個 Transaction 內，若物件已被管理（managed），
        // 直接修改屬性後 Transaction 結束時會自動 flush（dirty checking）
        return userRepository.save(existing);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }
}
```

---

## 分頁查詢

```java
// Controller 接收分頁參數
@GetMapping
public ResponseEntity<Page<User>> getUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "asc") String direction) {

    Sort.Direction sortDir = "desc".equalsIgnoreCase(direction)
        ? Sort.Direction.DESC : Sort.Direction.ASC;

    PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
    Page<User> result = userRepository.findAll(pageable);

    return ResponseEntity.ok(result);
}
```

回傳的 Page 物件包含：

```json
{
  "content": [...],           // 本頁資料
  "totalElements": 100,       // 總筆數
  "totalPages": 10,           // 總頁數
  "number": 0,                // 目前頁碼（從 0 開始）
  "size": 10,                 // 每頁筆數
  "first": true,              // 是否第一頁
  "last": false               // 是否最後一頁
}
```

---

## 實體關聯

### 一對多（@OneToMany）

```java
// 一個 User 有多個 Order
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // 一個 User 對多個 Order
    // mappedBy 指向 Order 的 user 欄位
    // cascade: 對 User 的操作會串聯到 Order
    // fetch: LAZY 表示用到時才查詢（建議）
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();
}

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;
    private int quantity;

    // 多個 Order 屬於一個 User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")  // 外鍵欄位名稱
    private User user;
}
```

### 多對多（@ManyToMany）

```java
@Entity
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany
    @JoinTable(
        name = "student_course",           // 中間表名稱
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
}

@Entity
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToMany(mappedBy = "courses")
    private Set<Student> students = new HashSet<>();
}
```

---

## 防止 N+1 問題

N+1 問題：查詢 100 個 User，再對每個 User 各發一次查詢取得 Orders，共 101 次查詢。

**解法：使用 JOIN FETCH**

```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.orders WHERE u.id = :id")
Optional<User> findByIdWithOrders(@Param("id") Long id);

// 或使用 @EntityGraph
@EntityGraph(attributePaths = {"orders"})
Optional<User> findWithOrdersById(Long id);
```

---

## 方法命名查詢關鍵字

| 關鍵字 | 範例 | SQL 等效 |
|--------|------|---------|
| `findBy` | `findByName` | `WHERE name = ?` |
| `And` | `findByNameAndEmail` | `WHERE name = ? AND email = ?` |
| `Or` | `findByNameOrEmail` | `WHERE name = ? OR email = ?` |
| `Between` | `findByAgeBetween` | `WHERE age BETWEEN ? AND ?` |
| `LessThan` | `findByAgeLessThan` | `WHERE age < ?` |
| `GreaterThan` | `findByAgeGreaterThan` | `WHERE age > ?` |
| `Like` | `findByNameLike` | `WHERE name LIKE ?` |
| `Containing` | `findByNameContaining` | `WHERE name LIKE %?%` |
| `StartingWith` | `findByNameStartingWith` | `WHERE name LIKE ?%` |
| `EndingWith` | `findByNameEndingWith` | `WHERE name LIKE %?` |
| `IsNull` | `findByEmailIsNull` | `WHERE email IS NULL` |
| `IsNotNull` | `findByEmailIsNotNull` | `WHERE email IS NOT NULL` |
| `In` | `findByStatusIn` | `WHERE status IN (?)` |
| `OrderBy` | `findByStatusOrderByNameAsc` | `ORDER BY name ASC` |
| `Top/First` | `findTop3ByOrderByAgeDesc` | `LIMIT 3` |

---

## 重點整理

- Entity 類別使用 `@Entity`、`@Id`、`@Column` 等注解定義資料庫結構
- Repository 繼承 `JpaRepository`，自動獲得 CRUD 方法
- 方法命名查詢讓 Spring Data 自動生成 SQL
- `@Transactional` 控制事務，寫入操作必須加上
- 分頁使用 `PageRequest.of(page, size)`
- 注意 N+1 問題，使用 JOIN FETCH 或 `@EntityGraph` 解決

---

## 相關文章

- [[04-Spring-MVC-RESTful-API]]
- [[06-Spring-Security]]
- [[10-異常處理-Exception-Handling]]
