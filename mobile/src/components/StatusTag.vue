<template>
  <van-tag :type="tagType" round size="medium">{{ label }}</van-tag>
</template>

<script setup lang="ts">
import { Tag as VanTag } from 'vant'
import { computed } from 'vue'

const props = defineProps<{
  status: number | string
  type?: 'project' | 'credit' | 'graduate' | 'course' | 'study'
}>()

const statusMap: Record<string, { type: any; label: string }> = {
  // project status
  'project-0': { type: 'default', label: '草稿' },
  'project-1': { type: 'primary', label: '进行中' },
  'project-2': { type: 'success', label: '已结束' },
  'project-3': { type: 'danger', label: '已关闭' },
  // credit audit
  'credit-0': { type: 'warning', label: '待审核' },
  'credit-1': { type: 'success', label: '已通过' },
  'credit-2': { type: 'danger', label: '已驳回' },
  'credit-3': { type: 'default', label: '已撤销' },
  // graduate
  'graduate-0': { type: 'warning', label: '待审核' },
  'graduate-1': { type: 'primary', label: '教员审核中' },
  'graduate-2': { type: 'primary', label: '领导审核中' },
  'graduate-3': { type: 'success', label: '已毕业' },
  'graduate-4': { type: 'danger', label: '已驳回' },
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
