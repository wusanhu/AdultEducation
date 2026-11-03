<template>
  <div class="page-container">
    <PageHeader title="培训班详情" />

    <van-loading v-if="loading" class="loading-center" />

    <template v-if="detail">
      <div class="card card--gilded detail-head">
        <div class="flex-between mb-8">
          <h3 class="detail-name">{{ detail.projectName }}</h3>
          <StatusTag :status="detail.status" type="project" />
        </div>
        <p class="desc">{{ detail.description }}</p>
        <div class="meta-list">
          <div class="meta-item">
            <span class="meta-label">编号</span>
            <span class="num">{{ detail.projectCode }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">时间</span>
            <span class="num">{{ formatDate(detail.startTime) }} ~ {{ formatDate(detail.endTime) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">所需学分</span>
            <span class="num credit-value">{{ detail.needCredit }}</span>
          </div>
        </div>
      </div>

      <div class="section-title">
        课程列表
        <span class="section-title-en">COURSES</span>
      </div>
      <div v-if="!detail.courses || detail.courses.length === 0" class="empty-ink">
        <div class="empty-ink-glyph">课</div>
        <p>暂无课程</p>
      </div>
      <div
        v-for="(course, i) in detail.courses"
        :key="course.id"
        class="card course-item"
      >
        <div class="flex-between mb-8">
          <span class="course-name">
            <i class="course-index num">{{ String(i + 1).padStart(2, '0') }}</i>
            {{ course.courseName }}
          </span>
          <StatusTag :status="course.studyStatus ?? 0" type="study" />
        </div>
        <div class="course-meta">
          <span>学分 <b class="num">{{ course.credit }}</b></span>
          <span>学时 <b class="num">{{ course.studyHour }}</b></span>
          <span v-if="course.needExam" class="need-exam">需考试</span>
        </div>
        <van-progress
          v-if="course.progress != null"
          :percentage="Math.min(100, Math.round(course.progress || 0))"
          :show-pivot="true"
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
.detail-name {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--text-strong);
  flex: 1;
  margin-right: 8px;
}
.desc {
  font-size: 13px;
  color: var(--text-sub);
  line-height: 1.7;
  margin-bottom: 12px;
}
.meta-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
}
.meta-item {
  font-size: 13px;
  color: var(--text-strong);
}
.meta-label {
  color: var(--text-faint);
  margin-right: 5px;
  font-size: 12px;
}
.credit-value {
  color: var(--seal);
  font-weight: 700;
  font-size: 15px;
}
.course-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-strong);
  flex: 1;
  min-width: 0;
  margin-right: 8px;
}
.course-index {
  flex: none;
  font-size: 11px;
  color: var(--gold);
  border: 1px solid rgba(176, 138, 62, 0.4);
  border-radius: 4px;
  padding: 2px 4px;
  letter-spacing: 0.05em;
}
.course-meta {
  font-size: 12px;
  color: var(--text-sub);
  display: flex;
  gap: 14px;
  align-items: center;
}
.course-meta b {
  color: var(--text-strong);
  font-weight: 600;
}
.need-exam {
  color: var(--warn);
  border: 1px solid rgba(176, 138, 62, 0.35);
  border-radius: 4px;
  padding: 1px 6px;
  font-size: 11px;
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
