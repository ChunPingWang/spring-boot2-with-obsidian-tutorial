# Spring Security

## 什麼是 Spring Security？

Spring Security 是 Spring 生態系中負責**認證（Authentication）**與**授權（Authorization）**的框架。

- **認證（Authentication）**：確認使用者身份（「你是誰？」）
- **授權（Authorization）**：確認使用者是否有權限（「你能做什麼？」）

---

## 加入依賴

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

加入後，Spring Boot 會自動：
- 要求所有請求都需要認證
- 產生一個預設帳號（`user`）和隨機密碼（在 console 輸出）
- 提供一個基本的登入頁面

---

## 基本設定：表單登入

```java
package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 設定安全規則
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 公開頁面：不需要認證
                .antMatchers("/", "/home", "/public/**").permitAll()
                // 管理員頁面：需要 ADMIN 角色
                .antMatchers("/admin/**").hasRole("ADMIN")
                // API：需要 USER 或 ADMIN 角色
                .antMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                // 其他所有請求都需要認證
                .anyRequest().authenticated()
            )
            // 啟用表單登入
            .formLogin(form -> form
                .loginPage("/login")           // 自定義登入頁面
                .defaultSuccessUrl("/dashboard")  // 登入成功後跳轉
                .failureUrl("/login?error")       // 登入失敗後跳轉
                .permitAll()
            )
            // 啟用登出
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }

    // 設定測試用使用者（實際應用要改成從資料庫讀取）
    @Bean
    public InMemoryUserDetailsManager userDetailsManager() {
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("user123"))
            .roles("USER")
            .build();

        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin123"))
            .roles("ADMIN", "USER")
            .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    // 密碼編碼器（BCrypt 是推薦的方式）
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## REST API 安全：JWT 認證

現代 REST API 通常使用 **JWT（JSON Web Token）** 做無狀態認證。

### 加入 JWT 依賴

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

### JWT 工具類別

```java
package com.example.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

    @Value("${app.jwt.secret:mySecretKey1234567890123456789012}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")  // 預設 24 小時
    private long jwtExpiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // 生成 JWT Token
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    // 從 Token 取得使用者名稱
    public String getUsernameFromToken(String token) {
        return getClaim(token, Claims::getSubject);
    }

    // 驗證 Token
    public boolean validateToken(String token, UserDetails userDetails) {
        String username = getUsernameFromToken(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return getClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
        return claimsResolver.apply(claims);
    }
}
```

### JWT 過濾器

```java
package com.example.demo.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 從 Authorization Header 取得 Token
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);  // 去掉 "Bearer " 前綴

            try {
                String username = jwtUtils.getUsernameFromToken(token);

                // 尚未認證才處理
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtUtils.validateToken(token, userDetails)) {
                        // 建立認證物件並放入 SecurityContext
                        UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception e) {
                // Token 無效，繼續過濾鏈（未認證狀態）
                logger.warn("JWT 驗證失敗：" + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### REST API 安全設定

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // REST API 不需要 CSRF 保護
            .csrf().disable()

            // 無狀態 Session（JWT 不用 Session）
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()

            .authorizeHttpRequests(auth -> auth
                // 登入、註冊、公開 API 不需要認證
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/public/**").permitAll()
                // 其他都需要認證
                .anyRequest().authenticated()
            )

            // 在 UsernamePasswordAuthenticationFilter 之前插入 JWT 過濾器
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 認證 Controller（登入/註冊）

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 建構子略...

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Spring Security 驗證帳號密碼
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "帳號或密碼錯誤"));
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(
            request.getUsername());
        String token = jwtUtils.generateToken(userDetails);

        return ResponseEntity.ok(Map.of(
            "token", token,
            "username", userDetails.getUsername(),
            "roles", userDetails.getAuthorities()
        ));
    }

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "使用者名稱已存在"));
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setEmail(request.getEmail());
        userRepository.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("message", "註冊成功"));
    }
}
```

---

## 從資料庫載入使用者

```java
package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("找不到使用者：" + username));

        // 將角色轉換為 Spring Security 的 GrantedAuthority
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            user.isActive(),  // enabled
            true,             // accountNonExpired
            true,             // credentialsNonExpired
            true,             // accountNonLocked
            authorities
        );
    }
}
```

---

## 方法層級的安全控制

```java
// 在 SecurityConfig 上加入
@EnableMethodSecurity  // Spring Boot 2.7+（或 @EnableGlobalMethodSecurity）

// 在 Service 或 Controller 上使用
@Service
public class UserService {

    // 只有 ADMIN 角色可以執行
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) { ... }

    // 必須是本人或 ADMIN
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
    public User getProfile(String username) { ... }

    // 執行後才檢查回傳值
    @PostAuthorize("returnObject.username == authentication.name")
    public User findUser(Long id) { ... }

    // 過濾集合：只回傳 userId 等於當前使用者的記錄
    @PostFilter("filterObject.userId == authentication.principal.id")
    public List<Order> findOrders() { ... }
}
```

---

## 取得目前登入的使用者

```java
// 方法一：注入 Authentication
@GetMapping("/me")
public ResponseEntity<?> currentUser(Authentication authentication) {
    return ResponseEntity.ok(authentication.getPrincipal());
}

// 方法二：注入 Principal
@GetMapping("/me")
public ResponseEntity<?> currentUser(Principal principal) {
    return ResponseEntity.ok(principal.getName());  // 取得 username
}

// 方法三：使用注解
@GetMapping("/me")
public ResponseEntity<?> currentUser(
        @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(userDetails.getUsername());
}

// 方法四：從 SecurityContext 取得（可在任何地方使用）
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
```

---

## application.properties 常用設定

```properties
# 設定預設帳號（加入 Security 後）
spring.security.user.name=admin
spring.security.user.password=admin123
spring.security.user.roles=ADMIN

# JWT 設定
app.jwt.secret=myVeryLongSecretKeyForJWT1234567890
app.jwt.expiration=86400000
```

---

## 重點整理

- `@EnableWebSecurity` + `SecurityFilterChain` Bean 是 Spring Boot 2.x 推薦的配置方式
- `permitAll()` 開放端點，`authenticated()` 要求認證，`hasRole()` 要求角色
- REST API 使用 JWT 認證，需禁用 CSRF 並設定 Stateless Session
- 密碼必須使用 `BCryptPasswordEncoder` 編碼，絕不能儲存明文
- `@PreAuthorize` 提供方法層級的安全控制
- 使用 `@AuthenticationPrincipal` 或 `SecurityContextHolder` 取得目前使用者

---

## 相關文章

- [[04-Spring-MVC-RESTful-API]]
- [[05-Spring-Data-JPA]]
- [[10-異常處理-Exception-Handling]]
