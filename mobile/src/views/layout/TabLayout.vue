<template>
  <div class="tab-layout">
    <router-view />
    <van-tabbar :model-value="activeIndex" safe-area-inset-bottom>
      <van-tabbar-item
        v-for="tab in tabs"
        :key="tab.key"
        :icon="tab.icon"
        :badge="badgeOf(tab)"
        @click="go(tab.route)"
      >
        {{ tab.label }}
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tabbar as VanTabbar, TabbarItem as VanTabbarItem } from 'vant'
import { useUserStore } from '@/stores/user'
import { useTodoStore } from '@/stores/todo'
import type { TabDef } from '@/config/nav'

const route = useRoute()
const router = useRouter()
const tabs = computed(() => useUserStore().tabs)
const todoStore = useTodoStore()

const activeIndex = computed(() => {
  const index = tabs.value.findIndex((t) => t.route === route.path)
  return index < 0 ? 0 : index
})

function badgeOf(tab: TabDef) {
  const count = tab.badge === 'approval' ? todoStore.approvalCount : tab.badge === 'unread' ? todoStore.unreadCount : 0
  if (count <= 0) return ''
  return String(count > 99 ? '99+' : count)
}

function go(path: string) {
  if (route.path !== path) router.push(path)
}

onMounted(() => todoStore.refresh())
// 只在回到与角标相关的页面时重算，避免每次切换 tab 都打待办接口
watch(
  () => route.path,
  (path) => {
    if (path === '/' || path === '/approvals' || path === '/notifications') todoStore.refresh()
  }
)
</script>

<style scoped>
.tab-layout {
  min-height: 100vh;
  min-height: 100dvh;
  background: var(--bg);
}
.tab-layout :deep(.van-tabbar-item) {
  font-weight: 500;
}
</style>
