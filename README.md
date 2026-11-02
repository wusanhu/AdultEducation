# 继续教育学分管理平台

面向继续教育业务的在线学习与学分管理系统，包含 **Spring Boot 后端 API**、**管理后台 Web** 和 **学员移动端 H5**。覆盖培训班管理、课程与视频学习、学习行为防刷、身份核验、学分发放/补录/撤销、毕业多级审批、电子证书、统计报表与系统管理。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 21、Spring Boot 3.3.5、Spring Security + JWT、MyBatis-Plus 3.5.7、MySQL 8、Redis、EasyExcel、OpenPDF |
| 管理后台 | Vue 3 + TypeScript + Vite + Ant Design Vue 4 + Pinia + ECharts |
| 移动端 | Vue 3 + TypeScript + Vite + Vant 4 + Pinia |
| 数据库 | MySQL 8，启动时自动建库建表并写入演示数据 |
| 部署 | Spring Boot 内置 Tomcat + Nginx 反向代理，见 `docs/deployment.md` |

## 目录结构

```
adulteducation/
├── backend/                 Spring Boot 后端（含自动建表脚本、静态托管的 admin/m）
│   └── src/main/resources/db/
│       ├── schema.sql        26 张业务表，CREATE TABLE IF NOT EXISTS，可重复执行
│       └── data.sql          角色、菜单、字典、参数、演示账号/课程/学习数据
├── admin/                   管理后台源码（Vue3 + Ant Design Vue）
├── mobile/                  学员移动端 H5 源码（Vue3 + Vant）
├── docs/                    数据库设计、接口清单、部署说明
└── deploy/                  Nginx / systemd 示例配置
```

## 快速启动

### 1. 环境要求

- JDK 21、Maven 3.9+
- Node.js 18+（建议 20）
- MySQL 8.0，`127.0.0.1:3306`，账号 `root`，默认空密码
- Redis 6+，`127.0.0.1:6379`

数据库连接可在 `backend/src/main/resources/application.yml` 中修改。首次启动会自动：

1. 通过 JDBC `createDatabaseIfNotExist=true` 创建 `adult_edu` 库；
2. 执行 `schema.sql` 建表（幂等）；
3. 执行 `data.sql` 写入初始数据（`INSERT IGNORE`，不覆盖已有数据）。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
# 或打包运行
mvn -DskipTests package
java -jar target/credit-platform.jar
```

后端地址：`http://127.0.0.1:8080`
Swagger UI：`http://127.0.0.1:8080/swagger-ui.html`

### 3. 启动管理后台（开发模式）

```bash
cd admin
npm install
npm run dev          # http://127.0.0.1:5173/admin/
```

生产构建会输出到后端静态目录，访问 `http://127.0.0.1:8080/admin/`：

```bash
npm run build
```

### 4. 启动移动端 H5（开发模式）

```bash
cd mobile
npm install
npm run dev          # http://127.0.0.1:5174/m/
```

生产构建输出到 `backend/src/main/resources/static/m`，访问 `http://127.0.0.1:8080/m/`：

```bash
npm run build
```

> 两个前端均使用 Hash 路由，因此不需要额外的 history 重写规则即可由 Spring Boot 直接托管。

## 演示账号

| 账号 | 密码 | 角色 | 说明 |
| --- | --- | --- | --- |
| `admin` | `admin123` | 超级管理员 | 全部菜单与数据 |
| `leader` | `leader123` | 领导 | 毕业复核、报表查看 |
| `teacher` | `teacher123` | 教员 | 课程管理、学习监控、初审 |
| `student` | `student123` | 学员 | 移动端学习、学分、毕业申请 |
| `student2` | `student123` | 学员 | 用于多学员数据演示 |
| `student3` | `student123` | 学员 | 用于多学员数据演示 |
| `teacher2` | `teacher123` | 教员 | 用于多教员演示 |

演示数据中的视频为占位 MP4，人脸核验与短信均为本地模拟模式（`app.face.mock=true`、`app.sms.mock=true`）。

## 核心业务规则

- **权限**：RBAC + 按钮权限；数据范围支持全部 / 本组织及下级 / 本部门 / 仅本人 / 指定组织。
- **登录安全**：BCrypt 密码、连续失败锁定、Redis 单会话令牌、首次登录强制改密、操作/登录日志。
- **学习防刷**：30 秒心跳；心跳间隔超过 90 秒暂停计时；禁止拖动进度（可配置）；切屏次数 ≥3 或时长 ≥30 秒标记异常；随机 300–600 秒弹窗，60 秒超时，3 次超时判定作弊。
- **身份核验**：档位 0/1/2/3/9 与自定义 JSON；支持人脸模拟比对与短信验证码兜底；抓拍图保留 180 天。
- **学分**：课程完成自动发放；管理端人工补录可配置“领导复核 + 管理员终审”；支持撤销并留痕。
- **毕业审批**：按项目配置教员 → 领导 → 管理员节点，全部通过后生成带编号的 PDF 电子证书。
- **报表**：总览、单位汇总、培训班学分、课程学习、学习预警，支持 Excel 导出与预警站内提醒。

## 文档

- [数据库设计](docs/database.md)
- [接口清单](docs/api.md)
- [部署说明](docs/deployment.md)
- [测试记录](docs/testing.md)
