# CI/CD PostgreSQL Demo — Java 17

Demo project dùng cho học và trình bày CI/CD:

- Java 17
- Spring Boot 3.5.16
- Spring Web
- Spring Data JPA / Hibernate
- PostgreSQL
- Docker Compose
- GitHub Actions CI/CD

## 1. Database tự tạo những bảng nào?

Khi Spring Boot khởi động lần đầu, Hibernate đọc các `@Entity` và tự tạo/cập nhật:

- `app_users`
- `products`
- `customer_orders`

`customer_orders` có khóa ngoại tới `app_users` và `products`.

Project cũng tự thêm một số dữ liệu mẫu nếu database đang rỗng.

## 2. Yêu cầu

- Java 17
- Maven 3.6.3+
- Docker Desktop hoặc Docker Engine + Compose

Kiểm tra:

```bash
java -version
mvn -version
docker --version
docker compose version
```

## 3. Cách nhanh nhất: chạy cả PostgreSQL + ứng dụng bằng 1 lệnh

Tại thư mục project:

```bash
docker compose up --build
```

Lệnh này sẽ:

1. Tạo PostgreSQL container.
2. Tự tạo database `cicd_demo`.
3. Build ứng dụng Java 17.
4. Chạy Spring Boot.
5. Hibernate tự tạo các bảng.
6. Chèn dữ liệu mẫu khi database đang rỗng.

Mở:

```text
http://localhost:8080/api/users
http://localhost:8080/api/products
http://localhost:8080/api/orders
```

Kiểm tra container:

```bash
docker ps
```

PostgreSQL mặc định:

- Host: `localhost`
- Port trên máy: `5488` (trong Docker network: `5432`)
- Database: `cicd_demo`
- Username: `postgres`
- Password: `postgres`

> Đây là thông tin demo local. Không dùng mật khẩu này cho production.

## 4. Hoặc chạy Java trực tiếp trên máy

Nếu chỉ muốn PostgreSQL chạy bằng Docker:

```bash
docker compose up -d postgres
```

Sau đó chạy Spring Boot trên máy:

```bash
DB_PORT=5488 mvn spring-boot:run
```

Hoặc build JAR:

```bash
mvn clean package
DB_PORT=5488 java -jar target/cicd-postgres-demo-0.0.1-SNAPSHOT.jar
```

Ứng dụng chạy tại:

```text
http://localhost:8080
```

## 5. API để thử

### Health

```bash
curl http://localhost:8080/api/health
```

### Danh sách user

```bash
curl http://localhost:8080/api/users
```

### Danh sách product

```bash
curl http://localhost:8080/api/products
```

### Danh sách order

```bash
curl http://localhost:8080/api/orders
```

### Tạo user

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"student","email":"student@example.com"}'
```

### Tạo product

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"USB-C Hub","price":49.99,"stock":20}'
```

### Tạo order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":1,"quantity":2}'
```

## 6. Xem bảng PostgreSQL

Vào container:

```bash
docker exec -it cicd-demo-postgres psql -U postgres -d cicd_demo
```

Trong `psql`:

```sql
\dt
SELECT * FROM app_users;
SELECT * FROM products;
SELECT * FROM customer_orders;
```

Thoát:

```text
\q
```

## 7. Vì sao bảng tự được tạo?

Trong `application.properties`:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Và mỗi model có `@Entity`, ví dụ:

```java
@Entity
@Table(name = "products")
public class Product {
    ...
}
```

Hibernate sẽ đối chiếu entity với PostgreSQL rồi tạo/cập nhật schema.

> `ddl-auto=update` phù hợp cho demo/dev. Trong production nên quản lý migration bằng Flyway hoặc Liquibase.

## 8. Pipeline CI/CD mới

File: `.github/workflows/ci.yml`.

```text
Push main / Pull request / Run workflow
    ↓
Test API với H2 + build JAR (mvn clean verify)
    ↓
Lưu JAR và báo cáo test vào Actions artifacts
    ↓
Build Docker image, tag theo commit SHA
    ↓
Chạy image + PostgreSQL thật, kiểm tra /api/health và /api/products
    ↓
Push image lên GHCR (chỉ main, không push từ pull request)
    ↓
Deploy qua SSH nếu đã bật → kiểm tra API trên server
```

Test thất bại sẽ chặn build image/publish/deploy. Container smoke test thất bại
sẽ chặn publish/deploy. CI dọn container và volume thử nghiệm sau mỗi lần chạy.

### Demo CI và publish, chưa cần server

1. Đưa project lên GitHub, branch `main`, bật GitHub Actions nếu đang tắt.
2. Push một thay đổi lên `main`.
3. Mở **Actions → CI/CD Demo** và xem từng bước chạy.
4. Tải `build-results` trong phần Artifacts để xem JAR và báo cáo test.
5. Mở **Packages** của tài khoản/repository để xem image. Image có dạng:
   `ghcr.io/<owner>/<repository>:<commit-sha>` (owner/repository chuyển thành chữ thường).

Workflow dùng `GITHUB_TOKEN` có quyền `packages: write`, không cần tạo token để
publish. Nếu tổ chức chặn tạo package/Actions, cần điều chỉnh policy tương ứng.
Job deploy được bỏ qua mặc định, CI và publish vẫn hoạt động.

Tài liệu GHCR: https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images

## 9. Bật CD lên server Linux qua SSH

Cần một server/VM Linux mà GitHub-hosted runner kết nối SSH được, có Docker Engine,
Docker Compose hỗ trợ `up --wait`, Bash và curl. Tài khoản deploy cần chạy được Docker
không dùng sudo. Image CI build trên Linux amd64, nên dùng server amd64 cho demo.

### Chuẩn bị server một lần

Tạo thư mục dưới home của tài khoản deploy:

```bash
mkdir -p ~/cicd-postgres-demo
cd ~/cicd-postgres-demo
```

Tạo `.env` theo `.env.example`, đặt `DB_PASSWORD` và `APP_PORT=8080`, rồi:

```bash
chmod 600 .env
```

Pipeline tự truyền `APP_IMAGE` của commit vừa build, nên giá trị APP_IMAGE trong
`.env` chỉ cần khi chạy thủ công. Không commit `.env` hoặc private key.

Để server pull image:

- Nếu package GHCR public: không cần đăng nhập; package mới có thể đang private,
  hãy kiểm tra visibility trong Package settings.
- Nếu package private: đăng nhập GHCR **trên server bằng đúng tài khoản deploy**
  với PAT classic có `read:packages` và quyền đọc package. Dùng
  `docker login ghcr.io -u <github-user>` và nhập token tại lời nhắc password.
  Token này dùng để pull trên server; GITHUB_TOKEN của workflow chỉ dùng publish.

Cho phép SSH public key trong `~/.ssh/authorized_keys` của tài khoản deploy.
Mở cổng SSH cho runner và cổng app nếu muốn truy cập từ trình duyệt.

### Cấu hình GitHub

Tạo environment **demo** trong **Settings → Environments**. Thêm các secrets
vào environment này (hoặc repository secrets):

| Secret | Giá trị |
| --- | --- |
| `DEPLOY_HOST` | IPv4 hoặc DNS của server, không kèm http:// |
| `DEPLOY_USER` | Tài khoản SSH trên server |
| `DEPLOY_SSH_KEY` | Toàn bộ private key SSH tương ứng public key trên server |
| `DEPLOY_KNOWN_HOSTS` | Dòng host key đã xác minh của server, định dạng known_hosts |

Có thể lấy host key bằng `ssh-keyscan -p 22 <server>`, rồi đối chiếu fingerprint
với server qua kênh tin cậy trước khi lưu. Với cổng khác 22, giữ dạng
`[host]:port` trong known_hosts.

Repository variables trong **Settings → Secrets and variables → Actions → Variables**:

| Variable | Giá trị |
| --- | --- |
| `DEPLOY_ENABLED` | `true` để tự deploy mỗi lần push main; mặc định chưa bật |
| `DEPLOY_PORT` | Cổng SSH, mặc định `22` |

Thử lần đầu bằng **Actions → CI/CD Demo → Run workflow → branch main → deploy = true**.
Khi đã chạy thành công, đặt repository variable `DEPLOY_ENABLED=true` để demo
continuous deployment. Nếu environment có required reviewers thì deploy sẽ chờ duyệt.

Pipeline copy Compose và script lên `~/cicd-postgres-demo`, pull đúng image theo
commit SHA, chạy app + PostgreSQL, rồi kiểm tra health và API đọc database.
Volume `cicd-demo_postgres_data` giữ dữ liệu qua các lần deploy. Không dùng
`down -v` trên server nếu cần giữ dữ liệu. Đây là stack riêng với Compose local;
không tự chuyển dữ liệu từ volume local. Khi demo cùng máy, chọn cổng app khác
trong `.env` nếu local đang dùng 8080.

Đổi DB_PASSWORD sau khi volume đã khởi tạo không tự đổi mật khẩu PostgreSQL;
cần đổi mật khẩu trong database trước và đồng bộ cấu hình.

### Kiểm tra và quay lại image cũ

Trên server:

```bash
cd ~/cicd-postgres-demo
docker compose -p cicd-demo -f docker-compose.deploy.yml ps
curl --fail http://localhost:8080/api/health
curl --fail http://localhost:8080/api/products
```

Nếu dùng cổng khác, thay `8080` bằng APP_PORT. Để quay lại image của commit cũ:

```bash
APP_IMAGE=ghcr.io/<owner>/<repository>:<old-commit-sha> bash scripts/deploy.sh
```

Deploy hiện báo FAILED nếu app không lên hoặc API lỗi; chưa tự rollback.
Rollback image không rollback schema/dữ liệu. `ddl-auto=update` chỉ dùng cho demo.

## 10. Kịch bản trình bày CI/CD

1. Push code lên main, xem test/build và image theo commit trong GHCR.
2. Bật deploy, xem job SSH và gọi API trên server.
3. Sửa chuỗi `application` trong `HealthController.home()`, push và mở `/`
   trên server để thấy phiên bản mới đã tự triển khai.
4. Trong test `healthEndpointReturnsUp`, cố tình đổi expected `UP` thành `DOWN`,
   push để pipeline đỏ. Image lỗi không được publish và job deploy bị bỏ qua.
5. Sửa expected về `UP`, push để pipeline xanh và deploy trở lại.

Chạy kiểm tra trước khi push:

```bash
mvn clean verify
docker compose up --build
```

Demo Docker image và PostgreSQL bằng cấu hình deploy ngay trên máy, không cần GHCR:

```bash
docker build -t cicd-postgres-demo:local .
APP_IMAGE=cicd-postgres-demo:local DB_PASSWORD=demo-password APP_PORT=18080 \
  COMPOSE_PROJECT_NAME=cicd-smoke SKIP_PULL=true bash scripts/deploy.sh
```

Dọn **stack thử nghiệm này** sau demo (xóa cả dữ liệu thử nghiệm):

```bash
APP_IMAGE=cicd-postgres-demo:local DB_PASSWORD=demo-password APP_PORT=18080 \
  docker compose -p cicd-smoke -f docker-compose.deploy.yml down -v
```
