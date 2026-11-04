<template>
  <HomeOps v-if="userStore.caps.isOps" />
  <div v-else class="page-container home-page">
    <!-- 身份带 -->
    <header class="home-hero">
      <div class="hero-top">
        <div class="greeting">
          <p class="hello">{{ greetingText }}</p>
          <h2>{{ userStore.user?.realName || '同学' }}</h2>
          <p class="org-name" v-if="userStore.user?.orgName">
            <van-icon name="cluster-o" size="13" />
            <span>{{ userStore.user?.orgName }}</span>
          </p>
        </div>
        <div class="notify-icon" @click="$router.push('/notifications')">
          <van-badge :content="unreadCount > 0 ? unreadCount : ''" :max="99">
            <van-icon name="bell" size="22" color="#fff" />
          </van-badge>
        </div>
      </div>
      <div class="hero-credit" @click="$router.push('/credits')">
        <div class="hero-credit-main">
          <span class="hero-credit-label">本年度已获学分</span>
          <span class="hero-credit-value">
            {{ stats.totalCredits || 0 }}<i>分</i>
          </span>
        </div>
        <span class="hero-credit-link">
          学分明细
          <van-icon name="arrow" size="12" />
        </span>
      </div>
    </header>

    <!-- 关键指标 -->
    <div class="stat-cards">
      <div class="stat-item" @click="$router.push('/projects')">
        <div class="stat-value">{{ stats.learningCourses || 0 }}</div>
        <div class="stat-label">
          <span class="stat-dot dot-brand"></span>
          <span>学习中</span>
        </div>
      </div>
      <div class="stat-item" @click="$router.push('/projects')">
        <div class="stat-value">{{ stats.completedCourses || 0 }}</div>
        <div class="stat-label">
          <span class="stat-dot dot-ok"></span>
          <span>已完成</span>
        </div>
      </div>
      <div class="stat-item" @click="$router.push('/credits')">
        <div class="stat-value">{{ stats.totalCredits || 0 }}</div>
        <div class="stat-label">
          <span class="stat-dot dot-warn"></span>
          <span>总学分</span>
        </div>
      </div>
      <div class="stat-item" @click="$router.push('/credits')">
        <div class="stat-value">{{ stats.pendingCredits || 0 }}</div>
        <div class="stat-label">
          <span class="stat-dot dot-bad"></span>
          <span>待审核</span>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
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

    <!-- 继续学习 -->
    <div class="section-title flex-between">
      <span>继续学习</span>
      <span class="more-link" @click="$router.push('/projects')">全部 <van-icon name="arrow" size="11" /></span>
    </div>
    <div v-if="loading" class="card skeleton-card">
      <van-skeleton title :row="2" />
    </div>
    <div v-else-if="continueList.length === 0" class="empty-card">
      <van-icon name="video-o" size="34" color="var(--ink-4)" />
      <p>暂无进行中的课程</p>
      <span class="empty-action" @click="$router.push('/projects')">去培训班看看</span>
    </div>
    <div
      v-for="item in continueList"
      :key="item.id"
      class="card course-card"
      @click="goStudy(item)"
    >
      <div class="course-cover">
        <img v-if="item.cover" :src="item.cover" alt="" @error="onCoverError" />
        <van-icon v-else name="video-o" size="22" color="rgba(255,255,255,0.85)" />
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
            track-color="var(--line)"
          />
          <span class="course-percent">{{ Math.min(100, Math.round(item.progress || 0)) }}%</span>
        </div>
      </div>
      <van-icon name="arrow" size="14" color="var(--ink-4)" class="course-arrow" />
    </div>

    <!-- 最新通知 -->
    <div class="section-title flex-between">
      <span>最新通知</span>
      <span class="more-link" @click="$router.push('/notifications')">更多 <van-icon name="arrow" size="11" /></span>
    </div>
    <div v-if="recentNotifications.length === 0" class="empty-card compact">
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
    // Load project details to collect course progress
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
    stats.value = {
      learningCourses: courses.length,
      completedCourses: details.reduce((sum, d) => sum + ((d?.courses || []).filter((c: any) => c.studyStatus === 2).length), 0),
      totalCredits: creditSummary?.issued || 0,
      pendingCredits: creditSummary?.pending || 0,
    }
    continueList.value = courses.slice(0, 5)
    recentNotifications.value = notifyList.records || []
    unreadCount.value = notifyCount as number
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home-hero {
  background: var(--brand);
  color: #fff;
  padding: calc(env(safe-area-inset-top) + 28px) var(--page-x) 30px;
}
.hero-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-3);
}
.hello {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.72);
  margin-bottom: 5px;
}
.greeting h2 {
  font-size: 25px;
  font-weight: 700;
  letter-spacing: 0.3px;
}
.org-name {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 9px;
}
.notify-icon {
  flex: none;
  width: 38px;
  height: 38px;
  border-radius: var(--r-control);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  cursor: pointer;
}
.notify-icon:active {
  background: rgba(255, 255, 255, 0.24);
}
.hero-credit {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-3);
  margin-top: var(--sp-5);
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.16);
  cursor: pointer;
}
.hero-credit-main {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.hero-credit-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}
.hero-credit-value {
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.01em;
  font-variant-numeric: tabular-nums;
}
.hero-credit-value i {
  font-style: normal;
  font-size: 12px;
  font-weight: 400;
  margin-left: 3px;
  color: rgba(255, 255, 255, 0.72);
}
.hero-credit-link {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}
.stat-cards {
  position: relative;
  z-index: 1;
  display: flex;
  margin: -20px var(--page-x) var(--sp-4);
  padding: 14px 0;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-card);
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
.course-card {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  padding: var(--sp-3);
  cursor: pointer;
  transition: opacity 0.15s;
}
.course-card:active {
  opacity: 0.7;
}
.course-cover {
  flex: none;
  width: 92px;
  height: 60px;
  border-radius: var(--r-chip);
  overflow: hidden;
  background: var(--brand);
  display: flex;
  align-items: center;
  justify-content: center;
}
.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.course-project {
  font-size: 11px;
  color: var(--ink-3);
  margin: 5px 0 9px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
.course-arrow {
  flex: none;
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
.empty-action {
  color: var(--brand);
  font-size: 13px;
  cursor: pointer;
}
.skeleton-card {
  padding: var(--sp-4);
}
.notify-card {
  cursor: pointer;
  transition: opacity 0.15s;
}
.notify-card:active {
  opacity: 0.7;
}
.notify-card.unread {
  border-left: 3px solid var(--brand);
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
  font-size: 14px;
  font-weight: 500;
  color: var(--ink);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.unread-dot {
  flex: none;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--bad);
}
.notify-time {
  flex: none;
  font-size: 11px;
  color: var(--ink-4);
}
.notify-content {
  font-size: 12px;
  color: var(--ink-2);
  margin-top: 6px;
  line-height: 1.55;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
