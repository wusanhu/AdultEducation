<template>
  <div class="page-container home-page">
    <!-- Hero -->
    <div class="home-header">
      <div class="hero-orb orb-a"></div>
      <div class="hero-orb orb-b"></div>
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
      <div class="hero-credit" @click="$router.push('/credits')">
        <div class="hero-credit-main">
          <span class="hero-credit-label">本年度已获学分</span>
          <span class="hero-credit-value">{{ stats.totalCredits || 0 }}</span>
        </div>
        <span class="hero-credit-link">
          学分明细
          <van-icon name="arrow" size="12" />
        </span>
      </div>
    </div>

    <!-- Stat Cards -->
    <div class="stat-cards">
      <div class="stat-item" @click="$router.push('/projects')">
        <span class="stat-dot dot-blue"></span>
        <div class="stat-value">{{ stats.learningCourses || 0 }}</div>
        <div class="stat-label">学习中</div>
      </div>
      <div class="stat-item" @click="$router.push('/projects')">
        <span class="stat-dot dot-green"></span>
        <div class="stat-value">{{ stats.completedCourses || 0 }}</div>
        <div class="stat-label">已完成</div>
      </div>
      <div class="stat-item" @click="$router.push('/credits')">
        <span class="stat-dot dot-orange"></span>
        <div class="stat-value">{{ stats.totalCredits || 0 }}</div>
        <div class="stat-label">总学分</div>
      </div>
      <div class="stat-item" @click="$router.push('/credits')">
        <span class="stat-dot dot-red"></span>
        <div class="stat-value">{{ stats.pendingCredits || 0 }}</div>
        <div class="stat-label">待审核</div>
      </div>
    </div>

    <!-- Entry Grid -->
    <div class="card entry-card">
      <div class="entry-grid">
        <div class="entry-item" @click="$router.push('/projects')">
          <span class="entry-icon ic-blue"><van-icon name="orders-o" size="22" /></span>
          <span>培训班</span>
        </div>
        <div class="entry-item" @click="$router.push('/credits')">
          <span class="entry-icon ic-green"><van-icon name="gold-coin-o" size="22" /></span>
          <span>学分查询</span>
        </div>
        <div class="entry-item" @click="$router.push('/graduate')">
          <span class="entry-icon ic-orange"><van-icon name="certificate" size="22" /></span>
          <span>毕业申请</span>
        </div>
        <div class="entry-item" @click="$router.push('/verify')">
          <span class="entry-icon ic-red"><van-icon name="shield-o" size="22" /></span>
          <span>核验记录</span>
        </div>
        <div class="entry-item" @click="$router.push('/notifications')">
          <span class="entry-icon ic-purple"><van-icon name="bell" size="22" /></span>
          <span>消息通知</span>
        </div>
        <div v-if="userStore.isTeacher" class="entry-item" @click="$router.push('/teacher')">
          <span class="entry-icon ic-teal"><van-icon name="manager-o" size="22" /></span>
          <span>教师管理</span>
        </div>
      </div>
    </div>

    <!-- Continue Learning -->
    <div class="section-title flex-between">
      <span>继续学习</span>
      <span class="more-link" @click="$router.push('/projects')">全部 &gt;</span>
    </div>
    <div v-if="loading" class="card skeleton-card">
      <van-skeleton title :row="2" />
    </div>
    <div v-else-if="continueList.length === 0" class="empty-card">
      <van-icon name="video-o" size="34" color="#c8c9cc" />
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
        <van-icon v-else name="video-o" size="24" color="#fff" />
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
            color="#1989fa"
            stroke-width="5"
            track-color="#eef1f6"
          />
          <span class="course-percent">{{ Math.min(100, Math.round(item.progress || 0)) }}%</span>
        </div>
      </div>
      <van-icon name="arrow" size="14" color="#c8c9cc" class="course-arrow" />
    </div>

    <!-- Recent Notifications -->
    <div class="section-title flex-between">
      <span>最新通知</span>
      <span class="more-link" @click="$router.push('/notifications')">更多 &gt;</span>
    </div>
    <div v-if="recentNotifications.length === 0" class="empty-card compact">
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
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #2f7cf6 0%, #4f9cff 58%, #33c3a4 100%);
  padding: 34px 20px 86px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  color: #fff;
  border-radius: 0 0 22px 22px;
}
.hero-orb {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.14);
  pointer-events: none;
}
.orb-a {
  width: 150px;
  height: 150px;
  top: -50px;
  right: -30px;
}
.orb-b {
  width: 90px;
  height: 90px;
  bottom: 18px;
  right: 88px;
  background: rgba(255, 255, 255, 0.1);
}
.greeting {
  position: relative;
  z-index: 1;
}
.hello {
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 2px;
}
.greeting h2 {
  font-size: 23px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.org-name {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  opacity: 0.88;
  margin-top: 6px;
}
.notify-icon {
  position: relative;
  z-index: 1;
  padding: 4px;
  cursor: pointer;
}
.hero-credit {
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 14px;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 8px 24px rgba(31, 95, 190, 0.18);
  cursor: pointer;
}
.hero-credit-main {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.hero-credit-label {
  font-size: 12px;
  color: #646566;
}
.hero-credit-value {
  font-size: 24px;
  font-weight: 700;
  color: #1f6fe5;
  line-height: 1;
}
.hero-credit-link {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: #969799;
}
.stat-cards {
  display: flex;
  background: #fff;
  margin: 16px 16px 14px;
  border-radius: 14px;
  padding: 16px 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
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
.stat-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-bottom: 4px;
}
.dot-blue { background: #1989fa; }
.dot-green { background: #07c160; }
.dot-orange { background: #ff976a; }
.dot-red { background: #ee0a24; }
.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #323233;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}
.stat-label {
  font-size: 12px;
  color: #969799;
  margin-top: 2px;
}
.entry-card {
  padding: 18px 8px 10px;
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
  gap: 7px;
  font-size: 12px;
  color: #323233;
  cursor: pointer;
  transition: transform 0.12s;
}
.entry-item:active {
  transform: scale(0.93);
}
.entry-icon {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ic-blue { color: #1989fa; background: #e8f3ff; }
.ic-green { color: #07c160; background: #e7f8ef; }
.ic-orange { color: #ff8f1f; background: #fff3e6; }
.ic-red { color: #ee0a24; background: #ffeceb; }
.ic-purple { color: #7d5cff; background: #f0ecff; }
.ic-teal { color: #00a6a6; background: #e3f7f7; }
.section-title {
  margin-top: 20px;
}
.more-link {
  font-size: 12px;
  color: #969799;
  font-weight: 400;
  cursor: pointer;
}
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
  width: 92px;
  height: 62px;
  border-radius: 10px;
  overflow: hidden;
  background: linear-gradient(135deg, #4f9cff, #33c3a4);
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
  gap: 8px;
}
.course-name {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: #323233;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.course-project {
  font-size: 11px;
  color: #969799;
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
  color: #969799;
  font-variant-numeric: tabular-nums;
}
.course-arrow {
  flex: none;
}
.empty-card {
  background: #fff;
  border-radius: 14px;
  padding: 30px 16px;
  text-align: center;
  color: #969799;
  font-size: 13px;
}
.empty-card p {
  margin: 8px 0 10px;
}
.empty-card.compact {
  padding: 20px 16px;
}
.empty-action {
  color: #1989fa;
  font-size: 13px;
  cursor: pointer;
}
.skeleton-card {
  padding: 16px;
}
.notify-card {
  cursor: pointer;
  transition: opacity 0.15s;
}
.notify-card:active {
  opacity: 0.7;
}
.notify-card.unread {
  border-left: 3px solid #1989fa;
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
  color: #323233;
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
  background: #ee0a24;
}
.notify-time {
  flex: none;
  font-size: 11px;
  color: #c8c9cc;
}
.notify-content {
  font-size: 12px;
  color: #646566;
  margin-top: 6px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
