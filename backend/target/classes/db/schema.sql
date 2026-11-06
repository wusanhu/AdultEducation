-- 继续教育学分管理系统 数据库结构（MySQL 8.0，utf8mb4）
-- 说明：所有业务表由应用启动时自动创建；主键统一 bigint 自增；业务数据逻辑删除。

CREATE TABLE IF NOT EXISTS sys_org (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '上级组织/部门',
  org_name VARCHAR(100) NOT NULL COMMENT '组织/部门名称',
  org_type TINYINT NOT NULL DEFAULT 1 COMMENT '类型 1组织 2部门',
  org_code VARCHAR(50) NOT NULL COMMENT '组织编码',
  leader_user_id BIGINT NULL COMMENT '负责人（领导账号）',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_org_code (org_code),
  KEY idx_org_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织/部门表';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_name VARCHAR(50) NOT NULL,
  role_code VARCHAR(50) NOT NULL,
  role_desc VARCHAR(255) NULL,
  data_scope TINYINT NOT NULL DEFAULT 4 COMMENT '1全部 2本组织及下级 3仅本部门 4仅本人 5指定组织',
  built_in TINYINT NOT NULL DEFAULT 0 COMMENT '1预置不可删除',
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt',
  real_name VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NULL,
  id_card VARCHAR(30) NULL,
  face_img VARCHAR(500) NULL COMMENT '人脸基准照URL，用于核验1:1比对',
  org_id BIGINT NULL COMMENT '所属组织',
  dept_id BIGINT NULL COMMENT '所属部门',
  role_id BIGINT NOT NULL,
  user_type TINYINT NOT NULL DEFAULT 4 COMMENT '1管理员 2领导 3教员 4学员',
  status TINYINT NOT NULL DEFAULT 1,
  avatar VARCHAR(500) NULL,
  open_id VARCHAR(100) NULL COMMENT '微信OpenID',
  must_change_pwd TINYINT NOT NULL DEFAULT 0,
  login_fail_count INT NOT NULL DEFAULT 0,
  lock_until DATETIME NULL,
  last_login_time DATETIME NULL,
  last_login_ip VARCHAR(50) NULL,
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  KEY idx_user_org (org_id, dept_id),
  KEY idx_user_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账号表';

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT NOT NULL AUTO_INCREMENT,
  parent_id BIGINT NOT NULL DEFAULT 0,
  menu_name VARCHAR(50) NOT NULL,
  menu_type TINYINT NOT NULL DEFAULT 2 COMMENT '1目录 2菜单 3按钮',
  path VARCHAR(200) NULL,
  component VARCHAR(200) NULL,
  icon VARCHAR(50) NULL,
  perms VARCHAR(100) NULL COMMENT '权限标识',
  sort INT NOT NULL DEFAULT 0,
  hidden TINYINT NOT NULL DEFAULT 0,
  mobile_visible TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_menu_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

CREATE TABLE IF NOT EXISTS sys_role_org (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  org_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_org (role_id, org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色指定组织表';

CREATE TABLE IF NOT EXISTS sys_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  config_name VARCHAR(100) NOT NULL,
  config_key VARCHAR(100) NOT NULL,
  config_value TEXT NULL,
  config_type VARCHAR(30) NOT NULL DEFAULT 'text',
  remark VARCHAR(255) NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数表';

CREATE TABLE IF NOT EXISTS sys_dict (
  id BIGINT NOT NULL AUTO_INCREMENT,
  dict_name VARCHAR(100) NOT NULL,
  dict_type VARCHAR(50) NOT NULL,
  dict_value VARCHAR(50) NOT NULL,
  dict_label VARCHAR(100) NOT NULL,
  sort INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dict_type_value (dict_type, dict_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典表';

CREATE TABLE IF NOT EXISTS sys_oper_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  user_name VARCHAR(50) NULL,
  org_id BIGINT NULL,
  oper_module VARCHAR(50) NULL,
  oper_content VARCHAR(1000) NULL,
  oper_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip VARCHAR(50) NULL,
  device VARCHAR(255) NULL,
  result TINYINT NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
  method VARCHAR(255) NULL,
  duration BIGINT NULL COMMENT '耗时毫秒',
  PRIMARY KEY (id),
  KEY idx_oper_time (oper_time),
  KEY idx_oper_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

CREATE TABLE IF NOT EXISTS sys_login_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  username VARCHAR(50) NULL,
  ip VARCHAR(50) NULL,
  device VARCHAR(255) NULL,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
  msg VARCHAR(255) NULL,
  login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

CREATE TABLE IF NOT EXISTS training_project (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_name VARCHAR(200) NOT NULL,
  project_code VARCHAR(50) NULL,
  description VARCHAR(1000) NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  need_credit DECIMAL(8,2) NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0未开始 1进行中 2已结束 3归档',
  certificate_template_url VARCHAR(500) NULL,
  create_user_id BIGINT NULL,
  allow_overdue TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许逾期学习',
  auto_graduate TINYINT NOT NULL DEFAULT 0 COMMENT '是否自动结业',
  need_teacher_audit TINYINT NOT NULL DEFAULT 1,
  need_leader_audit TINYINT NOT NULL DEFAULT 1,
  need_admin_audit TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_code (project_code),
  KEY idx_project_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培训班项目表';

CREATE TABLE IF NOT EXISTS project_member (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  join_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1在班 0移除',
  completion_status TINYINT NOT NULL DEFAULT 0 COMMENT '0学习中 1已完成 2已毕业',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_member (project_id, user_id),
  KEY idx_member_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培训班学员表';

CREATE TABLE IF NOT EXISTS project_teacher (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_teacher (project_id, user_id),
  KEY idx_teacher_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培训班教员表';

CREATE TABLE IF NOT EXISTS course (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  course_name VARCHAR(200) NOT NULL,
  cover VARCHAR(500) NULL,
  credit DECIMAL(6,2) NOT NULL DEFAULT 0,
  study_hour DECIMAL(6,2) NOT NULL DEFAULT 0,
  is_required TINYINT NOT NULL DEFAULT 1 COMMENT '1必修 2选修',
  need_exam TINYINT NOT NULL DEFAULT 0,
  pass_score INT NOT NULL DEFAULT 60,
  exam_limit INT NOT NULL DEFAULT 0 COMMENT '0不限次数',
  video_url VARCHAR(500) NULL,
  video_duration INT NOT NULL DEFAULT 0 COMMENT '视频秒数',
  intro VARCHAR(2000) NULL,
  create_user_id BIGINT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1上架 2下架',
  verify_gear_code INT NOT NULL DEFAULT 0,
  verify_gear_json TEXT NULL,
  allow_seek TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许拖拽',
  sort INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_course_project (project_id),
  KEY idx_course_teacher (create_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

CREATE TABLE IF NOT EXISTS course_attachment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  file_type VARCHAR(30) NULL,
  file_size BIGINT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_attachment_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程课件表';

CREATE TABLE IF NOT EXISTS question (
  id BIGINT NOT NULL AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  question_type TINYINT NOT NULL DEFAULT 1 COMMENT '1单选 2多选 3判断',
  content VARCHAR(1000) NOT NULL,
  options_json TEXT NULL,
  answer VARCHAR(200) NOT NULL,
  score DECIMAL(6,2) NOT NULL DEFAULT 10,
  sort INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_question_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程题库表';

CREATE TABLE IF NOT EXISTS exam_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  score DECIMAL(6,2) NOT NULL DEFAULT 0,
  is_pass TINYINT NOT NULL DEFAULT 0,
  answer_content TEXT NULL,
  attempt_no INT NOT NULL DEFAULT 1,
  submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_exam_user_course (user_id, course_id),
  KEY idx_exam_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测验记录表';

CREATE TABLE IF NOT EXISTS study_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  start_time DATETIME NULL,
  end_time DATETIME NULL,
  valid_duration INT NOT NULL DEFAULT 0 COMMENT '有效学习秒数',
  verify_count INT NOT NULL DEFAULT 0 COMMENT '随机防刷验证次数',
  screen_switch_count INT NOT NULL DEFAULT 0,
  screen_switch_duration INT NOT NULL DEFAULT 0,
  device VARCHAR(255) NULL,
  ip VARCHAR(50) NULL,
  is_abnormal TINYINT NOT NULL DEFAULT 0,
  abnormal_reason VARCHAR(500) NULL,
  progress DECIMAL(6,2) NOT NULL DEFAULT 0,
  verify_total INT NOT NULL DEFAULT 0 COMMENT '身份核验触发次数',
  verify_success INT NOT NULL DEFAULT 0,
  verify_fail INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0未开始 1学习中 2已完成',
  session_id VARCHAR(64) NULL,
  last_position INT NOT NULL DEFAULT 0 COMMENT '上次播放位置秒',
  last_heartbeat DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_study_user_course (user_id, course_id),
  KEY idx_study_project (project_id),
  KEY idx_study_abnormal (is_abnormal)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习记录表';

CREATE TABLE IF NOT EXISTS study_heartbeat_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  study_record_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  report_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  delta_seconds INT NOT NULL DEFAULT 0,
  progress DECIMAL(6,2) NOT NULL DEFAULT 0,
  in_foreground TINYINT NOT NULL DEFAULT 1,
  screen_switch TINYINT NOT NULL DEFAULT 0,
  ip VARCHAR(50) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_heartbeat_record (study_record_id, report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习心跳流水表';

CREATE TABLE IF NOT EXISTS verify_gear (
  id BIGINT NOT NULL AUTO_INCREMENT,
  gear_code INT NOT NULL,
  gear_name VARCHAR(50) NOT NULL,
  trigger_points VARCHAR(200) NULL COMMENT '进度触发点JSON数组',
  interval_minute INT NOT NULL DEFAULT 0 COMMENT '循环间隔分钟',
  timeout_second INT NOT NULL DEFAULT 60,
  max_fail_count INT NOT NULL DEFAULT 3,
  fail_strategy TINYINT NOT NULL DEFAULT 1 COMMENT '1暂停计时可重试 2标记异常需人工复核',
  live_check TINYINT NOT NULL DEFAULT 0 COMMENT '是否真人活体核验 0否 1是（触发核验时要求活体检测）',
  verify_mode TINYINT NOT NULL DEFAULT 1 COMMENT '核验方式 1人脸核验 2学习验证（弹窗确认）',
  built_in TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  description VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_gear_code (gear_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='身份核验档位模板';

CREATE TABLE IF NOT EXISTS user_verify_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  study_record_id BIGINT NULL,
  trigger_point VARCHAR(50) NULL COMMENT '触发点标识 如 pct:30 / int:15',
  verify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  verify_type TINYINT NOT NULL DEFAULT 1 COMMENT '1人脸 2短信',
  img_url VARCHAR(500) NULL,
  verify_result TINYINT NULL COMMENT '0失败 1通过 2超时',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待核验 1通过 2失败 3超时 4已复核',
  similarity DECIMAL(6,4) NULL,
  fail_reason VARCHAR(255) NULL,
  sms_phone VARCHAR(20) NULL,
  remark VARCHAR(500) NULL,
  review_user_id BIGINT NULL,
  review_time DATETIME NULL,
  review_action VARCHAR(30) NULL COMMENT 'allow/invalidate/violate',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_verify_user (user_id, course_id),
  KEY idx_verify_project (project_id),
  KEY idx_verify_record (study_record_id),
  KEY idx_verify_result (verify_result, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='身份核验记录表（审计流水）';

CREATE TABLE IF NOT EXISTS credit_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  course_id BIGINT NULL,
  credit_value DECIMAL(8,2) NOT NULL DEFAULT 0,
  source_type TINYINT NOT NULL DEFAULT 1 COMMENT '1课程自动 2人工补录',
  audit_status TINYINT NOT NULL DEFAULT 1 COMMENT '1待发放 2已发放 3已驳回 4已撤销',
  audit_user_id BIGINT NULL,
  audit_time DATETIME NULL,
  audit_remark VARCHAR(500) NULL,
  audit_node VARCHAR(20) NOT NULL DEFAULT 'ADMIN' COMMENT '当前审批节点 LEADER/ADMIN',
  apply_user_id BIGINT NULL,
  remark VARCHAR(500) NULL,
  revoke_reason VARCHAR(500) NULL,
  revoke_user_id BIGINT NULL,
  revoke_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_credit_user (user_id, project_id),
  KEY idx_credit_status (audit_status),
  KEY idx_credit_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学分记录表';

CREATE TABLE IF NOT EXISTS graduate_apply (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  apply_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  current_node VARCHAR(20) NOT NULL DEFAULT 'TEACHER' COMMENT 'TEACHER/LEADER/ADMIN/DONE/REJECTED',
  audit_status TINYINT NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2驳回',
  reject_reason VARCHAR(500) NULL,
  credit_snapshot DECIMAL(8,2) NOT NULL DEFAULT 0,
  study_hour_snapshot DECIMAL(8,2) NOT NULL DEFAULT 0,
  first_audit_user BIGINT NULL,
  first_audit_time DATETIME NULL,
  first_audit_opinion VARCHAR(500) NULL,
  second_audit_user BIGINT NULL,
  second_audit_time DATETIME NULL,
  second_audit_opinion VARCHAR(500) NULL,
  final_audit_user BIGINT NULL,
  final_audit_time DATETIME NULL,
  final_audit_opinion VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_graduate_user (user_id, project_id),
  KEY idx_graduate_status (audit_status, current_node)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='毕业申请表';

CREATE TABLE IF NOT EXISTS graduate_file (
  id BIGINT NOT NULL AUTO_INCREMENT,
  apply_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  certificate_no VARCHAR(80) NOT NULL,
  certificate_url VARCHAR(500) NULL,
  graduate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  credit_value DECIMAL(8,2) NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_no (certificate_no),
  KEY idx_file_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='毕业档案表';

CREATE TABLE IF NOT EXISTS approval_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  biz_type VARCHAR(30) NOT NULL COMMENT 'GRADUATE/CREDIT',
  biz_id BIGINT NOT NULL,
  node VARCHAR(20) NULL,
  auditor_id BIGINT NULL,
  auditor_name VARCHAR(50) NULL,
  action VARCHAR(20) NOT NULL COMMENT 'SUBMIT/APPROVE/REJECT/REVOKE',
  opinion VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_approval_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流转日志表';

CREATE TABLE IF NOT EXISTS notification (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  content VARCHAR(1000) NULL,
  type VARCHAR(30) NOT NULL DEFAULT 'SYSTEM' COMMENT 'ACCOUNT/COURSE/CREDIT/GRADUATE/APPROVAL/WARNING/VERIFY',
  biz_id BIGINT NULL,
  link VARCHAR(200) NULL COMMENT '移动端跳转路由，发送时按模板渲染',
  admin_link VARCHAR(200) NULL COMMENT '管理端跳转路由，发送时按模板渲染',
  is_read TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notify_user (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

CREATE TABLE IF NOT EXISTS notification_template (
  id BIGINT NOT NULL AUTO_INCREMENT,
  template_code VARCHAR(64) NOT NULL COMMENT '发送时按编码取用',
  template_name VARCHAR(100) NOT NULL,
  type VARCHAR(30) NOT NULL COMMENT 'ACCOUNT/COURSE/CREDIT/GRADUATE/APPROVAL/WARNING/VERIFY/SYSTEM',
  title VARCHAR(200) NOT NULL COMMENT '标题模板，支持 {变量} 占位',
  content VARCHAR(1000) NOT NULL COMMENT '内容模板，支持 {变量} 占位',
  mobile_link VARCHAR(200) NULL COMMENT '移动端路由模板，留空则该消息不可跳转',
  admin_link VARCHAR(200) NULL COMMENT '管理端路由模板，留空则该消息不可跳转',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '停用后该编码的消息不再发送',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notify_template_code (template_code),
  KEY idx_notify_template_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信模板表';
