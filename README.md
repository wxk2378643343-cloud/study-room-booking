# StudyHub

前后端分离项目目录：

- `backend/`：Spring Boot 服务端
- `admin-web/`：Vue 3 管理端
- `mobile-web/`：Vue 3 用户端（移动优先）
- `docs/`：方案与设计文档

## 启动顺序
1. 启动后端：`cd backend && mvn spring-boot:run`
2. 启动管理端：`cd admin-web && npm install && npm run dev`（`http://localhost:5174`）
3. 启动用户端：`cd mobile-web && npm install && npm run dev`（`http://localhost:5173`）

说明：
- 后端已支持自动建库建表和演示数据初始化
- 旧目录 `frontend/` 仅为迁移副本，待你关闭相关进程后可删除
