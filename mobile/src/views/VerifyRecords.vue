<template>
  <div class="page-container">
    <PageHeader title="核验记录" />

    <van-loading v-if="loading" class="loading-center" />

    <div v-if="!loading && list.length === 0" style="padding-top: 40px;">
      <EmptyState description="暂无核验记录" glyph="验" />
    </div>

    <div v-for="item in list" :key="item.id" class="card verify-item">
      <div class="flex-between mb-8">
        <span class="verify-course">{{ item.courseName || '课程核验' }}</span>
        <span class="verify-stamp" :class="stampClass(item.verifyResult)">
          {{ resultLabel(item.verifyResult) }}
        </span>
      </div>
      <div class="verify-meta">
        <span class="verify-method">
          <van-icon :name="methodIcon(item.verifyMethod)" size="12" />
          {{ methodLabel(item.verifyMethod) }}
        </span>
        <span class="num">{{ formatTime(item.createTime) }}</span>
      </div>
      <p v-if="item.remark" class="verify-remark mt-8">{{ item.remark }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Icon as VanIcon, Loading as VanLoading } from 'vant'
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

function stampClass(r: number) {
  switch (r) {
    case 1: return 'stamp-ok'
    case 2: return 'stamp-fail'
    default: return 'stamp-pending'
  }
}

function methodLabel(m: string | number) {
  switch (m) {
    case 'face': case 1: return '人脸核验'
    case 'sms': case 2: return '短信核验'
    default: return '其他'
  }
}

function methodIcon(m: string | number) {
  switch (m) {
    case 'face': case 1: return 'user-o'
    case 'sms': case 2: return 'chat-o'
    default: return 'shield-o'
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
  font-size: 14px;
  font-weight: 600;
  color: var(--text-strong);
  flex: 1;
  min-width: 0;
  margin-right: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
/* 结果印章 */
.verify-stamp {
  flex: none;
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.15em;
  padding: 4px 9px;
  border: 1.5px solid currentColor;
  border-radius: 5px;
  transform: rotate(-6deg);
}
.stamp-ok {
  color: var(--ok);
}
.stamp-fail {
  color: var(--danger);
}
.stamp-pending {
  color: var(--warn);
}
.verify-meta {
  font-size: 12px;
  color: var(--text-faint);
  display: flex;
  gap: 16px;
  align-items: center;
}
.verify-method {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.verify-remark {
  font-size: 12px;
  color: var(--text-sub);
  border-top: 1px dashed var(--line);
  padding-top: 8px;
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}
</style>
