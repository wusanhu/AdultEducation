<template>
  <div class="empty-state">
    <div class="empty-mark">
      <span class="empty-ring"></span>
      <van-icon :name="icon" size="24" />
    </div>
    <p class="empty-desc">{{ description }}</p>
    <div v-if="$slots.default" class="empty-action">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Icon as VanIcon } from 'vant'

withDefaults(defineProps<{
  description?: string
  icon?: string
}>(), {
  description: '暂无数据',
  icon: 'notes-o',
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 44px var(--page-x) 40px;
  text-align: center;
}

.empty-mark {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--surface-2);
  border: 1px solid var(--line);
  color: var(--accent);
}

.empty-ring {
  position: absolute;
  inset: -9px;
  border: 1px dashed var(--line-strong);
  border-radius: 50%;
  opacity: 0.7;
}

.empty-desc {
  margin-top: 18px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--ink-3);
}

.empty-action {
  margin-top: var(--sp-4);
  font-size: 13px;
  color: var(--brand);
}
</style>
