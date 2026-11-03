<template>
  <div class="empty-ink">
    <div class="empty-ink-glyph">{{ glyph }}</div>
    <p>{{ description }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  description?: string
  glyph?: string
}>(), {
  description: '暂无数据',
  glyph: '空',
})

const glyph = computed(() => {
  if (props.glyph && props.glyph !== 'default') return props.glyph
  return '空'
})
</script>
