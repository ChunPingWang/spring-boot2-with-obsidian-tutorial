# 驗證（Validation）

## 概述

Spring Boot 使用 **Bean Validation（JSR 380）** 規範，搭配 **Hibernate Validator** 實作，提供宣告式的資料驗證機制。只需在欄位上加注解，就能自動驗證進來的資料。

---

## 加入依賴

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## 常用驗證注解

| 注解 | 說明 | 適用型別 |
|------|------|---------|
| `@NotNull` | 不可為 null | 任意型別 |
| `@NotEmpty` | 不可為 null 或空字串/集合 | String, Collection, Array |
| `@NotBlank` | 不可為 null 或空白字串（含空格） | String |
| `@Size(min, max)` | 長度或大小範圍 | String, Collection, Array |
| `@Min(value)` | 最小值 | 數字型別 |
| `@Max(value)` | 最大值 | 數字型別 |
| `@Range(min, max)` | 數值範圍 | 數字型別 |
| `@Positive` | 正數（> 0） | 數字型別 |
| `@PositiveOrZero` | 正數或零（>= 0） | 數字型別 |
| `@Negative` | 負數（< 0） | 數字型別 |
| `@Email` | Email 格式 | String |
| `@Pattern(regexp)` | 符合正規表達式 | String |
| `@Past` | 過去的日期 | 日期型別 |
| `@Future` | 未來的日期 | 日期型別 |
| `@PastOrPresent` | 過去或現在的日期 | 日期型別 |
| `@Digits(integer, fraction)` | 數字位數限制 | 數字型別 |
| `@DecimalMin` | 最小值（Decimal） | 數字型別 |
| `@DecimalMax` | 最大值（Decimal） | 數字型別 |
| `@AssertTrue` | 必須為 true | boolean |
| `@AssertFalse` | 必須為 false | boolean |

---

## 在 DTO 上加驗證注解

使用 **DTO（Data Transfer Object）** 接收請求，與 Entity 分離：

```java
package com.example.demo.dto;

import javax.validation.constraints.*;
import java.time.LocalDate;

public class CreateUserRequest {

    @NotBlank(message = "名稱不可為空白")
    @Size(min = 2, max = 50, message = "名稱長度須在 {min} 到 {max} 個字元之間")
    private String name;

    @NotBlank(message = "Email 不可為空白")
    @Email(message = "Email 格式不正確")
    private String email;

    @NotBlank(message = "密碼不可為空白")
    @Size(min = 8, message = "密碼至少 {min} 個字元")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "密碼須包含大寫字母、小寫字母和數字"
    )
    private String password;

    @NotNull(message = "年齡不可為空")
    @Min(value = 1, message = "年齡最小為 {value}")
    @Max(value = 150, message = "年齡最大為 {value}")
    private Integer age;

    @NotNull(message = "生日不可為空")
    @Past(message = "生日必須是過去的日期")
    private LocalDate birthDate;

    @Pattern(regexp = "^09\\d{8}$", message = "手機號碼格式錯誤（例：0912345678）")
    private String phoneNumber;  // 選填

    // Getters and Setters...
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
```

---

## 在 Controller 啟用驗證

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        // 如果驗證失敗，@Valid 會自動拋出 MethodArgumentNotValidException
        // 由 GlobalExceptionHandler 處理（見 [[10-異常處理-Exception-Handling]]）

        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    // 驗證查詢參數
    @GetMapping
    public List<UserResponse> getUsers(
            @RequestParam @Min(0) int page,
            @RequestParam @Min(1) @Max(100) int size) {
        return userService.findAll(page, size);
    }

    // 驗證路徑參數
    @GetMapping("/{id}")
    public UserResponse getUser(
            @PathVariable @Positive Long id) {
        return toResponse(userService.findById(id));
    }
}
```

> **注意**：在 Controller 方法的參數上使用 `@Min`、`@Max` 等注解時，Controller 類別需要加上 `@Validated`：

```java
@RestController
@RequestMapping("/api/users")
@Validated  // 啟用方法參數驗證
public class UserController { ... }
```

---

## 巢狀物件驗證

```java
public class CreateOrderRequest {

    @NotNull
    @Valid  // 遞迴驗證巢狀物件
    private AddressDTO shippingAddress;

    @NotEmpty(message = "訂單至少需要一個項目")
    @Valid  // 驗證 List 中的每個元素
    private List<OrderItemDTO> items;

    // Getters and Setters...
}

public class AddressDTO {

    @NotBlank(message = "縣市不可為空")
    private String city;

    @NotBlank(message = "地址不可為空")
    private String address;

    @Pattern(regexp = "\\d{3}|\\d{5}", message = "郵遞區號格式不正確")
    private String zipCode;

    // Getters and Setters...
}

public class OrderItemDTO {

    @NotNull
    @Positive
    private Long productId;

    @NotNull
    @Min(1)
    private Integer quantity;

    // Getters and Setters...
}
```

---

## 自定義驗證注解

當內建注解無法滿足業務需求時，可以自定義：

### 1. 定義注解

```java
package com.example.demo.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueEmailValidator.class)
@Documented
public @interface UniqueEmail {

    String message() default "Email 已被使用";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

### 2. 實作驗證邏輯

```java
package com.example.demo.validation;

import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository userRepository;

    public UniqueEmailValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            return true;  // null 或空白交給 @NotBlank 處理
        }
        return !userRepository.existsByEmail(email);
    }
}
```

### 3. 使用自定義注解

```java
public class CreateUserRequest {

    @NotBlank
    @Email
    @UniqueEmail  // 自定義驗證：Email 是否已存在
    private String email;

    // ...
}
```

---

## 跨欄位驗證（類別級別）

當驗證需要比較多個欄位時（如確認密碼），使用類別級別的驗證：

```java
// 1. 定義注解
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchValidator.class)
public @interface PasswordMatch {
    String message() default "兩次密碼輸入不一致";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String password();
    String confirmPassword();
}

// 2. 實作驗證器
public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, Object> {

    private String passwordField;
    private String confirmPasswordField;

    @Override
    public void initialize(PasswordMatch constraintAnnotation) {
        this.passwordField = constraintAnnotation.password();
        this.confirmPasswordField = constraintAnnotation.confirmPassword();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        try {
            Object password = obj.getClass()
                .getMethod("get" + capitalize(passwordField))
                .invoke(obj);
            Object confirmPassword = obj.getClass()
                .getMethod("get" + capitalize(confirmPasswordField))
                .invoke(obj);

            if (password == null) return true;
            return password.equals(confirmPassword);
        } catch (Exception e) {
            return false;
        }
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}

// 3. 使用
@PasswordMatch(password = "password", confirmPassword = "confirmPassword")
public class RegisterRequest {

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    private String confirmPassword;

    // ...
}
```

---

## 驗證群組（Validation Groups）

不同操作使用不同的驗證規則：

```java
// 定義驗證群組（標記介面即可）
public interface OnCreate {}
public interface OnUpdate {}

public class UserDTO {

    // 只在更新時必填（ID）
    @NotNull(groups = OnUpdate.class, message = "更新時需要提供 ID")
    private Long id;

    // 建立時必填，更新時選填
    @NotBlank(groups = OnCreate.class, message = "名稱不可為空")
    @Size(max = 50)
    private String name;

    @Email
    @UniqueEmail(groups = OnCreate.class)  // 只在建立時檢查唯一性
    private String email;

    // Getters and Setters...
}
```

```java
@PostMapping
public ResponseEntity<?> createUser(
        @Validated(OnCreate.class) @RequestBody UserDTO dto) { ... }

@PutMapping("/{id}")
public ResponseEntity<?> updateUser(
        @PathVariable Long id,
        @Validated(OnUpdate.class) @RequestBody UserDTO dto) { ... }
```

---

## 在 Service 中手動驗證

有時需要在 Service 中程式化地觸發驗證：

```java
@Service
public class UserService {

    private final Validator validator;

    public UserService(Validator validator) {
        this.validator = validator;
    }

    public void processUser(CreateUserRequest request) {
        // 手動觸發驗證
        Set<ConstraintViolation<CreateUserRequest>> violations =
            validator.validate(request);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("驗證失敗：" + errors);
        }

        // 繼續業務邏輯...
    }
}
```

---

## 訊息自定義與國際化

建立 `src/main/resources/ValidationMessages.properties`：

```properties
user.name.notblank=使用者名稱不可為空白
user.email.invalid=Email 格式不正確
user.age.range=年齡必須在 {min} 到 {max} 歲之間
```

使用自定義訊息：
```java
@NotBlank(message = "{user.name.notblank}")
private String name;
```

---

## 重點整理

- `spring-boot-starter-validation` 提供 Bean Validation 支援
- 在 Controller 使用 `@Valid` 或 `@Validated` 觸發驗證
- 驗證失敗會拋出 `MethodArgumentNotValidException`，需在異常處理器中處理
- 自定義注解 = 自定義注解介面 + 實作 `ConstraintValidator`
- 跨欄位驗證使用類別級別注解
- Validation Groups 讓不同操作使用不同驗證規則

---

## 相關文章

- [[10-異常處理-Exception-Handling]]（驗證失敗的統一處理）
- [[04-Spring-MVC-RESTful-API]]
- [[05-Spring-Data-JPA]]（Entity 上也可加驗證注解）
