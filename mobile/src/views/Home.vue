<template>
  <HomeOps v-if="userStore.caps.isOps" />
  <div v-else class="page-container home-page">
    <!-- 身份区 -->
    <header class="home-hero">
      <span class="hero-ornament" aria-hidden="true">学</span>
      <div class="hero-top">
        <div class="greeting">
          <p class="hello">{{ greetingText }}</p>
          <h2>{{ userStore.user?.realName || '同学' }}</h2>
          <p class="org-name" v-if="userStore.user?.orgName">
            <van-icon name="cluster-o" size="12" />
            <span class="text-ellipsis">{{ userStore.user?.orgName }}</span>
          </p>
        </div>
        <button class="notify-icon" type="button" aria-label="消息通知" @click="$router.push('/notifications')">
          <van-badge :content="unreadCount > 0 ? unreadCount : ''" :max="99">
            <van-icon name="bell" size="21" color="var(--ink-2)" />
          </van-badge>
        </button>
      </div>
    </header>

    <!-- 首屏焦点：学分概览 -->
    <section class="credit-panel">
      <div class="panel-head">
        <span class="kicker">CREDIT SUMMARY</span>
        <span class="more-link" @click="$router.push('/credits')">
          学分明细 <van-icon name="arrow" size="11" />
        </span>
      </div>
      <div class="panel-main">
        <span class="panel-value">{{ stats.totalCredits || 0 }}<i>分</i></span>
        <span class="panel-label">已发放学分</span>
      </div>
      <div class="panel-stats">
        <div class="panel-stat" @click="$router.push('/projects')">
          <b>{{ stats.learningCourses || 0 }}<i>门</i></b>
          <span>学习中</span>
        </div>
        <div class="panel-stat" @click="$router.push('/projects')">
          <b>{{ stats.completedCourses || 0 }}<i>门</i></b>
          <span>已完成</span>
        </div>
        <div class="panel-stat" @click="$router.push('/credits')">
          <b>{{ stats.pendingCredits || 0 }}<i>分</i></b>
          <span>待发放</span>
        </div>
      </div>
    </section>

    <!-- 快捷入口 -->
    <div class="card entry-card">
      <div class="entry-head">
        <span class="kicker">学习服务</span>
        <span class="entry-hint">常用功能</span>
      </div>
      <div class="entry-grid">
        <div
          v-for="entry in userStore.entries"
          :key="entry.key"
          class="entry-item"
          @click="$router.push(entry.route)"
        >
          <span class="entry-icon" :class="`entry-icon--${entryTone(entry.key)}`">
            <van-icon :name="entry.icon" size="20" />
          </span>
          <span class="entry-label">{{ entry.label }}</span>
        </div>
      </div>
    </div>

    <!-- 继续学习 -->
    <div class="section-title flex-between">
      <span>继续学习</span>
      <span class="more-link" @click="$router.push('/projects')">全部 <van-icon name="arrow" size="11" /></span>
    </div>
    <div v-if="loading" class="card skeleton-card">
      <van-skeleton title :row="2" />
    </div>
    <div v-else-if="continueList.length === 0" class="empty-wrap">
      <EmptyState icon="video-o" description="暂无进行中的课程">
        <span class="empty-link" @click="$router.push('/projects')">去培训班看看</span>
      </EmptyState>
    </div>
    <div
      v-for="item in continueList"
      :key="item.id"
      class="card course-card"
      @click="goStudy(item)"
    >
      <div class="course-cover">
        <img v-if="item.cover" :src="item.cover" alt="" @error="onCoverError" />
        <van-icon v-else name="video-o" size="22" color="var(--brand)" />
        <span class="cover-play"><van-icon name="play" size="10" /></span>
      </div>
      <div class="course-body">
        <div class="course-top">
          <span class="course-name">{{ item.courseName }}</span>
          <StatusTag :status="item.studyStatus" type="study" />
        </div>
        <div class="course-project">{{ item.projectName }}</div>
        <div class="course-progress">
          <van-progress
            :percentage="Math.min(100, Math.round(item.progress || 0))"
            :show-pivot="false"
            color="var(--brand)"
            stroke-width="4"
            track-color="var(--surface-3)"
          />
          <span class="course-percent">{{ Math.min(100, Math.round(item.progress || 0)) }}%</span>
        </div>
      </div>
    </div>

    <!-- 最新通知 -->
    <div class="section-title flex-between">
      <span>最新通知</span>
      <span class="more-link" @click="$router.push('/notifications')">更多 <van-icon name="arrow" size="11" /></span>
    </div>
    <div v-if="recentNotifications.length === 0" class="card empty-card compact">
      <p>暂无通知</p>
    </div>
    <div
      v-for="n in recentNotifications"
      :key="n.id"
      class="card notify-card"
      :class="{ unread: !n.isRead }"
      @click="openNotice(n)"
    >
      <div class="notify-head">
        <span class="notify-title">
          <i v-if="!n.isRead" class="unread-dot"></i>
          {{ n.title }}
        </span>
        <span class="notify-time">{{ formatTime(n.createTime) }}</span>
      </div>
      <p class="notify-content">{{ n.content }}</p>
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
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import HomeOps from '@/views/HomeOps.vue'
import { useUserStore } from '@/stores/user'
import { projects, credits, notifications } from '@/api'
import dayjs from 'dayjs'

const userStore = useUserStore()
const router = useRouter()
const stats = ref<any>({})
const continueList = ref<any[]>([])
const recentNotifications = ref<any[]>([])
const unreadCount = ref(0)
const loading = ref(true)

const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

/** 学分/证书类入口用深蓝强调，其余用主色，避免宫格出现彩虹色 */
function entryTone(key: string) {
  if (['credits', 'graduate', 'creditApply'].includes(key)) return 'accent'
  if (['monitor', 'exam', 'creditAudit', 'graduateAudit', 'verifyReview'].includes(key)) return 'ink'
  return 'brand'
}

function formatTime(t: string) {
  return dayjs(t).format('MM-DD HH:mm')
}

function openNotice(item: any) {
  router.push(item.link || '/notifications')
}

function goStudy(item: any) {
  const id = item.id ?? item.courseId
  if (id) router.push(`/study/${id}`)
}

function onCoverError(e: Event) {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
}

onMounted(async () => {
  try {
    const [myProjects, creditSummary, notifyList, notifyCount] = await Promise.all([
      projects.my().catch(() => []),
      credits.summary().catch(() => ({})),
      notifications.list({ pageNum: 1, pageSize: 3 }).catch(() => ({ records: [] })),
      notifications.unreadCount().catch(() => 0),
    ])
    // 学习中/已完成直接累加列表下发的聚合字段（与培训班页、后端同口径），
    // 课程详情仅用于拼「继续学习」卡片
    const details = await Promise.all(
      (myProjects as any[]).map((p) => projects.myDetail(p.id).catch(() => null))
    )
    const courses: any[] = []
    for (const d of details) {
      if (!d) continue
      const project = d.project || d
      for (const c of (d.courses || [])) {
        if (c.studyStatus === 1) {
          courses.push({ ...c, projectName: project.projectName })
        }
      }
    }
    const myList = myProjects as any[]
    // 新版后端列表直接下发聚合字段；缺失时回退用详情统计
    const hasAgg = myList.length > 0 && myList[0].learningCourse != null
    stats.value = {
      learningCourses: hasAgg
        ? myList.reduce((s, p) => s + (p.learningCourse || 0), 0)
        : courses.length,
      completedCourses: hasAgg
        ? myList.reduce((s, p) => s + (p.completedCourse || 0), 0)
        : details.reduce((sum: number, d: any) => sum + ((d?.courses || []).filter((c: any) => c.studyStatus === 2).length), 0),
      totalCredits: creditSummary?.issued || 0,
      pendingCredits: creditSummary?.pending || 0,
    }
    continueList.value = courses.slice(0, 4)
    recentNotifications.value = (notifyList.records || []).slice(0, 2)
    unreadCount.value = notifyCount as number
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
/* ---------- 身份区 ---------- */
.home-hero {
  position: relative;
  overflow: hidden;
  padding: calc(env(safe-area-inset-top) + 24px) var(--page-x) 54px;
  color: var(--ink);
  background: linear-gradient(180deg, #e8eefc 0%, #f1f5fe 58%, var(--bg) 100%);
}

.hero-ornament {
  position: absolute;
  top: 6px;
  right: -6px;
  font-family: var(--font-serif);
  font-size: 96px;
  line-height: 1;
  color: rgba(29, 78, 216, 0.09);
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
  letter-spacing: 0.04em;
  color: var(--ink-3);
}

.greeting h2 {
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0.02em;
  color: var(--ink);
}

.org-name {
  display: flex;
  align-items: center;
  gap: 5px;
  max-width: 236px;
  margin-top: 10px;
  padding: 3px 9px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  font-size: 11px;
  color: var(--ink-2);
}

.notify-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 42px;
  height: 42px;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--surface);
  box-shadow: var(--shadow-card);
  -webkit-appearance: none;
  appearance: none;
  cursor: pointer;
  transition: background-color 0.18s ease;
}

.notify-icon:active {
  background: var(--surface-2);
}

/* ---------- 首屏焦点卡 ---------- */
.credit-panel {
  position: relative;
  z-index: 1;
  margin: -36px var(--page-x) var(--sp-4);
  padding: var(--sp-5) var(--sp-5) var(--sp-3);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-card);
  box-shadow: var(--shadow-float);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-main {
  display: flex;
  align-items: baseline;
  gap: var(--sp-3);
  margin-top: 10px;
}

.panel-value {
  font-family: var(--font-serif);
  font-size: 42px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.01em;
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}

.panel-value i {
  margin-left: 4px;
  font-size: 12px;
  font-weight: 400;
  font-style: normal;
  color: var(--ink-3);
}

.panel-label {
  font-size: 12px;
  color: var(--ink-3);
}

.panel-stats {
  display: flex;
  margin-top: var(--sp-5);
  padding-top: var(--sp-4);
  border-top: 1px solid var(--line);
}

.panel-stat {
  flex: 1;
  min-width: 0;
  text-align: center;
  cursor: pointer;
  transition: opacity 0.15s;
}

.panel-stat:active {
  opacity: 0.6;
}

.panel-stat + .panel-stat {
  border-left: 1px solid var(--line);
}

.panel-stat b {
  display: block;
  font-family: var(--font-serif);
  font-size: 21px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.panel-stat b i {
  font-style: normal;
  font-size: 11px;
  margin-left: 2px;
  color: var(--ink-3);
  font-family: var(--font-sans, inherit);
}

.panel-stat span {
  display: block;
  margin-top: 3px;
  font-size: 11px;
  color: var(--ink-3);
}

/* ---------- 快捷入口 ---------- */
.entry-card {
  padding: var(--sp-5) var(--sp-3) var(--sp-5);
}

.entry-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 0 var(--sp-1) var(--sp-5);
}

.entry-hint {
  font-size: 11px;
  color: var(--ink-3);
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px 4px;
}

.entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  min-width: 0;
  padding: 2px 0;
  cursor: pointer;
  transition: transform 0.12s ease;
}

.entry-item:active {
  transform: scale(0.94);
}

.entry-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: var(--r-control);
  color: var(--brand);
  background: var(--brand-tint);
  border: 1px solid var(--brand-line);
}

.entry-icon--accent {
  color: var(--accent-strong);
  background: var(--accent-tint);
  border-color: var(--accent-line);
}

.entry-icon--ink {
  color: var(--ink-2);
  background: var(--surface-2);
  border-color: var(--line);
}

.entry-label {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  line-height: 1.3;
  color: var(--ink-2);
}

/* ---------- 课程卡 ---------- */
.course-card {
  display: flex;
  align-items: center;
  gap: var(--sp-4);
  padding: 14px;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease;
}

.course-card:active {
  background: var(--surface-2);
}

.course-cover {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 104px;
  height: 68px;
  overflow: hidden;
  border-radius: var(--r-chip);
  background: linear-gradient(135deg, var(--brand-tint) 0%, #d5e2fa 100%);
  border: 1px solid var(--brand-line);
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-play {
  position: absolute;
  right: 6px;
  bottom: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  color: var(--brand);
}

.course-body {
  flex: 1;
  min-width: 0;
}

.course-top {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}

.course-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.course-project {
  margin: 5px 0 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  color: var(--ink-3);
}

.course-progress {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}

.course-progress :deep(.van-progress) {
  flex: 1;
}

.course-percent {
  flex: none;
  font-size: 11px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

/* ---------- 空状态 ---------- */
.empty-wrap {
  margin: 0 var(--page-x) var(--sp-3);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-card);
  box-shadow: var(--shadow-card);
}

.empty-link {
  cursor: pointer;
  font-weight: 500;
  color: var(--brand);
}

.empty-card {
  padding: 28px var(--sp-4);
  text-align: center;
  color: var(--ink-3);
  font-size: 13px;
}

.empty-card.compact {
  padding: var(--sp-5) var(--sp-4);
}

.skeleton-card {
  padding: var(--sp-4);
}

/* ---------- 通知 ---------- */
.notify-card {
  cursor: pointer;
  transition: transform 0.15s ease;
}

.notify-card:active {
  transform: scale(0.985);
}

.notify-card.unread {
  padding-left: 15px;
}

.notify-card.unread::before {
  content: '';
  position: absolute;
  top: 16px;
  bottom: 16px;
  left: 0;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--accent);
}

.notify-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.notify-title {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.unread-dot {
  flex: none;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
}

.notify-time {
  flex: none;
  font-size: 11px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

.notify-content {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.55;
  color: var(--ink-2);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
