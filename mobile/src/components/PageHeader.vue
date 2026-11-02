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
  background: '#fff',
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
  font-weight: 600;
}
</style>
