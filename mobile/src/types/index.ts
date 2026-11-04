/** 后端统一响应结构 */
export interface ApiResult<T> {
  code: number
  msg: string
  data: T
}

/** 分页响应结构 */
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** 当前登录用户 */
export interface UserInfo {
  id: number
  username: string
  realName: string
  phone?: string
  avatar?: string
  roleCode?: string
  roleName?: string
  perms?: string[]
  orgName?: string
}

/** 登录返回 */
export interface LoginResult {
  token: string
  tokenType?: string
  expiresIn?: number
  user: UserInfo
  roleCode: string
  roleName: string
  perms: string[]
  menus?: unknown[]
  mustChangePwd?: boolean
}

/** 通用状态标签：0 未开始 / 1 进行中 / 2 已结束 / 3 已归档 */
export type ProjectStatus = 0 | 1 | 2 | 3

/** 学分状态：1 待发放 / 2 已发放 / 3 已驳回 / 4 已撤销 */
export type CreditStatus = 1 | 2 | 3 | 4

/** 毕业审批状态：0 审批中 / 1 已通过 / 2 已驳回 */
export type GraduateStatus = 0 | 1 | 2
