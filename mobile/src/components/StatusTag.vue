<template>
  <van-tag :type="tagType" plain size="medium" class="status-tag">{{ label }}</van-tag>
</template>

<script setup lang="ts">
import { Tag as VanTag } from 'vant'
import { computed } from 'vue'

const props = defineProps<{
  status: number | string
  type?: 'project' | 'credit' | 'graduate' | 'course' | 'study'
}>()

const statusMap: Record<string, { type: any; label: string }> = {
  // project status (0未开始 1进行中 2已结束 3归档)
  'project-0': { type: 'default', label: '未开始' },
  'project-1': { type: 'primary', label: '进行中' },
  'project-2': { type: 'success', label: '已结束' },
  'project-3': { type: 'danger', label: '已归档' },
  // credit audit (1待发放 2已发放 3已驳回 4已撤销)
  'credit-1': { type: 'warning', label: '待发放' },
  'credit-2': { type: 'success', label: '已发放' },
  'credit-3': { type: 'danger', label: '已驳回' },
  'credit-4': { type: 'default', label: '已撤销' },
  // graduate (0待审 1通过 2驳回)
  'graduate-0': { type: 'warning', label: '审批中' },
  'graduate-1': { type: 'success', label: '已通过' },
  'graduate-2': { type: 'danger', label: '已驳回' },
  // course
  'course-0': { type: 'default', label: '未开始' },
  'course-1': { type: 'primary', label: '学习中' },
  'course-2': { type: 'success', label: '已完成' },
  // study
  'study-0': { type: 'default', label: '未开始' },
  'study-1': { type: 'primary', label: '学习中' },
  'study-2': { type: 'success', label: '已完成' },
  'study-3': { type: 'danger', label: '已锁定' },
}

const tagType = computed(() => {
  const key = `${props.type}-${props.status}`
  return statusMap[key]?.type || 'default'
})

const label = computed(() => {
  const key = `${props.type}-${props.status}`
  return statusMap[key]?.label || String(props.status)
})
</script>

<style scoped>
/* plain 只给描边与文字色，底色用语义浅调补足层级 */
.status-tag {
  padding: 3px 8px;
  font-weight: 500;
}
.status-tag.van-tag--primary { background: var(--brand-tint); }
.status-tag.van-tag--success { background: var(--ok-tint); }
.status-tag.van-tag--warning { background: var(--warn-tint); }
.status-tag.van-tag--danger { background: var(--bad-tint); }
.status-tag.van-tag--default { background: #f1f3f6; }
</style>
