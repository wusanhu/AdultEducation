<template>
  <div class="page-container">
    <PageHeader title="核验记录" />

    <van-loading v-if="loading" class="loading-center" />

    <div v-if="!loading && list.length === 0" style="padding-top: 40px;">
      <EmptyState description="暂无核验记录" />
    </div>

    <div v-for="item in list" :key="item.id" class="card verify-item">
      <div class="flex-between mb-8">
        <span class="verify-course">{{ item.courseName || '课程核验' }}</span>
        <van-tag :type="item.verifyResult === 1 ? 'success' : item.verifyResult === 2 ? 'danger' : 'warning'" size="medium">
          {{ resultLabel(item.verifyResult) }}
        </van-tag>
      </div>
      <div class="verify-meta">
        <span>方式：{{ methodLabel(item.verifyMethod) }}</span>
        <span>时间：{{ formatTime(item.createTime) }}</span>
      </div>
      <p v-if="item.remark" class="verify-remark mt-8">{{ item.remark }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Tag as VanTag, Loading as VanLoading } from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { verify } from '@/api'
import dayjs from 'dayjs'

const loading = ref(true)
const list = ref<any[]>([])

function resultLabel(r: number) {
  switch (r) {
    case 1: return '通过'
    case 2: return '未通过'
    default: return '待审核'
  }
}

function methodLabel(m: string | number) {
  switch (m) {
    case 'face': case 1: return '人脸核验'
    case 'sms': case 2: return '短信核验'
    default: return '其他'
  }
}

function formatTime(t: string) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
}

onMounted(async () => {
  try {
    list.value = await verify.my() as any[]
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.verify-item {
  cursor: default;
}
.verify-course {
  font-size: 15px;
  font-weight: 500;
  flex: 1;
  margin-right: 8px;
}
.verify-meta {
  font-size: 12px;
  color: #969799;
  display: flex;
  gap: 16px;
}
.verify-remark {
  font-size: 12px;
  color: #646566;
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}
</style>
