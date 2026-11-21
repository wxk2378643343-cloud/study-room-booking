# StudyHub 自习室预约系统

![Java 17](https://img.shields.io/badge/Java%2017-2563EB?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-0F766E?style=flat-square)
![Vue 3](https://img.shields.io/badge/Vue%203-334155?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-B45309?style=flat-square)

## 📌 项目简介

面向自习室使用者和管理人员。用户在移动端浏览、预约座位并签到，管理人员维护座位、规则和报表。

## 🏗️ 系统架构

- 移动端和管理端共享 Spring Boot API。
- MySQL 保存自习室、座位、预约与用户数据，两个前端分别通过 Nginx 对接 API。

## 📁 项目结构

| 路径 | 用途 |
| --- | --- |
| [`backend`](backend) | API 和数据库初始化 |
| [`mobile-web`](mobile-web) | 移动端 |
| [`admin-web`](admin-web) | 管理端 |
| [`docs`](docs) | 设计及启动指南 |

## 🚀 如何启动

可以用 Compose 一次启动两个前端、后端和数据库。

### Docker 本地部署

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

### 本地开发

安装 JDK 17、Maven、MySQL 8 和 Node.js 22。启动 MySQL 后，按 [后端说明](backend/README.md) 配置数据库连接。在三个终端分别运行以下命令。

| 工作目录 | 命令 | 地址 |
| --- | --- | --- |
| `backend/` | `mvn spring-boot:run` | `http://localhost:8080/api/v1/health` |
| `mobile-web/` | `npm ci` 后执行 `npm run dev` | `http://localhost:5173` |
| `admin-web/` | `npm ci` 后执行 `npm run dev` | `http://localhost:5174` |

遇到页面可以打开但请求失败的情况，先检查后端健康接口，再检查 MySQL 连接与对应的 Vite 代理。
