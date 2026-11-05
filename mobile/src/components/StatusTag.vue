<template>
  <span class="status-tag" :class="`status-tag--${tone}`">
    <i class="status-dot"></i>{{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: number | string
  type?: 'project' | 'credit' | 'graduate' | 'course' | 'study'
}>()

const statusMap: Record<string, { tone: string; label: string }> = {
  // project status (0未开始 1进行中 2已结束 3归档)
  'project-0': { tone: 'neutral', label: '未开始' },
  'project-1': { tone: 'primary', label: '进行中' },
  'project-2': { tone: 'success', label: '已结束' },
  'project-3': { tone: 'danger', label: '已归档' },
  // credit audit (1待发放 2已发放 3已驳回 4已撤销)
  'credit-1': { tone: 'warning', label: '待发放' },
  'credit-2': { tone: 'success', label: '已发放' },
  'credit-3': { tone: 'danger', label: '已驳回' },
  'credit-4': { tone: 'neutral', label: '已撤销' },
  // graduate (0待审 1通过 2驳回)
  'graduate-0': { tone: 'warning', label: '审批中' },
  'graduate-1': { tone: 'success', label: '已通过' },
  'graduate-2': { tone: 'danger', label: '已驳回' },
  // course
  'course-0': { tone: 'neutral', label: '未开始' },
  'course-1': { tone: 'primary', label: '学习中' },
  'course-2': { tone: 'success', label: '已完成' },
  // study
  'study-0': { tone: 'neutral', label: '未开始' },
  'study-1': { tone: 'primary', label: '学习中' },
  'study-2': { tone: 'success', label: '已完成' },
  'study-3': { tone: 'danger', label: '已锁定' },
}

const entry = computed(() => statusMap[`${props.type}-${props.status}`])
const tone = computed(() => entry.value?.tone || 'neutral')
const label = computed(() => entry.value?.label || String(props.status))
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex: none;
  padding: 3px 8px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface-2);
  font-size: 11px;
  font-weight: 500;
  line-height: 1.5;
  color: var(--ink-2);
  letter-spacing: 0.01em;
  white-space: nowrap;
}

.status-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.85;
}

.status-tag--primary {
  border-color: var(--brand-line);
  background: var(--brand-tint);
  color: var(--brand);
}

.status-tag--success {
  border-color: #cfe5da;
  background: var(--ok-tint);
  color: var(--ok);
}

.status-tag--warning {
  border-color: var(--accent-line);
  background: var(--warn-tint);
  color: var(--warn);
}

.status-tag--danger {
  border-color: #edcfcd;
  background: var(--bad-tint);
  color: var(--bad);
}
</style>
