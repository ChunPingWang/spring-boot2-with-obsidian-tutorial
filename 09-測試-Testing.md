# 測試（Testing）

## 本章已驗證範例

- 對應專案：`examples/boot2-tutorial-app`、`examples/commerce-microservices`、`examples/commerce-modulith`
- 主要程式：`src/test/java` 下各測試類別
- 驗證測試：`mvn test` 全部通過
- 校正重點：本倉庫現在以單元測試、JPA 測試、MockMvc 測試與模組驗證測試覆蓋所有示範程式。


## 概述

Spring Boot 提供完整的測試支援，`spring-boot-starter-test` 包含：

| 函式庫 | 用途 |
|--------|------|
| JUnit 5 | 測試框架 |
| Mockito | Mock 物件 |
| AssertJ | 流暢風格的斷言 |
| Spring Test | Spring 整合測試支援 |
| MockMvc | 測試 Web 層 |
| Testcontainers | 用 Docker 啟動真實資料庫 |

---

## 加入依賴

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 單元測試（Unit Test）

單元測試只測試單一類別，不啟動 Spring 容器，速度最快。

### 測試 Service

```java
package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // 啟用 Mockito
@DisplayName("UserService 測試")
class UserServiceTest {

    @Mock  // 建立 Mock 物件（不真正呼叫資料庫）
    private UserRepository userRepository;

    @InjectMocks  // 建立被測試物件，並自動注入 Mock
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User("Alice", "alice@example.com", 28);
        mockUser.setId(1L);
    }

    @Test
    @DisplayName("根據 ID 查詢使用者 - 成功")
    void findById_WhenExists_ReturnUser() {
        // Arrange（準備）：設定 Mock 行為
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // Act（執行）：呼叫被測試的方法
        Optional<User> result = userService.findById(1L);

        // Assert（驗證）：確認結果
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice");
        assertThat(result.get().getEmail()).isEqualTo("alice@example.com");

        // 驗證 Mock 有被呼叫一次
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("根據 ID 查詢使用者 - 不存在")
    void findById_WhenNotExists_ReturnEmpty() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = userService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("建立使用者 - Email 重複時拋出例外")
    void createUser_WhenEmailExists_ThrowException() {
        // 設定 email 已存在
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        // 驗證是否拋出例外
        assertThatThrownBy(() -> userService.createUser(mockUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email 已存在");

        // 驗證 save 沒有被呼叫
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("建立使用者 - 成功")
    void createUser_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        User result = userService.createUser(mockUser);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(userRepository).save(mockUser);
    }
}
```

---

## Web 層測試（MockMvc）

`@WebMvcTest` 只載入 Web 層（Controller），不啟動完整 Spring 容器：

```java
package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)  // 只載入 UserController 相關的 Bean
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;  // 模擬 HTTP 請求

    @MockBean  // 在 Spring 容器中建立 Mock Bean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllUsers_ShouldReturn200WithUserList() throws Exception {
        // Arrange
        List<User> users = Arrays.asList(
            createUser(1L, "Alice", "alice@example.com"),
            createUser(2L, "Bob",   "bob@example.com")
        );
        when(userService.findAll()).thenReturn(users);

        // Act & Assert
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())  // 印出請求和回應詳情
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Alice"))
            .andExpect(jsonPath("$[0].email").value("alice@example.com"))
            .andExpect(jsonPath("$[1].name").value("Bob"));
    }

    @Test
    void getUserById_WhenExists_ShouldReturn200() throws Exception {
        User user = createUser(1L, "Alice", "alice@example.com");
        when(userService.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void getUserById_WhenNotExists_ShouldReturn404() throws Exception {
        when(userService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void createUser_ShouldReturn201WithCreatedUser() throws Exception {
        User inputUser = createUser(null, "Charlie", "charlie@example.com");
        User savedUser = createUser(3L, "Charlie", "charlie@example.com");

        when(userService.createUser(any(User.class))).thenReturn(savedUser);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputUser)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(3))
            .andExpect(jsonPath("$.name").value("Charlie"));
    }

    private User createUser(Long id, String name, String email) {
        User user = new User(name, email, 25);
        user.setId(id);
        return user;
    }
}
```

---

## Repository 測試

`@DataJpaTest` 只載入 JPA 相關元件，使用內嵌資料庫（H2）：

```java
package com.example.demo.repository;

import com.example.demo.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;  // 直接操作 EntityManager（測試用）

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_WhenExists_ShouldReturnUser() {
        // Arrange：存入測試資料
        User user = new User("Alice", "alice@example.com", 28);
        entityManager.persist(user);
        entityManager.flush();

        // Act
        Optional<User> result = userRepository.findByEmail("alice@example.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice");
    }

    @Test
    void findByNameContaining_ShouldReturnMatchingUsers() {
        entityManager.persist(new User("Alice Smith", "alice@example.com", 28));
        entityManager.persist(new User("Bob Johnson", "bob@example.com", 32));
        entityManager.persist(new User("Alice Wong", "alice2@example.com", 25));
        entityManager.flush();

        List<User> result = userRepository.findByNameContaining("Alice");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getName)
            .containsExactlyInAnyOrder("Alice Smith", "Alice Wong");
    }

    @Test
    void save_ShouldPersistAndReturnUser() {
        User user = new User("Charlie", "charlie@example.com", 30);

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Charlie");
    }
}
```

---

## 整合測試（Integration Test）

`@SpringBootTest` 啟動完整 Spring 容器，測試整個應用程式流程：

```java
package com.example.demo;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest  // 啟動完整 Spring 容器
@AutoConfigureMockMvc  // 自動配置 MockMvc
@ActiveProfiles("test")  // 使用 test Profile（application-test.properties）
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        // 每個測試後清除資料
        userRepository.deleteAll();
    }

    @Test
    void createAndGetUser_IntegrationFlow() throws Exception {
        // 1. 建立使用者
        User newUser = new User("Alice", "alice@example.com", 28);

        String location = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

        // 2. 查詢建立的使用者
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Alice"));
    }
}
```

---

## 測試設定檔

```properties
# src/test/resources/application-test.properties

# 使用 H2 記憶體資料庫進行測試
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# 關閉 Security（若不需要測試安全性）
spring.security.enabled=false
```

---

## 測試 Security

```java
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)  // 載入 Security 設定
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void getUsers_WithoutAuth_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")  // 模擬已登入的使用者
    void getUsers_WithAuth_ShouldReturn200() throws Exception {
        when(userService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteUser_WithUserRole_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_WithAdminRole_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isNoContent());
    }
}
```

---

## 參數化測試

```java
@Test
@ParameterizedTest
@ValueSource(strings = {"alice@example.com", "bob@test.org", "charlie@mail.com"})
void validateEmail_ValidEmails_ShouldPass(String email) {
    assertThat(emailValidator.isValid(email)).isTrue();
}

@ParameterizedTest
@CsvSource({
    "Alice, 28, ACTIVE",
    "Bob,   32, INACTIVE",
    "Carol, 25, ACTIVE"
})
void createUser_WithVariousInputs(String name, int age, String status) {
    User user = new User(name, "test@example.com", age);
    user.setStatus(UserStatus.valueOf(status));

    assertThat(user.getName()).isEqualTo(name);
    assertThat(user.getAge()).isEqualTo(age);
}
```

---

## 重點整理

| 注解 | 載入範圍 | 適用場景 |
|------|---------|---------|
| `@ExtendWith(MockitoExtension.class)` | 不啟動 Spring | 純單元測試，使用 Mock |
| `@WebMvcTest` | 只有 Web 層 | 測試 Controller |
| `@DataJpaTest` | 只有 JPA 層 | 測試 Repository |
| `@SpringBootTest` | 完整容器 | 整合測試 |

- 單元測試用 `@Mock` + `@InjectMocks`（速度快）
- Web 層用 `MockMvc` 測試 HTTP 請求和回應
- 整合測試使用 `application-test.properties` 設定獨立的測試資料庫

---

## 相關文章

- [[04-Spring-MVC-RESTful-API]]
- [[05-Spring-Data-JPA]]
- [[06-Spring-Security]]
- [[10-異常處理-Exception-Handling]]
