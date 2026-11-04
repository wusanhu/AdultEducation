import { defineStore } from 'pinia'
import { ref } from 'vue'
import { credits, graduate, notifications, reports, verify } from '@/api'
import { useUserStore } from './user'
import type { Caps } from '@/config/nav'

const num = (v: unknown) => (typeof v === 'number' && v > 0 ? v : 0)

/** 底部 tab 角标：教务角色用概览接口一次取齐待办数，其余按需查询有权限的清单 */
export const useTodoStore = defineStore('todo', () => {
  const approvalCount = ref(0)
  const unreadCount = ref(0)

  async function approvalTotal(caps: Caps): Promise<number> {
    if (caps.isOps && caps.canDashboard) {
      const o = await reports.overview().catch(() => null)
      return num(o?.pendingCreditCount) + num(o?.pendingGraduateCount) + num(o?.abnormalCount)
    }
    const parts: Promise<number>[] = []
    if (caps.canApproveCredit) {
      parts.push(credits.pending().then((list) => (Array.isArray(list) ? list.length : 0)))
    }
    if (caps.canAuditGraduate) {
      parts.push(graduate.applies({ pageNum: 1, pageSize: 1, auditStatus: 0 }).then((r: any) => num(r?.total)))
    }
    if (caps.canReviewVerify) {
      parts.push(verify.abnormal({ pageNum: 1, pageSize: 1 }).then((r: any) => num(r?.total)))
    }
    const counts = await Promise.all(parts.map((p) => p.catch(() => 0)))
    return counts.reduce((sum, n) => sum + n, 0)
  }

  async function refresh() {
    const caps = useUserStore().caps
    const [approvals, unread] = await Promise.all([
      approvalTotal(caps),
      notifications.unreadCount().catch(() => 0),
    ])
    approvalCount.value = approvals
    unreadCount.value = num(unread as number)
  }

  return { approvalCount, unreadCount, refresh }
})
