# StudyHub 自习室预约系统

StudyHub 提供自习室和座位浏览、预约、签到及离场流程。移动端面向预约用户，管理端面向自习室、座位、规则和报表管理。两端共用 Spring Boot API 和 MySQL。

| 目录 | 用途 |
| --- | --- |
| [backend](backend/README.md) | Java 17、Spring Boot API 和数据库初始化 |
| [mobile-web](mobile-web/) | Vue 3 移动端 |
| [admin-web](admin-web/) | Vue 3 管理端 |
| [docs](docs/) | 系统设计、使用与启动说明 |

## Docker 本地部署

需要 Docker 和 Compose v2。在仓库根目录创建 `.env`，填写以下变量。

```dotenv
DB_ROOT_PASSWORD=replace_with_a_mysql_root_password
DB_PASSWORD=replace_with_an_app_database_password
AUTH_SECRET=replace_with_a_long_random_value
MOBILE_PORT=5173
ADMIN_PORT=5174
```

```sh
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

移动端 [http://localhost:5173](http://localhost:5173)，管理端 [http://localhost:5174](http://localhost:5174)。健康检查路径为 `/api/v1/health`。

MySQL 首次启动时创建数据库和应用用户，后端首次运行时初始化表与演示数据。MySQL 和上传文件分别保存在数据卷中。演示账号见 [后端说明](backend/README.md)。

使用 `docker compose logs --tail=100` 查看日志。使用 `docker compose down` 停止服务并保留数据卷。

## 本地开发

安装 JDK 17、Maven、MySQL 8 和 Node.js 22。启动 MySQL 后，按 [后端说明](backend/README.md) 配置数据库连接。在三个终端分别运行以下命令。

| 工作目录 | 命令 | 地址 |
| --- | --- | --- |
| `backend/` | `mvn spring-boot:run` | `http://localhost:8080/api/v1/health` |
| `mobile-web/` | `npm ci` 后执行 `npm run dev` | `http://localhost:5173` |
| `admin-web/` | `npm ci` 后执行 `npm run dev` | `http://localhost:5174` |

遇到页面可以打开但请求失败的情况，先检查后端健康接口，再检查 MySQL 连接与对应的 Vite 代理。
