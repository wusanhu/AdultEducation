<template>
  <div class="page-container ops-home">
    <header class="home-hero">
      <span class="hero-ornament" aria-hidden="true">学</span>
      <div class="hero-top">
        <div class="greeting">
          <p class="hello">{{ greetingText }}</p>
          <h2 class="text-ellipsis">{{ userStore.user?.realName || '老师' }}</h2>
          <p class="hero-role">
            <van-icon name="manager-o" size="13" />
            <span class="role-name text-ellipsis">{{ userStore.user?.roleName || '教务' }}</span>
            <span v-if="userStore.user?.orgName" class="hero-org text-ellipsis">{{ userStore.user?.orgName }}</span>
          </p>
        </div>
        <div class="notify-icon" @click="go('/notifications')">
          <van-badge :content="unreadCount > 0 ? unreadCount : ''" :max="99">
            <van-icon name="bell" size="21" color="var(--ink-2)" />
          </van-badge>
        </div>
      </div>
      <div class="hero-strip">
        <div class="strip-cell" @click="go(auditTab('credit'))">
          <span class="strip-label">待办事项</span>
          <span class="strip-value">{{ pendingTotal }}<i>项</i></span>
        </div>
        <div class="strip-cell" @click="go(verifyRoute)">
          <span class="strip-label">核验通过率</span>
          <span class="strip-value">{{ passRateText }}<i v-if="passRateText !== '—'">%</i></span>
        </div>
      </div>
    </header>

    <div class="stat-cards">
      <div v-for="k in kpis" :key="k.label" class="stat-item" @click="go(k.route)">
        <div class="stat-value">{{ k.value }}</div>
        <div class="stat-label">
          <span class="stat-dot" :class="`dot-${k.dot}`"></span>
          <span>{{ k.label }}</span>
        </div>
      </div>
    </div>

    <div class="section-title flex-between">
      <span>{{ progressTitle }}</span>
      <span class="more-link" @click="go(progressMoreRoute)">
        {{ progressMode === 'course' ? '监控' : '全部' }}
        <van-icon name="arrow" size="11" />
      </span>
    </div>
    <div v-if="loading" class="card skeleton-card">
      <van-skeleton title :row="3" />
    </div>
    <div v-else-if="progressRows.length === 0" class="empty-card compact">
      <p>暂无{{ progressMode === 'course' ? '课程' : '培训班' }}进展数据</p>
    </div>
    <div
      v-for="row in progressRows"
      :key="row.id"
      class="card prog-card"
      @click="go(row.route)"
    >
      <div class="prog-head">
        <span class="prog-name text-ellipsis">{{ row.title }}</span>
        <span class="prog-percent">{{ row.percent }}%</span>
      </div>
      <p class="prog-sub text-ellipsis">{{ row.sub }}</p>
      <van-progress
        :percentage="row.percent"
        :show-pivot="false"
        color="var(--accent)"
        stroke-width="5"
        track-color="var(--surface-3)"
      />
      <div class="flex-between mt-8">
        <span class="prog-meta text-ellipsis">{{ row.left }}</span>
        <span class="prog-meta">{{ row.right }}</span>
      </div>
    </div>

    <div class="section-title flex-between">
      <span>待办清单</span>
      <span class="more-link" @click="go(auditTab('credit'))">
        全部
        <van-icon name="arrow" size="11" />
      </span>
    </div>
    <div v-if="loading" class="card skeleton-card">
      <van-skeleton title :row="4" />
    </div>
    <div v-else-if="todoRows.length === 0" class="empty-card compact">
      <p>暂无待办事项</p>
    </div>
    <div v-else class="card list-card">
      <div
        v-for="row in todoRows"
        :key="row.key"
        class="todo-item"
        @click="go(row.route)"
      >
        <span class="todo-chip">{{ row.typeLabel }}</span>
        <div class="todo-main">
          <span class="todo-name text-ellipsis">{{ row.userName }}</span>
          <p class="todo-sub text-ellipsis">{{ row.sub }}</p>
        </div>
        <div class="todo-side">
          <span v-if="row.valueText" class="todo-value">{{ row.valueText }}</span>
          <span class="todo-time">{{ formatTime(row.time) }}</span>
        </div>
      </div>
    </div>

    <div class="section-title flex-between">
      <span>学习预警</span>
      <span v-if="userStore.caps.canMonitor" class="more-link" @click="go('/monitor')">
        明细
        <van-icon name="arrow" size="11" />
      </span>
    </div>
    <div v-if="loading" class="card skeleton-card">
      <van-skeleton title :row="3" />
    </div>
    <div v-else-if="warningRows.length === 0" class="empty-card compact">
      <van-icon name="shield-o" size="30" color="var(--ink-4)" />
      <p>暂无学习预警</p>
    </div>
    <div v-else class="card list-card">
      <div v-for="(w, i) in warningRows" :key="i" class="warn-item" :class="`warn-${warnKind(w)}`">
        <div class="warn-body">
          <div class="warn-head">
            <span class="warn-label">{{ w.typeLabel || w.type }}</span>
            <span class="warn-title text-ellipsis">{{ warnTitle(w) }}</span>
          </div>
          <p class="warn-msg">{{ w.message }}</p>
          <p v-if="warnSub(w)" class="warn-sub text-ellipsis">{{ warnSub(w) }}</p>
        </div>
        <div v-if="warnNum(w) !== ''" class="warn-side">
          <span class="warn-num">{{ warnNum(w) }}</span>
          <span class="warn-unit">{{ warnUnit(w) }}</span>
        </div>
      </div>
    </div>

    <div class="card entry-card">
      <div class="entry-grid">
        <div
          v-for="entry in userStore.entries"
          :key="entry.key"
          class="entry-item"
          @click="$router.push(entry.route)"
        >
          <span class="entry-icon"><van-icon :name="entry.icon" size="21" /></span>
          <span>{{ entry.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Icon as VanIcon,
  Badge as VanBadge,
  Progress as VanProgress,
  Skeleton as VanSkeleton,
} from 'vant'
import { useUserStore } from '@/stores/user'
import { useTodoStore } from '@/stores/todo'
import { reports, credits, graduate, verify, notifications } from '@/api'
import dayjs from 'dayjs'

// 首页学习预警只取临期班：进入结班前 3 天窗口（含已结班）的班里学分不足的学员
const ENDING_WINDOW_DAYS = 3

const userStore = useUserStore()
const todoStore = useTodoStore()
const router = useRouter()

const loading = ref(true)
const overview = ref<any>({})
const warnings = ref<any[]>([])
const unreadCount = ref(0)

const n = (v: unknown) => {
  const x = Number(v)
  return Number.isFinite(x) ? x : 0
}
const asArr = (v: unknown): any[] => (Array.isArray(v) ? v : [])
const percent = (v: unknown) => Math.min(100, Math.max(0, Math.round(n(v))))
const formatTime = (t: any) => (t ? dayjs(t).format('MM-DD HH:mm') : '')
const formatDate = (t: any) => (t ? dayjs(t).format('MM-DD') : '')

const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

// 带班动线用 caps 推导（教员/超管有 project:member + credit:apply，领导只审批不带班），不取角色名
const managesCourses = computed(
  () => userStore.caps.managesMembers && userStore.caps.canApplyCredit
)

function auditTab(tab: 'credit' | 'graduate' | 'verify') {
  if (userStore.caps.canApprove) return `/approvals?tab=${tab}`
  return tab === 'credit' ? '/credits' : '/graduate'
}

const abnormalRoute = computed(() =>
  userStore.caps.canMonitor ? '/monitor' : auditTab('verify')
)
const verifyRoute = computed(() => (userStore.caps.canViewVerify ? '/verify' : '/projects'))
const courseRoute = computed(() => (userStore.caps.canMonitor ? '/monitor' : '/projects'))

const pendingTotal = computed(
  () => n(overview.value?.pendingCreditCount) + n(overview.value?.pendingGraduateCount) + n(overview.value?.abnormalCount)
)
const passRateText = computed(() =>
  n(overview.value?.verifyTotal) > 0 ? String(Math.round(n(overview.value?.verifyPassRate))) : '—'
)

const focusKpi = computed(() =>
  managesCourses.value
    ? { label: '在校学员', value: n(overview.value?.studentCount), dot: 'ok', route: '/projects' }
    : { label: '进行中班', value: n(overview.value?.activeProjectCount), dot: 'ok', route: '/projects' }
)

const kpis = computed(() => [
  { label: '学分待审', value: n(overview.value?.pendingCreditCount), dot: 'warn', route: auditTab('credit') },
  { label: '毕业待审', value: n(overview.value?.pendingGraduateCount), dot: 'brand', route: auditTab('graduate') },
  { label: '学习异常', value: n(overview.value?.abnormalCount), dot: 'bad', route: abnormalRoute.value },
  focusKpi.value,
])

const courseRows = computed(() => asArr(overview.value?.myCourses))
const projectRows = computed(() => asArr(overview.value?.projectProgress))

const progressMode = computed<'course' | 'project' | ''>(() => {
  if (managesCourses.value && courseRows.value.length) return 'course'
  if (projectRows.value.length) return 'project'
  if (courseRows.value.length) return 'course'
  return ''
})

const progressTitle = computed(() => (progressMode.value === 'course' ? '我的课程进度' : '培训班进展'))
const progressMoreRoute = computed(() => (progressMode.value === 'course' ? courseRoute.value : '/projects'))

const progressRows = computed(() => {
  if (progressMode.value === 'course') {
    return courseRows.value.slice(0, 6).map((r: any) => ({
      id: r.id,
      title: r.courseName || '课程',
      sub: r.projectName || '',
      percent: percent(r.avgProgress),
      left: `${n(r.studentCount)} 名学员`,
      right: `${n(r.completedCount)} 人已完课`,
      route: courseRoute.value,
    }))
  }
  if (progressMode.value === 'project') {
    return projectRows.value.slice(0, 6).map((r: any) => ({
      id: r.id,
      title: r.projectName || '培训班',
      sub: r.status === 2 ? '已结束' : r.status === 1 ? '进行中' : '',
      percent: percent(r.rate),
      left: `${n(r.memberCount)} 名学员`,
      right: `${n(r.graduateCount)} 人已毕业`,
      route: '/projects',
    }))
  }
  return []
})

const MOBILE_ROUTES = ['/', '/projects', '/credits', '/approvals', '/monitor', '/notifications', '/profile', '/graduate', '/verify']

function todoRoute(row: any, type: string) {
  const link = String(row.link || '')
  // 后端 link 是 PC 后台路径，不在移动端路由表内时会落到通配重定向，故只复用命中的路径
  if (MOBILE_ROUTES.includes(link.split('?')[0])) return link
  return auditTab(type === 'GRADUATE' ? 'graduate' : 'credit')
}

const todoRows = computed(() => {
  const rows = [
    ...asArr(overview.value?.pendingCreditList).map((r: any) => todoShape(r, 'CREDIT')),
    ...asArr(overview.value?.pendingGraduateList).map((r: any) => todoShape(r, 'GRADUATE')),
  ]
  return rows
    .sort((a, b) => dayjs(a.time || 0).valueOf() - dayjs(b.time || 0).valueOf())
    .slice(0, 6)
})

function todoShape(row: any, type: string) {
  const isCredit = type === 'CREDIT'
  return {
    key: `${type}-${row.id}`,
    typeLabel: isCredit ? '学分' : '毕业',
    userName: row.userName || '学员',
    sub: [row.projectName, isCredit ? row.node : ''].filter(Boolean).join(' · '),
    valueText: isCredit && row.value != null ? `${row.value} 分` : isCredit ? '' : row.node || '',
    time: row.time,
    route: todoRoute(row, type),
  }
}

const warningRows = computed(() => warnings.value.slice(0, 5))

function warnKind(w: any) {
  const t = String(w.type || '').toUpperCase()
  if (t === 'EXPIRE') return 'expire'
  if (t === 'INACTIVE') return 'inactive'
  return 'credit'
}
function warnTitle(w: any) {
  return w.type === 'EXPIRE' ? w.projectName || '培训班' : w.userName || '学员'
}
function warnSub(w: any) {
  if (w.type === 'EXPIRE') {
    return [w.endTime ? `截止 ${formatDate(w.endTime)}` : '', `${n(w.memberCount)} 名学员`].filter(Boolean).join(' · ')
  }
  if (w.type === 'INACTIVE') {
    return [`最近登录 ${formatDate(w.lastLoginTime) || '无'}`, `最近学习 ${formatDate(w.lastStudyTime) || '无'}`].join(' · ')
  }
  return [w.orgName, w.projectName].filter(Boolean).join(' · ')
}
function warnNum(w: any) {
  if (w.type === 'CREDIT') return w.deficit == null ? '' : String(w.deficit)
  if (w.type === 'EXPIRE') return w.daysLeft == null ? '' : String(w.daysLeft)
  return ''
}
function warnUnit(w: any) {
  return w.type === 'EXPIRE' ? '天' : '分'
}

function go(path: string) {
  router.push(path)
}

async function fillFromListApis(base: any) {
  const out = { ...base }
  const jobs: Promise<void>[] = []
  if (out.pendingCreditCount == null && userStore.caps.canApproveCredit) {
    jobs.push(
      credits
        .pending()
        .then((list) => {
          const rows = asArr(list)
          out.pendingCreditCount = rows.length
          out.pendingCreditList = rows.slice(0, 5).map((r: any) => ({
            id: r.id,
            userName: r.userName,
            projectName: r.projectName,
            value: r.creditValue,
            node: '领导审核',
            time: r.createTime,
          }))
        })
        .catch(() => {})
    )
  }
  if (out.pendingGraduateCount == null && userStore.caps.canAuditGraduate) {
    jobs.push(
      graduate
        .applies({ pageNum: 1, pageSize: 5, auditStatus: 0 })
        .then((page: any) => {
          out.pendingGraduateCount = n(page?.total)
          out.pendingGraduateList = asArr(page?.records).map((a: any) => ({
            id: a.id,
            userName: a.userName,
            projectName: a.projectName,
            node: a.currentNodeLabel,
            time: a.applyTime,
          }))
        })
        .catch(() => {})
    )
  }
  if (out.abnormalCount == null && userStore.caps.canReviewVerify) {
    jobs.push(
      verify
        .abnormal({ pageNum: 1, pageSize: 1 })
        .then((page: any) => {
          out.abnormalCount = n(page?.total)
        })
        .catch(() => {})
    )
  }
  await Promise.all(jobs)
  return out
}

onMounted(async () => {
  const [ov, warnRows] = await Promise.all([
    reports.overview().catch(() => null),
    reports.warnings({ type: 'CREDIT', endWithinDays: ENDING_WINDOW_DAYS }).catch(() => []),
  ])
  warnings.value = asArr(warnRows)
  const merged = await fillFromListApis(ov || {})
  overview.value = merged
  const unread = await notifications.unreadCount().catch(() => merged?.unreadCount)
  unreadCount.value = n(unread ?? merged?.unreadCount)
  loading.value = false
  todoStore.refresh().catch(() => {})
})
</script>

<style scoped>
.home-hero {
  position: relative;
  overflow: hidden;
  padding: calc(env(safe-area-inset-top) + 24px) var(--page-x) var(--sp-5);
  color: var(--ink);
  background: linear-gradient(180deg, #edf3f9 0%, #f3f6fa 58%, var(--bg) 100%);
}
.hero-ornament {
  position: absolute;
  top: 4px;
  right: -6px;
  font-family: var(--font-serif);
  font-size: 96px;
  line-height: 1;
  color: rgba(79, 109, 143, 0.09);
  pointer-events: none;
  user-select: none;
}
.hero-top {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-3);
}
.hello {
  margin-bottom: 7px;
  font-size: 12px;
  color: var(--ink-3);
}
.greeting h2 {
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--ink);
}
.hero-role {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  min-width: 0;
  font-size: 11px;
  color: var(--ink-2);
}
.role-name {
  flex: none;
  max-width: 45%;
  padding: 2px 8px;
  border: 1px solid var(--brand-line);
  border-radius: 999px;
  background: var(--brand-tint);
  color: var(--brand);
}
.hero-org {
  padding-left: 7px;
  border-left: 1px solid var(--line-strong);
  min-width: 0;
}
.notify-icon {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--surface);
  box-shadow: var(--shadow-card);
  cursor: pointer;
  transition: background-color 0.18s ease;
}
.notify-icon:active {
  background: var(--surface-2);
}
.hero-strip {
  position: relative;
  display: flex;
  margin-top: var(--sp-5);
  padding: 14px var(--sp-4);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-control);
  box-shadow: var(--shadow-card);
}
.strip-cell {
  flex: 1;
  min-width: 0;
  min-height: 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  cursor: pointer;
  transition: opacity 0.15s;
}
.strip-cell + .strip-cell {
  border-left: 1px solid var(--line);
  padding-left: var(--sp-4);
}
.strip-cell:active {
  opacity: 0.6;
}
.strip-label {
  font-size: 11px;
  color: var(--ink-3);
}
.strip-value {
  font-family: var(--font-serif);
  font-size: 23px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.01em;
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}
.strip-value i {
  font-style: normal;
  font-size: 11px;
  font-weight: 400;
  margin-left: 3px;
  color: var(--ink-3);
}
.stat-cards {
  position: relative;
  z-index: 1;
  display: flex;
  margin: 0 var(--page-x) var(--sp-4);
  padding: 18px 0;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-card);
  box-shadow: var(--shadow-card);
}
.stat-item {
  flex: 1;
  text-align: center;
  cursor: pointer;
  transition: opacity 0.15s;
}
.stat-item:active {
  opacity: 0.6;
}
.stat-item + .stat-item {
  border-left: 1px solid var(--line);
}
.stat-value {
  font-size: 21px;
  font-weight: 700;
  color: var(--ink);
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}
.stat-label {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 5px;
  font-size: 12px;
  color: var(--ink-3);
}
.stat-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}
.dot-brand { background: var(--brand); }
.dot-ok { background: var(--ok); }
.dot-warn { background: var(--warn); }
.dot-bad { background: var(--bad); }
.more-link {
  display: flex;
  align-items: center;
  gap: 1px;
  font-size: 12px;
  color: var(--ink-3);
  font-weight: 400;
  cursor: pointer;
}
.more-link:active {
  color: var(--brand);
}
.prog-card {
  padding: var(--sp-4);
  cursor: pointer;
  transition: opacity 0.15s;
}
.prog-card:active {
  opacity: 0.7;
}
.prog-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--sp-2);
}
.prog-name {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}
.prog-percent {
  flex: none;
  font-size: 13px;
  font-weight: 600;
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}
.prog-sub {
  font-size: 11px;
  color: var(--ink-3);
  margin: 5px 0 9px;
}
.prog-card :deep(.van-progress) {
  margin-top: 2px;
}
.prog-meta {
  font-size: 11px;
  color: var(--ink-3);
  min-width: 0;
}
.list-card {
  padding: var(--sp-1) var(--sp-4);
}
.todo-item {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  min-height: 44px;
  padding: var(--sp-3) 0;
  cursor: pointer;
  transition: opacity 0.15s;
}
.todo-item + .todo-item {
  border-top: 1px solid var(--line);
}
.todo-item:active {
  opacity: 0.6;
}
.todo-chip {
  flex: none;
  font-size: 11px;
  line-height: 1;
  padding: 6px 8px;
  border-radius: var(--r-tag);
  background: var(--brand-tint);
  border: 1px solid var(--brand-line);
  color: var(--brand);
  font-weight: 500;
}
.todo-main {
  flex: 1;
  min-width: 0;
}
.todo-name {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--ink);
}
.todo-sub {
  font-size: 11px;
  color: var(--ink-3);
  margin-top: 4px;
}
.todo-side {
  flex: none;
  text-align: right;
}
.todo-value {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}
.todo-time {
  display: block;
  font-size: 11px;
  color: var(--ink-4);
  margin-top: 4px;
}
.warn-item {
  display: flex;
  align-items: flex-start;
  gap: var(--sp-3);
  padding: var(--sp-3) 0 var(--sp-3) var(--sp-3);
  border-left: 2px solid var(--line-strong);
}
.warn-item + .warn-item {
  border-top: 1px solid var(--line);
}
.warn-credit { border-left-color: var(--warn); }
.warn-expire { border-left-color: var(--bad); }
.warn-inactive { border-left-color: var(--brand); }
.warn-body {
  flex: 1;
  min-width: 0;
}
.warn-head {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  min-width: 0;
}
.warn-label {
  flex: none;
  font-size: 11px;
  line-height: 1;
  padding: 4px 6px;
  border-radius: var(--r-tag);
  background: var(--surface-2);
  border: 1px solid var(--line);
  color: var(--ink-2);
}
.warn-credit .warn-label {
  background: var(--warn-tint);
  color: var(--warn);
}
.warn-expire .warn-label {
  background: var(--bad-tint);
  color: var(--bad);
}
.warn-inactive .warn-label {
  background: var(--brand-tint);
  color: var(--brand);
}
.warn-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  min-width: 0;
}
.warn-msg {
  font-size: 12px;
  color: var(--ink-2);
  margin-top: 6px;
  line-height: 1.5;
}
.warn-sub {
  font-size: 11px;
  color: var(--ink-3);
  margin-top: 4px;
}
.warn-side {
  flex: none;
  text-align: right;
  padding-top: 2px;
}
.warn-num {
  font-size: 18px;
  font-weight: 700;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
.warn-unit {
  font-size: 11px;
  color: var(--ink-3);
  margin-left: 2px;
}
.entry-card {
  padding: var(--sp-4) var(--sp-2) var(--sp-3);
}
.entry-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--sp-5) var(--sp-2);
}
.entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--sp-2);
  font-size: 12px;
  color: var(--ink-2);
  cursor: pointer;
  transition: transform 0.12s;
}
.entry-item:active {
  transform: scale(0.93);
}
.entry-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--r-control);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--brand);
  background: var(--brand-tint);
}
.empty-card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-card);
  margin: 0 var(--page-x) var(--sp-3);
  padding: 34px var(--sp-4);
  text-align: center;
  color: var(--ink-3);
  font-size: 13px;
}
.empty-card p {
  margin: var(--sp-2) 0 var(--sp-3);
}
.empty-card.compact {
  padding: var(--sp-5) var(--sp-4);
}
.skeleton-card {
  padding: var(--sp-4);
}
</style>
