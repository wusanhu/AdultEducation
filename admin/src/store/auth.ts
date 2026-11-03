import { defineStore } from 'pinia'
import { authApi } from '../api'
import type { LoginResult, MenuNode, UserInfo } from '../types'

interface AuthState {
  token: string
  user: UserInfo | null
  perms: string[]
  menus: MenuNode[]
  roleCode: string
  roleName: string
  mustChangePwd: boolean
  loaded: boolean
}

function readJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
  }
}

// 权限标识可能来自按钮节点，隐藏按钮（hidden=1）即使已授权也视为不可用
function findMenuByPerm(nodes: MenuNode[], perm: string): MenuNode | undefined {
  for (const node of nodes) {
    if (node.menuType === 3 && node.perms === perm) return node
    if (node.children?.length) {
      const found = findMenuByPerm(node.children, perm)
      if (found) return found
    }
  }
  return undefined
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('admin_token') || '',
    user: readJson<UserInfo | null>('admin_user', null),
    perms: readJson<string[]>('admin_perms', []),
    menus: readJson<MenuNode[]>('admin_menus', []),
    roleCode: localStorage.getItem('admin_role_code') || '',
    roleName: localStorage.getItem('admin_role_name') || '',
    mustChangePwd: localStorage.getItem('admin_must_change') === '1',
    loaded: false
  }),
  getters: {
    isLogin: (state) => Boolean(state.token),
    displayName: (state) => state.user?.realName || state.user?.username || '用户'
  },
  actions: {
    apply(result: LoginResult) {
      this.user = result.user
      this.perms = result.perms || []
      this.menus = result.menus || []
      this.roleCode = result.roleCode || result.user?.roleCode || ''
      this.roleName = result.roleName || result.user?.roleName || ''
      this.mustChangePwd = Boolean(result.mustChangePwd || result.user?.mustChangePwd)
      localStorage.setItem('admin_user', JSON.stringify(this.user))
      localStorage.setItem('admin_perms', JSON.stringify(this.perms))
      localStorage.setItem('admin_menus', JSON.stringify(this.menus))
      localStorage.setItem('admin_role_code', this.roleCode)
      localStorage.setItem('admin_role_name', this.roleName)
      localStorage.setItem('admin_must_change', this.mustChangePwd ? '1' : '0')
    },
    async login(username: string, password: string) {
      const result = await authApi.login(username, password)
      this.token = result.token
      localStorage.setItem('admin_token', result.token)
      this.apply(result)
      this.loaded = true
    },
    async refresh() {
      if (!this.token) return
      const result = await authApi.me()
      this.apply(result)
      this.loaded = true
    },
    hasPerm(perm?: string) {
      if (!perm) return true
      if (!this.perms.includes(perm)) return false
      const node = findMenuByPerm(this.menus, perm)
      return !node || node.hidden !== 1
    },
    async logout() {
      try {
        await authApi.logout()
      } catch {
        // ignore
      }
      this.clear()
    },
    clear() {
      this.token = ''
      this.user = null
      this.perms = []
      this.menus = []
      this.roleCode = ''
      this.roleName = ''
      this.mustChangePwd = false
      this.loaded = false
      ;['admin_token', 'admin_user', 'admin_perms', 'admin_menus', 'admin_role_code', 'admin_role_name', 'admin_must_change']
        .forEach((key) => localStorage.removeItem(key))
    }
  }
})
