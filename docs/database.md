# 数据库设计说明

## 1. 初始化方式

- 数据库：`adult_edu`，字符集 `utf8mb4`，排序规则 `utf8mb4_general_ci`。
- JDBC URL 自带 `createDatabaseIfNotExist=true`，首次启动自动建库。
- `backend/src/main/resources/db/schema.sql` 使用 `CREATE TABLE IF NOT EXISTS`，可重复执行。
- `backend/src/main/resources/db/data.sql` 使用 `INSERT IGNORE`，不会覆盖人工修改的数据。
- 所有业务表包含 `create_time`、`update_time`（部分日志表仅创建时间），业务主表使用逻辑删除字段 `is_deleted`。
- 金额/学分使用 `DECIMAL(10,2)`，进度使用 `DECIMAL(5,2)`。

## 2. 表清单（26 张）

### 系统与权限

| 表 | 说明 | 关键字段 |
| --- | --- | --- |
| `sys_org` | 组织/部门树 | `parent_id`、`org_type`（1组织 2部门）、`org_code`、`leader_user_id`、`status` |
| `sys_role` | 角色 | `role_code`、`data_scope`（1全部 2本组织及下级 3本部门 4本人 5指定组织）、`built_in` |
| `sys_user` | 账号 | `username`、`password`(BCrypt)、`org_id`、`dept_id`、`role_id`、`user_type`、`must_change_pwd`、`login_fail_count`、`lock_until` |
| `sys_menu` | 菜单/按钮权限 | `menu_type`（1目录 2菜单 3按钮）、`perms`、`path`、`hidden`、`mobile_visible` |
| `sys_role_menu` | 角色-菜单 | `role_id`、`menu_id`，唯一索引 |
| `sys_role_org` | 角色-指定组织 | `role_id`、`org_id`，唯一索引 |
| `sys_config` | 平台参数 | `config_key` 唯一、`config_value`、`config_type` |
| `sys_dict` | 数据字典 | `dict_type`、`dict_value`、`dict_label` |
| `sys_oper_log` | 操作日志 | 操作人、模块、内容、IP、终端、结果、耗时 |
| `sys_login_log` | 登录日志 | 账号、IP、终端、结果、消息、登录时间 |

### 培训与课程

| 表 | 说明 | 关键字段 |
| --- | --- | --- |
| `training_project` | 培训班 | `project_code`、起止时间、`need_credit`、`status`（0未开始 1进行中 2已结束 3归档）、审批节点开关、`certificate_template_url` |
| `project_member` | 培训班学员 | `project_id`、`user_id`、`completion_status`、`status`，唯一索引 |
| `project_teacher` | 培训班教员 | `project_id`、`user_id`，唯一索引 |
| `course` | 课程 | `project_id`、封面、`video_url`、`video_duration`、`credit`、`study_hour`、`need_exam`、`pass_score`、`exam_limit`、`verify_gear_code`、`verify_gear_json`、`allow_seek`、`status` |
| `course_attachment` | 课程资料 | `course_id`、`file_name`、`file_url`、`file_type`、`file_size` |
| `question` | 题库 | `course_id`、`question_type`（1单选 2多选 3判断）、`options_json`、`answer`、`score` |
| `exam_record` | 测验记录 | `user_id`、`course_id`、`score`、`is_pass`、`attempt_no`、`answers_json` |

### 学习与防刷

| 表 | 说明 | 关键字段 |
| --- | --- | --- |
| `study_record` | 学习记录（一人一课程） | 有效时长、`progress`、`last_position`、`status`（1学习中 2已完成 3异常）、核验统计、切屏统计、`is_abnormal`、`session_id`、`last_heartbeat` |
| `study_heartbeat_log` | 心跳明细 | `study_record_id`、`position`、`delta_seconds`、`in_foreground`、`screen_switch`、`device`、`ip` |
| `verify_gear` | 核验档位模板 | `gear_code`、`trigger_points`(JSON)、`interval_minute`、`timeout_second`、`max_fail_count`、`fail_strategy` |
| `user_verify_log` | 身份核验记录 | 触发点、核验类型（1人脸 2短信）、抓拍图、相似度、结果、审核状态、审核人 |

### 学分与毕业

| 表 | 说明 | 关键字段 |
| --- | --- | --- |
| `credit_record` | 学分台账 | 来源（1课程自动 2人工补录）、`audit_status`（1待发放 2已发放 3已驳回 4已撤销）、审批节点、撤销原因、申请人/审批人 |
| `graduate_apply` | 毕业申请 | 当前节点、审批状态、学分/学时快照、三级审批人/时间/意见、驳回原因 |
| `graduate_file` | 毕业档案 | 证书编号唯一、证书地址、毕业时间、学分 |
| `approval_log` | 审批流水 | 业务类型、业务ID、节点、动作、审批人、意见 |
| `notification` | 站内通知 | 接收人、标题、内容、类型、是否已读、关联业务ID |

## 3. 主要关系

```
sys_org 1 ── n sys_user
sys_role 1 ── n sys_user
sys_role n ── n sys_menu        (sys_role_menu)
sys_role n ── n sys_org         (sys_role_org, data_scope=5)

training_project 1 ── n project_member n ── 1 sys_user
training_project 1 ── n project_teacher n ── 1 sys_user
training_project 1 ── n course 1 ── n course_attachment
course 1 ── n question
course 1 ── n study_record 1 ── n study_heartbeat_log
course 1 ── n user_verify_log
course 1 ── n exam_record

sys_user 1 ── n credit_record
training_project 1 ── n credit_record
sys_user 1 ── n graduate_apply 1 ── 1 graduate_file
graduate_apply 1 ── n approval_log
sys_user 1 ── n notification
```

## 4. 唯一约束与索引

- `sys_org.org_code`、`sys_role.role_code`、`sys_user.username`、`sys_user.phone`、`sys_menu.perms` 唯一。
- `project_member(project_id, user_id)`、`project_teacher(project_id, user_id)`、`sys_role_menu(role_id, menu_id)`、`sys_role_org(role_id, org_id)`、`graduate_file.certificate_no` 唯一。
- `study_record(user_id, course_id)` 唯一，保证一人一课程一条学习记录。
- 常用查询字段均建立索引：`project_id`、`user_id`、`course_id`、`status`、`last_heartbeat`、`create_time` 等。

## 5. 数据一致性

- 学分、毕业、审批等多表写操作使用 `@Transactional`。
- 学习记录、核验记录、学分撤销、审批动作均保留历史流水，不做物理删除。
- 系统业务表逻辑删除（`is_deleted`），日志表永久保留。
- 证书编号由规则 `{prefix}-{year}-{projectId}-{seq}` 生成，数据库唯一索引兜底。

## 6. 演示数据

`data.sql` 内置：

- 1 个学院 + 2 个分院 + 4 个部门；
- 4 个角色、完整菜单树与按钮权限；
- 20 项系统参数、10 组数据字典；
- 7 个演示账号；
- 3 个培训班、7 门课程、题库与课程资料；
- 学习记录、心跳、核验、学分与毕业申请样例。

所有种子数据均可重复执行，不会重复插入。
