<template>
  <div class="empty-state">
    <van-empty :image="image" :description="description" />
    <slot />
  </div>
</template>

<script setup lang="ts">
import { Empty as VanEmpty } from 'vant'

withDefaults(defineProps<{
  description?: string
  image?: string
}>(), {
  description: '暂无数据',
  image: 'default',
})
</script>

<style scoped>
.empty-state {
  padding: 60px 0;
}
</style>
