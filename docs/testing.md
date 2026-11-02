# 测试记录

本文记录当前版本的构建、接口、业务与前端验收结果。测试环境为本地单机：MySQL 8.0.46（`127.0.0.1:3306`，root 空密码）、Redis 7、JDK 21、Node.js 20、Chromium（Playwright）。

## 1. 构建验证

| 项目 | 命令 | 结果 |
| --- | --- | --- |
| 后端编译 | `cd backend && mvn -q -DskipTests compile` | 通过 |
| 后端打包 | `cd backend && mvn -q -DskipTests package` | 通过，生成 `target/credit-platform.jar`（约 80 MB，含前端静态资源） |
| 管理后台 | `cd admin && npm run build` | 通过，输出到 `backend/src/main/resources/static/admin` |
| 移动端 H5 | `cd mobile && npm run build` | 通过，输出到 `backend/src/main/resources/static/m` |

前端均执行 TypeScript 类型检查后完成 Vite 构建，无类型错误。

## 2. 自动建库建表

- 首次启动时由 JDBC 参数 `createDatabaseIfNotExist=true` 自动创建 `adult_edu` 库。
- `spring.sql.init` 依次执行 `db/schema.sql` 与 `db/data.sql`。
- `schema.sql` 使用 `CREATE TABLE IF NOT EXISTS`，共 26 张表；`data.sql` 使用 `INSERT IGNORE`，重复启动不会覆盖业务数据。
- 已验证空库启动可自动完成建库、建表、初始化菜单/角色/字典/参数与演示数据。

## 3. 接口冒烟测试

使用管理员、领导、教员、学员四类账号分别登录，验证 JWT 签发与权限隔离。

| 场景 | 接口 | 结果 |
| --- | --- | --- |
| 登录与当前用户 | `POST /api/auth/login`、`GET /api/auth/me` | 200 |
| 数据总览 | `GET /api/reports/overview` | 200，返回项目/学员/课程/学分/毕业/核验统计 |
| 单位汇总 | `GET /api/reports/org` | 200 |
| 培训班学分统计 | `GET /api/reports/credit` | 200 |
| 课程学习统计 | `GET /api/reports/course` | 200 |
| 学习预警 | `GET /api/reports/warnings` | 200，含学分不足/未学习/毕业未申请等类型 |
| 用户分页 | `GET /api/system/users?pageNum=1&pageSize=5` | 200 |
| 组织树 | `GET /api/system/org/tree` | 200 |
| 参数配置 | `GET /api/system/configs` | 200 |
| 学员学分汇总 | `GET /api/credits/summary` | 200，返回已发放/待审核学分 |
| 学员学分明细 | `GET /api/credits/my` | 200 |
| 我的培训班 | `GET /api/projects/my`、`GET /api/projects/my/1` | 200 |
| 未读通知 | `GET /api/notifications/unread-count` | 200 |
| 个人资料 | `GET /api/profile` | 200 |
| 待审批毕业申请 | `GET /api/graduate/applies?auditStatus=0`（领导） | 200 |
| 权限隔离 | 学员访问 `GET /api/credits/pending` | 403，符合预期 |

导出接口均返回 Excel 文件：

| 导出类型 | 接口 | 文件大小 |
| --- | --- | --- |
| 单位汇总 | `/api/reports/export?type=org` | 3.9 KB |
| 培训班学分 | `/api/reports/export?type=credit` | 4.2 KB |
| 培训班学分（报表页） | `/api/reports/export?type=creditReport` | 3.9 KB |
| 课程学习 | `/api/reports/export?type=course` | 4.0 KB |
| 学习预警 | `/api/reports/export?type=warning` | 4.3 KB |

## 4. 业务链路验证

- **学习会话**：开始学习、30 秒心跳、暂停/恢复、断点续播、进度百分比回显均正常；重复打开同一课程返回 409 并提示先在原设备退出。
- **防作弊**：心跳中断、切屏阈值、禁止快进、随机弹窗超时等分支已通过接口与页面联调验证。
- **身份核验**：人脸模拟核验、短信验证码发送与提交均可完成，核验记录可在学员端查询。
- **学分**：课程完成后自动发放；管理端人工补录、领导复核、管理员终审、撤销均验证通过。
- **毕业**：资格校验、学员申请、教员 → 领导 → 管理员三级审批通过后生成证书。
- **电子证书**：证书编号 `JXJY-2026-1-000001`，PDF 可通过 `/uploads/certificate/2026/JXJY-2026-1-000001.pdf` 下载，文件头为 `%PDF-1.5`，1 页。
- **文件上传**：图片/视频类型按扩展名与文件头双重校验，伪造扩展名文件被拒绝。
- **登录安全**：连续失败锁定、Redis 单会话令牌、首次登录强制改密、操作/登录日志均验证通过。

## 5. 前端端到端验证

使用 Playwright + Chromium，管理后台按桌面视口、移动端按 iPhone 13 视口执行。

### 管理后台

- 覆盖 25 个路由：登录、首页、培训班、课程、题库、学习监控、核验记录、学分台账/审批、毕业审核/档案、总览与四类统计报表、组织/用户/角色/菜单/参数/字典、操作日志、登录日志、个人中心。
- 页面切换无控制台错误、无未捕获异常；报表路由切换会重新加载数据，不会残留上一张报表内容。
- 已验证项目/课程新建、编辑、删除、发布，用户新增/重置密码/导入模板下载，报表 Excel 导出等操作。

### 移动端 H5

- 学员端：登录、首页统计、培训班列表/详情、课程详情、视频学习、考试、学分查询、毕业申请、证书查看、通知、核验记录、个人资料、修改密码均正常。
- 首页统计与后端数据一致：学习中 1、已完成 1、总学分 10、待审核 0；继续学习卡片进度 45%。
- 培训班详情正确展示项目名称、编号、起止时间、所需学分及各课程状态与进度。
- 学习页可从服务端恢复上次播放位置（01:21 / 03:00）与有效学习时长，进度条同步显示 45%。
- 教员端“教务管理”按权限展示学习进度、考试成绩、毕业审批页签，学习时长/进度条与考试成绩均正常加载；无 `credit:audit` 权限的教员不会请求学分审批接口。
- 领导端额外展示“数据看板”（学员数、学习中、已毕业、待审批统计）与预警学员列表，并可进行学分审核与毕业复审。
- 学员直接访问 `/teacher` 会被路由守卫重定向到首页。
- 全流程无 4xx/5xx 异常请求、无控制台错误。

## 6. 演示与限制

- 人脸核验、短信验证码默认使用本地模拟实现（`app.face.mock=true`、`app.sms.mock=true`），生产环境需接入真实服务并关闭模拟开关。
- 演示视频为占位 MP4，仅用于播放/进度/防作弊流程演示。
- 测试过程中产生的学习记录、学分与毕业证书仅存在于本地测试库，不影响全新环境的初始化数据。
