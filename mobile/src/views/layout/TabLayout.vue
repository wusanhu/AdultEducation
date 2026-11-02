<template>
  <div class="tab-layout">
    <router-view />
    <van-tabbar v-model="active" route>
      <van-tabbar-item icon="home-o" to="/">首页</van-tabbar-item>
      <van-tabbar-item icon="orders-o" to="/projects">培训班</van-tabbar-item>
      <van-tabbar-item icon="gold-coin-o" to="/credits">学分</van-tabbar-item>
      <van-tabbar-item icon="contact" to="/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Tabbar as VanTabbar, TabbarItem as VanTabbarItem } from 'vant'
import { useRoute } from 'vue-router'

const active = ref(0)
const route = useRoute()

const pathIndexMap: Record<string, number> = {
  '/': 0,
  '/projects': 1,
  '/credits': 2,
  '/profile': 3,
}

onMounted(() => {
  const path = route.path
  active.value = pathIndexMap[path] ?? 0
})
</script>

<style scoped>
.tab-layout {
  min-height: 100vh;
  padding-bottom: 50px;
}
</style>
