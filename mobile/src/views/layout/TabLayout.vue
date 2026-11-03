<template>
  <div class="tab-layout">
    <router-view />
    <van-tabbar :model-value="active">
      <van-tabbar-item icon="home-o" @click="go('/')">首页</van-tabbar-item>
      <van-tabbar-item icon="orders-o" @click="go('/projects')">培训班</van-tabbar-item>
      <van-tabbar-item icon="gold-coin-o" @click="go('/credits')">学分</van-tabbar-item>
      <van-tabbar-item icon="contact" @click="go('/profile')">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tabbar as VanTabbar, TabbarItem as VanTabbarItem } from 'vant'

const route = useRoute()
const router = useRouter()

const pathIndexMap: Record<string, number> = {
  '/': 0,
  '/projects': 1,
  '/credits': 2,
  '/profile': 3,
}
const active = computed(() => pathIndexMap[route.path] ?? 0)

function go(path: string) {
  if (route.path !== path) router.push(path)
}
</script>

<style scoped>
.tab-layout {
  min-height: 100vh;
  padding-bottom: 50px;
}
</style>
