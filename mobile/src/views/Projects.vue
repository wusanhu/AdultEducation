<template>
  <div class="page-container">
    <div class="search-bar">
      <van-search v-model="keyword" placeholder="搜索培训班" shape="round" @search="loadData" />
    </div>

    <div v-if="list.length === 0 && !loading" style="padding-top: 40px;">
      <EmptyState description="暂无培训班" glyph="班" />
    </div>

    <div
      v-for="item in list"
      :key="item.id"
      class="card project-card"
      @click="$router.push(`/projects/${item.id}`)"
    >
      <span class="bookmark"></span>
      <div class="flex-between mb-8">
        <span class="project-name">{{ item.projectName }}</span>
        <StatusTag :status="item.status" type="project" />
      </div>
      <div class="project-meta">
        <span class="num meta-code">{{ item.projectCode }}</span>
        <span v-if="item.startTime">{{ formatDate(item.startTime) }} ~ {{ formatDate(item.endTime) }}</span>
      </div>
      <div class="project-progress mt-8">
        <div class="flex-between mb-8">
          <span class="progress-text">学习进度</span>
          <span class="progress-text num">{{ item.completedCourse || 0 }}/{{ item.courseTotal || 0 }} 课程</span>
        </div>
        <van-progress
          :percentage="item.courseTotal ? Math.round(((item.completedCourse || 0) / item.courseTotal) * 100) : 0"
          :show-pivot="false"
          stroke-width="5"
        />
      </div>
      <div class="rule-dashed"></div>
      <div class="project-credits">
        学分
        <span class="num credit-num">{{ item.credit || 0 }}</span>
        <span class="credit-sep">/</span>
        需修
        <span class="num">{{ item.needCredit || 0 }}</span>
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
  padding-top: 6px;
}
.project-card {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: opacity 0.15s;
}
.project-card:active {
  opacity: 0.75;
}
.bookmark {
  position: absolute;
  top: -1px;
  left: 16px;
  width: 34px;
  height: 5px;
  background: var(--seal);
  border-radius: 0 0 3px 3px;
}
.project-name {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--text-strong);
  flex: 1;
  margin-right: 8px;
}
.project-meta {
  font-size: 11px;
  color: var(--text-faint);
  display: flex;
  gap: 12px;
}
.meta-code {
  letter-spacing: 0.08em;
}
.progress-text {
  font-size: 12px;
  color: var(--text-sub);
}
.project-credits {
  font-size: 12px;
  color: var(--text-sub);
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.credit-num {
  font-size: 17px;
  font-weight: 700;
  color: var(--seal);
}
.credit-sep {
  color: var(--text-faint);
  margin: 0 2px;
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 20px;
}
</style>
