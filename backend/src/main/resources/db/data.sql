-- 初始化数据（幂等）。所有密码均为 BCrypt 加密。
-- 演示账号：admin/admin123  leader/leader123  teacher/teacher123  student/student123

INSERT IGNORE INTO sys_org (id,parent_id,org_name,org_type,org_code,leader_user_id,sort,status) VALUES
 (1,0,'示范继续教育学院',1,'ORG_ROOT',2,1,1),
 (2,1,'第一分院',1,'ORG_BRANCH1',2,1,1),
 (3,2,'护理部',2,'DEPT_NURSE',NULL,1,1),
 (4,2,'信息技术部',2,'DEPT_IT',NULL,2,1),
 (5,1,'第二分院',1,'ORG_BRANCH2',NULL,2,1),
 (6,5,'综合办公室',2,'DEPT_OFFICE',NULL,1,1);

INSERT IGNORE INTO sys_role (id,role_name,role_code,role_desc,data_scope,built_in,status) VALUES
 (1,'超级管理员','super_admin','平台最高权限，管理全部数据',1,1,1),
 (2,'领导','leader','查看本单位及下级数据、参与审批',2,1,1),
 (3,'教员','teacher','管理自己负责的课程与学员',4,1,1),
 (4,'学员','student','学习主体，仅访问本人数据',4,1,1);

INSERT IGNORE INTO sys_user (id,username,password,real_name,phone,id_card,org_id,dept_id,role_id,user_type,status,must_change_pwd,remark) VALUES
 (1,'admin','$2b$10$IaK49/LLEbdpJchAriFFIuMt6llfxIrnl/kt/iMSKQJidCrN2N8fO','系统管理员','13800000001','110101199001010011',1,NULL,1,1,1,0,'内置超级管理员'),
 (2,'leader','$2b$10$d31evE6QN2CToY3HlTYM9.1CQKjdiQ6oQOsHa7d5UfAnOZE0PPlD.','张明','13800000002','110101198001010022',2,NULL,2,2,1,0,'第一分院负责人'),
 (3,'teacher','$2b$10$gSmNx0baZC1K665fur9DN.TQ0MoDT6i6ske1dmJ66KHyGbXbWtUSO','李静','13800000003','110101198501010033',2,3,3,3,1,0,'护理课程讲师'),
 (4,'student','$2b$10$TGOdEM2c..Ah3oDOhY4K7.pPwT8TR6nunLV09uQX5NadMkCBGjqZW','王小雨','13800000004','110101199501010044',3,3,4,4,1,0,'护理部学员'),
 (5,'student2','$2b$10$TGOdEM2c..Ah3oDOhY4K7.pPwT8TR6nunLV09uQX5NadMkCBGjqZW','赵晨','13800000005','110101199401010055',3,3,4,4,1,0,'护理部学员'),
 (6,'student3','$2b$10$TGOdEM2c..Ah3oDOhY4K7.pPwT8TR6nunLV09uQX5NadMkCBGjqZW','陈思远','13800000006','110101199601010066',4,4,4,4,1,0,'信息技术部学员'),
 (7,'teacher2','$2b$10$gSmNx0baZC1K665fur9DN.TQ0MoDT6i6ske1dmJ66KHyGbXbWtUSO','周涛','13800000007','110101198301010077',2,4,3,3,1,0,'信息技术课程讲师');

INSERT IGNORE INTO sys_menu (id,parent_id,menu_name,menu_type,path,component,icon,perms,sort,hidden,mobile_visible,status) VALUES
 (100,0,'工作台',2,'/dashboard','dashboard/index','DashboardOutlined','dashboard:view',1,0,0,1),
 (200,0,'培训管理',1,'/training',NULL,'ReadOutlined',NULL,2,0,0,1),
 (201,200,'培训班管理',2,'/project/list','project/list','TeamOutlined','project:list',1,0,0,1),
 (2011,201,'新增项目',3,NULL,NULL,NULL,'project:add',1,0,0,1),
 (2012,201,'编辑项目',3,NULL,NULL,NULL,'project:edit',2,0,0,1),
 (2013,201,'删除项目',3,NULL,NULL,NULL,'project:delete',3,0,0,1),
 (2014,201,'学员管理',3,NULL,NULL,NULL,'project:member',4,0,0,1),
 (2015,201,'教员分配',3,NULL,NULL,NULL,'project:teacher',5,0,0,1),
 (210,200,'课程管理',2,'/course/list','course/list','VideoCameraOutlined','course:list',2,0,0,1),
 (211,210,'新增课程',3,NULL,NULL,NULL,'course:add',1,0,0,1),
 (212,210,'编辑课程',3,NULL,NULL,NULL,'course:edit',2,0,0,1),
 (213,210,'删除课程',3,NULL,NULL,NULL,'course:delete',3,0,0,1),
 (214,210,'课程上下架',3,NULL,NULL,NULL,'course:publish',4,0,0,1),
 (215,210,'题库管理',3,NULL,NULL,NULL,'course:question',5,0,0,1),
 (220,200,'学习监控',2,'/study/monitor','study/monitor','MonitorOutlined','study:monitor',3,0,0,1),
 (221,200,'身份核验记录',2,'/verify/record','verify/record','CameraOutlined','verify:list',4,0,0,1),
 (222,200,'核验异常清单',2,'/verify/abnormal','verify/abnormal','WarningOutlined','verify:abnormal',5,0,0,1),
 (223,200,'核验档位模板',2,'/verify/gear','verify/gear','ControlOutlined','verify:gear',6,0,0,1),
 (230,0,'学分管理',1,'/credit',NULL,'CreditCardOutlined',NULL,3,0,0,1),
 (231,230,'学分台账',2,'/credit/list','credit/list','ProfileOutlined','credit:list',1,0,0,1),
 (232,230,'学分补录',2,'/credit/apply','credit/apply','PlusSquareOutlined','credit:apply',2,0,0,1),
 (233,230,'补录审批',2,'/credit/audit','credit/audit','AuditOutlined','credit:audit',3,0,0,1),
 (234,230,'学分撤销',3,NULL,NULL,NULL,'credit:revoke',4,0,0,1),
 (240,0,'毕业审核',1,'/graduate',NULL,'SafetyCertificateOutlined',NULL,4,0,0,1),
 (241,240,'毕业申请',2,'/graduate/apply','graduate/apply','FileDoneOutlined','graduate:list',1,0,0,1),
 (242,240,'毕业档案',2,'/graduate/file','graduate/file','FolderOpenOutlined','graduate:file',2,0,0,1),
 (250,0,'统计报表',1,'/report',NULL,'BarChartOutlined','report:view',5,0,0,1),
 (251,250,'单位汇总',2,'/report/org','report/org','PieChartOutlined','report:org',1,0,0,1),
 (252,250,'学分统计',2,'/report/credit','report/credit','LineChartOutlined','report:credit',2,0,0,1),
 (253,250,'课程学习',2,'/report/course','report/course','AreaChartOutlined','report:course',3,0,0,1),
 (254,250,'报表导出',3,NULL,NULL,NULL,'report:export',4,0,0,1),
 (255,250,'学习预警',2,'/report/warning','report/warning','WarningOutlined','report:warning',5,0,0,1),
 (300,0,'系统管理',1,'/system',NULL,'SettingOutlined','system:view',6,0,0,1),
 (301,300,'组织架构',2,'/system/org','system/org','ApartmentOutlined','system:org',1,0,0,1),
 (3011,301,'组织维护',3,NULL,NULL,NULL,'system:org:edit',1,0,0,1),
 (302,300,'账号管理',2,'/system/user','system/user','UserOutlined','system:user',2,0,0,1),
 (3021,302,'新增账号',3,NULL,NULL,NULL,'system:user:add',1,0,0,1),
 (3022,302,'编辑账号',3,NULL,NULL,NULL,'system:user:edit',2,0,0,1),
 (3023,302,'删除账号',3,NULL,NULL,NULL,'system:user:delete',3,0,0,1),
 (3024,302,'重置密码',3,NULL,NULL,NULL,'system:user:reset',4,0,0,1),
 (3025,302,'批量导入',3,NULL,NULL,NULL,'system:user:import',5,0,0,1),
 (3026,302,'导出账号',3,NULL,NULL,NULL,'system:user:export',6,0,0,1),
 (303,300,'角色管理',2,'/system/role','system/role','KeyOutlined','system:role',3,0,0,1),
 (3031,303,'角色维护',3,NULL,NULL,NULL,'system:role:edit',1,0,0,1),
 (304,300,'菜单管理',2,'/system/menu','system/menu','MenuOutlined','system:menu',4,0,0,1),
 (3041,304,'菜单维护',3,NULL,NULL,NULL,'system:menu:edit',1,0,0,1),
 (305,300,'系统参数',2,'/system/config','system/config','ToolOutlined','system:config',5,0,0,1),
 (3051,305,'参数保存',3,NULL,NULL,NULL,'system:config:edit',1,0,0,1),
 (306,300,'数据字典',2,'/system/dict','system/dict','BookOutlined','system:dict',6,0,0,1),
 (3061,306,'字典维护',3,NULL,NULL,NULL,'system:dict:edit',1,0,0,1),
 (307,300,'操作日志',2,'/system/log','system/log','FileTextOutlined','system:log',7,0,0,1),
 (3071,307,'日志导出',3,NULL,NULL,NULL,'system:log:export',1,0,0,1),
 (308,300,'登录日志',2,'/system/loginlog','system/loginlog','LoginOutlined','system:loginlog',8,0,0,1);

-- 超级管理员拥有全部菜单/按钮
INSERT IGNORE INTO sys_role_menu (role_id,menu_id) SELECT 1,id FROM sys_menu;
-- 领导：查看 + 审批 + 报表
INSERT IGNORE INTO sys_role_menu (role_id,menu_id) VALUES
 (2,100),(2,200),(2,201),(2,210),(2,220),(2,221),(2,222),(2,230),(2,231),(2,233),(2,240),(2,241),(2,242),(2,250),(2,251),(2,252),(2,253),(2,254),(2,255);
-- 教员：课程管理、学习监控、核验记录、学分补录、毕业初审
INSERT IGNORE INTO sys_role_menu (role_id,menu_id) VALUES
 (3,100),(3,200),(3,201),(3,210),(3,211),(3,212),(3,213),(3,214),(3,215),(3,220),(3,221),(3,222),(3,230),(3,231),(3,232),(3,240),(3,241),(3,250),(3,251),(3,252),(3,253),(3,254),(3,255);

INSERT IGNORE INTO verify_gear (id,gear_code,gear_name,trigger_points,interval_minute,timeout_second,max_fail_count,fail_strategy,built_in,status,description) VALUES
 (1,0,'不验证','[]',0,60,3,1,1,1,'全程不触发人员身份核验，仅保留原有防刷（切屏、随机弹窗确认）。适用于普通选修课。'),
 (2,1,'低频核验','[50]',0,60,3,1,1,1,'播放到 50% 进度触发 1 次人脸核验，通过后继续学习。适用于一般必修课程。'),
 (3,2,'中频核验','[30,70]',0,60,3,1,1,1,'播放到 30%、70% 进度点各触发 1 次，共 2 次人脸核验。适用于重点必修课程。'),
 (4,3,'高频核验','[30,60,90]',15,60,3,2,1,1,'每 15 分钟循环触发 1 次，并在 30%、60%、90% 进度点额外核验。适用于核心考核课。'),
 (5,9,'自定义档位','[40]',20,60,5,1,0,1,'管理员可自定义触发节点、循环间隔、超时时间、失败次数与处理策略。');

INSERT IGNORE INTO sys_config (id,config_name,config_key,config_value,config_type,remark) VALUES
 (1,'平台名称','platform.name','继续教育学分管理平台','text','平台信息'),
 (2,'平台LOGO','platform.logo','','image','平台 LOGO 地址'),
 (3,'登录页Banner','platform.banner','','image','登录页横幅地址'),
 (4,'版权信息','platform.copyright','© 2026 示范继续教育学院 继续教育学分管理平台','text','页脚版权'),
 (5,'系统简介','platform.intro','面向在职人员的继续教育在线学习与学分管理平台','textarea','系统简介'),
 (6,'证书编号规则','cert.no.rule','{prefix}-{year}-{projectId}-{seq}','text','证书编号生成规则'),
 (7,'证书水印','cert.watermark','继续教育电子证书','text','证书水印文字'),
 (8,'默认禁止拖拽','study.allowSeek','false','boolean','是否允许拖动视频进度'),
 (9,'随机弹窗下限(秒)','study.popupMin','300','number','随机防刷弹窗最小间隔'),
 (10,'随机弹窗上限(秒)','study.popupMax','600','number','随机防刷弹窗最大间隔'),
 (11,'心跳间隔(秒)','study.heartbeat','30','number','移动端学习心跳上报间隔'),
 (12,'人脸相似度阈值','face.threshold','0.75','number','人脸比对通过阈值'),
 (13,'抓拍图保存天数','verify.image.keepDays','180','number','到期自动清理'),
 (14,'短信每日上限','sms.dailyLimit','10','number','单个账号每日验证码发送上限'),
 (15,'默认防刷开关','study.antiCheat','true','boolean','全局防刷开关'),
 (16,'微信通知模式','wechat.mock','true','boolean','本地演示使用模拟推送'),
 (17,'审批超时工作日','approval.timeoutDays','3','number','审批节点超时天数'),
 (18,'OSS存储配置','oss.config','{"provider":"local","bucket":"adult-edu"}','json','本地演示使用本地磁盘，生产可切换 OSS'),
 (19,'学分补录需领导复核','credit.needLeaderAudit','false','boolean','开启后补录申请先由领导复核再由管理员终审'),
 (20,'证书下载开放','cert.download','true','boolean','学员是否可下载电子证书');

INSERT IGNORE INTO sys_dict (dict_name,dict_type,dict_value,dict_label,sort,status) VALUES
 ('账号类型','user_type','1','管理员',1,1),('账号类型','user_type','2','领导',2,1),('账号类型','user_type','3','教员',3,1),('账号类型','user_type','4','学员',4,1),
 ('数据范围','data_scope','1','全部数据',1,1),('数据范围','data_scope','2','本组织及下级',2,1),('数据范围','data_scope','3','仅本部门',3,1),('数据范围','data_scope','4','仅本人数据',4,1),('数据范围','data_scope','5','指定组织',5,1),
 ('培训项目状态','project_status','0','未开始',1,1),('培训项目状态','project_status','1','进行中',2,1),('培训项目状态','project_status','2','已结束',3,1),('培训项目状态','project_status','3','归档',4,1),
 ('学分来源','credit_source','1','课程自动发放',1,1),('学分来源','credit_source','2','人工补录',2,1),
 ('学分状态','credit_status','1','待发放',1,1),('学分状态','credit_status','2','已发放',2,1),('学分状态','credit_status','3','已驳回',3,1),('学分状态','credit_status','4','已撤销',4,1),
 ('核验档位','verify_gear','0','不验证',1,1),('核验档位','verify_gear','1','低频核验',2,1),('核验档位','verify_gear','2','中频核验',3,1),('核验档位','verify_gear','3','高频核验',4,1),('核验档位','verify_gear','9','自定义',5,1),
 ('核验类型','verify_type','1','人脸比对',1,1),('核验类型','verify_type','2','短信验证码',2,1),
 ('核验结果','verify_result','0','失败',1,1),('核验结果','verify_result','1','通过',2,1),('核验结果','verify_result','2','超时',3,1),
 ('失败处理策略','fail_strategy','1','暂停计时可重试',1,1),('失败处理策略','fail_strategy','2','标记异常需人工复核',2,1),
 ('审批状态','audit_status','0','待审',1,1),('审批状态','audit_status','1','通过',2,1),('审批状态','audit_status','2','驳回',3,1),
 ('课程分类','course_type','1','必修',1,1),('课程分类','course_type','2','选修',2,1),
 ('课程状态','course_status','0','草稿',1,1),('课程状态','course_status','1','已上架',2,1),('课程状态','course_status','2','已下架',3,1),
 ('测验结果','exam_pass','0','未通过',1,1),('测验结果','exam_pass','1','通过',2,1),
 ('逻辑删除','is_deleted','0','正常',1,1),('逻辑删除','is_deleted','1','已删除',2,1);

INSERT IGNORE INTO training_project (id,project_name,project_code,description,start_time,end_time,need_credit,status,create_user_id,allow_overdue,auto_graduate,need_teacher_audit,need_leader_audit,need_admin_audit) VALUES
 (1,'2026年度专业技术人员继续教育','P2026-01','面向全院专业技术人员的年度继续教育项目，完成课程学习并通过考核后获得相应学分。','2026-01-01 00:00:00','2026-12-31 23:59:59',10.00,1,1,0,0,1,1,1),
 (2,'护理岗位胜任力提升培训','P2026-02','护理岗位专项能力提升，涵盖护理质量、院感防控等核心课程。','2026-03-01 00:00:00','2026-10-31 23:59:59',6.00,1,1,0,0,1,1,1),
 (3,'2025年度公需科目培训','P2025-01','2025 年度公需科目学习项目，已结束归档。','2025-01-01 00:00:00','2025-12-31 23:59:59',4.00,2,1,0,1,1,1,1);

INSERT IGNORE INTO project_member (project_id,user_id,completion_status) VALUES
 (1,4,0),(1,5,0),(1,6,0),(2,4,0),(2,5,0),(3,6,1);

INSERT IGNORE INTO project_teacher (project_id,user_id) VALUES (1,3),(2,3),(1,7),(3,7);

INSERT IGNORE INTO course (id,project_id,course_name,cover,credit,study_hour,is_required,need_exam,pass_score,exam_limit,video_url,video_duration,intro,create_user_id,status,verify_gear_code,verify_gear_json,allow_seek,sort) VALUES
 (1,1,'继续教育政策与法规','/uploads/seed/cover1.jpg',3.00,6.00,1,0,60,0,'/uploads/seed/sample.mp4',180,'解读专业技术人员继续教育的政策要求、学时认定与学分管理办法，帮助学员建立规范学习的整体认知。',3,1,0,'{"gearCode":0,"triggerPoints":[],"intervalMinute":0,"timeoutSecond":60,"maxFailCount":3,"failStrategy":1}',0,1),
 (2,1,'人工智能与数字素养','/uploads/seed/cover2.jpg',3.00,6.00,1,1,60,0,'/uploads/seed/sample.mp4',180,'介绍人工智能基础概念、典型应用与数字工具使用规范，提升学员的数字协作与信息安全意识。',3,1,1,'{"gearCode":1,"triggerPoints":[50],"intervalMinute":0,"timeoutSecond":60,"maxFailCount":3,"failStrategy":1}',0,2),
 (3,1,'职业道德与诚信建设','/uploads/seed/cover3.jpg',2.00,4.00,2,1,60,0,'/uploads/seed/sample.mp4',180,'通过案例解析职业道德规范与诚信要求，强化学员的职业操守与责任意识。',7,1,2,'{"gearCode":2,"triggerPoints":[30,70],"intervalMinute":0,"timeoutSecond":60,"maxFailCount":3,"failStrategy":1}',0,3),
 (4,1,'心理健康与压力管理','/uploads/seed/cover4.jpg',2.00,4.00,2,0,60,0,'/uploads/seed/sample.mp4',180,'识别常见压力信号，学习情绪调节与压力管理方法，建立积极健康的工作状态。',3,1,3,'{"gearCode":3,"triggerPoints":[30,60,90],"intervalMinute":15,"timeoutSecond":60,"maxFailCount":3,"failStrategy":2}',0,4),
 (5,2,'护理质量与安全管理','/uploads/seed/cover1.jpg',3.00,6.00,1,1,60,0,'/uploads/seed/sample.mp4',180,'系统讲解护理质量管理体系、不良事件管理与持续改进方法，提升临床护理安全水平。',3,1,2,'{"gearCode":2,"triggerPoints":[30,70],"intervalMinute":0,"timeoutSecond":60,"maxFailCount":3,"failStrategy":1}',0,1),
 (6,2,'院感防控实务','/uploads/seed/cover2.jpg',3.00,6.00,1,1,60,0,'/uploads/seed/sample.mp4',180,'围绕手卫生、消毒隔离、职业暴露处置等院感防控重点，结合场景演示规范操作。',3,1,1,'{"gearCode":1,"triggerPoints":[50],"intervalMinute":0,"timeoutSecond":60,"maxFailCount":3,"failStrategy":1}',0,2),
 (7,3,'公需科目：数字经济','/uploads/seed/cover3.jpg',4.00,8.00,1,0,60,0,'/uploads/seed/sample.mp4',180,'介绍数字经济的发展趋势、产业应用与数据要素基础制度。',7,1,0,'{"gearCode":0,"triggerPoints":[],"intervalMinute":0,"timeoutSecond":60,"maxFailCount":3,"failStrategy":1}',0,1);

INSERT IGNORE INTO course_attachment (course_id,file_name,file_url,file_type,file_size) VALUES
 (1,'继续教育政策文件汇编.pdf','/uploads/seed/policy.pdf','pdf',1024),
 (2,'数字素养学习手册.pdf','/uploads/seed/digital.pdf','pdf',1024),
 (3,'职业道德案例集.pdf','/uploads/seed/ethics.pdf','pdf',1024);

INSERT IGNORE INTO question (course_id,question_type,content,options_json,answer,score,sort) VALUES
 (2,1,'人工智能训练所依赖的数据应当优先保证什么？','[{"key":"A","text":"数量越多越好"},{"key":"B","text":"质量与合规"},{"key":"C","text":"全部公开"},{"key":"D","text":"仅需图片"}]','B',20,1),
 (2,1,'下列哪项属于良好的数字安全习惯？','[{"key":"A","text":"多个平台使用同一简单密码"},{"key":"B","text":"定期更新密码并开启二次验证"},{"key":"C","text":"随意连接陌生Wi-Fi处理敏感数据"},{"key":"D","text":"将密码写在便签上"}]','B',20,2),
 (2,2,'使用人工智能生成内容时，应当注意哪些事项？','[{"key":"A","text":"核验事实准确性"},{"key":"B","text":"尊重知识产权"},{"key":"C","text":"保护个人与业务敏感信息"},{"key":"D","text":"直接用于正式发布无需审核"}]','A,B,C',20,3),
 (2,3,'只要人工智能给出的结论看起来合理，就不需要人工复核。','[{"key":"true","text":"正确"},{"key":"false","text":"错误"}]','false',20,4),
 (2,1,'个人信息处理应遵循的原则不包括？','[{"key":"A","text":"合法正当必要"},{"key":"B","text":"最小必要"},{"key":"C","text":"随意共享"},{"key":"D","text":"公开透明"}]','C',20,5),
 (3,1,'职业活动中遇到利益冲突时，最恰当的做法是？','[{"key":"A","text":"隐瞒冲突继续处理"},{"key":"B","text":"主动报告并按规定回避"},{"key":"C","text":"交给同事私下处理"},{"key":"D","text":"视金额大小决定"}]','B',25,1),
 (3,2,'诚信履职要求做到哪些？','[{"key":"A","text":"如实报告工作"},{"key":"B","text":"保守职业秘密"},{"key":"C","text":"伪造数据美化结果"},{"key":"D","text":"遵守承诺"}]','A,B,D',25,2),
 (3,3,'为按时完成任务，适度篡改数据是可以接受的。','[{"key":"true","text":"正确"},{"key":"false","text":"错误"}]','false',25,3),
 (3,1,'发现同事存在违规行为时，首先应当？','[{"key":"A","text":"公开传播"},{"key":"B","text":"按渠道如实反映"},{"key":"C","text":"视而不见"},{"key":"D","text":"私下勒索"}]','B',25,4),
 (5,1,'护理不良事件管理的核心目的是？','[{"key":"A","text":"追究个人责任"},{"key":"B","text":"发现系统问题并持续改进"},{"key":"C","text":"减少上报数量"},{"key":"D","text":"隐瞒事件"}]','B',20,1),
 (5,2,'护理质量持续改进常用方法包括？','[{"key":"A","text":"PDCA循环"},{"key":"B","text":"根因分析"},{"key":"C","text":"随意调整流程"},{"key":"D","text":"质量指标监测"}]','A,B,D',20,2),
 (5,3,'发生不良事件后应第一时间隐瞒以避免影响科室考核。','[{"key":"true","text":"正确"},{"key":"false","text":"错误"}]','false',20,3),
 (5,1,'给药前核对患者身份至少应使用几种标识？','[{"key":"A","text":"1种"},{"key":"B","text":"2种"},{"key":"C","text":"无需核对"},{"key":"D","text":"只看床号"}]','B',20,4),
 (5,1,'护理交接班时最应重点关注？','[{"key":"A","text":"个人排班"},{"key":"B","text":"患者病情与风险要点"},{"key":"C","text":"办公用品"},{"key":"D","text":"考勤"}]','B',20,5),
 (6,1,'手卫生的“两前三后”中，接触患者前应？','[{"key":"A","text":"无需洗手"},{"key":"B","text":"执行手卫生"},{"key":"C","text":"戴手套代替"},{"key":"D","text":"仅用清水冲洗"}]','B',20,1),
 (6,2,'标准预防的措施包括？','[{"key":"A","text":"手卫生"},{"key":"B","text":"合理使用防护用品"},{"key":"C","text":"安全注射"},{"key":"D","text":"共用个人防护用品"}]','A,B,C',20,2),
 (6,3,'锐器使用后应立即回套针帽。','[{"key":"true","text":"正确"},{"key":"false","text":"错误"}]','false',20,3),
 (6,1,'发生职业暴露后，首要处理是？','[{"key":"A","text":"立即局部处理并上报"},{"key":"B","text":"等待观察"},{"key":"C","text":"自行服药"},{"key":"D","text":"隐瞒不报"}]','A',20,4),
 (6,1,'医疗废物应如何处置？','[{"key":"A","text":"与生活垃圾混放"},{"key":"B","text":"按类别分类收集并规范交接"},{"key":"C","text":"随意丢弃"},{"key":"D","text":"自行出售"}]','B',20,5);

INSERT IGNORE INTO study_record (id,user_id,course_id,project_id,start_time,end_time,valid_duration,verify_count,screen_switch_count,device,ip,is_abnormal,progress,verify_total,verify_success,verify_fail,status,last_position) VALUES
 (1,4,1,1,'2026-03-02 09:00:00','2026-03-02 10:30:00',21600,2,0,'iPhone / iOS','127.0.0.1',0,100.00,0,0,0,2,180),
 (2,4,2,1,'2026-03-03 14:00:00',NULL,4200,1,0,'iPhone / iOS','127.0.0.1',0,45.00,0,0,0,1,81),
 (3,5,1,1,'2026-03-01 10:00:00','2026-03-01 11:20:00',21600,1,0,'Android','127.0.0.1',0,100.00,0,0,0,2,180),
 (4,5,3,1,'2026-03-04 15:00:00',NULL,1200,0,1,'Android','127.0.0.1',0,30.00,0,0,0,1,54),
 (5,6,1,1,'2026-03-05 08:30:00',NULL,600,0,0,'Chrome / Windows','127.0.0.1',0,10.00,0,0,0,1,18);

INSERT IGNORE INTO study_heartbeat_log (study_record_id,user_id,course_id,report_time,delta_seconds,progress,in_foreground,screen_switch,ip) VALUES
 (2,4,2,'2026-03-03 14:10:00',30,35.00,1,0,'127.0.0.1'),
 (2,4,2,'2026-03-03 14:20:00',30,45.00,1,0,'127.0.0.1');

INSERT IGNORE INTO exam_record (user_id,course_id,project_id,score,is_pass,answer_content,attempt_no,submit_time) VALUES
 (4,2,1,80.00,1,'演示数据',1,'2026-03-03 15:00:00');

INSERT IGNORE INTO credit_record (id,user_id,project_id,course_id,credit_value,source_type,audit_status,audit_user_id,audit_time,apply_user_id,remark) VALUES
 (1,4,1,1,3.00,1,2,NULL,'2026-03-02 10:30:00',NULL,'完成课程《继续教育政策与法规》自动发放'),
 (2,5,1,1,3.00,1,2,NULL,'2026-03-01 11:20:00',NULL,'完成课程《继续教育政策与法规》自动发放'),
 (3,5,1,NULL,7.00,2,2,1,'2026-02-20 09:00:00',3,'参加线下专题培训，补录 7 学分'),
 (4,6,3,7,4.00,1,2,NULL,'2025-12-20 16:00:00',NULL,'完成课程《公需科目：数字经济》自动发放');

INSERT IGNORE INTO user_verify_log (user_id,course_id,project_id,study_record_id,trigger_point,verify_time,verify_type,img_url,verify_result,status,similarity,fail_reason,remark) VALUES
 (4,2,1,2,'pct:50','2026-03-03 14:16:00',1,NULL,1,1,0.9312,NULL,'演示：核验通过'),
 (4,2,1,2,'pct:50','2026-03-03 14:18:00',1,NULL,0,2,0.4123,'人脸相似度低于阈值','演示：核验失败'),
 (5,3,1,4,'pct:30','2026-03-04 15:05:00',2,NULL,2,3,NULL,'核验超时','演示：短信核验超时');

INSERT IGNORE INTO graduate_apply (id,user_id,project_id,apply_time,current_node,audit_status,credit_snapshot,study_hour_snapshot) VALUES
 (1,5,1,'2026-03-06 09:30:00','TEACHER',0,10.00,12.00);

INSERT IGNORE INTO approval_log (biz_type,biz_id,node,auditor_id,auditor_name,action,opinion) VALUES
 ('GRADUATE',1,'TEACHER',5,'赵晨','SUBMIT','已满足项目学分要求，申请毕业');

INSERT IGNORE INTO notification (user_id,title,content,type,biz_id,is_read) VALUES
 (3,'新的毕业申请待初审','学员赵晨提交了《2026年度专业技术人员继续教育》毕业申请，请及时初审。','APPROVAL',1,0),
 (4,'课程学习提醒','您有一门课程学习进度为 45%，请合理安排时间继续学习。','WARNING',NULL,0),
 (5,'毕业申请已提交','您的毕业申请已提交，当前审批节点：教员初审。','GRADUATE',1,0);
