<template>
  <a-layout style="min-height:100vh">
    <a-layout-sider v-model:collapsed="collapsed" :width="232" :collapsed-width="68" class="admin-sider" style="background:#0b1728" collapsible :trigger="null">
      <div class="sider-logo">
        <div style="min-width:34px;height:34px;border-radius:9px;background:#2563eb;display:flex;align-items:center;justify-content:center;color:#fff;font-weight:700">继</div>
        <div v-if="!collapsed" style="white-space:nowrap">
          <div style="color:#fff;font-size:14px;font-weight:600">继续教育平台</div>
          <div style="color:#64748b;font-size:10px;letter-spacing:.5px">ADMIN CONSOLE</div>
        </div>
      </div>
      <div class="sider-menu">
        <a-menu
          v-model:openKeys="openKeys"
          :selected-keys="[route.path]"
          :items="menuItems"
          theme="dark"
          mode="inline"
          :inline-collapsed="collapsed"
          style="background:transparent;border-inline-end:none;padding:10px 8px"
          @click="onMenuClick"
        />
      </div>
      <template #trigger>
        <div />
      </template>
    </a-layout-sider>

    <a-layout>
      <a-layout-header style="background:#fff;height:62px;line-height:62px;padding:0 22px;display:flex;align-items:center;border-bottom:1px solid #e8eef7;position:sticky;top:0;z-index:20">
        <a-button type="text" style="margin-right:10px" @click="collapsed = !collapsed">
          <MenuUnfoldOutlined v-if="collapsed" />
          <MenuFoldOutlined v-else />
        </a-button>
        <div style="font-size:15px;font-weight:600;color:#0f172a">{{ route.meta.title || '工作台' }}</div>
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
            <a-button type="text" shape="circle"><BellOutlined style="font-size:18px;color:#475569" /></a-button>
          </a-badge>
        </a-popover>

        <a-divider type="vertical" style="height:26px;margin:0 16px" />
        <a-dropdown>
          <div style="display:flex;align-items:center;gap:10px;cursor:pointer;line-height:1.2">
            <a-avatar :src="auth.user?.avatar || undefined" style="background:#1f6feb">{{ auth.displayName.slice(0, 1) }}</a-avatar>
            <div style="display:flex;flex-direction:column">
              <span style="font-size:13px;font-weight:600;color:#1e293b">{{ auth.displayName }}</span>
              <span style="font-size:11px;color:#94a3b8">{{ auth.roleName || '用户' }}</span>
            </div>
            <DownOutlined style="font-size:11px;color:#94a3b8" />
          </div>
          <template #overlay>
            <a-menu @click="onUserMenu">
              <a-menu-item key="profile"><UserOutlined /> 个人中心</a-menu-item>
              <a-menu-item key="password"><LockOutlined /> 修改密码</a-menu-item>
              <a-menu-divider />
              <a-menu-item key="logout" danger><LogoutOutlined /> 退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-layout-header>

      <a-layout-content style="padding:20px 22px 30px">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>

    <a-modal v-model:open="pwdOpen" title="修改登录密码" :confirm-loading="pwdLoading" @ok="submitPwd">
      <a-form layout="vertical" style="margin-top:16px">
        <a-form-item label="原密码" required><a-input-password v-model:value="pwd.oldPassword" /></a-form-item>
        <a-form-item label="新密码" required><a-input-password v-model:value="pwd.newPassword" placeholder="8-32 位，包含字母和数字" /></a-form-item>
        <a-form-item label="确认新密码" required><a-input-password v-model:value="pwd.confirm" /></a-form-item>
      </a-form>
    </a-modal>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { ItemType } from 'ant-design-vue'
import {
  ApartmentOutlined, AppstoreOutlined, AreaChartOutlined, AuditOutlined, BarChartOutlined, BellOutlined,
  BookOutlined, CameraOutlined, ControlOutlined, CreditCardOutlined, DashboardOutlined, DownOutlined,
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

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const openKeys = ref<string[]>([])
const notifyOpen = ref(false)
const notifyLoading = ref(false)
const notifications = ref<NotifyRow[]>([])
const unread = ref(0)
const pwdOpen = ref(false)
const pwdLoading = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const iconMap: Record<string, ReturnType<typeof h>> = {
  DashboardOutlined: h(DashboardOutlined), ReadOutlined: h(ReadOutlined), VideoCameraOutlined: h(VideoCameraOutlined),
  MonitorOutlined: h(MonitorOutlined), CameraOutlined: h(CameraOutlined), WarningOutlined: h(WarningOutlined),
  ControlOutlined: h(ControlOutlined), CreditCardOutlined: h(CreditCardOutlined), SafetyCertificateOutlined: h(SafetyCertificateOutlined),
  BarChartOutlined: h(BarChartOutlined), SettingOutlined: h(SettingOutlined), TeamOutlined: h(TeamOutlined),
  ApartmentOutlined: h(ApartmentOutlined), UserOutlined: h(UserOutlined), KeyOutlined: h(KeyOutlined),
  MenuOutlined: h(MenuOutlined), PieChartOutlined: h(PieChartOutlined), LineChartOutlined: h(LineChartOutlined),
  AreaChartOutlined: h(AreaChartOutlined), ProfileOutlined: h(ProfileOutlined), PlusSquareOutlined: h(PlusSquareOutlined),
  AuditOutlined: h(AuditOutlined), FileDoneOutlined: h(FileDoneOutlined), FolderOpenOutlined: h(FolderOpenOutlined),
  LoginOutlined: h(LoginOutlined), FileTextOutlined: h(FileTextOutlined), BookOutlined: h(BookOutlined)
}

function toItems(nodes: MenuNode[]): ItemType[] {
  const items: ItemType[] = []
  for (const node of nodes) {
    if (node.menuType === 3 || node.hidden === 1) continue
    const children = toItems(node.children || [])
    if (node.menuType === 1 && children.length === 0) continue
    const item: any = {
      key: node.path || `menu-${node.id}`,
      label: node.menuName,
      icon: node.icon ? (iconMap[node.icon] || h(AppstoreOutlined)) : undefined
    }
    if (children.length) item.children = children
    items.push(item)
  }
  return items
}

const menuItems = computed(() => toItems(auth.menus))

function collectOpen(nodes: MenuNode[]): string[] {
  const keys: string[] = []
  for (const node of nodes) {
    if (node.menuType === 1 && node.children?.length) {
      keys.push(node.path || `menu-${node.id}`)
    }
    keys.push(...collectOpen(node.children || []))
  }
  return keys
}

watch(() => auth.menus, (menus) => { openKeys.value = collectOpen(menus || []) }, { immediate: true, deep: true })

function onMenuClick(info: { key: string | number }) {
  const key = String(info.key)
  if (key.startsWith('/')) router.push(key)
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

async function onUserMenu(info: { key: string | number }) {
  const key = String(info.key)
  if (key === 'profile') router.push('/profile')
  if (key === 'password') pwdOpen.value = true
  if (key === 'logout') {
    await auth.logout()
    router.replace('/login')
  }
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
}
.admin-sider :deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.sider-logo {
  flex: none;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255,255,255,.06);
  overflow: hidden;
}
.sider-menu {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: rgba(148,163,184,.35) transparent;
}
.sider-menu::-webkit-scrollbar { width: 6px; }
.sider-menu::-webkit-scrollbar-thumb { background: rgba(148,163,184,.3); border-radius: 3px; }
.sider-menu::-webkit-scrollbar-track { background: transparent; }
.fade-enter-active, .fade-leave-active { transition: opacity .16s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
:deep(.ant-menu-dark .ant-menu-item-selected) { background: #1d4ed8 !important; border-radius: 8px; }
:deep(.ant-menu-dark .ant-menu-item) { border-radius: 8px; margin-block: 3px; }
</style>
