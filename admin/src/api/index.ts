import { del, download, get, post, put, upload } from './http'
import type {
  AbnormalRow, ConfigRow, CourseDetail, CourseRow, CreditRow, DictRow, GraduateApplyRow,
  GraduateFileRow, LoginResult, LogRow, NoticeTemplateRow, OrgNode, OverviewData, PageResult, ProjectMemberRow,
  ProjectRow, RoleRow, StudentRow, StudyRecordRow, UserInfo, UserRow, VerifyGear, VerifyLogRow
} from '../types'

export const authApi = {
  login: (username: string, password: string) => post<LoginResult>('/auth/login', { username, password }),
  me: () => get<LoginResult>('/auth/me'),
  changePassword: (oldPassword: string, newPassword: string) =>
    post<void>('/auth/change-password', { oldPassword, newPassword }),
  logout: () => post<void>('/auth/logout')
}

export const dashboardApi = {
  overview: () => get<OverviewData>('/reports/overview')
}

export interface Option {
  id: number
  label: string
  [key: string]: unknown
}

export const projectApi = {
  page: (params: Record<string, unknown>) => get<PageResult<ProjectRow>>('/projects', params),
  options: () => get<Option[]>('/projects/options'),
  teacherOptions: () => get<Option[]>('/projects/teacher-options'),
  studentOptions: (projectId?: number) => get<Option[]>('/projects/student-options', { projectId }),
  detail: (id: number) => get<ProjectRow>(`/projects/${id}`),
  nextCode: () => get<string>('/projects/next-code'),
  save: (data: Record<string, unknown>) => post<number>('/projects', data),
  update: (id: number, data: Record<string, unknown>) => put<number>(`/projects/${id}`, data),
  remove: (id: number) => del<void>(`/projects/${id}`),
  status: (id: number, status: number, period?: { startTime: string; endTime: string }) =>
    put<void>(`/projects/${id}/status?status=${status}`, period),
  members: (id: number, params: Record<string, unknown>) => get<PageResult<ProjectMemberRow>>(`/projects/${id}/members`, params),
  addMembers: (id: number, userIds: number[]) => post<{ added: number }>(`/projects/${id}/members`, { userIds }),
  removeMember: (id: number, userId: number) => del<void>(`/projects/${id}/members/${userId}`),
  teachers: (id: number) => get<{ id: number; realName: string; phone?: string; orgName?: string; courseCount?: number }[]>(`/projects/${id}/teachers`),
  assignTeachers: (id: number, userIds: number[]) => put<void>(`/projects/${id}/teachers`, { userIds })
}

export const courseApi = {
  page: (params: Record<string, unknown>) => get<PageResult<CourseRow>>('/courses', params),
  options: (projectId?: number) => get<Option[]>('/courses/options', { projectId }),
  detail: (id: number) => get<CourseDetail>(`/courses/${id}`),
  gears: (onlyEnabled = false) => get<VerifyGear[]>('/courses/gears', { onlyEnabled }),
  save: (data: Record<string, unknown>) => post<number>('/courses', data),
  update: (id: number, data: Record<string, unknown>) => put<number>(`/courses/${id}`, data),
  remove: (id: number) => del<void>(`/courses/${id}`),
  status: (id: number, status: number) => put<void>(`/courses/${id}/status?status=${status}`),
  saveGear: (data: Record<string, unknown>) => post<number>('/courses/gears', data),
  removeGear: (id: number) => del<void>(`/courses/gears/${id}`),
  importQuestions: (id: number, file: File) =>
    upload<{ imported: number; questions: CourseDetail['questions'] }>(`/courses/${id}/questions/import`, file),
  downloadQuestionTemplate: () => download('/courses/questions/template', undefined, '课后题库导入模板.xlsx')
}

export const studyApi = {
  monitor: (params: Record<string, unknown>) => get<PageResult<StudyRecordRow>>('/verify/study-records', params),
  detail: (id: number) => get<Record<string, unknown>>(`/verify/study-records/${id}`),
  unlock: (id: number, clearAbnormal = true) => post<void>(`/study/records/${id}/unlock?clearAbnormal=${clearAbnormal}&remark=管理员解除学习限制`)
}

export const verifyApi = {
  records: (params: Record<string, unknown>) => get<PageResult<VerifyLogRow>>('/verify/records', params),
  abnormal: (params: Record<string, unknown>) => get<PageResult<AbnormalRow>>('/verify/abnormal', params),
  review: (id: number, action: string, remark?: string) => post<void>(`/verify/${id}/review`, { action, remark })
}

export const studentApi = {
  page: (params: Record<string, unknown>) => get<PageResult<StudentRow>>('/students', params),
  updateFaceImg: (id: number, faceImg: string) => put<void>(`/students/${id}/face-img`, { faceImg })
}

export const creditApi = {
  page: (params: Record<string, unknown>) => get<PageResult<CreditRow>>('/credits', params),
  apply: (data: Record<string, unknown>) => post<number>('/credits/apply', data),
  pending: () => get<CreditRow[]>('/credits/pending'),
  audit: (id: number, approve: boolean, remark?: string) => post<void>(`/credits/${id}/audit`, { approve, remark }),
  revoke: (id: number, reason: string) => post<void>(`/credits/${id}/revoke`, { reason })
}

export const graduateApi = {
  page: (params: Record<string, unknown>) => get<PageResult<GraduateApplyRow>>('/graduate/applies', params),
  detail: (id: number) => get<GraduateApplyRow>(`/graduate/applies/${id}`),
  audit: (id: number, action: 'approve' | 'reject', opinion?: string) =>
    post<void>(`/graduate/applies/${id}/audit`, { action, opinion }),
  files: (params: Record<string, unknown>) => get<PageResult<GraduateFileRow>>('/graduate/files', params)
}

export const reportApi = {
  overview: () => get<OverviewData>('/reports/overview'),
  org: (projectId?: number) => get<Record<string, unknown>[]>('/reports/org', { projectId }),
  credit: (projectId?: number) => get<Record<string, unknown>[]>('/reports/credit', { projectId }),
  course: (projectId?: number) => get<Record<string, unknown>[]>('/reports/course', { projectId }),
  warnings: (params: Record<string, unknown>) => get<Record<string, unknown>[]>('/reports/warnings', params),
  notifyWarnings: (params: Record<string, unknown>) => post<number>('/reports/warnings/notify', undefined, { params }),
  export: (params: Record<string, unknown>) => download('/reports/export', params, '统计报表.xlsx')
}

export const systemApi = {
  orgTree: () => get<OrgNode[]>('/system/org/tree'),
  saveOrg: (data: Record<string, unknown>) => data.id ? put<number>(`/system/org/${data.id}`, data) : post<number>('/system/org', data),
  removeOrg: (id: number) => del<void>(`/system/org/${id}`),
  orgStatus: (id: number, status: number) => put<void>(`/system/org/${id}/status`, { status }),
  users: (params: Record<string, unknown>) => get<PageResult<UserRow>>('/system/users', params),
  userDetail: (id: number) => get<UserRow>(`/system/users/${id}`),
  saveUser: (data: Record<string, unknown>) => data.id ? put<UserRow>(`/system/users/${data.id}`, data) : post<UserRow>('/system/users', data),
  resetPassword: (id: number) => post<{ password: string }>(`/system/users/${id}/reset-password`),
  userStatus: (id: number, status: number) => put<void>(`/system/users/${id}/status`, { status }),
  removeUser: (id: number) => del<void>(`/system/users/${id}`),
  importUsers: (file: File) => upload<Record<string, unknown>>('/system/users/import', file),
  upload: (file: File) => upload<{ url: string }>('/files/upload', file),
  exportUsers: (params: Record<string, unknown>) => download('/system/users/export', params, '账号列表.xlsx'),
  roles: () => get<RoleRow[]>('/system/roles'),
  roleDetail: (id: number) => get<{ role: RoleRow; menuIds: number[]; orgIds: number[] }>(`/system/roles/${id}`),
  saveRole: (data: Record<string, unknown>) => data.id ? put<number>(`/system/roles/${data.id}`, data) : post<number>('/system/roles', data),
  removeRole: (id: number) => del<void>(`/system/roles/${id}`),
  roleStatus: (id: number, status: number) => put<void>(`/system/roles/${id}/status`, { status }),
  menuTree: () => get<import('../types').MenuNode[]>('/system/menus/tree'),
  saveMenu: (data: Record<string, unknown>) => data.id ? put<number>(`/system/menus/${data.id}`, data) : post<number>('/system/menus', data),
  removeMenu: (id: number) => del<void>(`/system/menus/${id}`),
  menuStatus: (id: number, status: number) => put<void>(`/system/menus/${id}/status`, { status }),
  configs: () => get<ConfigRow[]>('/system/configs'),
  saveConfigs: (data: Record<string, unknown>[]) => put<void>('/system/configs', data),
  dicts: (dictType?: string) => get<DictRow[]>('/system/dicts', { dictType }),
  saveDict: (data: Record<string, unknown>) => data.id ? put<number>(`/system/dicts/${data.id}`, data) : post<number>('/system/dicts', data),
  removeDict: (id: number) => del<void>(`/system/dicts/${id}`),
  operLogs: (params: Record<string, unknown>) => get<PageResult<LogRow>>('/system/oper-logs', params),
  loginLogs: (params: Record<string, unknown>) => get<PageResult<LogRow>>('/system/login-logs', params)
}

export const profileApi = {
  profile: () => get<UserInfo>('/profile'),
  update: (data: Record<string, unknown>) => put<UserInfo>('/profile', data)
}

export const notificationApi = {
  page: (params: Record<string, unknown>) => get<PageResult<Record<string, unknown>>>('/notifications', params),
  unread: () => get<number>('/notifications/unread-count'),
  read: (id: number) => post<void>(`/notifications/${id}/read`),
  readAll: () => post<number>('/notifications/read-all')
}

export const noticeTemplateApi = {
  list: (params: Record<string, unknown> = {}) => get<NoticeTemplateRow[]>('/system/notice-templates', params),
  categories: () => get<Record<string, number>>('/system/notice-templates/categories'),
  save: (data: Record<string, unknown>) => post<number>('/system/notice-templates', data),
  update: (id: number, data: Record<string, unknown>) => put<number>(`/system/notice-templates/${id}`, data),
  remove: (id: number) => del<void>(`/system/notice-templates/${id}`)
}
