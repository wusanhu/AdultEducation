import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { auth as authApi } from '@/api'

export interface UserInfo {
  id: number
  username: string
  realName: string
  phone?: string
  avatar?: string
  roleCode?: string
  roleName?: string
  perms?: string[]
  orgName?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('mobile_token') || '')
  const user = ref<UserInfo | null>(
    localStorage.getItem('mobile_user')
      ? JSON.parse(localStorage.getItem('mobile_user')!)
      : null
  )
  const mustChangePwd = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const isTeacher = computed(() => ['teacher', 'leader', 'admin'].includes(user.value?.roleCode || ''))
  const isLeader = computed(() => ['leader', 'admin'].includes(user.value?.roleCode || ''))

  function setLogin(data: any) {
    token.value = data.token
    mustChangePwd.value = !!data.mustChangePwd
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
    } catch {
      // ignore
    }
  }

  return { token, user, mustChangePwd, isLoggedIn, isTeacher, isLeader, setLogin, clearUser, refreshMe }
})
