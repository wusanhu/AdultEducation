<template>
  <div class="page-container">
    <PageHeader title="培训班详情" />

    <van-loading v-if="loading" class="loading-center" />

    <template v-if="detail">
      <div class="card">
        <div class="flex-between mb-8">
          <h3>{{ detail.projectName }}</h3>
          <StatusTag :status="detail.status" type="project" />
        </div>
        <p class="desc">{{ detail.description }}</p>
        <div class="meta-list">
          <div class="meta-item">
            <span class="meta-label">编号</span>
            <span>{{ detail.projectCode }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">时间</span>
            <span>{{ formatDate(detail.startTime) }} ~ {{ formatDate(detail.endTime) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">所需学分</span>
            <span class="credit-value">{{ detail.needCredit }}</span>
          </div>
        </div>
      </div>

      <div class="section-title">课程列表</div>
      <div v-if="!detail.courses || detail.courses.length === 0" class="card" style="text-align: center; color: #969799;">
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
          <span>学分：{{ course.credit }}</span>
          <span>学时：{{ course.studyHour }}</span>
          <span v-if="course.needExam">需考试</span>
        </div>
        <van-progress
          v-if="course.progress != null"
          :percentage="Math.min(100, Math.round(course.progress || 0))"
          :show-pivot="true"
          color="#1989fa"
          stroke-width="4"
          class="mt-8"
        />
        <div class="course-actions mt-8">
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Button as VanButton, Progress as VanProgress, Loading as VanLoading } from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { projects } from '@/api'
import dayjs from 'dayjs'

const route = useRoute()
const detail = ref<any>(null)
const loading = ref(true)

function formatDate(d: string) {
  return d ? dayjs(d).format('YYYY-MM-DD') : '-'
}

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    const res: any = await projects.myDetail(id)
    detail.value = { ...(res.project || res), courses: res.courses || [] }
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.desc {
  font-size: 13px;
  color: #646566;
  line-height: 1.6;
  margin-bottom: 12px;
}
.meta-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.meta-item {
  font-size: 13px;
  color: #323233;
}
.meta-label {
  color: #969799;
  margin-right: 4px;
}
.credit-value {
  color: #ff976a;
  font-weight: 600;
}
.course-item {
  cursor: default;
}
.course-name {
  font-size: 15px;
  font-weight: 500;
  flex: 1;
  margin-right: 8px;
}
.course-meta {
  font-size: 12px;
  color: #969799;
  display: flex;
  gap: 12px;
}
.course-actions {
  display: flex;
  gap: 8px;
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}
</style>
