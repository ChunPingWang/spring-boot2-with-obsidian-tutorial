# Spring Boot DevTools

## 本章已驗證範例

- 對應專案：`examples/boot2-tutorial-app`
- 主要程式：`boot2-tutorial-app/pom.xml`、`application.properties`
- 驗證測試：`mvn test`（功能不依賴 DevTools 啟用）
- 校正重點：DevTools 只應在開發期使用，本倉庫以 optional 依賴示範，不把它當成正式環境必要元件。


## 什麼是 DevTools？

Spring Boot DevTools 是一套**開發期間**的工具集，能夠提升開發體驗：

- **自動重啟（Automatic Restart）**：程式碼變更後自動重啟應用程式
- **LiveReload**：靜態資源（HTML、CSS、JS）變更後自動刷新瀏覽器
- **快取停用**：開發時停用 Thymeleaf 等模板引擎的快取
- **Remote Debugging**：支援遠端除錯

> **注意**：DevTools 只在開發環境生效。打包成 JAR 執行或 IDE 執行時偵測到 `fully packaged application` 會自動停用。

---

## 加入依賴

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>  <!-- 只在執行期間使用 -->
    <optional>true</optional>  <!-- 不傳遞給依賴此專案的其他模組 -->
</dependency>
```

---

## 自動重啟

DevTools 使用兩個 ClassLoader：

1. **Base ClassLoader**：載入不常變動的類別（第三方依賴）
2. **Restart ClassLoader**：載入你自己寫的程式碼

當你的程式碼變更時，只重新載入 **Restart ClassLoader**，速度比完整重啟快很多。

### 設定

```properties
# application.properties

# 關閉自動重啟（如果你不需要）
spring.devtools.restart.enabled=false

# 設定觸發重啟的目錄（預設監看 classpath 上的檔案）
spring.devtools.restart.additional-paths=src/main/java

# 排除不觸發重啟的目錄
spring.devtools.restart.exclude=static/**,public/**,templates/**

# 使用觸發器檔案（只有此檔案變更才重啟，適合大型專案）
spring.devtools.restart.trigger-file=.reloadtrigger
```

---

## LiveReload

DevTools 內建 LiveReload 伺服器（port 35729），配合瀏覽器插件可以在靜態資源變更後自動刷新頁面。

```properties
# 關閉 LiveReload（如果不需要）
spring.devtools.livereload.enabled=false
```

---

## 開發時停用快取

DevTools 自動停用開發環境的快取，你不需要手動設定：

| 設定 | DevTools 自動設定的值 |
|------|---------------------|
| `spring.thymeleaf.cache` | `false` |
| `spring.freemarker.cache` | `false` |
| `spring.mvc.log-resolved-exception` | `true` |
| `spring.web.resources.cache.period` | `0` |

---

## H2 Console 設定

使用 DevTools + H2 時，會自動開啟 H2 Console：

```properties
# 這些是 DevTools 自動設定的值（不需要手動加入）
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

啟動後，到 `http://localhost:8080/h2-console` 可以管理 H2 資料庫。

---

## 全域 DevTools 設定

可以設定套用到所有使用 DevTools 的專案：

**位置（Windows）**：`%USERPROFILE%\.config\spring-boot\spring-boot-devtools.properties`

**位置（Linux/Mac）**：`~/.config/spring-boot/spring-boot-devtools.properties`

```properties
# 全域 DevTools 設定
spring.devtools.restart.enabled=true
spring.devtools.livereload.enabled=true
```

---

## 搭配 IDE 設定

### IntelliJ IDEA

1. **啟用自動建置**：
   - `Settings > Build, Execution, Deployment > Compiler`
   - 勾選 `Build project automatically`

2. **啟用執行中自動建置**：
   - `Settings > Advanced Settings`
   - 勾選 `Allow auto-make to start even if developed application is currently running`

3. 現在程式碼存檔後，IDEA 會自動建置，DevTools 偵測到 classpath 變更後自動重啟

### Eclipse / Spring Tool Suite

1. 預設開啟 `Project > Build Automatically`，儲存時自動建置
2. DevTools 會自動偵測並重啟

---

## Remote DevTools（遠端除錯）

可以連接到遠端執行中的 Spring Boot 應用程式進行除錯（需要謹慎使用，不可用於正式環境）：

```properties
# 遠端應用程式的 application.properties
spring.devtools.remote.secret=my-secret-key
```

本地執行遠端客戶端：
```bash
java -cp app.jar org.springframework.boot.devtools.RemoteSpringApplication https://remote-host
```

---

## 重點整理

- DevTools 只在開發時使用，打包後自動停用
- 自動重啟比完整重啟快，因為只重載你自己的程式碼
- 自動停用模板引擎快取，讓變更立即生效
- 搭配 IDE 的「自動建置」功能效果最佳
- H2 Console 在 DevTools 下自動開啟

---

## 相關文章

- [[00-Spring-Boot-2-簡介與快速入門]]
- [[17-打包與部署]]（打包後 DevTools 自動停用）
- [[03-應用程式配置-Properties-and-YAML]]
