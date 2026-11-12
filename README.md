# Bài tập 3 — Refresh Token Rotation

Identity Service dùng Spring Boot 3.5.6, JJWT 0.11.5 và PostgreSQL. Bài này kế thừa đăng ký, đăng nhập và cấp cặp token từ bài 1.

## Chạy

Tạo database `identity_db`, cấu hình `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` nếu khác giá trị mặc định trong `src/main/resources/application.yml`, rồi chạy `./gradlew bootRun`. Đặt `JWT_SECRET` riêng khi triển khai thực tế.

## Kiểm thử API

1. `POST /api/auth/register` với `{"username":"alice","password":"secret123"}`.
2. `POST /api/auth/login` với cùng body để nhận `accessToken` A và `refreshToken` R1.
3. `POST /api/auth/refresh` với `{"refreshToken":"<R1>"}`. Endpoint này public, không cần gửi Access Token. Kết quả là `accessToken` B và `refreshToken` R2; cả hai khác token cũ.
4. Gửi lại R1 tới `/api/auth/refresh`: kết quả `403 Forbidden`.
5. Gửi R2 tới `/api/auth/refresh`: kết quả `200 OK` cùng cặp token mới.

Việc kiểm tra hạn, xóa R1 và cấp R2 nằm trong một transaction. Truy vấn R1 dùng khóa ghi để hai yêu cầu đồng thời không cùng sử dụng được một token. JWT có `jti` ngẫu nhiên để token mới luôn khác token cũ ngay cả khi tạo trong cùng một giây.

Chạy unit test: `./gradlew test`.
