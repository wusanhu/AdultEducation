<template>
  <a-layout style="min-height:100vh">
    <a-layout-sider
      v-model:collapsed="collapsed"
      :width="236"
      :collapsed-width="72"
      collapsible
      :trigger="null"
      class="admin-sider"
    >
      <div class="sider-brand" :class="{ collapsed }">
        <div class="brand-logo">继</div>
        <div v-if="!collapsed" class="brand-text">
          <div class="brand-line">
            <span class="brand-name">继续教育平台</span>
            <span class="brand-version">2.0</span>
          </div>
          <div class="brand-sub">ADULT EDUCATION</div>
        </div>
      </div>

      <nav class="sider-nav">
        <div v-for="(group, index) in navGroups" :key="`${group.title}-${index}`" class="nav-group">
          <div v-if="!collapsed" class="nav-group-title">{{ group.title }}</div>
          <div v-else-if="index > 0" class="nav-group-line" />
          <a-tooltip
            v-for="item in group.items"
            :key="item.key"
            placement="right"
            :title="collapsed ? item.label : ''"
          >
            <div class="nav-item" :class="{ 'is-active': activeKey === item.key }" @click="go(item.key)">
              <component :is="item.icon" class="nav-icon" />
              <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
              <span v-if="!collapsed && activeKey === item.key" class="nav-dot" />
            </div>
          </a-tooltip>
        </div>
      </nav>

      <div class="sider-footer">
        <div class="quick-links" :class="{ collapsed }">
          <a-tooltip v-for="link in quickLinks" :key="link.key" placement="right" :title="collapsed ? link.label : ''">
            <button type="button" class="quick-link" @click="link.action()">
              <component :is="link.icon" />
              <span v-if="!collapsed">{{ link.label }}</span>
            </button>
          </a-tooltip>
        </div>
        <a-tooltip placement="right" :title="collapsed ? auth.displayName + ' · ' + (auth.roleName || '用户') : ''">
          <div class="user-card" :class="{ collapsed }">
            <a-avatar :src="auth.user?.avatar || undefined" :size="34" class="user-avatar">{{ initial }}</a-avatar>
            <div v-if="!collapsed" class="user-meta">
              <div class="user-name">{{ auth.displayName }}</div>
              <div class="user-role">{{ auth.roleName || '用户' }}</div>
            </div>
            <span v-if="!collapsed" class="online-dot" title="在线" />
          </div>
        </a-tooltip>
      </div>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="admin-header">
        <a-button type="text" class="collapse-btn" @click="toggleCollapsed">
          <MenuUnfoldOutlined v-if="collapsed" />
          <MenuFoldOutlined v-else />
        </a-button>
        <div class="header-divider" />
        <div class="header-title-box">
          <div class="header-title">{{ route.meta.title || '工作台' }}</div>
          <div class="header-crumb">继续教育学分管理平台</div>
        </div>
        <div style="flex:1" />
        <a-popover v-model:open="notifyOpen" placement="bottomRight" :arrow="false" :overlay-style="{ width: '380px' }">
          <template #content>
            <div style="padding:4px 4px 0;display:flex;align-items:center;justify-content:space-between">
              <b style="font-size:14px">消息通知</b>
              <a-button type="link" size="small" @click="readAll">全部已读</a-button>
            </div>
            <a-spin :spinning="notifyLoading">
              <div style="max-height:360px;overflow:auto;margin-top:4px">
                <div v-if="notifications.length === 0" style="padding:36px 0;text-align:center;color:#94a3b8">暂无通知</div>
                <div v-for="item in notifications" :key="Number(item.id)" style="padding:12px 8px;border-bottom:1px solid #f1f5f9;cursor:pointer" @click="readOne(item)">
                  <div style="display:flex;gap:8px;align-items:flex-start">
                    <span :style="{ width: '7px', height: '7px', borderRadius: '50%', marginTop: '7px', background: item.isRead === 0 ? '#2563eb' : '#cbd5e1', flex: 'none' }"></span>
                    <div style="min-width:0">
                      <div style="font-size:13px;font-weight:600;color:#1e293b">{{ item.title }}</div>
                      <div style="font-size:12px;color:#64748b;line-height:1.6;margin-top:3px">{{ item.content }}</div>
                      <div style="font-size:11px;color:#94a3b8;margin-top:4px">{{ item.createTime }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </a-spin>
          </template>
          <a-badge :count="unread" :offset="[-4, 6]" size="small">
            <a-button type="text" shape="circle" class="header-icon-btn"><BellOutlined /></a-button>
          </a-badge>
        </a-popover>
      </a-layout-header>

      <a-layout-content style="padding:20px 22px 30px">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>

    <a-modal v-model:open="pwdOpen" title="修改登录密码" :width="420" :confirm-loading="pwdLoading" @ok="submitPwd">
      <a-form layout="vertical" style="margin-top:16px">
        <a-form-item label="原密码" required><a-input-password v-model:value="pwd.oldPassword" /></a-form-item>
        <a-form-item label="新密码" required><a-input-password v-model:value="pwd.newPassword" placeholder="8-32 位，包含字母和数字" /></a-form-item>
        <a-form-item label="确认新密码" required><a-input-password v-model:value="pwd.confirm" /></a-form-item>
      </a-form>
    </a-modal>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ApartmentOutlined, AppstoreOutlined, AreaChartOutlined, AuditOutlined, BarChartOutlined, BellOutlined,
  BookOutlined, CameraOutlined, ControlOutlined, CreditCardOutlined, DashboardOutlined,
  FileDoneOutlined, FileTextOutlined, FolderOpenOutlined, KeyOutlined, LineChartOutlined, LockOutlined,
  LoginOutlined, LogoutOutlined, MenuFoldOutlined, MenuOutlined, MenuUnfoldOutlined, MonitorOutlined,
  PieChartOutlined, PlusSquareOutlined, ProfileOutlined, ReadOutlined, SafetyCertificateOutlined,
  SettingOutlined, TeamOutlined, UserOutlined, VideoCameraOutlined, WarningOutlined
} from '@ant-design/icons-vue'
import { authApi, notificationApi } from '../api'
import { useAuthStore } from '../store/auth'
import type { MenuNode } from '../types'

interface NotifyRow {
  id: number
  title: string
  content: string
  type?: string
  isRead: number
  createTime: string
}

interface NavItem {
  key: string
  label: string
  icon: Component
}

interface NavGroup {
  title: string
  items: NavItem[]
}

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(localStorage.getItem('admin_sider_collapsed') === '1')
const notifyOpen = ref(false)
const notifyLoading = ref(false)
const notifications = ref<NotifyRow[]>([])
const unread = ref(0)
const pwdOpen = ref(false)
const pwdLoading = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const iconMap: Record<string, Component> = {
  DashboardOutlined, ReadOutlined, VideoCameraOutlined, MonitorOutlined, CameraOutlined, WarningOutlined,
  ControlOutlined, CreditCardOutlined, SafetyCertificateOutlined, BarChartOutlined, SettingOutlined,
  TeamOutlined, ApartmentOutlined, UserOutlined, KeyOutlined, MenuOutlined, PieChartOutlined,
  LineChartOutlined, AreaChartOutlined, ProfileOutlined, PlusSquareOutlined, AuditOutlined,
  FileDoneOutlined, FolderOpenOutlined, LoginOutlined, FileTextOutlined, BookOutlined
}

function resolveIcon(name?: string): Component {
  return (name && iconMap[name]) || AppstoreOutlined
}

function toNavGroups(nodes: MenuNode[]): NavGroup[] {
  const groups: NavGroup[] = []
  for (const node of nodes) {
    if (node.hidden === 1 || node.menuType === 3) continue
    if (node.menuType === 2 && node.path) {
      let group = groups.find((g) => g.title === '概览')
      if (!group) {
        group = { title: '概览', items: [] }
        groups.push(group)
      }
      group.items.push({ key: node.path, label: node.menuName, icon: resolveIcon(node.icon) })
      continue
    }
    if (node.menuType === 1) {
      const items = (node.children || [])
        .filter((child) => child.menuType === 2 && child.hidden !== 1 && child.path)
        .map((child) => ({ key: child.path as string, label: child.menuName, icon: resolveIcon(child.icon) }))
      if (items.length) groups.push({ title: node.menuName, items })
    }
  }
  return groups
}

const navGroups = computed(() => toNavGroups(auth.menus || []))
const activeKey = computed(() => {
  const path = route.path
  let matched = path
  let matchedLength = 0
  for (const group of navGroups.value) {
    for (const item of group.items) {
      if ((path === item.key || path.startsWith(`${item.key}/`)) && item.key.length > matchedLength) {
        matched = item.key
        matchedLength = item.key.length
      }
    }
  }
  return matched
})
const initial = computed(() => auth.displayName.slice(0, 1))

const quickLinks: { key: string; label: string; icon: Component; action: () => void }[] = [
  { key: 'profile', label: '个人中心', icon: UserOutlined, action: () => router.push('/profile') },
  { key: 'password', label: '修改密码', icon: LockOutlined, action: () => { pwdOpen.value = true } },
  { key: 'logout', label: '退出登录', icon: LogoutOutlined, action: () => { void doLogout() } }
]

function go(path: string) {
  if (path.startsWith('/')) router.push(path)
}

function toggleCollapsed() {
  collapsed.value = !collapsed.value
  localStorage.setItem('admin_sider_collapsed', collapsed.value ? '1' : '0')
}

async function loadUnread() {
  try { unread.value = await notificationApi.unread() } catch { unread.value = 0 }
}

async function loadNotifications() {
  notifyLoading.value = true
  try {
    const page = await notificationApi.page({ pageNum: 1, pageSize: 10 })
    notifications.value = page.records as unknown as NotifyRow[]
    unread.value = await notificationApi.unread()
  } catch (e) {
    message.error((e as Error).message)
  } finally {
    notifyLoading.value = false
  }
}

watch(notifyOpen, (open) => { if (open) loadNotifications() })

async function readOne(item: NotifyRow) {
  if (item.isRead === 0) {
    await notificationApi.read(item.id)
    item.isRead = 1
    await loadUnread()
  }
}

async function readAll() {
  await notificationApi.readAll()
  notifications.value.forEach((n) => { n.isRead = 1 })
  unread.value = 0
}

async function doLogout() {
  await auth.logout()
  router.replace('/login')
}

async function submitPwd() {
  if (!pwd.oldPassword || !pwd.newPassword) {
    message.warning('请填写完整')
    return
  }
  if (pwd.newPassword !== pwd.confirm) {
    message.warning('两次输入的新密码不一致')
    return
  }
  pwdLoading.value = true
  try {
    await authApi.changePassword(pwd.oldPassword, pwd.newPassword)
    message.success('密码修改成功，请重新登录')
    pwdOpen.value = false
    await auth.logout()
    router.replace('/login')
  } catch (e) {
    message.error((e as Error).message)
  } finally {
    pwdLoading.value = false
  }
}

onMounted(loadUnread)
</script>

<style scoped>
.admin-sider {
  height: 100vh;
  position: sticky;
  top: 0;
  overflow: hidden;
  background: #f7f9fc !important;
  border-right: 1px solid #e8eef7;
}
.admin-sider :deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.sider-brand {
  flex: none;
  height: 66px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #e8eef7;
  overflow: hidden;
}
.sider-brand.collapsed {
  justify-content: center;
  padding: 0;
}
.brand-logo {
  flex: none;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #6d5efc);
  box-shadow: 0 4px 10px rgba(37, 99, 235, .28);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}
.brand-text { min-width: 0; }
.brand-line { display: flex; align-items: center; gap: 6px; }
.brand-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: .2px;
  white-space: nowrap;
}
.brand-version {
  flex: none;
  font-size: 10px;
  font-weight: 600;
  color: #4f46e5;
  background: #eef2ff;
  border: 1px solid #e0e7ff;
  border-radius: 6px;
  padding: 0 5px;
  line-height: 16px;
}
.brand-sub {
  margin-top: 3px;
  font-size: 9px;
  font-weight: 500;
  letter-spacing: .12em;
  color: #94a3b8;
  white-space: nowrap;
}
.sider-nav {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 12px 10px 16px;
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, .35) transparent;
}
.sider-nav::-webkit-scrollbar { width: 5px; }
.sider-nav::-webkit-scrollbar-thumb { background: rgba(148, 163, 184, .32); border-radius: 3px; }
.sider-nav::-webkit-scrollbar-track { background: transparent; }
.nav-group + .nav-group { margin-top: 15px; }
.nav-group-title {
  padding: 0 10px 7px;
  font-size: 11px;
  font-weight: 500;
  color: #94a3b8;
  letter-spacing: .05em;
}
.nav-group-line {
  height: 1px;
  margin: 8px 10px 10px;
  background: #e8eef7;
}
.nav-item {
  position: relative;
  height: 38px;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 2px;
  padding: 0 10px;
  border-radius: 8px;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  user-select: none;
  transition: background .15s ease, color .15s ease;
}
.nav-item:hover { background: #eef2f7; color: #0f172a; }
.nav-item:hover .nav-icon { color: #334155; }
.nav-item.is-active {
  background: #e8f1ff;
  color: #1d4ed8;
  font-weight: 600;
}
.nav-item.is-active .nav-icon { color: #2563eb; }
.nav-icon {
  flex: none;
  font-size: 16px;
  color: #64748b;
  transition: color .15s ease;
}
.nav-label {
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.nav-dot {
  flex: none;
  width: 6px;
  height: 6px;
  margin-left: auto;
  border-radius: 50%;
  background: #2563eb;
}
.sider-footer {
  flex: none;
  padding: 9px 10px 10px;
  background: #fff;
  border-top: 1px solid #e8eef7;
}
.quick-links {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
}
.quick-links.collapsed {
  flex-direction: column;
  align-items: center;
  gap: 3px;
}
.quick-link {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 6px 2px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #64748b;
  font-size: 11px;
  white-space: nowrap;
  cursor: pointer;
  transition: background .15s ease, color .15s ease;
}
.quick-link:hover { background: #eef2f7; color: #1d4ed8; }
.quick-link :deep(.anticon) { font-size: 14px; }
.quick-links.collapsed .quick-link {
  flex: none;
  width: 40px;
  height: 32px;
}
.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 8px 9px;
  border: 1px solid #e8eef7;
  border-radius: 10px;
  background: #f7f9fc;
}
.user-card.collapsed {
  justify-content: center;
  padding: 6px;
  border-color: transparent;
  background: transparent;
}
.user-avatar {
  flex: none;
  background: linear-gradient(135deg, #2563eb, #6d5efc) !important;
  font-size: 14px;
  font-weight: 600;
}
.user-meta { flex: 1; min-width: 0; }
.user-name {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.user-role {
  font-size: 11px;
  line-height: 1.4;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.online-dot {
  flex: none;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 2px #dcfce7;
}
.admin-sider :deep(.ant-layout-sider-collapsed) .nav-item,
.admin-sider.ant-layout-sider-collapsed .nav-item {
  justify-content: center;
  padding: 0;
}
.admin-header {
  height: 64px;
  line-height: normal;
  padding: 0 20px;
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e8eef7;
  position: sticky;
  top: 0;
  z-index: 20;
}
.collapse-btn {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  color: #475569;
  font-size: 17px;
}
.collapse-btn:hover { background: #f1f5f9 !important; }
.header-divider {
  width: 1px;
  height: 24px;
  margin: 0 14px 0 10px;
  background: #e8eef7;
}
.header-title { font-size: 16px; font-weight: 650; line-height: 1.25; color: #0f172a; }
.header-crumb { margin-top: 2px; font-size: 11px; color: #94a3b8; }
.header-icon-btn {
  color: #475569;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.header-icon-btn:hover { background: #f1f5f9 !important; }
.fade-enter-active, .fade-leave-active { transition: opacity .16s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
