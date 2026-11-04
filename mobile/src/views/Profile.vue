<template>
  <div class="page-container">
    <!-- Profile Header -->
    <div class="profile-header">
      <div class="avatar-wrap" @click="triggerUpload">
        <van-image
          round
          width="64"
          height="64"
          :src="userStore.user?.avatar || ''"
          fit="cover"
        >
          <template #error>
            <div class="avatar-fallback">
              {{ (userStore.user?.realName || 'U').charAt(0) }}
            </div>
          </template>
        </van-image>
        <van-icon name="photograph" class="avatar-edit" color="var(--brand)" />
        <input
          ref="fileInput"
          type="file"
          accept="image/*"
          style="display: none"
          @change="onAvatarChange"
        />
      </div>
      <h3 class="profile-name">{{ userStore.user?.realName }}</h3>
      <p class="profile-role">{{ userStore.user?.roleName || '学员' }}</p>
      <p class="profile-org">{{ userStore.user?.orgName }}</p>
    </div>

    <!-- Menu Cells -->
    <div class="section-title">账号信息</div>
    <van-cell-group inset>
      <van-cell title="姓名" icon="contact" :value="userStore.user?.realName" is-link @click="showEditName = true" />
      <van-cell title="手机号" icon="phone-o" :value="userStore.user?.phone || '未设置'" is-link @click="showEditPhone = true" />
      <van-cell title="账号" icon="user-o" :value="userStore.user?.username" />
    </van-cell-group>

    <div class="section-title">常用功能</div>
    <van-cell-group inset>
      <van-cell
        v-for="entry in userStore.entries"
        :key="entry.key"
        :title="entry.label"
        :icon="entry.icon"
        is-link
        @click="$router.push(entry.route)"
      />
      <van-cell title="修改密码" icon="lock" is-link @click="$router.push('/change-password')" />
    </van-cell-group>

    <div class="logout-wrap mt-24">
      <van-button plain type="danger" block round @click="onLogout">退出登录</van-button>
    </div>

    <!-- Edit Name Dialog -->
    <van-dialog
      v-model:show="showEditName"
      title="修改姓名"
      show-cancel-button
      @confirm="onSaveName"
    >
      <van-field v-model="editName" label="姓名" placeholder="请输入姓名" />
    </van-dialog>

    <!-- Edit Phone Dialog -->
    <van-dialog
      v-model:show="showEditPhone"
      title="修改手机号"
      show-cancel-button
      @confirm="onSavePhone"
    >
      <van-field v-model="editPhone" label="手机号" placeholder="请输入手机号" type="tel" />
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Image as VanImage, Icon as VanIcon, Cell as VanCell,
  CellGroup as VanCellGroup, Button as VanButton, Field as VanField,
  Dialog as VanDialog, showToast, showConfirmDialog,
} from 'vant'
import { useUserStore } from '@/stores/user'
import { profile, auth, files } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const fileInput = ref<HTMLInputElement | null>(null)
const showEditName = ref(false)
const showEditPhone = ref(false)
const editName = ref(userStore.user?.realName || '')
const editPhone = ref(userStore.user?.phone || '')

function triggerUpload() {
  fileInput.value?.click()
}

async function onAvatarChange(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  try {
    const res = await files.upload(file, 'avatar')
    const avatarUrl = res?.url || res?.fileUrl || ''
    await profile.update({ avatar: avatarUrl })
    userStore.user!.avatar = avatarUrl
    localStorage.setItem('mobile_user', JSON.stringify(userStore.user))
    showToast('头像更新成功')
  } catch {
    showToast('上传失败')
  }
  target.value = ''
}

async function onSaveName() {
  if (!editName.value.trim()) {
    showToast('请输入姓名')
    return
  }
  try {
    await profile.update({ realName: editName.value.trim() })
    userStore.user!.realName = editName.value.trim()
    localStorage.setItem('mobile_user', JSON.stringify(userStore.user))
    showToast('修改成功')
  } catch {
    // handled
  }
}

async function onSavePhone() {
  if (editPhone.value && !/^1[3-9]\d{9}$/.test(editPhone.value)) {
    showToast('手机号格式不正确')
    return
  }
  try {
    await profile.update({ phone: editPhone.value })
    userStore.user!.phone = editPhone.value
    localStorage.setItem('mobile_user', JSON.stringify(userStore.user))
    showToast('修改成功')
  } catch {
    // handled
  }
}

async function onLogout() {
  try {
    await showConfirmDialog({ title: '提示', message: '确定退出登录？' })
    await auth.logout().catch(() => {})
    userStore.clearUser()
    router.replace('/login')
  } catch {
    // cancelled
  }
}
</script>

<style scoped>
.profile-header {
  background: var(--brand-deep);
  padding: calc(env(safe-area-inset-top) + 36px) var(--page-x) var(--sp-8);
  text-align: center;
  color: #fff;
}
.avatar-wrap {
  position: relative;
  display: inline-block;
  cursor: pointer;
}
.avatar-wrap :deep(.van-image) {
  border: 2px solid rgba(255, 255, 255, 0.32);
  border-radius: 50%;
}
.avatar-fallback {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}
.avatar-edit {
  position: absolute;
  bottom: 0;
  right: 0;
  background: var(--surface);
  border-radius: 50%;
  padding: var(--sp-1);
}
.profile-name {
  font-size: 21px;
  font-weight: 700;
  letter-spacing: 0.3px;
  margin-top: var(--sp-3);
}
.profile-role {
  display: inline-block;
  font-size: 12px;
  margin-top: var(--sp-2);
  padding: var(--sp-1) var(--sp-3);
  border-radius: var(--r-chip);
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  line-height: 1.5;
}
.profile-org {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.68);
  margin-top: var(--sp-2);
  line-height: 1.5;
}
:deep(.van-cell-group--inset) {
  border-radius: var(--r-card);
  border: 1px solid var(--line);
}
:deep(.van-cell__title) {
  color: var(--ink-2);
  font-size: 14px;
}
:deep(.van-cell__value) {
  color: var(--ink);
  font-size: 14px;
}
:deep(.van-cell__left-icon) {
  color: var(--brand);
  font-size: 17px;
  margin-right: var(--sp-2);
  display: flex;
  align-items: center;
}
.logout-wrap {
  padding: 0 var(--page-x);
}
.logout-wrap :deep(.van-button) {
  height: 46px;
  border-radius: var(--r-control);
  font-size: 15px;
  font-weight: 500;
  background: var(--surface);
}
</style>
