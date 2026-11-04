import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { auth as authApi } from '@/api'
import { buildCaps, buildEntries, buildTabs } from '@/config/nav'
import type { LoginResult, UserInfo } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('mobile_token') || '')
  const user = ref<UserInfo | null>(
    localStorage.getItem('mobile_user')
      ? JSON.parse(localStorage.getItem('mobile_user')!)
      : null
  )
  const mustChangePwd = ref(localStorage.getItem('mobile_must_change') === '1')

  const isLoggedIn = computed(() => !!token.value)
  const caps = computed(() => buildCaps(user.value?.perms))
  const tabs = computed(() => buildTabs(caps.value))
  const entries = computed(() => buildEntries(caps.value))

  function hasPerm(perm: string) {
    return !!user.value?.perms?.includes(perm)
  }

  function setLogin(data: LoginResult) {
    token.value = data.token
    mustChangePwd.value = !!data.mustChangePwd
    localStorage.setItem('mobile_must_change', mustChangePwd.value ? '1' : '0')
    user.value = {
      id: data.user?.id,
      username: data.user?.username,
      realName: data.user?.realName,
      phone: data.user?.phone,
      avatar: data.user?.avatar,
      roleCode: data.roleCode,
      roleName: data.roleName,
      perms: data.perms,
      orgName: data.user?.orgName,
    }
    localStorage.setItem('mobile_token', data.token)
    localStorage.setItem('mobile_user', JSON.stringify(user.value))
  }

  function clearUser() {
    token.value = ''
    user.value = null
    mustChangePwd.value = false
    localStorage.removeItem('mobile_token')
    localStorage.removeItem('mobile_user')
    localStorage.removeItem('mobile_must_change')
  }

  async function refreshMe() {
    try {
      const data = await authApi.me()
      if (data?.user) {
        user.value = {
          ...user.value,
          id: data.user.id,
          username: data.user.username,
          realName: data.user.realName,
          phone: data.user.phone,
          avatar: data.user.avatar,
          roleCode: data.roleCode || user.value?.roleCode,
          roleName: data.roleName || user.value?.roleName,
          perms: data.perms || user.value?.perms,
          orgName: data.user.orgName,
        }
        localStorage.setItem('mobile_user', JSON.stringify(user.value))
      }
      if (data?.mustChangePwd !== undefined) {
        mustChangePwd.value = !!data.mustChangePwd
        localStorage.setItem('mobile_must_change', mustChangePwd.value ? '1' : '0')
      }
    } catch {
      // ignore
    }
  }

  return {
    token,
    user,
    mustChangePwd,
    isLoggedIn,
    caps,
    tabs,
    entries,
    hasPerm,
    setLogin,
    clearUser,
    refreshMe,
  }
})
