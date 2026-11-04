<template>
  <div class="admin-shell">
    <header class="app-topbar">
      <div class="topbar-brand" :class="{ collapsed }">
        <div class="brand-logo">继</div>
        <div v-if="!collapsed" class="brand-text">
          <div class="brand-line">
            <span class="brand-name">继续教育平台</span>
            <span class="brand-version">2.0</span>
          </div>
          <div class="brand-sub">ADULT EDUCATION</div>
        </div>
      </div>

      <a-tooltip :title="collapsed ? '展开菜单' : '收起菜单'">
        <button type="button" class="topbar-toggle" @click="toggleCollapsed">
          <MenuUnfoldOutlined v-if="collapsed" />
          <MenuFoldOutlined v-else />
        </button>
      </a-tooltip>
      <div class="topbar-divider" />

      <div class="topbar-titles">
        <div class="topbar-title">{{ route.meta.title || '工作台' }}</div>
        <div class="topbar-breadcrumb">
          <span>管理控制台</span>
          <template v-if="routeGroup">
            <span class="sep">/</span>
            <span>{{ routeGroup.title }}</span>
          </template>
          <span class="sep">/</span>
          <span class="current">{{ route.meta.title || '工作台' }}</span>
        </div>
      </div>

      <div class="topbar-spacer" />

      <a-popover v-model:open="notifyOpen" placement="bottomRight" :arrow="false" :overlay-style="{ width: '380px' }">
        <template #content>
          <div style="padding:4px 4px 0;display:flex;align-items:center;justify-content:space-between">
            <b style="font-size:14px">消息通知</b>
            <a-button type="link" size="small" @click="readAll">全部已读</a-button>
          </div>
          <a-spin :spinning="notifyLoading">
            <div style="max-height:360px;overflow:auto;margin-top:4px">
              <div v-if="notifications.length === 0" style="padding:36px 0;text-align:center;color:#94a3b8">暂无通知</div>
              <div
                v-for="item in notifications"
                :key="Number(item.id)"
                class="notify-item"
                :class="{ 'notify-item-link': !!item.adminLink }"
                @click="readOne(item)"
              >
                <div style="display:flex;gap:8px;align-items:flex-start">
                  <span :style="{ width: '7px', height: '7px', borderRadius: '50%', marginTop: '7px', background: item.isRead === 0 ? '#2563eb' : '#cbd5e1', flex: 'none' }"></span>
                  <div style="min-width:0">
                    <div style="display:flex;gap:6px;align-items:center">
                      <a-tag :color="notifyCategory(item.type).color" style="margin-inline-end:0">{{ notifyCategory(item.type).label }}</a-tag>
                      <span style="font-size:13px;font-weight:600;color:#1e293b">{{ item.title }}</span>
                    </div>
                    <div style="font-size:12px;color:#64748b;line-height:1.6;margin-top:3px">{{ item.content }}</div>
                    <div style="font-size:11px;color:#94a3b8;margin-top:4px">{{ item.createTime }}</div>
                  </div>
                </div>
              </div>
            </div>
          </a-spin>
        </template>
        <a-badge :count="unread" :offset="[-4, 6]" size="small">
          <button type="button" class="topbar-icon-btn"><BellOutlined /></button>
        </a-badge>
      </a-popover>
    </header>

    <div class="admin-body">
      <aside class="app-sider" :class="{ collapsed }">
        <div class="nav-stage">
          <transition :name="transitionName">
            <div v-if="navLevel === 0" key="root" class="nav-panel">
              <div v-if="!collapsed" class="panel-head">
                <div class="panel-title">全部功能</div>
                <div class="panel-sub">共 {{ rootEntries.length }} 个入口</div>
              </div>
              <div class="root-list">
                <a-tooltip v-for="entry in rootEntries" :key="entry.key" placement="right" :title="collapsed ? entry.label : ''">
                  <div
                    class="root-item"
                    :class="{ 'is-active': isRootActive(entry), 'is-group': Boolean(entry.group) }"
                    @click="onRootClick(entry)"
                  >
                    <span class="root-icon"><component :is="entry.icon" /></span>
                    <span v-if="!collapsed" class="root-text">
                      <span class="root-label">{{ entry.label }}</span>
                      <span class="root-desc">{{ entry.description }}</span>
                    </span>
                    <span v-if="!collapsed && isRootActive(entry)" class="nav-dot" />
                    <RightOutlined v-if="!collapsed && entry.group" class="root-chev" />
                  </div>
                </a-tooltip>
              </div>
            </div>

            <div v-else-if="activeGroup" key="group" class="nav-panel">
              <div class="group-head">
                <a-tooltip title="返回全部功能" placement="right">
                  <button type="button" class="back-btn" @click="backToRoot">
                    <ArrowLeftOutlined />
                  </button>
                </a-tooltip>
                <div v-if="!collapsed" class="group-head-text">
                  <div class="group-head-title">{{ activeGroup.title }}</div>
                  <div class="group-head-sub">{{ activeGroup.items.length }} 个功能</div>
                </div>
              </div>
              <div class="child-list">
                <a-tooltip v-for="item in activeGroup.items" :key="item.key" placement="right" :title="collapsed ? item.label : ''">
                  <div class="child-item" :class="{ 'is-active': activeKey === item.key }" @click="go(item.key)">
                    <component :is="item.icon" class="child-icon" />
                    <span v-if="!collapsed" class="child-label">{{ item.label }}</span>
                    <span v-if="!collapsed && activeKey === item.key" class="nav-dot" />
                  </div>
                </a-tooltip>
              </div>
            </div>
          </transition>
        </div>

        <div class="sider-footer">
          <div class="footer-links">
            <a-tooltip v-for="link in quickLinks" :key="link.key" placement="right" :title="collapsed ? link.label : ''">
              <button type="button" class="footer-link" :class="{ danger: link.key === 'logout' }" @click="link.action()">
                <component :is="link.icon" class="footer-link-icon" />
                <span v-if="!collapsed" class="footer-link-label">{{ link.label }}</span>
                <RightOutlined v-if="!collapsed" class="footer-link-arrow" />
              </button>
            </a-tooltip>
          </div>

          <div class="footer-user">
            <a-tooltip placement="right" :title="collapsed ? auth.displayName + ' · ' + (auth.roleName || '用户') : ''">
              <a-avatar :src="auth.user?.avatar || undefined" :size="38" class="user-avatar">{{ initial }}</a-avatar>
            </a-tooltip>
            <div v-if="!collapsed" class="footer-user-meta">
              <div class="footer-user-name">{{ auth.displayName }}</div>
              <div class="footer-user-role">
                <span class="online-dot" />{{ auth.roleName || '用户' }} · 在线
              </div>
            </div>
          </div>
        </div>
      </aside>

      <main class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <a-modal v-model:open="pwdOpen" title="修改登录密码" :width="420" :confirm-loading="pwdLoading" @ok="submitPwd">
      <a-form layout="vertical" style="margin-top:16px">
        <a-form-item label="原密码" required><a-input-password v-model:value="pwd.oldPassword" /></a-form-item>
        <a-form-item label="新密码" required><a-input-password v-model:value="pwd.newPassword" placeholder="8-32 位，包含字母和数字" /></a-form-item>
        <a-form-item label="确认新密码" required><a-input-password v-model:value="pwd.confirm" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ApartmentOutlined, AppstoreOutlined, AreaChartOutlined, ArrowLeftOutlined, AuditOutlined,
  BarChartOutlined, BellOutlined, BookOutlined, CameraOutlined,
  ControlOutlined, CreditCardOutlined, DashboardOutlined, FileDoneOutlined, FileTextOutlined,
  FolderOpenOutlined, KeyOutlined, LineChartOutlined, LockOutlined, LoginOutlined,
  LogoutOutlined, MenuFoldOutlined, MenuOutlined, MenuUnfoldOutlined, MonitorOutlined,
  PieChartOutlined, PlusSquareOutlined, ProfileOutlined, ReadOutlined, RightOutlined,
  SafetyCertificateOutlined, SettingOutlined, TeamOutlined, UserOutlined, VideoCameraOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'
import { authApi, notificationApi } from '../api'
import { notifyCategory } from '../constants/notify'
import { useAuthStore } from '../store/auth'
import type { MenuNode } from '../types'

interface NotifyRow {
  id: number
  title: string
  content: string
  type?: string
  adminLink?: string
  isRead: number
  createTime: string
}

interface NavItem {
  key: string
  label: string
  icon: Component
}

interface NavGroup {
  key: string
  title: string
  icon: Component
  items: NavItem[]
}

interface RootEntry {
  key: string
  label: string
  description: string
  icon: Component
  group: NavGroup | null
}

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(localStorage.getItem('admin_sider_collapsed') === '1')
const navLevel = ref(0)
const activeGroupKey = ref('')
const transitionName = ref<'nav-forward' | 'nav-back'>('nav-forward')
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
  FileDoneOutlined, FolderOpenOutlined, LoginOutlined, FileTextOutlined, BookOutlined, BellOutlined
}

function resolveIcon(name?: string): Component {
  return (name && iconMap[name]) || AppstoreOutlined
}

const MODULE_DESCRIPTIONS: Record<string, string> = {
  '工作台': '数据总览 · 待办提醒 · 学习趋势',
  '培训管理': '培训班 · 课程 · 学习监控',
  '学分管理': '学分台账 · 补录 · 审批',
  '毕业审核': '毕业申请 · 审核 · 电子档案',
  '统计报表': '单位 · 学分 · 课程报表',
  '系统管理': '组织 · 账号 · 权限与配置'
}

function moduleDescription(title: string, items: NavItem[]): string {
  return MODULE_DESCRIPTIONS[title] || items.slice(0, 3).map((item) => item.label).join(' · ')
}

function buildNav(nodes: MenuNode[]): { entries: RootEntry[]; groups: NavGroup[] } {
  const entries: RootEntry[] = []
  const groups: NavGroup[] = []
  for (const node of nodes) {
    if (node.hidden === 1 || node.menuType === 3) continue
    if (node.menuType === 2 && node.path) {
      entries.push({
        key: node.path,
        label: node.menuName,
        description: MODULE_DESCRIPTIONS[node.menuName] || '',
        icon: resolveIcon(node.icon),
        group: null
      })
      continue
    }
    if (node.menuType === 1) {
      const items = (node.children || [])
        .filter((child) => child.menuType === 2 && child.hidden !== 1 && child.path)
        .map((child) => ({ key: child.path as string, label: child.menuName, icon: resolveIcon(child.icon) }))
      if (!items.length) continue
      const group: NavGroup = {
        key: `group-${node.id}`,
        title: node.menuName,
        icon: resolveIcon(node.icon),
        items
      }
      groups.push(group)
      entries.push({
        key: group.key,
        label: group.title,
        description: moduleDescription(group.title, items),
        icon: group.icon,
        group
      })
    }
  }
  return { entries, groups }
}

const navData = computed(() => buildNav(auth.menus || []))
const rootEntries = computed(() => navData.value.entries)
const navGroups = computed(() => navData.value.groups)

function pathSegment(path: string): string {
  return path.split('/')[1] || ''
}

function groupForPath(path: string): NavGroup | null {
  for (const group of navGroups.value) {
    if (group.items.some((item) => path === item.key || path.startsWith(`${item.key}/`))) return group
  }
  const segment = pathSegment(path)
  if (!segment) return null
  for (const group of navGroups.value) {
    if (group.items.some((item) => pathSegment(item.key) === segment)) return group
  }
  return null
}

const activeKey = computed(() => {
  const path = route.path
  let matched = ''
  let matchedLength = 0
  for (const group of navGroups.value) {
    for (const item of group.items) {
      if ((path === item.key || path.startsWith(`${item.key}/`)) && item.key.length > matchedLength) {
        matched = item.key
        matchedLength = item.key.length
      }
    }
  }
  if (matched) return matched
  const segment = pathSegment(path)
  for (const group of navGroups.value) {
    const fallback = group.items.find((item) => pathSegment(item.key) === segment)
    if (fallback) return fallback.key
  }
  return path
})

const routeGroup = computed(() => groupForPath(route.path))
const activeGroup = computed(() => navGroups.value.find((group) => group.key === activeGroupKey.value) || null)
const initial = computed(() => auth.displayName.slice(0, 1))

const quickLinks: { key: string; label: string; icon: Component; action: () => void }[] = [
  { key: 'profile', label: '个人中心', icon: UserOutlined, action: () => router.push('/profile') },
  { key: 'password', label: '修改密码', icon: LockOutlined, action: () => { pwdOpen.value = true } },
  { key: 'logout', label: '退出登录', icon: LogoutOutlined, action: () => { void doLogout() } }
]

function isRootActive(entry: RootEntry): boolean {
  if (entry.group) return routeGroup.value?.key === entry.group.key
  return activeKey.value === entry.key
}

function onRootClick(entry: RootEntry) {
  if (entry.group) {
    enterGroup(entry.group)
    return
  }
  go(entry.key)
}

function enterGroup(group: NavGroup) {
  if (collapsed.value) setCollapsed(false)
  transitionName.value = 'nav-forward'
  activeGroupKey.value = group.key
  navLevel.value = 1
}

function backToRoot() {
  transitionName.value = 'nav-back'
  navLevel.value = 0
  activeGroupKey.value = ''
}

function go(path: string) {
  if (path.startsWith('/')) router.push(path)
}

function setCollapsed(value: boolean) {
  collapsed.value = value
  localStorage.setItem('admin_sider_collapsed', value ? '1' : '0')
}

function toggleCollapsed() {
  setCollapsed(!collapsed.value)
}

function syncNavToRoute() {
  const group = groupForPath(route.path)
  if (group) {
    if (navLevel.value !== 1 || activeGroupKey.value !== group.key) {
      transitionName.value = 'nav-forward'
      activeGroupKey.value = group.key
      navLevel.value = 1
    }
    return
  }
  if (navLevel.value !== 0) {
    transitionName.value = 'nav-back'
    navLevel.value = 0
    activeGroupKey.value = ''
  }
}

watch([() => route.path, navGroups], syncNavToRoute, { immediate: true })

// 内容区独立滚动后，路由切换需手动回到顶部
watch(() => route.path, () => {
  requestAnimationFrame(() => {
    const content = document.querySelector('.admin-content')
    if (content) content.scrollTop = 0
  })
})

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
  if (item.adminLink) {
    notifyOpen.value = false
    void router.push(item.adminLink)
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
.notify-item {
  padding: 12px 8px;
  border-bottom: 1px solid #f1f5f9;
  cursor: default;
}
.notify-item-link {
  cursor: pointer;
}
.notify-item-link:hover {
  background: #f8fafc;
}
.admin-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--admin-shell-bg, #fafcff);
}
.app-topbar {
  flex: none;
  position: relative;
  z-index: 40;
  height: 64px;
  display: flex;
  align-items: center;
  padding-right: 22px;
  background: var(--admin-shell-bg, #fafcff);
}
.topbar-brand {
  flex: none;
  width: 240px;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 18px;
  overflow: hidden;
  transition: width .25s cubic-bezier(.4, 0, .2, 1), padding .25s ease;
}
.topbar-brand.collapsed {
  width: 76px;
  justify-content: center;
  padding: 0;
}
.brand-logo {
  flex: none;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #6d5efc);
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
  line-height: 16px;
  padding: 0 5px;
  color: #4f46e5;
  background: #eef2ff;
  border: 1px solid #e0e7ff;
  border-radius: 6px;
}
.brand-sub {
  margin-top: 3px;
  font-size: 9px;
  font-weight: 500;
  letter-spacing: .12em;
  color: #94a3b8;
  white-space: nowrap;
}
.topbar-toggle {
  flex: none;
  width: 34px;
  height: 34px;
  margin-left: 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #475569;
  font-size: 17px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background .15s ease, color .15s ease;
}
.topbar-toggle:hover { background: #f1f5f9; color: #1d4ed8; }
.topbar-divider {
  flex: none;
  width: 1px;
  height: 24px;
  margin: 0 16px 0 12px;
  background: #e9eef6;
}
.topbar-titles { min-width: 0; }
.topbar-title {
  font-size: 16px;
  font-weight: 650;
  line-height: 1.25;
  color: #0f172a;
  white-space: nowrap;
}
.topbar-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 3px;
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
}
.topbar-breadcrumb .sep { color: #cbd5e1; }
.topbar-breadcrumb .current { color: #64748b; }
.topbar-spacer { flex: 1; }
.topbar-icon-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: #475569;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background .15s ease, color .15s ease;
}
.topbar-icon-btn:hover { background: #f1f5f9; color: #1d4ed8; }

.admin-body {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: stretch;
  overflow: hidden;
}
.app-sider {
  flex: none;
  width: 240px;
  height: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
  background: var(--admin-shell-bg, #fafcff);
  transition: width .25s cubic-bezier(.4, 0, .2, 1);
  overflow: hidden;
}
.app-sider.collapsed { width: 76px; }
.nav-stage {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
}
.nav-panel {
  position: absolute;
  inset: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 14px 10px 18px;
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, .3) transparent;
}
.nav-panel::-webkit-scrollbar { width: 5px; }
.nav-panel::-webkit-scrollbar-thumb { background: rgba(148, 163, 184, .3); border-radius: 3px; }
.nav-panel::-webkit-scrollbar-track { background: transparent; }
.panel-head { padding: 2px 10px 12px; }
.panel-title {
  font-size: 15px;
  font-weight: 650;
  color: #0f172a;
  line-height: 1.3;
}
.panel-sub {
  margin-top: 3px;
  font-size: 10.5px;
  color: #94a3b8;
  letter-spacing: .02em;
}
.root-list { display: flex; flex-direction: column; gap: 4px; }
.root-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  height: 60px;
  padding: 0 10px;
  border-radius: 10px;
  color: #334155;
  font-size: 13.5px;
  cursor: pointer;
  user-select: none;
  transition: background .16s ease, color .16s ease;
}
.root-item:hover { background: #f1f5f9; color: #0f172a; }
.root-item.is-active { background: #e8f1ff; color: #1d4ed8; font-weight: 600; }
.root-icon {
  flex: none;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background .16s ease, color .16s ease;
}
.root-item:hover .root-icon { background: #e2e8f0; color: #334155; }
.root-item.is-active .root-icon { background: #dbeafe; color: #2563eb; }
.root-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.root-label {
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.root-desc {
  min-width: 0;
  font-size: 10.5px;
  font-weight: 400;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.root-item.is-active .root-desc { color: #60a5fa; }
.root-chev {
  flex: none;
  font-size: 10px;
  color: #cbd5e1;
  transition: color .15s ease, transform .15s ease;
}
.root-item:hover .root-chev { color: #94a3b8; transform: translateX(2px); }
.root-item.is-active .root-chev { color: #60a5fa; }
.nav-dot {
  flex: none;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2563eb;
}
.group-head {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 2px 2px 12px;
  margin-bottom: 8px;
  border-bottom: 1px solid #eef2f7;
}
.back-btn {
  flex: none;
  width: 32px;
  height: 32px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .15s ease;
}
.back-btn:hover {
  color: #1d4ed8;
  border-color: #bfdbfe;
  background: #eff6ff;
}
.group-head-text { min-width: 0; }
.group-head-title {
  font-size: 14px;
  font-weight: 650;
  color: #0f172a;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.group-head-sub { margin-top: 2px; font-size: 10.5px; color: #94a3b8; }
.child-list { display: flex; flex-direction: column; gap: 2px; }
.child-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 10px;
  border-radius: 8px;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  user-select: none;
  transition: background .15s ease, color .15s ease;
}
.child-item:hover { background: #eef2f7; color: #0f172a; }
.child-item:hover .child-icon { color: #334155; }
.child-item.is-active { background: #e8f1ff; color: #1d4ed8; font-weight: 600; }
.child-item.is-active .child-icon { color: #2563eb; }
.child-icon { flex: none; font-size: 15px; color: #64748b; transition: color .15s ease; }
.child-label {
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sider-footer {
  flex: none;
  padding: 10px 12px 14px;
  background: transparent;
  border-top: 1px solid var(--admin-line-strong, #e3eaf4);
}
.footer-links { display: flex; flex-direction: column; gap: 2px; }
.footer-link {
  width: 100%;
  height: 34px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 9px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 12.5px;
  text-align: left;
  cursor: pointer;
  transition: background .15s ease, color .15s ease;
}
.footer-link:hover { background: #f1f5f9; color: #1d4ed8; }
.footer-link.danger:hover { background: #fef2f2; color: #dc2626; }
.footer-link-icon { flex: none; font-size: 14px; }
.footer-link-label { flex: 1; min-width: 0; white-space: nowrap; }
.footer-link-arrow { flex: none; font-size: 9px; color: #cbd5e1; }
.footer-user {
  margin-top: 10px;
  padding-top: 14px;
  border-top: 1px solid var(--admin-line, #eef2f7);
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  text-align: left;
}
.footer-user-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.user-avatar {
  flex: none;
  background: linear-gradient(135deg, #2563eb, #6d5efc) !important;
  font-size: 15px;
  font-weight: 600;
  border: 1px solid rgba(255, 255, 255, .6);
}
.footer-user-name {
  margin-top: 0;
  font-size: 13px;
  font-weight: 650;
  line-height: 1.35;
  color: #0f172a;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.footer-user-role {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 5px;
  margin-top: 0;
  font-size: 10.5px;
  color: #94a3b8;
  white-space: nowrap;
}
.online-dot {
  flex: none;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  border: 2px solid #dcfce7;
  box-sizing: border-box;
}
.app-sider.collapsed .nav-panel { padding: 12px 8px 16px; }
.app-sider.collapsed .root-item {
  justify-content: center;
  gap: 0;
  padding: 0;
  height: 44px;
}
.app-sider.collapsed .child-item {
  justify-content: center;
  gap: 0;
  padding: 0;
  height: 40px;
}
.app-sider.collapsed .group-head {
  justify-content: center;
  gap: 0;
  padding: 2px 0 10px;
}
.app-sider.collapsed .sider-footer { padding: 10px 8px 12px; }
.app-sider.collapsed .footer-link {
  justify-content: center;
  padding: 0;
  height: 34px;
}
.app-sider.collapsed .footer-user {
  padding-top: 12px;
  justify-content: center;
  gap: 0;
}
.admin-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-gutter: stable;
  margin: 0 16px 16px 12px;
  padding: 20px 20px 24px;
  background: var(--admin-canvas-bg, #fff);
  border: 1px solid var(--admin-canvas-border, #e6edf7);
  border-radius: var(--admin-radius-lg, 16px);
}
.nav-forward-enter-active,
.nav-forward-leave-active,
.nav-back-enter-active,
.nav-back-leave-active {
  transition: transform .26s cubic-bezier(.4, 0, .2, 1), opacity .2s ease;
}
.nav-forward-enter-from { transform: translateX(34px); opacity: 0; }
.nav-forward-leave-to { transform: translateX(-22px); opacity: 0; }
.nav-back-enter-from { transform: translateX(-34px); opacity: 0; }
.nav-back-leave-to { transform: translateX(22px); opacity: 0; }
.fade-enter-active, .fade-leave-active { transition: opacity .16s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
