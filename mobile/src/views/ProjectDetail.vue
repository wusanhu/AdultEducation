<template>
  <div class="page-container">
    <PageHeader title="培训班详情" />

    <van-loading v-if="loading" class="loading-center" />

    <!-- 学员：我的学习详情，可从课程进入学习/考试 -->
    <template v-if="!isOps && detail">
      <div class="card detail-head">
        <div class="flex-between mb-12">
          <h3 class="detail-title">{{ detail.projectName }}</h3>
          <StatusTag :status="detail.status" type="project" />
        </div>
        <p class="desc">{{ detail.description }}</p>
        <div class="meta-list">
          <div class="meta-item">
            <span class="meta-label">编号</span>
            <span class="meta-value">{{ detail.projectCode }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">时间</span>
            <span class="meta-value">{{ formatDate(detail.startTime) }} ~ {{ formatDate(detail.endTime) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">所需学分</span>
            <span class="meta-value credit-value">{{ detail.needCredit }}</span>
          </div>
        </div>
      </div>

      <div class="section-title">课程列表</div>
      <div v-if="!detail.courses || detail.courses.length === 0" class="card empty-course">
        暂无课程
      </div>
      <div
        v-for="course in detail.courses"
        :key="course.id"
        class="card course-item"
      >
        <div class="flex-between mb-8">
          <span class="course-name">{{ course.courseName }}</span>
          <StatusTag :status="course.studyStatus ?? 0" type="study" />
        </div>
        <div class="course-meta">
          <span class="course-chip">学分 {{ course.credit }}</span>
          <span class="course-chip">学时 {{ course.studyHour }}</span>
          <span v-if="course.needExam" class="course-chip course-chip--warn">需考试</span>
        </div>
        <div v-if="course.progress != null" class="course-progress mt-12">
          <van-progress
            :percentage="Math.min(100, Math.round(course.progress || 0))"
            :show-pivot="false"
            color="var(--brand)"
            track-color="var(--line)"
            stroke-width="4"
          />
          <span class="course-percent">{{ Math.min(100, Math.round(course.progress || 0)) }}%</span>
        </div>
        <hr class="hairline" />
        <div class="course-actions">
          <van-button
            v-if="(course.studyStatus ?? 0) <= 1"
            size="small"
            type="primary"
            @click="$router.push(`/study/${course.id}`)"
          >
            {{ course.studyStatus === 1 ? '继续学习' : '开始学习' }}
          </van-button>
          <van-button
            v-if="course.studyStatus === 2 && course.needExam"
            size="small"
            type="warning"
            plain
            @click="$router.push(`/exam/${course.id}`)"
          >
            进入考试
          </van-button>
          <van-button
            size="small"
            plain
            @click="$router.push(`/course/${course.id}`)"
          >
            课程详情
          </van-button>
        </div>
      </div>
    </template>

    <!-- 教务：托管班只读概览，配置与编辑动作留在 PC 后台 -->
    <template v-else-if="isOps && detail">
      <div class="card detail-head">
        <div class="flex-between mb-12">
          <h3 class="detail-title">{{ detail.projectName }}</h3>
          <StatusTag :status="detail.status" type="project" />
        </div>
        <p v-if="detail.description" class="desc">{{ detail.description }}</p>
        <div class="meta-list">
          <div class="meta-item">
            <span class="meta-label">编号</span>
            <span class="meta-value">{{ detail.projectCode || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">时间</span>
            <span class="meta-value">{{ formatDate(detail.startTime) }} ~ {{ formatDate(detail.endTime) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">所需学分</span>
            <span class="meta-value credit-value">{{ detail.needCredit }}</span>
          </div>
          <div v-if="detail.createUserName" class="meta-item">
            <span class="meta-label">创建人</span>
            <span class="meta-value">{{ detail.createUserName }}</span>
          </div>
        </div>
      </div>

      <div class="section-title">培训班概况</div>
      <div class="card overview">
        <div class="overview-item">
          <span class="stat-value overview-num">{{ detail.memberCount || 0 }}</span>
          <span class="stat-label">学员</span>
        </div>
        <div class="overview-item">
          <span class="stat-value overview-num">{{ detail.courseCount || 0 }}</span>
          <span class="stat-label">在架课程</span>
        </div>
        <div class="overview-item">
          <span class="stat-value overview-num">{{ (detail.teachers || []).length }}</span>
          <span class="stat-label">师资</span>
        </div>
      </div>

      <div class="section-title">课程</div>
      <div v-if="!detail.courses || detail.courses.length === 0" class="card empty-course">
        暂无课程
      </div>
      <div v-for="course in detail.courses" :key="course.id" class="card course-item">
        <div class="flex-between mb-8">
          <span class="course-name">{{ course.courseName }}</span>
          <!-- 教务侧 course.status 是上架态（0草稿/1已上架/2已下架），与学员侧学习态不同，只用后端译文 -->
          <span v-if="course.statusName" class="course-state">{{ course.statusName }}</span>
        </div>
        <div class="course-meta">
          <span class="course-chip">学分 {{ course.credit }}</span>
        </div>
      </div>

      <div class="section-title">师资</div>
      <div v-if="!detail.teachers || detail.teachers.length === 0" class="card empty-course">
        暂无师资
      </div>
      <div v-else class="card list-card">
        <div v-for="(t, i) in detail.teachers" :key="t.id || i" class="teacher-item">
          <span class="teacher-avatar"><van-icon name="manager" size="15" /></span>
          <span class="teacher-name text-ellipsis">{{ t.realName || '未命名' }}</span>
        </div>
      </div>

      <div class="section-title">学员名册</div>
      <div v-if="!detail.members || detail.members.length === 0" class="card empty-course">
        暂无学员
      </div>
      <template v-else>
        <div class="card list-card">
          <div v-for="m in detail.members" :key="m.userId" class="member-item">
            <div class="member-main">
              <span class="member-name text-ellipsis">{{ m.realName || '未命名' }}</span>
              <span v-if="m.orgName" class="member-org text-ellipsis">{{ m.orgName }}</span>
            </div>
            <div class="member-side">
              <span class="member-credit">{{ m.credit || 0 }} 分</span>
              <span v-if="m.phone" class="member-phone">{{ m.phone }}</span>
            </div>
          </div>
        </div>
        <p class="member-hint">仅展示前 20 人，完整名册请在 PC 端查看</p>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  Button as VanButton,
  Progress as VanProgress,
  Loading as VanLoading,
  Icon as VanIcon,
} from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { projects } from '@/api'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const route = useRoute()
const userStore = useUserStore()
/** 分支只看能力：project:list 走托管视图 */
const isOps = computed(() => userStore.caps.canListProjects)

const detail = ref<any>(null)
const loading = ref(true)

function formatDate(d: string) {
  return d ? dayjs(d).format('YYYY-MM-DD') : '-'
}

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    if (isOps.value) {
      // /projects/{id} 返回扁平对象（projectMap + members/courses/teachers），不像 /projects/my/{id} 那样包 {project}
      detail.value = await projects.detail(id)
    } else {
      const res: any = await projects.myDetail(id)
      detail.value = { ...(res.project || res), courses: res.courses || [] }
    }
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail-head {
  padding-bottom: var(--sp-3);
}
.detail-title {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--ink);
  flex: 1;
  margin-right: var(--sp-2);
}
.desc {
  font-size: 13px;
  color: var(--ink-2);
  line-height: 1.6;
  margin-bottom: var(--sp-3);
}
.meta-list {
  display: flex;
  flex-direction: column;
  gap: var(--sp-1);
  padding: var(--sp-3);
  border: 1px solid var(--line);
  border-radius: var(--r-chip);
  background: var(--surface-2);
}
.meta-item {
  display: flex;
  align-items: baseline;
  gap: var(--sp-3);
  font-size: 13px;
  line-height: 1.6;
}
.meta-label {
  flex: none;
  width: 60px;
  color: var(--ink-3);
}
.meta-value {
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
.credit-value {
  color: var(--brand);
  font-weight: 600;
}
.overview {
  display: flex;
  padding: var(--sp-5) var(--sp-2);
}
.overview-item {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.overview-item + .overview-item {
  border-left: 1px solid var(--line);
}
.overview-num {
  font-size: 22px;
  color: var(--brand);
}
.empty-course {
  text-align: center;
  color: var(--ink-3);
  font-size: 13px;
  padding: var(--sp-6) var(--page-x);
}
.course-item {
  cursor: default;
}
.course-name {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--ink);
  flex: 1;
  margin-right: var(--sp-2);
}
.course-state {
  flex: none;
  padding: 3px 8px;
  border: 1px solid var(--line);
  border-radius: var(--r-tag);
  background: var(--surface-2);
  font-size: 11px;
  color: var(--ink-2);
}
.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
}
.course-chip {
  padding: 2px 8px;
  border: 1px solid var(--brand-line);
  border-radius: var(--r-tag);
  background: var(--brand-tint);
  font-size: 11px;
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}
.course-chip--warn {
  border-color: var(--line);
  background: var(--warn-tint);
  color: var(--warn);
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
.course-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
}
.course-actions :deep(.van-button) {
  border-radius: var(--r-chip);
  padding: 0 var(--sp-4);
}
.course-actions :deep(.van-button--plain) {
  background: var(--surface);
}
.list-card {
  padding-top: var(--sp-2);
  padding-bottom: var(--sp-2);
}
.teacher-item {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  min-height: 44px;
}
.teacher-item + .teacher-item {
  border-top: 1px solid var(--line);
}
.teacher-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 30px;
  height: 30px;
  border: 1px solid var(--brand-line);
  border-radius: var(--r-tag);
  background: var(--brand-tint);
  color: var(--brand);
}
.teacher-name {
  min-width: 0;
  font-size: 14px;
  color: var(--ink);
}
.member-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-3);
  min-height: 44px;
  padding: var(--sp-1) 0;
}
.member-item + .member-item {
  border-top: 1px solid var(--line);
}
.member-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.member-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--ink);
}
.member-org {
  font-size: 12px;
  color: var(--ink-3);
}
.member-side {
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}
.member-credit {
  font-size: 13px;
  font-weight: 600;
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}
.member-phone {
  font-size: 11px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}
.member-hint {
  padding: 0 var(--page-x) var(--sp-4);
  font-size: 12px;
  line-height: 1.6;
  color: var(--ink-3);
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}
</style>
