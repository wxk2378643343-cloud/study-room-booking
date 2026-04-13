# StudyHub Backend

## Run
1. Update `src/main/resources/application.yml` database credentials.
2. Start service:

```bash
mvn spring-boot:run
```

默认已开启自动初始化：
- 启动时会检测 `sys_user` 表是否存在
- 不存在则自动执行 `classpath:db/schema.sql` 和 `classpath:db/seed.sql`
- JDBC URL 已开启 `createDatabaseIfNotExist=true`，数据库不存在会自动创建

如需关闭自动初始化：
- `application.yml` 设置 `app.db.auto-init: false`

## Base URL
- `http://localhost:8080/api/v1`
- Health check: `GET /health`

## Demo Accounts
- Admin: `13800000001 / 123456`
- User: `13800000002 / 123456`

## Main APIs
- Auth: `/auth/send-code`, `/auth/register`, `/auth/login`, `/auth/me`
- Room: `/rooms`, `/rooms/{id}`, `/rooms/{id}/seat-slots`, `/rooms/{id}/reviews`
- Booking: `/bookings`, `/bookings/mine`, `/bookings/checkin`, `/bookings/{id}/cancel|leave|return|checkout`
- User: `/users/profile`, `/users/violations`, `/users/points`, `/users/coupons`, `/users/notifications`
- Admin: `/admin/rooms`, `/admin/rooms/{id}/seats`, `/admin/rooms/{id}/rule`, `/admin/reports`, `/admin/dashboard`
