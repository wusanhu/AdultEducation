export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface MenuNode {
  id: number
  parentId: number
  menuName: string
  menuType: number
  path?: string
  component?: string
  icon?: string
  perms?: string
  sort: number
  hidden: number
  mobileVisible: number
  status?: number
  children: MenuNode[]
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  phone?: string
  avatar?: string
  orgId?: number
  orgName?: string
  roleCode: string
  roleName: string
  userType: number
  mustChangePwd: boolean
  idCard?: string
  lastLoginTime?: string
}

export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
  user: UserInfo
  roleCode: string
  roleName: string
  perms: string[]
  menus: MenuNode[]
  mustChangePwd: boolean
}

export interface OrgNode {
  id: number
  parentId: number
  orgName: string
  orgType: number
  orgCode: string
  leaderUserId?: number
  leaderName?: string
  sort: number
  status: number
  userCount?: number
  children: OrgNode[]
}

export interface RoleRow {
  id: number
  roleName: string
  roleCode: string
  roleDesc?: string
  dataScope: number
  builtIn: number
  status: number
  createTime?: string
  userCount?: number
}

export interface UserRow {
  id: number
  username: string
  realName: string
  phone?: string
  idCard?: string
  orgId?: number
  orgName?: string
  deptId?: number
  deptName?: string
  roleId: number
  roleName?: string
  roleCode?: string
  userType: number
  status: number
  mustChangePwd?: number
  lastLoginTime?: string
  lastLoginIp?: string
  createTime?: string
  remark?: string
}

export interface StudentRow {
  id: number
  username: string
  realName: string
  phone?: string
  orgId?: number
  orgName?: string
  deptName?: string
  /** 人脸基准照URL，核验 1:1 比对依据 */
  faceImg?: string
  status: number
  createTime?: string
  lastLoginTime?: string
  enrolledProjects: number
  learningCourses: number
  completedCourses: number
  totalCredits: number
  lastStudyTime?: string
}

export interface ProjectRow {
  id: number
  projectName: string
  projectCode: string
  description?: string
  startTime: string
  endTime: string
  needCredit: number
  status: number
  certificateTemplateUrl?: string
  allowOverdue: number
  autoGraduate: number
  needTeacherAudit: number
  needLeaderAudit: number
  editable?: boolean
  createUserId?: number
  createUserName?: string
  createTime?: string
  memberCount: number
  courseCount: number
  teachers: { id: number; realName: string }[]
  credit?: number
  progress?: number
}

export interface ProjectMemberRow {
  userId: number
  username: string
  realName: string
  phone?: string
  orgName?: string
  joinTime?: string
  completionStatus?: number
  credit?: number
  progress?: number
  courseTotal?: number
  completedCourse?: number
  graduateStatus?: number
  graduateNode?: string
}

export interface CourseRow {
  id: number
  projectId: number
  projectName?: string
  projectStatus?: number
  projectArchived?: boolean
  courseName: string
  cover?: string
  credit: number
  studyHour: number
  isRequired: number
  needExam: number
  passScore: number
  examLimit: number
  videoUrl?: string
  videoDuration?: number
  intro?: string
  createUserId?: number
  createUserName?: string
  status: number
  verifyGearCode: number
  verifyGearName?: string
  allowSeek: number
  sort: number
  studyCount: number
  avgProgress: number
  questionCount: number
  createTime?: string
}

export interface Attachment {
  id?: number
  fileName: string
  fileUrl: string
  fileType?: string
  fileSize?: number
}

export interface Question {
  id?: number
  questionType: number
  content: string
  optionsJson?: string
  answer: string
  score: number
  sort: number
}

export interface CourseDetail extends CourseRow {
  verifyGearJson?: string
  attachments: Attachment[]
  questions: Question[]
  manageable: boolean
  studyRecord?: Record<string, unknown>
  bestExam?: Record<string, unknown>
}

export interface VerifyGear {
  id: number
  gearCode: number
  gearName: string
  triggerPoints: string
  intervalMinute: number
  timeoutSecond: number
  maxFailCount: number
  failStrategy: number
  /** 是否真人活体核验 0否 1是 */
  liveCheck: number
  /** 核验方式 1人脸核验 2学习验证 */
  verifyMode: number
  builtIn: number
  status: number
  description?: string
}

export interface StudyRecordRow {
  id: number
  userId: number
  userName?: string
  username?: string
  orgName?: string
  courseId: number
  courseName?: string
  projectId: number
  projectName?: string
  projectArchived?: boolean
  startTime?: string
  endTime?: string
  validDuration: number
  progress: number
  status: number
  statusLabel?: string
  verifyCount: number
  verifyTotal?: number
  verifySuccess: number
  verifyFail: number
  screenSwitchCount: number
  screenSwitchDuration: number
  isAbnormal: number
  abnormalReason?: string
  lastHeartbeat?: string
  online: boolean
  lastPosition?: number
  device?: string
  ip?: string
}

export interface VerifyLogRow {
  id: number
  userId: number
  userName?: string
  orgName?: string
  courseId: number
  courseName?: string
  projectName?: string
  triggerPoint?: string
  verifyTime?: string
  verifyType: number
  verifyTypeLabel: string
  imgUrl?: string
  verifyResult?: number
  verifyResultLabel?: string
  status: number
  statusLabel?: string
  similarity?: number
  failReason?: string
  reviewTime?: string
  reviewAction?: string
  remark?: string
}

export interface AbnormalRow extends StudyRecordRow {
  latestVerify?: VerifyLogRow
  locked?: boolean
}

export interface CreditRow {
  id: number
  userId: number
  userName?: string
  username?: string
  orgName?: string
  projectId: number
  projectName?: string
  projectArchived?: boolean
  courseId?: number
  courseName?: string
  creditValue: number
  sourceType: number
  sourceLabel: string
  auditStatus: number
  auditStatusLabel: string
  auditNode?: string
  auditUserName?: string
  auditTime?: string
  auditRemark?: string
  applyUserName?: string
  remark?: string
  revokeReason?: string
  revokeUserName?: string
  revokeTime?: string
  createTime?: string
}

export interface GraduateApplyRow {
  id: number
  userId: number
  userName?: string
  username?: string
  orgName?: string
  projectId: number
  projectName?: string
  projectArchived?: boolean
  needCredit?: number
  applyTime?: string
  currentNode: string
  currentNodeLabel?: string
  auditStatus: number
  auditStatusLabel?: string
  rejectReason?: string
  creditSnapshot: number
  studyHourSnapshot: number
  firstAuditUserName?: string
  firstAuditTime?: string
  firstAuditOpinion?: string
  secondAuditUserName?: string
  secondAuditTime?: string
  secondAuditOpinion?: string
  finalAuditUserName?: string
  finalAuditTime?: string
  finalAuditOpinion?: string
  certificateNo?: string
  certificateUrl?: string
  graduateTime?: string
  approvalLogs?: ApprovalLog[]
}

export interface ApprovalLog {
  id: number
  node?: string
  action?: string
  actionLabel?: string
  auditorId?: number
  auditorName?: string
  auditUserId?: number
  auditUserName?: string
  opinion?: string
  createTime?: string
  auditTime?: string
}

export interface GraduateFileRow {
  id: number
  applyId: number
  userId: number
  userName?: string
  username?: string
  orgName?: string
  projectId: number
  projectName?: string
  certificateNo: string
  certificateUrl?: string
  graduateTime?: string
  creditValue: number
}

export interface DictRow {
  id: number
  dictName: string
  dictType: string
  dictValue: string
  dictLabel: string
  sort: number
  status: number
}

export interface NoticeTemplateRow {
  id: number
  templateCode: string
  templateName: string
  type: string
  title: string
  content: string
  mobileLink?: string
  adminLink?: string
  enabled: number
  createTime?: string
  updateTime?: string
}

export interface ConfigRow {
  id: number
  configName: string
  configKey: string
  configValue: string
  configType: string
  remark?: string
}

export interface LogRow {
  id: number
  userName?: string
  orgName?: string
  module?: string
  content?: string
  operTime?: string
  loginTime?: string
  ip?: string
  device?: string
  result?: number
  resultLabel?: string
  status?: number
  [key: string]: unknown
}

export interface DashboardTodo {
  type: 'CREDIT' | 'GRADUATE'
  id: number
  userName?: string
  projectName?: string
  value?: number
  node?: string
  time?: string
  link: string
}

export interface DashboardProjectRow {
  id: number
  projectName: string
  status?: number
  endTime?: string
  memberCount: number
  graduateCount: number
  creditTotal: number
  rate: number
}

export interface DashboardCourseRow {
  id: number
  courseName: string
  projectName?: string
  status?: number
  studentCount: number
  completedCount: number
  avgProgress: number
}

export interface OverviewData {
  role: 'admin' | 'leader' | 'teacher' | 'student'
  projectCount: number
  activeProjectCount: number
  studentCount: number
  courseCount: number
  studyCount: number
  completedStudyCount: number
  validHours: number
  creditTotal: number
  creditIssuedCount: number
  pendingCreditCount: number
  pendingCreditList: DashboardTodo[]
  graduateCount: number
  pendingGraduateCount: number
  pendingGraduateList: DashboardTodo[]
  projectProgress?: DashboardProjectRow[]
  myCourses?: DashboardCourseRow[]
  verifyTotal: number
  verifyPassRate: number
  abnormalCount: number
  unreadCount: number
  trend: { date: string; credits: number; studyMinutes: number }[]
  recentGraduate: { userName: string; projectName: string; graduateTime: string; certificateNo: string }[]
  recentCredits: { userName: string; courseName: string; creditValue: number; createTime: string; sourceLabel?: string; auditTime?: string }[]
  [key: string]: unknown
}
