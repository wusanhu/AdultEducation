<template>
  <div style="max-width:900px">
    <div class="page-card" style="margin-bottom:16px">
      <div style="display:flex;gap:24px;align-items:center;flex-wrap:wrap">
        <a-avatar :size="78" :src="form.avatar">
          <template #icon><UserOutlined /></template>
        </a-avatar>
        <div style="flex:1;min-width:200px">
          <div style="font-size:20px;font-weight:600">{{ form.realName || form.username }}</div>
          <div style="color:#64748b;margin-top:4px">
            <a-tag color="blue">{{ info.roleName }}</a-tag>
            <span>{{ info.orgName || '未分配组织' }}</span>
          </div>
          <div style="color:#94a3b8;font-size:12px;margin-top:6px">账号：{{ form.username }} · 最近登录：{{ info.lastLoginTime || '—' }}</div>
        </div>
        <a-upload :show-upload-list="false" accept="image/*" :before-upload="uploadAvatar">
          <a-button><CameraOutlined /> 更换头像</a-button>
        </a-upload>
      </div>
    </div>

    <div class="page-card">
      <a-tabs v-model:activeKey="tab">
        <a-tab-pane key="base" tab="基本资料">
          <a-form :model="form" layout="vertical" style="max-width:480px">
            <a-form-item label="姓名" required><a-input v-model:value="form.realName" /></a-form-item>
            <a-form-item label="手机号"><a-input v-model:value="form.phone" placeholder="用于接收短信验证码" /></a-form-item>
            <a-form-item label="身份证号">
              <a-input :value="info.idCard" disabled placeholder="身份证号由管理员维护" />
            </a-form-item>
            <a-form-item label="所属组织"><a-input :value="info.orgName" disabled /></a-form-item>
            <a-button type="primary" :loading="saving" @click="saveProfile">保存资料</a-button>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="pwd" tab="修改密码">
          <a-form ref="pwdRef" :model="pwd" :rules="pwdRules" layout="vertical" style="max-width:480px">
            <a-form-item label="当前密码" name="oldPassword"><a-input-password v-model:value="pwd.oldPassword" /></a-form-item>
            <a-form-item label="新密码" name="newPassword">
              <a-input-password v-model:value="pwd.newPassword" placeholder="至少 8 位，含字母和数字" />
            </a-form-item>
            <a-form-item label="确认新密码" name="confirm">
              <a-input-password v-model:value="pwd.confirm" />
            </a-form-item>
            <a-alert type="info" show-icon style="margin-bottom:16px" message="密码修改后当前登录令牌将失效，需要重新登录。" />
            <a-button type="primary" :loading="changing" @click="changePassword">确认修改</a-button>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="notice" tab="通知设置">
          <div style="max-width:640px">
            <a-radio-group v-model:value="noticeType" button-style="solid" size="small" @change="loadNotifications">
              <a-radio-button value="">全部</a-radio-button>
              <a-radio-button v-for="c in NOTICE_CATEGORIES" :key="c.value" :value="c.value">{{ c.label }}</a-radio-button>
            </a-radio-group>
          </div>
          <a-list :data-source="notifications" :loading="noticeLoading" style="max-width:640px;margin-top:8px">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :description="String(item.content)">
                  <template #title>
                    <a-space size="small">
                      <a-tag :color="notifyCategory(item.type).color" style="margin-inline-end:0">{{ notifyCategory(item.type).label }}</a-tag>
                      <span>{{ String(item.title) }}</span>
                    </a-space>
                  </template>
                </a-list-item-meta>
                <template #actions>
                  <a v-if="item.adminLink" @click="openNotice(item)">查看</a>
                  <a-tag v-if="!item.isRead" color="red">未读</a-tag>
                  <span style="font-size:12px;color:#94a3b8">{{ item.createTime }}</span>
                </template>
              </a-list-item>
            </template>
            <template #emptyText><a-empty description="暂无通知" /></template>
          </a-list>
          <div v-if="notifications.length" style="margin-top:12px">
            <a-button @click="readAll">全部标为已读</a-button>
          </div>
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, type FormInstance } from 'ant-design-vue'
import { CameraOutlined, UserOutlined } from '@ant-design/icons-vue'
import { authApi, notificationApi, profileApi } from '../api'
import { NOTICE_CATEGORIES, notifyCategory } from '../constants/notify'
import { upload } from '../api/http'
import { useAuthStore } from '../store/auth'
import type { UserInfo } from '../types'

const router = useRouter()
const auth = useAuthStore()
const tab = ref('base')
const saving = ref(false)
const changing = ref(false)
const noticeLoading = ref(false)
const info = ref<UserInfo>({} as UserInfo)
const form = reactive<Record<string, any>>({ username: '', realName: '', phone: '', avatar: '' })
const pwdRef = ref<FormInstance>()
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const notifications = ref<Record<string, any>[]>([])
const noticeType = ref('')
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码' }],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { min: 8, message: '密码至少 8 位' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '密码需同时包含字母和数字' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码' },
    { validator: (_r: unknown, v: string) => v === pwd.newPassword ? Promise.resolve() : Promise.reject('两次输入的密码不一致') }
  ]
}

async function load() {
  try {
    info.value = await profileApi.profile()
    Object.assign(form, info.value)
  } catch (e) { message.error((e as Error).message) }
}
async function saveProfile() {
  if (!form.realName) { message.warning('请填写姓名'); return }
  saving.value = true
  try {
    info.value = await profileApi.update({ realName: form.realName, phone: form.phone, avatar: form.avatar })
    message.success('资料已保存')
    await auth.refresh()
  } catch (e) { message.error((e as Error).message) } finally { saving.value = false }
}
async function uploadAvatar(file: File) {
  try {
    const res = await upload<{ url: string }>('/files/upload', file, { type: 'avatar' })
    form.avatar = res.url
    await saveProfile()
  } catch (e) { message.error((e as Error).message) }
  return false
}
async function changePassword() {
  try { await pwdRef.value?.validate() } catch { return }
  changing.value = true
  try {
    await authApi.changePassword(pwd.oldPassword, pwd.newPassword)
    message.success('密码已修改，请重新登录')
    auth.clear()
    await router.replace('/login')
  } catch (e) { message.error((e as Error).message) } finally { changing.value = false }
}
async function loadNotifications() {
  noticeLoading.value = true
  try {
    const page = await notificationApi.page({ pageNum: 1, pageSize: 20, type: noticeType.value || undefined })
    notifications.value = page.records
  } catch (e) { message.error((e as Error).message) } finally { noticeLoading.value = false }
}
async function openNotice(item: Record<string, any>) {
  if (!item.adminLink) return
  if (!item.isRead) {
    try { await notificationApi.read(Number(item.id)) } catch { /* 跳转优先，标记失败不阻塞 */ }
  }
  void router.push(String(item.adminLink))
}
async function readAll() {
  try { await notificationApi.readAll(); await loadNotifications(); message.success('已全部标为已读') } catch (e) { message.error((e as Error).message) }
}

onMounted(async () => {
  await load()
  await loadNotifications()
})
</script>
