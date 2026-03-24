# Spring Boot 教學文件實作總結

本倉庫已將各章 Markdown 的主要範例補成可執行專案，並以 `mvn test` 驗證所有示範程式。

## 專案結構

- `examples/demo-greeting-spring-boot-autoconfigure`：Spring Boot 2 自動配置範例
- `examples/demo-greeting-spring-boot-starter`：自製 Starter 封裝範例
- `examples/boot2-tutorial-app`：00–17 章的主要可執行教學應用程式
- `examples/commerce-microservices`：第 19 章的電商微服務最小範例
- `examples/commerce-modulith`：第 20 章的 Moduliths / 模組化單體範例（Boot 2.7 / JDK 8）

## 測試方式

在倉庫根目錄執行：

```bash
mvn test
```

若要產出 Boot 2 教學應用程式 JAR，可再執行：

```bash
mvn -pl examples/boot2-tutorial-app -am package
```

## 各文件說明

| Markdown | 主題摘要 | 對應實作 | 主要測試 |
| --- | --- | --- | --- |
| `00-Spring-Boot-2-簡介與快速入門.md` | 介紹 Spring Boot 2 與第一個 REST API。 | `examples/boot2-tutorial-app` 的 `TutorialApplication`、`GreetingController` | `GreetingControllerTest` |
| `01-自動配置-Auto-Configuration.md` | 說明自動配置原理與屬性綁定。 | `examples/demo-greeting-spring-boot-autoconfigure` | `GreetingAutoConfigurationTest` |
| `02-Spring-Boot-Starter.md` | 說明 Starter 的拆分方式與引用方式。 | `examples/demo-greeting-spring-boot-starter` | `GreetingAutoConfigurationTest`、`GreetingControllerTest` |
| `03-應用程式配置-Properties-and-YAML.md` | 介紹外部化設定、屬性綁定與 Profile。 | `application.properties`、`TutorialProperties` | `TutorialPropertiesTest` |
| `04-Spring-MVC-RESTful-API.md` | 示範 RESTful API 設計與 Spring MVC 控制器。 | `ProductController`、`ProductService` | `ProductApiTest` |
| `05-Spring-Data-JPA.md` | 示範 JPA Entity、Repository 與 H2 資料庫。 | `Product`、`ProductRepository` | `ProductRepositoryTest`、`ProductApiTest` |
| `06-Spring-Security.md` | 示範 JWT 驗證、登入與受保護 API。 | `SecurityConfig`、`AuthController`、`JwtTokenService` | `AuthControllerTest`、`JwtTokenServiceTest`、`ProductApiTest` |
| `07-Spring-Boot-Actuator.md` | 說明健康檢查與管理端點。 | `boot2-tutorial-app` 管理設定 | `ActuatorHealthTest` |
| `08-日誌管理-Logging.md` | 示範 SLF4J / Logback 的使用方式。 | `CheckoutService` | `CheckoutServiceTest` |
| `09-測試-Testing.md` | 說明本倉庫如何以多層次測試驗證範例。 | 所有 `src/test/java` 測試 | `mvn test` 全部通過 |
| `10-異常處理-Exception-Handling.md` | 示範例外類別與全域錯誤處理。 | `ProductNotFoundException`、`GlobalExceptionHandler` | `ProductApiTest` |
| `11-驗證-Validation.md` | 示範 DTO 驗證與欄位錯誤回應。 | `CreateProductRequest` | `ProductApiTest` |
| `12-快取-Caching.md` | 示範 `@Cacheable` 與快取命中。 | `CachedCatalogService` | `CachedCatalogServiceTest` |
| `13-排程任務-Scheduling.md` | 示範 `@Scheduled` 排程任務。 | `ReportScheduler` | `ReportSchedulerTest` |
| `14-非同步處理-Async.md` | 示範 `@Async` 與命名執行緒池。 | `AsyncConfig`、`NotificationService` | `NotificationServiceTest` |
| `15-AOP-切面導向程式設計.md` | 示範切面攔截與方法記錄。 | `ExecutionTimeAspect`、`MonitoredBusinessService` | `ExecutionTimeAspectTest` |
| `16-Spring-Boot-DevTools.md` | 說明 DevTools 在開發期的用途與限制。 | `boot2-tutorial-app/pom.xml`、`application.properties` | `mvn test` |
| `17-打包與部署.md` | 說明 JAR 打包與 Dockerfile 配置。 | `boot2-tutorial-app/pom.xml`、`Dockerfile` | `mvn test` |
| `19-電商微服務系統-實戰總結.md` | 以三個服務示範電商微服務切分。 | `examples/commerce-microservices` | `UserProfileServiceTest`、`CatalogProductServiceTest`、`OrderApplicationServiceTest` |
| `20-電商系統-Spring-Modulith-版本.md` | 以 Modulith 示範模組邊界與事件。 | `examples/commerce-modulith` | `ApplicationModulesTest`、`OrderServiceIntegrationTest` |

## 補充說明

- 本倉庫所有可執行範例已統一對齊 Spring Boot 2.7 與 JDK 8；第 20 章改以 legacy Moduliths 示範 Boot 2 可用的模組化單體作法。
- 若要延伸文件中的 YAML、Docker、部署或 Observability 片段，可直接以 `examples/` 下的專案為基礎擴充。
