# 接口清单

- 基础路径：`/api`
- 认证：除登录、短信登录外，均需请求头 `Authorization: Bearer <token>`
- 统一响应：

```json
{ "code": 200, "msg": "success", "data": {} }
```

- 业务错误使用 `code=400/403/409` 与中文 `msg`；未登录/令牌失效返回 `401`。
- 分页响应：`{ records, total, pageNum, pageSize, pages }`。
- 在线文档：`/swagger-ui.html`。

## 认证与个人中心

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/login` | 账号密码登录，返回 token、用户、权限、菜单 |
| POST | `/auth/sms/send` | 发送登录短信验证码 |
| POST | `/auth/sms/login` | 短信验证码登录 |
| GET | `/auth/me` | 当前用户与权限菜单 |
| POST | `/auth/change-password` | 修改密码（成功后令牌失效） |
| POST | `/auth/logout` | 退出登录 |
| GET | `/profile` | 个人资料（身份证脱敏） |
| PUT | `/profile` | 修改姓名、手机号、头像 |

## 通知

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/notifications` | 分页列表，参数 `isRead`、`type` |
| GET | `/notifications/unread-count` | 未读数 |
| POST | `/notifications/{id}/read` | 标记已读 |
| POST | `/notifications/read-all` | 全部已读 |
| DELETE | `/notifications/{id}` | 删除通知 |

## 培训班

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/projects` | 管理端分页，支持名称、状态、年份 |
| GET | `/projects/options` | 培训班下拉 |
| GET | `/projects/teacher-options` | 教员下拉 |
| GET | `/projects/student-options` | 学员下拉（可排除已加入） |
| GET | `/projects/my` | 学员我的培训班（含进度/学分/可申请） |
| GET | `/projects/my/{id}` | 学员培训班详情与课程 |
| GET | `/projects/{id}` | 管理端详情（含前 20 名学员、课程） |
| POST | `/projects` | 新增培训班 |
| PUT | `/projects/{id}` | 编辑培训班 |
| DELETE | `/projects/{id}` | 删除 |
| PUT | `/projects/{id}/status` | 开始/结束/归档，参数 `status` |
| GET | `/projects/{id}/members` | 学员分页与学习进度 |
| POST | `/projects/{id}/members` | 批量添加学员 `{userIds}` |
| DELETE | `/projects/{id}/members/{userId}` | 移除学员 |
| GET | `/projects/{id}/teachers` | 培训班教员 |
| PUT | `/projects/{id}/teachers` | 分配教员 `{userIds}` |

## 课程

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/courses` | 管理端分页，支持项目、关键字、状态、档位 |
| GET | `/courses/options` | 课程下拉 |
| GET | `/courses/gears` | 核验档位模板，参数 `onlyEnabled` |
| POST | `/courses/gears` | 新增/保存档位 |
| DELETE | `/courses/gears/{id}` | 删除档位 |
| GET | `/courses/{id}` | 课程详情（含资料、题库、学习记录） |
| POST | `/courses` | 新增课程（含题库、资料） |
| PUT | `/courses/{id}` | 编辑课程 |
| DELETE | `/courses/{id}` | 删除课程 |
| PUT | `/courses/{id}/status` | 上架/下架，参数 `status` |

## 学习与防刷

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/study/start` | 开始学习 `{courseId}`，返回会话、断点、核验配置 |
| POST | `/study/heartbeat` | 30 秒心跳：`courseId, sessionId, deltaSeconds, progress, position, inForeground, screenSwitch, screenSwitchDuration` |
| POST | `/study/pause` | 暂停学习，参数 `courseId` |
| POST | `/study/popup/confirm` | 随机弹窗确认 `{courseId, sessionId}` |
| POST | `/study/verify/start` | 发起核验 `{courseId, triggerPoint}` |
| POST | `/study/verify/face` | 提交人脸核验 `{logId, imageBase64}` |
| POST | `/study/verify/sms/send` | 发送短信验证码，参数 `logId` |
| POST | `/study/verify/sms/submit` | 提交短信核验 `{logId, code}` |
| GET | `/study/state` | 学习状态，参数 `courseId` |
| POST | `/study/records/{id}/unlock` | 管理员/任课教员解除学习限制（`clearAbnormal`、`remark`） |

## 核验与测验

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/verify/records` | 核验记录分页（项目、课程、结果、状态、日期） |
| GET | `/verify/abnormal` | 核验异常/学习异常清单 |
| POST | `/verify/{id}/review` | 审核核验记录 `{action: allow/invalidate/violate, remark}` |
| GET | `/verify/my` | 我的核验记录 |
| GET | `/verify/study-records` | 学习监控分页 |
| GET | `/verify/study-records/{id}` | 学习详情（心跳、核验、学分） |
| GET | `/exams/{courseId}/paper` | 获取测验试卷 |
| POST | `/exams/{courseId}/submit` | 提交答卷 `{answers:[{questionId, answer}]}` |
| GET | `/exams/my` | 我的测验记录 |
| GET | `/exams` | 管理端测验记录分页 |

## 学分

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/credits` | 学分台账分页（管理端） |
| GET | `/credits/my` | 我的学分明细 |
| GET | `/credits/summary` | 我的学分汇总 |
| POST | `/credits/apply` | 人工补录 `{userId, projectId, creditValue, remark}` |
| GET | `/credits/pending` | 待审批补录 |
| POST | `/credits/{id}/audit` | 审批 `{approve, remark}` |
| POST | `/credits/{id}/revoke` | 撤销 `{reason}` |

## 毕业

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/graduate/eligibility` | 毕业资格，参数 `projectId` |
| POST | `/graduate/apply` | 提交毕业申请 `{projectId}` |
| GET | `/graduate/my` | 我的当前毕业申请 |
| GET | `/graduate/my/list` | 我的毕业申请历史 |
| GET | `/graduate/my/certificate` | 我的电子证书，参数 `projectId` |
| GET | `/graduate/applies` | 管理端申请分页 |
| GET | `/graduate/applies/{id}` | 申请详情与审批流水 |
| POST | `/graduate/applies/{id}/audit` | 审批 `{action: approve/reject, opinion}` |
| GET | `/graduate/files` | 毕业档案分页 |

## 报表

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/reports/overview` | 工作台总览与 7 日趋势 |
| GET | `/reports/org` | 单位汇总，参数 `projectId` |
| GET | `/reports/credit` | 培训班学分统计 |
| GET | `/reports/course` | 课程学习统计 |
| GET | `/reports/warnings` | 学习预警，参数 `projectId`、`type`（ALL/CREDIT/EXPIRE/INACTIVE） |
| POST | `/reports/warnings/notify` | 发送预警提醒，参数 `projectId`、`type`、`userIds` |
| GET | `/reports/export` | Excel 导出，`type`：credit/creditReport/org/course/warning/graduate |

## 系统管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/system/org/tree` | 组织树 |
| POST/PUT/DELETE | `/system/org`、`/system/org/{id}` | 组织增改删 |
| PUT | `/system/org/{id}/status` | 启停组织 |
| GET | `/system/users` | 账号分页 |
| GET | `/system/users/{id}` | 账号详情 |
| POST/PUT/DELETE | `/system/users`、`/system/users/{id}` | 账号增改删 |
| POST | `/system/users/{id}/reset-password` | 重置密码（返回新密码） |
| PUT | `/system/users/{id}/status` | 启停账号 |
| POST | `/system/users/import` | Excel 批量导入 |
| GET | `/system/users/export` | Excel 导出 |
| GET | `/system/users/template` | 导入模板 |
| GET | `/system/roles`、`/system/roles/{id}` | 角色列表/详情（含菜单与组织） |
| POST/PUT/DELETE | `/system/roles`、`/system/roles/{id}` | 角色增改删 |
| PUT | `/system/roles/{id}/status` | 启停角色 |
| GET | `/system/menus/tree` | 菜单树 |
| POST/PUT/DELETE | `/system/menus`、`/system/menus/{id}` | 菜单增改删 |
| PUT | `/system/menus/{id}/status` | 启停菜单 |
| GET/PUT | `/system/configs` | 参数列表/批量保存 |
| GET | `/system/dicts` | 字典列表（可按类型） |
| POST/PUT/DELETE | `/system/dicts`、`/system/dicts/{id}` | 字典增改删 |
| GET | `/system/oper-logs` | 操作日志分页 |
| GET | `/system/login-logs` | 登录日志分页 |

## 文件

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/files/upload` | 上传文件，参数 `file`、`type`（avatar/image/video/document/template/common） |

上传限制：图片 5MB、视频 500MB、文档 100MB、模板 20MB；同时校验扩展名与文件头，返回 `{url, fileName, fileSize, fileType, type}`。上传文件通过 `/uploads/**` 访问。
