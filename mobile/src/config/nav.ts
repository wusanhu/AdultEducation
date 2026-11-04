/**
 * 移动端导航的唯一来源：能力全部由后端登录时下发的 perms 推导，
 * 前端不维护「角色名 → 功能」白名单，后台调整授权即移动端自动跟随。
 */

export interface Caps {
  perms: string[]
  /** 教务视角：持有任何管理侧权限 */
  isOps: boolean
  /** 带班管人（教员、超管有；领导只审批不带班） */
  managesMembers: boolean
  canListProjects: boolean
  canApproveCredit: boolean
  canAuditGraduate: boolean
  canReviewVerify: boolean
  canApprove: boolean
  canMonitor: boolean
  canListCredits: boolean
  canApplyCredit: boolean
  canViewVerify: boolean
  canDashboard: boolean
}

const has = (perms: string[], perm: string) => perms.includes(perm)

export function buildCaps(perms: string[] | undefined): Caps {
  const list = perms || []
  const canApproveCredit = has(list, 'credit:audit')
  const canAuditGraduate = has(list, 'graduate:list')
  const canReviewVerify = has(list, 'verify:abnormal')
  return {
    perms: list,
    isOps:
      canApproveCredit ||
      canAuditGraduate ||
      canReviewVerify ||
      has(list, 'project:list') ||
      has(list, 'study:monitor'),
    managesMembers: has(list, 'project:member'),
    canListProjects: has(list, 'project:list'),
    canApproveCredit,
    canAuditGraduate,
    canReviewVerify,
    canApprove: canApproveCredit || canAuditGraduate || canReviewVerify,
    canMonitor: has(list, 'study:monitor'),
    canListCredits: has(list, 'credit:list'),
    canApplyCredit: has(list, 'credit:apply'),
    canViewVerify: has(list, 'verify:list') || has(list, 'verify:abnormal'),
    canDashboard: has(list, 'dashboard:view') || has(list, 'report:view'),
  }
}

export interface TabDef {
  key: string
  label: string
  icon: string
  route: string
  badge?: 'approval' | 'unread'
}

const TAB: Record<string, TabDef> = {
  home: { key: 'home', label: '首页', icon: 'home-o', route: '/' },
  projects: { key: 'projects', label: '培训班', icon: 'orders-o', route: '/projects' },
  credits: { key: 'credits', label: '学分', icon: 'gold-coin-o', route: '/credits' },
  approvals: { key: 'approvals', label: '审批', icon: 'audit', route: '/approvals', badge: 'approval' },
  notifications: { key: 'notifications', label: '消息', icon: 'chat-o', route: '/notifications', badge: 'unread' },
  monitor: { key: 'monitor', label: '监控', icon: 'eye-o', route: '/monitor' },
  profile: { key: 'profile', label: '我的', icon: 'contact', route: '/profile' },
}

/** 中部两格按角色动线排序：带班者先「培训班」后「审批」，只审批者先「审批」后「消息」 */
export function buildTabs(c: Caps): TabDef[] {
  if (!c.isOps) return [TAB.home, TAB.projects, TAB.credits, TAB.profile]
  const candidates = c.managesMembers
    ? [TAB.projects, TAB.approvals, TAB.monitor, TAB.notifications]
    : [TAB.approvals, TAB.notifications, TAB.projects, TAB.monitor]
  const available = (t: TabDef) =>
    t === TAB.projects
      ? c.canListProjects
      : t === TAB.approvals
        ? c.canApprove
        : t === TAB.monitor
          ? c.canMonitor
          : true
  return [TAB.home, ...candidates.filter(available).slice(0, 2), TAB.profile]
}

export interface EntryDef extends TabDef {
  perms?: string[]
}

/** 首页宫格：教务入口只到「看 + 审批」，编辑类动作留在 PC 后台 */
const ENTRIES: EntryDef[] = [
  { key: 'projects', label: '培训班', icon: 'orders-o', route: '/projects' },
  { key: 'credits', label: '学分查询', icon: 'gold-coin-o', route: '/credits' },
  { key: 'graduate', label: '毕业申请', icon: 'certificate', route: '/graduate' },
  { key: 'verify', label: '核验记录', icon: 'shield-o', route: '/verify' },
  { key: 'notifications', label: '消息通知', icon: 'bell', route: '/notifications' },
  { key: 'monitor', label: '学习监控', icon: 'eye-o', route: '/monitor', perms: ['study:monitor'] },
  { key: 'exam', label: '考试成绩', icon: 'award-o', route: '/monitor?tab=exam', perms: ['study:monitor'] },
  { key: 'creditAudit', label: '学分审核', icon: 'balance-o', route: '/approvals?tab=credit', perms: ['credit:audit'] },
  { key: 'graduateAudit', label: '毕业审批', icon: 'todo-list-o', route: '/approvals?tab=graduate', perms: ['graduate:list'] },
  { key: 'verifyReview', label: '异常复核', icon: 'warning-o', route: '/approvals?tab=verify', perms: ['verify:abnormal'] },
  { key: 'creditApply', label: '学分补录', icon: 'edit', route: '/credits?apply=1', perms: ['credit:apply'] },
]

/** 学员自用入口，教务视角下不展示 */
const STUDENT_ONLY = ['credits', 'graduate']

export function buildEntries(c: Caps): EntryDef[] {
  return ENTRIES.filter((e) => {
    if (e.perms && !e.perms.some((p) => c.perms.includes(p))) return false
    if (c.isOps && STUDENT_ONLY.includes(e.key)) return false
    if (c.isOps && e.key === 'projects' && !c.canListProjects) return false
    return true
  }).slice(0, 9)
}

/** 底部 tab 之外的路由准入：与后端 @PreAuthorize 同一套权限标识 */
export const ROUTE_PERMS: Record<string, string[]> = {
  Approvals: ['credit:audit', 'graduate:list', 'verify:abnormal'],
  Monitor: ['study:monitor'],
}
