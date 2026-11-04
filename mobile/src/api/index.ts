import { get, post, put, del, upload } from './http'

// ---- Auth ----
export const auth = {
  login: (data: { username: string; password: string }) =>
    post<any>('/auth/login', data),
  logout: () => post('/auth/logout'),
  me: () => get<any>('/auth/me'),
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    post('/auth/change-password', data),
  sendSms: (phone: string) => post('/auth/sms/send', { phone }),
  smsLogin: (data: { phone: string; code: string }) =>
    post<any>('/auth/sms/login', data),
}

// ---- Profile ----
export const profile = {
  get: () => get<any>('/profile'),
  update: (data: { realName?: string; phone?: string; avatar?: string }) =>
    put<any>('/profile', data),
}

// ---- Notifications ----
export const notifications = {
  list: (params: { pageNum?: number; pageSize?: number; isRead?: number; type?: string }) =>
    get<any>('/notifications', params),
  unreadCount: () => get<number>('/notifications/unread-count'),
  read: (id: number) => post(`/notifications/${id}/read`),
  readAll: () => post('/notifications/read-all'),
  delete: (id: number) => del(`/notifications/${id}`),
}

// ---- Projects ----
export const projects = {
  my: () => get<any[]>('/projects/my'),
  myDetail: (id: number) => get<any>(`/projects/my/${id}`),
  options: () => get<any[]>('/projects/options'),
  studentOptions: (projectId: number) => get<any[]>('/projects/student-options', { projectId }),
  detail: (id: number) => get<any>(`/projects/${id}`),
  page: (params: { pageNum?: number; pageSize?: number; keyword?: string; status?: number }) =>
    get<any>('/projects', params),
}

// ---- Courses ----
export const courses = {
  detail: (id: number) => get<any>(`/courses/${id}`),
  options: (projectId?: number) => get<any[]>('/courses/options', { projectId }),
}

// ---- Study ----
export const study = {
  start: (courseId: number) => post<any>('/study/start', { courseId }),
  heartbeat: (data: {
    courseId: number
    sessionId: string
    deltaSeconds?: number
    progress?: number
    position?: number
    inForeground?: number
    screenSwitch?: number
    screenSwitchDuration?: number
  }) => post<any>('/study/heartbeat', data),
  pause: (courseId: number) => post<any>('/study/pause', null as any),
  popupConfirm: (courseId: number, sessionId: string) =>
    post<any>('/study/popup/confirm', { courseId, sessionId }),
  state: (courseId: number) => get<any>('/study/state', { courseId }),
  verifyStart: (courseId: number, triggerPoint: string) =>
    post<any>('/study/verify/start', { courseId, triggerPoint }),
  verifyFace: (logId: number, imageBase64?: string) =>
    post<any>('/study/verify/face', { logId, imageBase64 }),
  verifySmsSend: (logId: number) =>
    post<any>(`/study/verify/sms/send?logId=${logId}`),
  verifySmsSubmit: (logId: number, code: string) =>
    post<any>('/study/verify/sms/submit', { logId, code }),
}

// pause needs courseId as query param
export function pauseStudy(courseId: number) {
  return post<any>(`/study/pause?courseId=${courseId}`)
}

// ---- Exam ----
export const exam = {
  paper: (courseId: number) => get<any>(`/exams/${courseId}/paper`),
  submit: (courseId: number, answers: { questionId: number; answer: string }[]) =>
    post<any>(`/exams/${courseId}/submit`, { answers }),
  my: (courseId?: number) => get<any[]>('/exams/my', { courseId }),
  page: (params: { pageNum?: number; pageSize?: number; projectId?: number; courseId?: number; keyword?: string }) =>
    get<any>('/exams', params),
}

// ---- Credits ----
export const credits = {
  my: () => get<any[]>('/credits/my'),
  summary: () => get<any>('/credits/summary'),
  apply: (data: { userId: number; projectId: number; creditValue: number; remark: string }) =>
    post<any>('/credits/apply', data),
  pending: () => get<any[]>('/credits/pending'),
  audit: (id: number, data: { approve: boolean; remark?: string }) =>
    post<any>(`/credits/${id}/audit`, data),
}

// ---- Graduate ----
export const graduate = {
  eligibility: (projectId: number) => get<any>('/graduate/eligibility', { projectId }),
  apply: (projectId: number) => post<any>('/graduate/apply', { projectId }),
  my: (projectId?: number) => get<any>('/graduate/my', { projectId }),
  myList: () => get<any[]>('/graduate/my/list'),
  myCertificate: (projectId?: number) => get<any>('/graduate/my/certificate', { projectId }),
  detail: (id: number) => get<any>(`/graduate/applies/${id}`),
  applies: (params: {
    pageNum?: number
    pageSize?: number
    projectId?: number
    auditStatus?: number
    currentNode?: string
    keyword?: string
  }) => get<any>('/graduate/applies', params),
  audit: (id: number, data: { action: 'approve' | 'reject'; opinion?: string }) =>
    post<any>(`/graduate/applies/${id}/audit`, data),
}

// ---- Verify ----
export const verify = {
  my: (courseId?: number) => get<any[]>('/verify/my', { courseId }),
  records: (params: {
    pageNum?: number
    pageSize?: number
    projectId?: number
    courseId?: number
    userId?: number
    verifyResult?: number
    status?: number
    startDate?: string
    endDate?: string
  }) => get<any>('/verify/records', params),
  abnormal: (params: { pageNum?: number; pageSize?: number; projectId?: number; courseId?: number; keyword?: string }) =>
    get<any>('/verify/abnormal', params),
  review: (logId: number, data: { action: 'allow' | 'invalidate' | 'violate'; remark?: string }) =>
    post<any>(`/verify/${logId}/review`, data),
  studyRecords: (params: {
    pageNum?: number
    pageSize?: number
    projectId?: number
    courseId?: number
    userId?: number
    keyword?: string
    abnormal?: number
    status?: number
  }) => get<any>('/verify/study-records', params),
  studyRecordDetail: (id: number) => get<any>(`/verify/study-records/${id}`),
}

// ---- Reports ----
export const reports = {
  overview: () => get<any>('/reports/overview'),
  warnings: (params?: { projectId?: number; type?: string }) => get<any[]>('/reports/warnings', params),
}

// ---- Files ----
export const files = {
  upload: (file: File, type?: string) => upload<any>('/files/upload', file, type),
}

// ---- System (dicts/configs) ----
export const system = {
  dicts: (dictType?: string) => get<any[]>('/system/dicts', { dictType }),
  configs: () => get<any[]>('/system/configs'),
  orgTree: () => get<any[]>('/system/org/tree'),
}
