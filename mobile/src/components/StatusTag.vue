<template>
  <span class="status-tag" :class="[`is-${tone}`]">
    <i class="dot" :class="`dot-${tone}`"></i>
    {{ label }}
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
  'project-0': { tone: 'idle', label: '未开始' },
  'project-1': { tone: 'ink', label: '进行中' },
  'project-2': { tone: 'ok', label: '已结束' },
  'project-3': { tone: 'danger', label: '已归档' },
  // credit audit (1待发放 2已发放 3已驳回 4已撤销)
  'credit-1': { tone: 'warn', label: '待发放' },
  'credit-2': { tone: 'ok', label: '已发放' },
  'credit-3': { tone: 'danger', label: '已驳回' },
  'credit-4': { tone: 'idle', label: '已撤销' },
  // graduate (0待审 1通过 2驳回)
  'graduate-0': { tone: 'warn', label: '审批中' },
  'graduate-1': { tone: 'ok', label: '已通过' },
  'graduate-2': { tone: 'danger', label: '已驳回' },
  // course
  'course-0': { tone: 'idle', label: '未开始' },
  'course-1': { tone: 'ink', label: '学习中' },
  'course-2': { tone: 'ok', label: '已完成' },
  // study
  'study-0': { tone: 'idle', label: '未开始' },
  'study-1': { tone: 'ink', label: '学习中' },
  'study-2': { tone: 'ok', label: '已完成' },
  'study-3': { tone: 'danger', label: '已锁定' },
}

const tone = computed(() => {
  const key = `${props.type}-${props.status}`
  return statusMap[key]?.tone || 'idle'
})

const label = computed(() => {
  const key = `${props.type}-${props.status}`
  return statusMap[key]?.label || String(props.status)
})
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  line-height: 1;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid transparent;
  white-space: nowrap;
  letter-spacing: 0.02em;
}
.is-ink {
  color: #1b2a4a;
  background: rgba(27, 42, 74, 0.07);
  border-color: rgba(27, 42, 74, 0.18);
}
.is-ok {
  color: #2f6249;
  background: rgba(62, 122, 94, 0.1);
  border-color: rgba(62, 122, 94, 0.28);
}
.is-warn {
  color: #8a6a2c;
  background: rgba(176, 138, 62, 0.12);
  border-color: rgba(176, 138, 62, 0.35);
}
.is-danger {
  color: #c2432e;
  background: rgba(194, 67, 46, 0.08);
  border-color: rgba(194, 67, 46, 0.3);
}
.is-idle {
  color: #7a8194;
  background: rgba(122, 129, 148, 0.08);
  border-color: rgba(122, 129, 148, 0.25);
}
.dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}
.dot-ink { background: #1b2a4a; }
.dot-ok { background: #3e7a5e; }
.dot-warn { background: #b08a3e; }
.dot-danger { background: #c2432e; }
.dot-idle { background: #a8adba; }
</style>
