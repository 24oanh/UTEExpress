# UTEExpress — README

> Tài liệu hướng dẫn cài đặt, cấu hình và chạy dự án backend (Spring Boot)

---

## Tổng quan

Dự án này là một ứng dụng backend Java Spring Boot (packaging: WAR) dành cho hệ thống quản lý vận chuyển/giao nhận (UTEExpress). Nó bao gồm các chức năng: xác thực người dùng, quản lý đơn hàng, kho, shipper, báo cáo, thanh toán (ZaloPay/Momo), chat/hỗ trợ, thông báo, và tracking.

> Lưu ý bảo mật: repository chứa file cấu hình mẫu `application.properties` có **những khóa bí mật (API keys, mật khẩu DB, v.v.)**. **KHÔNG** giữ những giá trị thật trong repo công khai. Thay thế bằng biến môi trường hoặc tệp cấu hình riêng (ví dụ `application.properties.local`) và không push lên Git.

---

## Yêu cầu (Prerequisites)

- Java 17 (OpenJDK hoặc Oracle JDK)
- Maven (nếu không dùng `./mvnw` wrapper)
- SQL Server (theo cấu hình mặc định trong project) hoặc cơ sở dữ liệu tương thích với Hibernate/SQL Server dialect.
- (Tùy chọn) Cloudinary account, ZaloPay sandbox keys, Momo config nếu muốn chạy tính năng upload & thanh toán.

Phiên bản Java được chỉ định trong `pom.xml` là `17`.

---

## Cấu trúc thư mục chính

```
UTEExpress-3/
├─ pom.xml
├─ mvnw, mvnw.cmd
├─ src/
│  ├─ main/
│  │  ├─ java/ltweb/           # mã nguồn Java (controller, service, repository, dto, config...)
│  │  └─ resources/
│  │     ├─ application.properties  # cấu hình mặc định (chứa placeholders/secrets nên cẩn trọng)
│  └─ test/
```

Các controller chính nằm trong `src/main/java/ltweb/controller/` (ví dụ: `AuthController`, `CustomerOrderController`, `WarehouseController`, `ShipperController`, `PaymentController`, `TrackingController`, ...).

---

## Cấu hình (Configuration)

Project dùng file `src/main/resources/application.properties`. Thông tin quan trọng cần cấu hình trước khi chạy:

- `spring.datasource.url` — URL kết nối tới SQL Server
- `spring.datasource.username` và `spring.datasource.password`
- `spring.jpa.*` — cấu hình JPA/Hibernate (mặc định `ddl-auto=update`)
- `cloudinary.cloud-name`, `cloudinary.api-key`, `cloudinary.api-secret` — (nếu dùng Cloudinary)
- `payment.*` — endpoint/keys cho ZaloPay/Momo

### Ví dụ file `application.properties.template` (KHÔNG CHỨA GIÁ TRỊ THẬT)

```properties
spring.application.name=UTEExpress

# Database (ví dụ: SQL Server)
spring.datasource.url=jdbc:sqlserver://<DB_HOST>:<PORT>;databaseName=<DB_NAME>;encrypt=true;trustServerCertificate=true
spring.datasource.username=<DB_USERNAME>
spring.datasource.password=<DB_PASSWORD>

spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect

# Cloudinary (replace with env vars in production)
cloudinary.cloud-name=<CLOUD_NAME>
cloudinary.api-key=<CLOUD_KEY>
cloudinary.api-secret=<CLOUD_SECRET>

# Payment gateways (example placeholders)
payment.zalopay.endpoint=<ZALOPAY_ENDPOINT>
payment.momo.endpoint=<MOMO_ENDPOINT>

# Connection pool (HikariCP)
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=20
```

**Khuyến cáo:** lưu các giá trị bí mật trong biến môi trường hoặc secret management (Vault, Docker secrets, Kubernetes secrets). Không commit secrets vào Git.

---

## Cách chạy (Local)

1. Mở terminal tại thư mục gốc project (nơi có `pom.xml`).

2. Build & chạy bằng Maven wrapper (Linux/macOS):

```bash
# build
./mvnw clean package

# hoặc chạy trực tiếp (development)
./mvnw spring-boot:run
```

Trên Windows dùng `mvnw.cmd`:

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

Sau khi chạy, ứng dụng mặc định sẽ lắng nghe port cấu hình trong code (mặc định là `8080` nếu không thay đổi). Kiểm tra logs để biết port chính xác hoặc giá trị `server.port` trong `application.properties`.

---

## Packaging

Project được packaging dạng `war`. Sau khi `mvnw package` sẽ sinh file `.war` trong `target/`. Có thể deploy file `.war` vào container/servlet container nếu cần.

---

## Endpoints nổi bật (tổng quan)

Dự án có nhiều controller phục vụ các chức năng. Dưới đây là các base path chính (tổng hợp từ mã nguồn):

- `POST/GET /customer` — các thao tác liên quan đến khách hàng (đăng ký, xác thực, profile, tracking)
- `/customer/orders` — quản lý đơn hàng của customer
- `/warehouse` — thông tin kho, inventory, báo cáo
- `/warehouse/orders` — quản lý đơn hàng ở kho (assign shipper, update status)
- `/shipper` — API dành cho shipper (xem đơn, cập nhật trạng thái)
- `/admin/*` — các API quản trị (users, notifications, reports, chat admin)
- `/api/payment/*` — callback/endpoint thanh toán (ZaloPay/Momo)
- `/api/tracking/*` — tracking và cập nhật tracking

> Đây là tóm tắt mục đích; để biết đầy đủ danh sách endpoint và tham số, mở thư mục `src/main/java/ltweb/controller` và đọc annotation `@GetMapping`, `@PostMapping`, `@RequestMapping` trong từng file controller.

---

## Cách chạy test

Nếu dự án có test, chạy:

```bash
./mvnw test
```

---

## Debug & Logging

- `spring.jpa.show-sql=true` để in SQL ra console (đang dùng trong `application.properties`).
- Thay đổi mức log trong `application.properties` hoặc cấu hình `logback` nếu project có file config logging.

---

## Gợi ý phát triển & đóng gói

- Tách cấu hình môi trường bằng profile Spring (`application-dev.properties`, `application-prod.properties`) hoặc dùng biến môi trường.
- Bảo mật: remove khóa ở `application.properties`, dùng hệ thống secret management.
- Viết thêm collection Postman / OpenAPI (Swagger) để test và tài liệu hóa API.

---

## Hướng dẫn đóng góp

1. Fork repository
2. Tạo branch feature: `git checkout -b feat/xxx`
3. Viết code, unit test
4. Tạo pull request mô tả thay đổi

---

## Liên hệ

Nếu cần hỗ trợ chỉnh sửa README theo nhu cầu (thêm phần hướng dẫn chạy bằng Docker, cấu hình CI/CD, hoặc tóm tắt API chi tiết), cho mình biết chi tiết mong muốn.

---

*README này được tạo tự động dựa trên cấu trúc project. Kiểm tra và chỉnh sửa các phần placeholder (DB, API keys) trước khi chạy.*

