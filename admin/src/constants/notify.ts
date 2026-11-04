/** 站内信类别，取值与后端 NotificationService.CATEGORIES 一致 */
export interface NotifyCategory {
  value: string
  label: string
  color: string
}

export const NOTICE_CATEGORIES: NotifyCategory[] = [
  { value: 'COURSE', label: '课程学习', color: 'blue' },
  { value: 'CREDIT', label: '学分管理', color: 'geekblue' },
  { value: 'GRADUATE', label: '毕业审核', color: 'purple' },
  { value: 'APPROVAL', label: '待办审批', color: 'orange' },
  { value: 'WARNING', label: '预警提醒', color: 'red' },
  { value: 'VERIFY', label: '身份核验', color: 'cyan' },
  { value: 'ACCOUNT', label: '账号消息', color: 'green' },
  { value: 'SYSTEM', label: '系统通知', color: 'default' }
]

const CATEGORY_MAP = new Map(NOTICE_CATEGORIES.map((c) => [c.value, c]))

export function notifyCategory(type?: string): NotifyCategory {
  return CATEGORY_MAP.get(type || '') || { value: type || 'SYSTEM', label: '系统通知', color: 'default' }
}
