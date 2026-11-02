<template>
  <div class="page-container">
    <!-- Greeting -->
    <div class="home-header">
      <div class="greeting">
        <h2>{{ greetingText }}，{{ userStore.user?.realName || '同学' }}</h2>
        <p class="org-name">{{ userStore.user?.orgName || '' }}</p>
      </div>
      <div class="notify-icon" @click="$router.push('/notifications')">
        <van-badge :content="unreadCount > 0 ? unreadCount : ''" :max="99">
          <van-icon name="bell" size="24" color="#fff" />
        </van-badge>
      </div>
    </div>

    <!-- Stat Cards -->
    <div class="stat-cards">
      <div class="stat-item" @click="$router.push('/projects')">
        <div class="stat-value">{{ stats.learningCourses || 0 }}</div>
        <div class="stat-label">学习中</div>
      </div>
      <div class="stat-item">
        <div class="stat-value" style="color: #07c160">{{ stats.completedCourses || 0 }}</div>
        <div class="stat-label">已完成</div>
      </div>
      <div class="stat-item" @click="$router.push('/credits')">
        <div class="stat-value" style="color: #ff976a">{{ stats.totalCredits || 0 }}</div>
        <div class="stat-label">总学分</div>
      </div>
      <div class="stat-item">
        <div class="stat-value" style="color: #ee0a24">{{ stats.pendingCredits || 0 }}</div>
        <div class="stat-label">待审核</div>
      </div>
    </div>

    <!-- Entry Grid -->
    <div class="card">
      <div class="entry-grid">
        <div class="entry-item" @click="$router.push('/projects')">
          <van-icon name="orders-o" size="28" color="#1989fa" />
          <span>培训班</span>
        </div>
        <div class="entry-item" @click="$router.push('/credits')">
          <van-icon name="gold-coin-o" size="28" color="#07c160" />
          <span>学分查询</span>
        </div>
        <div class="entry-item" @click="$router.push('/graduate')">
          <van-icon name="certificate" size="28" color="#ff976a" />
          <span>毕业申请</span>
        </div>
        <div class="entry-item" @click="$router.push('/verify')">
          <van-icon name="shield-o" size="28" color="#ee0a24" />
          <span>核验记录</span>
        </div>
        <div class="entry-item" @click="$router.push('/notifications')">
          <van-icon name="bell" size="28" color="#1989fa" />
          <span>消息通知</span>
        </div>
        <div v-if="userStore.isTeacher" class="entry-item" @click="$router.push('/teacher')">
          <van-icon name="manager-o" size="28" color="#07c160" />
          <span>教师管理</span>
        </div>
      </div>
    </div>

    <!-- Continue Learning -->
    <div class="section-title">继续学习</div>
    <div v-if="continueList.length === 0" class="card" style="text-align: center; color: #969799; padding: 32px;">
      暂无进行中的课程
    </div>
    <div
      v-for="item in continueList"
      :key="item.courseId"
      class="card course-card"
      @click="$router.push(`/study/${item.courseId}`)"
    >
      <div class="flex-between mb-8">
        <span class="course-name text-ellipsis">{{ item.courseName }}</span>
        <StatusTag :status="item.studyStatus" type="study" />
      </div>
      <div class="course-project">{{ item.projectName }}</div>
      <van-progress
        :percentage="Math.min(100, Math.round(item.progress || 0))"
        :show-pivot="true"
        color="#1989fa"
        class="mt-8"
      />
    </div>

    <!-- Recent Notifications -->
    <div class="section-title flex-between">
      <span>最新通知</span>
      <span class="more-link" @click="$router.push('/notifications')">更多 &gt;</span>
    </div>
    <div v-if="recentNotifications.length === 0" class="card" style="text-align: center; color: #969799; padding: 24px;">
      暂无通知
    </div>
    <div
      v-for="n in recentNotifications"
      :key="n.id"
      class="card notify-card"
      :class="{ unread: !n.isRead }"
      @click="$router.push('/notifications')"
    >
      <div class="flex-between">
        <span class="notify-title">{{ n.title }}</span>
        <span class="notify-time">{{ formatTime(n.createTime) }}</span>
      </div>
      <p class="notify-content">{{ n.content }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Icon as VanIcon, Badge as VanBadge, Progress as VanProgress } from 'vant'
import StatusTag from '@/components/StatusTag.vue'
import { useUserStore } from '@/stores/user'
import { projects, credits, notifications } from '@/api'
import dayjs from 'dayjs'

const userStore = useUserStore()
const stats = ref<any>({})
const continueList = ref<any[]>([])
const recentNotifications = ref<any[]>([])
const unreadCount = ref(0)

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
  }
})
</script>

<style scoped>
.home-header {
  background: linear-gradient(135deg, #1989fa, #07c160);
  padding: 48px 20px 32px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  color: #fff;
}
.greeting h2 {
  font-size: 20px;
  font-weight: 600;
}
.org-name {
  font-size: 13px;
  opacity: 0.85;
  margin-top: 4px;
}
.notify-icon {
  padding: 4px;
}
.stat-cards {
  display: flex;
  background: #fff;
  margin: -20px 16px 16px;
  border-radius: 12px;
  padding: 20px 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  position: relative;
  z-index: 1;
}
.stat-item {
  flex: 1;
  text-align: center;
}
.entry-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 8px 0;
}
.entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #323233;
  cursor: pointer;
}
.course-card {
  cursor: pointer;
}
.course-name {
  font-size: 15px;
  font-weight: 500;
  max-width: 60%;
}
.course-project {
  font-size: 12px;
  color: #969799;
}
.more-link {
  font-size: 13px;
  color: #1989fa;
  cursor: pointer;
}
.notify-card {
  cursor: pointer;
}
.notify-card.unread {
  border-left: 3px solid #1989fa;
}
.notify-title {
  font-size: 14px;
  font-weight: 500;
}
.notify-time {
  font-size: 12px;
  color: #c8c9cc;
}
.notify-content {
  font-size: 13px;
  color: #646566;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
