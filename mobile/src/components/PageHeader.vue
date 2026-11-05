<template>
  <div class="page-header" :style="{ background }">
    <van-nav-bar
      :title="title"
      :left-arrow="leftArrow"
      :fixed="fixed"
      :placeholder="fixed"
      safe-area-inset-top
      @click-left="onClickLeft"
    >
      <template v-if="$slots.right" #right>
        <slot name="right" />
      </template>
    </van-nav-bar>
  </div>
</template>

<script setup lang="ts">
import { NavBar as VanNavBar } from 'vant'
import { useRouter } from 'vue-router'

const props = withDefaults(defineProps<{
  title?: string
  leftArrow?: boolean
  fixed?: boolean
  background?: string
}>(), {
  title: '',
  leftArrow: true,
  fixed: true,
  background: 'var(--surface)',
})

const router = useRouter()
function onClickLeft() {
  if (router.options.history.state.back) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<style scoped>
.page-header :deep(.van-nav-bar) {
  border-bottom: 1px solid var(--line);
}

.page-header :deep(.van-nav-bar__title) {
  font-family: var(--font-serif);
  font-weight: 600;
  letter-spacing: 0.02em;
}

.page-header :deep(.van-nav-bar__left) {
  padding-left: 12px;
}

/* 返回键做成 36px 的圆形触点，兼顾点击面积与视觉重量 */
.page-header :deep(.van-nav-bar__left .van-icon) {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--surface-2);
  font-size: 16px;
  color: var(--ink-2);
  transition: background-color 0.18s ease;
}

.page-header :deep(.van-nav-bar__left .van-icon:active) {
  background: var(--brand-tint);
  border-color: var(--brand-line);
}

.page-header :deep(.van-nav-bar__right) {
  padding-right: 12px;
}
</style>
