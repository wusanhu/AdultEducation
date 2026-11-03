<template>
  <div class="page-container home-page">
    <!-- Hero -->
    <div class="home-header ink-hero">
      <span class="hero-watermark">笃学</span>
      <div class="greeting">
        <p class="hello">{{ greetingText }}</p>
        <h2 class="hero-name">{{ userStore.user?.realName || '同学' }}</h2>
        <p class="org-name" v-if="userStore.user?.orgName">
          <van-icon name="cluster-o" size="12" />
          <span>{{ userStore.user?.orgName }}</span>
        </p>
      </div>
      <div class="notify-icon" @click="$router.push('/notifications')">
        <van-badge :content="unreadCount > 0 ? unreadCount : ''" :max="99">
          <van-icon name="bell" size="20" color="rgba(245,242,234,0.9)" />
        </van-badge>
      </div>
    </div>

    <!-- 学分票券 -->
    <div class="credit-ticket" @click="$router.push('/credits')">
      <div class="ticket-left">
        <span class="ticket-label">本年度已获学分</span>
        <div class="ticket-value-row">
          <span class="ticket-value num">{{ stats.totalCredits || 0 }}</span>
          <span class="ticket-unit">学分</span>
        </div>
      </div>
      <div class="ticket-badge">学分</div>
      <span class="ticket-link">
        明细
        <van-icon name="arrow" size="11" />
      </span>
    </div>

    <!-- Stat Strip -->
    <div class="stat-cards">
      <div class="stat-item" @click="$router.push('/projects')">
        <div class="stat-value num">{{ stats.learningCourses || 0 }}</div>
        <div class="stat-label">学习中</div>
      </div>
      <div class="stat-item" @click="$router.push('/projects')">
        <div class="stat-value num">{{ stats.completedCourses || 0 }}</div>
        <div class="stat-label">已完成</div>
      </div>
      <div class="stat-item" @click="$router.push('/credits')">
        <div class="stat-value num stat-value--seal">{{ stats.pendingCredits || 0 }}</div>
        <div class="stat-label">待审核</div>
      </div>
    </div>

    <!-- Entry Grid -->
    <div class="card entry-card">
      <div class="entry-grid">
        <div class="entry-item" @click="$router.push('/projects')">
          <span class="entry-icon"><van-icon name="bookmark-o" size="21" /></span>
          <span>培训班</span>
        </div>
        <div class="entry-item" @click="$router.push('/credits')">
          <span class="entry-icon"><van-icon name="medal-o" size="21" /></span>
          <span>学分查询</span>
        </div>
        <div class="entry-item" @click="$router.push('/graduate')">
          <span class="entry-icon"><van-icon name="certificate" size="21" /></span>
          <span>毕业申请</span>
        </div>
        <div class="entry-item" @click="$router.push('/verify')">
          <span class="entry-icon"><van-icon name="shield-o" size="21" /></span>
          <span>核验记录</span>
        </div>
        <div class="entry-item" @click="$router.push('/notifications')">
          <span class="entry-icon"><van-icon name="bell" size="21" /></span>
          <span>消息通知</span>
        </div>
        <div v-if="userStore.isTeacher" class="entry-item" @click="$router.push('/teacher')">
          <span class="entry-icon"><van-icon name="manager-o" size="21" /></span>
          <span>教务管理</span>
        </div>
      </div>
    </div>

    <!-- Continue Learning -->
    <div class="section-title">
      继续学习
      <span class="section-title-en">CONTINUE</span>
      <span class="more-link" @click="$router.push('/projects')">全部 ›</span>
    </div>
    <div v-if="loading" class="card skeleton-card">
      <van-skeleton title :row="2" />
    </div>
    <div v-else-if="continueList.length === 0" class="empty-ink">
      <div class="empty-ink-glyph">学</div>
      <p>暂无进行中的课程</p>
      <span class="empty-action" @click="$router.push('/projects')">去培训班看看 ›</span>
    </div>
    <div
      v-for="item in continueList"
      :key="item.id"
      class="card course-card"
      @click="goStudy(item)"
    >
      <div class="course-cover">
        <img v-if="item.cover" :src="item.cover" alt="" @error="onCoverError" />
        <span v-else class="cover-glyph num">{{ indexNum(item) }}</span>
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
            stroke-width="4"
          />
          <span class="course-percent num">{{ Math.min(100, Math.round(item.progress || 0)) }}%</span>
        </div>
      </div>
    </div>

    <!-- Recent Notifications -->
    <div class="section-title">
      最新通知
      <span class="section-title-en">NOTICES</span>
      <span class="more-link" @click="$router.push('/notifications')">更多 ›</span>
    </div>
    <div v-if="recentNotifications.length === 0" class="empty-ink compact">
      <div class="empty-ink-glyph">函</div>
      <p>暂无通知</p>
    </div>
    <div
      v-for="n in recentNotifications"
      :key="n.id"
      class="card notify-card"
      :class="{ unread: !n.isRead }"
      @click="$router.push('/notifications')"
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

function indexNum(item: any, idx = 0) {
  const id = item.id ?? item.courseId
  return id != null ? String(id).padStart(2, '0') : String(idx).padStart(2, '0')
}

function formatTime(t: string) {
  return dayjs(t).format('MM-DD HH:mm')
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
.home-header {
  padding: 36px 20px 46px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-radius: 0 0 20px 20px;
}
.greeting {
  position: relative;
  z-index: 1;
}
.hello {
  font-size: 12px;
  letter-spacing: 0.25em;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 6px;
}
.hero-name {
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: #fff;
}
.org-name {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.68);
  margin-top: 8px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 99px;
  padding: 3px 10px;
}
.notify-icon {
  position: relative;
  z-index: 1;
  padding: 6px;
  cursor: pointer;
}

/* 学分票券 */
.credit-ticket {
  position: relative;
  z-index: 2;
  margin: -34px 16px 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border: 1px solid var(--line-strong);
  border-radius: var(--radius-md);
  box-shadow: 0 6px 18px rgba(15, 40, 90, 0.1);
  cursor: pointer;
}
.ticket-left {
  flex: 1;
  min-width: 0;
}
.ticket-label {
  font-size: 11px;
  color: var(--text-sub);
  letter-spacing: 0.1em;
}
.ticket-value-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 4px;
}
.ticket-value {
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
  color: var(--primary);
}
.ticket-unit {
  font-size: 12px;
  color: var(--text-sub);
}
.ticket-badge {
  flex: none;
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: auto;
}
.ticket-link {
  flex: none;
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: var(--text-faint);
}

/* 统计条 */
.stat-cards {
  display: flex;
  background: #fff;
  margin: 12px 16px 0;
  border-radius: var(--radius-md);
  padding: 14px 4px;
  border: 1px solid var(--line);
}
.stat-item {
  flex: 1;
  text-align: center;
  position: relative;
  cursor: pointer;
  transition: opacity 0.15s;
}
.stat-item:active {
  opacity: 0.6;
}
.stat-item + .stat-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 20%;
  height: 60%;
  width: 1px;
  background: var(--line);
}
.stat-value {
  font-size: 21px;
  font-weight: 700;
  color: var(--text-strong);
  line-height: 1.2;
}
.stat-value--seal {
  color: var(--warning);
}
.stat-label {
  font-size: 11px;
  color: var(--text-sub);
  margin-top: 3px;
  letter-spacing: 0.06em;
}

/* 入口 */
.entry-card {
  padding: 18px 8px 12px;
}
.entry-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px 8px;
}
.entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-main);
  cursor: pointer;
  transition: transform 0.12s;
}
.entry-item:active {
  transform: scale(0.93);
}
.entry-icon {
  width: 46px;
  height: 46px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
  background: var(--primary-soft);
}
.more-link {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-sub);
  font-weight: 400;
  cursor: pointer;
}

/* 课程卡 */
.course-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  cursor: pointer;
  transition: opacity 0.15s;
}
.course-card:active {
  opacity: 0.7;
}
.course-cover {
  flex: none;
  width: 88px;
  height: 60px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: linear-gradient(150deg, #1f6feb, #0e7490);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-glyph {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 0.1em;
}
.course-body {
  flex: 1;
  min-width: 0;
}
.course-top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.course-name {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-strong);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.course-project {
  font-size: 11px;
  color: var(--text-faint);
  margin: 5px 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.course-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}
.course-progress :deep(.van-progress) {
  flex: 1;
}
.course-percent {
  flex: none;
  font-size: 11px;
  color: var(--text-sub);
}
.empty-action {
  display: inline-block;
  margin-top: 10px;
  color: var(--primary);
  font-size: 12px;
  cursor: pointer;
}
.empty-ink.compact {
  padding: 26px 16px;
}
.skeleton-card {
  padding: 16px;
}

/* 通知卡 */
.notify-card {
  cursor: pointer;
  transition: opacity 0.15s;
}
.notify-card:active {
  opacity: 0.7;
}
.notify-card.unread {
  border-left: 3px solid var(--primary);
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
  font-weight: 600;
  color: var(--text-strong);
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
  background: var(--danger);
}
.notify-time {
  flex: none;
  font-size: 11px;
  color: var(--text-faint);
}
.notify-content {
  font-size: 12px;
  color: var(--text-sub);
  margin-top: 6px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
