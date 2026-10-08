# Demo ATTT: đưa API token vào source code

## Rủi ro và kiểm soát

- Tài sản: thông tin xác thực truy cập dịch vụ.
- Tình huống: lập trình viên ghi token trực tiếp vào file cấu hình rồi push.
- Tác động nếu token thật: người đọc repository/lịch sử/artifact có thể sử dụng token trái phép.
- Kiểm soát: Gitleaks quét source trước build. Nếu phát hiện, job `secret-scan` thất bại;
  `build-and-test` và `deploy` bị bỏ qua, không publish image của commit đó.
- Bằng chứng: log job và artifact `secret-scan-report` (giá trị secret đã che).

Token trong demo được tạo với tiền tố `demo_only_`, không do nhà cung cấp nào phát hành
và không thể dùng để đăng nhập. Rule `demo-hardcoded-api-token` là rule mô phỏng bổ sung;
Gitleaks vẫn bật các rule mặc định để phát hiện các loại secret được hỗ trợ.
File demo không được Spring tự nạp; việc vi phạm chính là lưu credential dạng plaintext
trong source, không cần app sử dụng nó.

## 1. Đưa bước quét bảo mật lên GitHub

Trước khi bật case lỗi, commit các thay đổi setup:

```bash
git add .github/workflows/ci.yml .gitleaks.toml .gitignore scripts/scan-secrets.sh scripts/security-demo.sh docs/security-risk-demo.md README.md
git commit -m "Add secret scanning gate and security risk demo"
git push origin main
```

Mở Actions: job **Secret scan (security gate)** xanh, rồi build/publish tiếp tục.
Nếu có secret thật đã tồn tại, xử lý phát hiện trước; không tắt rule để ép pipeline xanh.

## 2. Bật vi phạm và xem pipeline chặn

Tại thư mục dự án:

```bash
bash scripts/security-demo.sh enable
bash scripts/scan-secrets.sh
```

Lệnh quét local cần Docker. Kết quả mong đợi: exit code 1, phát hiện
`demo-hardcoded-api-token`, file `src/main/resources/security-demo.properties`.

Đưa **file giả** này lên GitHub để quan sát gate:

```bash
git add src/main/resources/security-demo.properties
git commit -m "Demo ATTT: simulated hardcoded API token"
git push origin main
```

Trong Actions:

1. **Secret scan (security gate)** đỏ ở bước **Scan source for leaked secrets**.
2. Mở log để xem rule, file và vị trí phát hiện, không thấy giá trị token đầy đủ.
3. Tải artifact **secret-scan-report** để xem báo cáo JSON đã che secret.
4. `build-and-test` và `deploy` là **Skipped**. Không tạo/publish image của commit lỗi.

Nếu muốn demo trên PR, tạo nhánh `demo/secret-leak` trước khi bật vi phạm,
push nhánh rồi mở PR vào main. Để GitHub thực sự chặn merge, cần cấu hình ruleset
hoặc branch protection yêu cầu check **Secret scan (security gate)** thành công;
workflow thất bại tự nó không cấm người có quyền merge.

## 3. Khắc phục và chạy lại

```bash
bash scripts/security-demo.sh disable
bash scripts/scan-secrets.sh
git add -u src/main/resources/security-demo.properties
git commit -m "Remove simulated hardcoded token"
git push origin main
```

Job secret scan xanh trở lại, build/publish tiếp tục. Nếu làm trên nhánh PR,
push cùng nhánh để cập nhật check; publish chỉ chạy ở main.

## Giới hạn và xử lý sự cố thật

Gate quét thư mục source hiện tại, **không quét toàn bộ lịch sử Git**. Nhờ đó xóa
fixture giả làm demo xanh trở lại. Nó không ngăn dữ liệu được push lên GitHub;
nó chặn các bước build/publish/deploy sau push. Muốn chặn trước push, bổ sung
pre-commit hoặc GitHub push protection cho các mẫu được hỗ trợ.

Nếu token thật đã bị commit: thu hồi/đổi token ngay, kiểm tra nhật ký sử dụng,
xử lý lịch sử Git và bản sao/artifact liên quan theo phạm vi sự cố. Xóa file khỏi
commit mới không làm token cũ an toàn trở lại. Scanner có thể bỏ sót hoặc báo nhầm;
không dùng kết quả xanh làm bằng chứng rằng không còn mọi rủi ro ATTT.

Cấu hình `.gitleaks.toml` và workflow cũng cần review/bảo vệ để tránh người sửa code
đồng thời vô hiệu hóa kiểm soát. Demo này chưa bao gồm quét CVE, CodeQL hay kiểm soát
phê duyệt deploy.

Gitleaks CLI: https://github.com/gitleaks/gitleaks#usage
