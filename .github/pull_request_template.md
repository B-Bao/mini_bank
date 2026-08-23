## Mô tả thay đổi (Description)
<!-- Tóm tắt ngắn gọn tính năng hoặc bug fix trong PR này -->

## Loại thay đổi (Type of Change)
- [ ] `feat`: Tính năng mới
- [ ] `fix`: Sửa lỗi
- [ ] `refactor`: Tái cấu trúc code (không đổi logic nghiệp vụ)
- [ ] `chore`: Cập nhật cấu hình, dependency, migration

## Checklist Kỷ luật Code (Bắt buộc tích đủ trước khi Merge)
- [ ] Đã chạy `./mvnw spotless:apply` để format code chuẩn Google Style.
- [ ] Đã chạy `./mvnw verify` ở local và toàn bộ Unit/Integration Test đều PASS.
- [ ] Nếu có thay đổi Database: Đã tạo file migration Flyway mới (`V...__...sql`), tuyệt đối không sửa file cũ.
- [ ] Đã kiểm tra kỹ: **Không có secret, password, private key hoặc file `.env`** bị commit vào PR.
- [ ] Đã đồng bộ các biến môi trường mới vào file `.env.example` (nếu có).