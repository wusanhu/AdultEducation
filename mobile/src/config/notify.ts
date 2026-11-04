/**
 * 站内信类别的唯一来源，取值与后端 NotificationService.CATEGORIES 一致。
 * 消息列表的分类筛选、标签配色都从这里推导，后台新增类别时移动端自动跟随。
 */

export interface NotifyCategory {
  value: string
  label: string
  /** vant tag 类型 */
  tag: 'default' | 'primary' | 'success' | 'warning' | 'danger'
}

export const NOTICE_CATEGORIES: NotifyCategory[] = [
  { value: 'COURSE', label: '课程学习', tag: 'primary' },
  { value: 'CREDIT', label: '学分管理', tag: 'success' },
  { value: 'GRADUATE', label: '毕业审核', tag: 'primary' },
  { value: 'APPROVAL', label: '待办审批', tag: 'warning' },
  { value: 'WARNING', label: '预警提醒', tag: 'danger' },
  { value: 'VERIFY', label: '身份核验', tag: 'success' },
  { value: 'ACCOUNT', label: '账号消息', tag: 'default' },
  { value: 'SYSTEM', label: '系统通知', tag: 'default' }
]

const CATEGORY_MAP = new Map(NOTICE_CATEGORIES.map((c) => [c.value, c]))

export function notifyCategory(type?: string): NotifyCategory {
  const hit = CATEGORY_MAP.get(type || '')
  return hit || { value: type || 'SYSTEM', label: '系统通知', tag: 'default' }
}
