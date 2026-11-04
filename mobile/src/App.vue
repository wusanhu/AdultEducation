<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 权限可能在后台被调整，启动时用服务端最新 perms 覆盖本地缓存
onMounted(() => {
  if (userStore.isLoggedIn) userStore.refreshMe()
})
</script>
