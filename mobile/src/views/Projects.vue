<template>
  <div class="page-container">
    <div class="search-bar">
      <van-search v-model="keyword" placeholder="搜索培训班" shape="round" @search="loadData" />
    </div>

    <div v-if="list.length === 0 && !loading" style="padding-top: 40px;">
      <EmptyState description="暂无培训班" />
    </div>

    <div
      v-for="item in list"
      :key="item.id"
      class="card project-card"
      @click="$router.push(`/projects/${item.id}`)"
    >
      <div class="flex-between mb-8">
        <span class="project-name">{{ item.projectName }}</span>
        <StatusTag :status="item.status" type="project" />
      </div>
      <div class="project-meta">
        <span>{{ item.projectCode }}</span>
        <span v-if="item.startTime">{{ formatDate(item.startTime) }} ~ {{ formatDate(item.endTime) }}</span>
      </div>
      <div class="project-progress mt-8">
        <div class="flex-between mb-8">
          <span class="progress-text">学习进度</span>
          <span class="progress-text">{{ item.completedCourse || 0 }}/{{ item.courseTotal || 0 }} 课程</span>
        </div>
        <van-progress
          :percentage="item.courseTotal ? Math.round(((item.completedCourse || 0) / item.courseTotal) * 100) : 0"
          :show-pivot="false"
          color="#1989fa"
          stroke-width="6"
        />
      </div>
      <div class="project-credits mt-8">
        学分：{{ item.credit || 0 }} / {{ item.needCredit || 0 }}
      </div>
    </div>

    <van-loading v-if="loading" class="loading-center" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search as VanSearch, Progress as VanProgress, Loading as VanLoading } from 'vant'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { projects } from '@/api'
import dayjs from 'dayjs'

const keyword = ref('')
const list = ref<any[]>([])
const loading = ref(false)

function formatDate(d: string) {
  return dayjs(d).format('YYYY-MM-DD')
}

async function loadData() {
  loading.value = true
  try {
    const data = await projects.my()
    let result = data as any[]
    if (keyword.value) {
      result = result.filter((p: any) =>
        (p.projectName || '').includes(keyword.value) ||
        (p.projectCode || '').includes(keyword.value)
      )
    }
    list.value = result
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.search-bar {
  background: #fff;
}
.project-card {
  cursor: pointer;
}
.project-name {
  font-size: 16px;
  font-weight: 600;
  flex: 1;
  margin-right: 8px;
}
.project-meta {
  font-size: 12px;
  color: #969799;
  display: flex;
  gap: 12px;
}
.progress-text {
  font-size: 12px;
  color: #646566;
}
.project-credits {
  font-size: 13px;
  color: #ff976a;
  font-weight: 500;
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 20px;
}
</style>
