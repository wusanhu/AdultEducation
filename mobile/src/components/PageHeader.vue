<template>
  <div class="page-header" :style="{ background }">
    <van-nav-bar
      :title="title"
      :left-arrow="leftArrow"
      :fixed="fixed"
      :placeholder="fixed"
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
  background: 'rgba(255, 253, 248, 0.94)',
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
.page-header :deep(.van-nav-bar__title) {
  font-family: var(--font-display);
  font-weight: 700;
  letter-spacing: 0.1em;
}
</style>
