# 異常處理（Exception Handling）

## 本章已驗證範例

- 對應專案：`examples/boot2-tutorial-app`
- 主要程式：`ProductNotFoundException`、`GlobalExceptionHandler`
- 驗證測試：`ProductApiTest`
- 校正重點：異常處理章節已對齊 `@RestControllerAdvice` 的實作，驗證 404 與 400 的結構化錯誤回應。


## 概述

良好的異常處理能讓 API 回傳清晰的錯誤訊息，而不是讓前端看到難懂的 500 Stack Trace。Spring Boot 提供 `@ExceptionHandler` 和 `@ControllerAdvice` 實現集中式異常處理。

---

## 預設的錯誤處理

不加任何設定時，Spring Boot 的 `BasicErrorController` 會處理未捕獲的異常：

```json
{
  "timestamp": "2024-01-01T10:00:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/users/99"
}
```

這不夠友善，我們需要自定義。

---

## 自定義例外類別

### 業務例外（不需要 Stack Trace，不影響效能）

```java
package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// @ResponseStatus 讓 Spring 自動回傳對應的 HTTP 狀態碼
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s 不存在，%s：%s", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName() { return fieldName; }
    public Object getFieldValue() { return fieldValue; }
}
```

```java
// 業務邏輯例外
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("Email 已被使用：" + email);
    }
}

// 無效操作例外
public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}

// 未授權例外
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
```

---

## 統一錯誤回應格式

```java
package com.example.demo.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)  // null 欄位不序列化
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<FieldError> fieldErrors;  // 驗證錯誤時使用

    // 建構子
    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // 欄位錯誤（驗證失敗）
    public static class FieldError {
        private String field;
        private Object rejectedValue;
        private String message;

        public FieldError(String field, Object rejectedValue, String message) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
        }

        public String getField() { return field; }
        public Object getRejectedValue() { return rejectedValue; }
        public String getMessage() { return message; }
    }

    // Getters...
    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public List<FieldError> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(List<FieldError> fieldErrors) { this.fieldErrors = fieldErrors; }
}
```

---

## 全域異常處理器 @RestControllerAdvice

```java
package com.example.demo.exception;

import com.example.demo.common.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

// @RestControllerAdvice = @ControllerAdvice + @ResponseBody
// 攔截所有 Controller 拋出的例外
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 處理資源找不到
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("資源不存在：{}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            getPath(request)
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // 處理重複資源
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex, WebRequest request) {

        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            getPath(request)
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // 處理驗證失敗（@Valid 拋出）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        BindingResult bindingResult = ex.getBindingResult();

        List<ErrorResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
            .map(fe -> new ErrorResponse.FieldError(
                fe.getField(),
                fe.getRejectedValue(),
                fe.getDefaultMessage()
            ))
            .collect(Collectors.toList());

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "請求資料驗證失敗，請檢查各欄位",
            getPath(request)
        );
        error.setFieldErrors(fieldErrors);

        return ResponseEntity.badRequest().body(error);
    }

    // 處理路徑參數型別錯誤（如 /api/users/abc，但 id 要 Long）
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        String message = String.format("參數 '%s' 的值 '%s' 格式不正確",
            ex.getName(), ex.getValue());

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            message,
            getPath(request)
        );
        return ResponseEntity.badRequest().body(error);
    }

    // 處理未授權
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex, WebRequest request) {

        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            ex.getMessage(),
            getPath(request)
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // 兜底處理（所有未預期的例外）
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        // 未預期的錯誤要記錄完整 Stack Trace
        log.error("未預期的錯誤：{}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "系統發生錯誤，請稍後再試",  // 不要暴露內部錯誤訊息
            getPath(request)
        );
        return ResponseEntity.internalServerError().body(error);
    }

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
```

---

## 在 Service 中拋出例外

```java
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateEmailException(user.getEmail());
        }
        return userRepository.save(user);
    }

    public User updateUser(Long id, User updatedUser) {
        User existing = findById(id);  // 找不到會拋出 ResourceNotFoundException

        if (!existing.getEmail().equals(updatedUser.getEmail())
                && userRepository.existsByEmail(updatedUser.getEmail())) {
            throw new DuplicateEmailException(updatedUser.getEmail());
        }

        existing.setName(updatedUser.getName());
        existing.setEmail(updatedUser.getEmail());
        return userRepository.save(existing);
    }
}
```

---

## 錯誤回應範例

### 找不到資源（404）

```json
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "User 不存在，id：99",
  "path": "/api/users/99"
}
```

### 驗證失敗（400）

```json
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "請求資料驗證失敗，請檢查各欄位",
  "path": "/api/users",
  "fieldErrors": [
    {"field": "name", "rejectedValue": "", "message": "名稱不可為空"},
    {"field": "email", "rejectedValue": "invalid-email", "message": "Email 格式不正確"}
  ]
}
```

### 重複資源（409）

```json
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Email 已被使用：alice@example.com",
  "path": "/api/users"
}
```

---

## 測試異常處理

```java
@WebMvcTest(UserController.class)
class UserControllerExceptionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void getUser_WhenNotFound_ShouldReturn404WithErrorBody() throws Exception {
        when(userService.findById(99L))
            .thenThrow(new ResourceNotFoundException("User", "id", 99));

        mockMvc.perform(get("/api/users/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("User 不存在，id：99"));
    }
}
```

---

## 重點整理

- 使用自定義例外類別表達業務錯誤（`ResourceNotFoundException`、`DuplicateEmailException` 等）
- `@RestControllerAdvice` + `@ExceptionHandler` 集中處理所有例外
- 提供統一的錯誤回應格式（`ErrorResponse`），方便前端處理
- 業務例外不需要印 Stack Trace（用 `log.warn`），未預期的例外要印（用 `log.error`）
- 不要把系統內部錯誤訊息暴露給客戶端

---

## 相關文章

- [[04-Spring-MVC-RESTful-API]]
- [[11-驗證-Validation]]（驗證失敗的異常處理）
- [[09-測試-Testing]]
- [[08-日誌管理-Logging]]
