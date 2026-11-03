<template>
  <div class="page-container">
    <!-- Profile Header -->
    <div class="profile-header ink-hero">
      <span class="hero-watermark">吾</span>
      <div class="avatar-wrap" @click="triggerUpload">
        <van-image
          round
          width="68"
          height="68"
          :src="userStore.user?.avatar || ''"
          fit="cover"
        >
          <template #error>
            <div class="avatar-fallback">
              {{ (userStore.user?.realName || 'U').charAt(0) }}
            </div>
          </template>
        </van-image>
        <van-icon name="photograph" class="avatar-edit" />
        <input
          ref="fileInput"
          type="file"
          accept="image/*"
          style="display: none"
          @change="onAvatarChange"
        />
      </div>
      <h3 class="profile-name">{{ userStore.user?.realName }}</h3>
      <p class="profile-role">
        <span class="role-chip">{{ userStore.user?.roleName || '学员' }}</span>
      </p>
      <p class="profile-org" v-if="userStore.user?.orgName">{{ userStore.user?.orgName }}</p>
    </div>

    <!-- Menu Cells -->
    <van-cell-group inset class="mt-16">
      <van-cell title="姓名" :value="userStore.user?.realName" is-link @click="showEditName = true" />
      <van-cell title="手机号" :value="userStore.user?.phone || '未设置'" is-link @click="showEditPhone = true" />
      <van-cell title="账号" :value="userStore.user?.username" />
    </van-cell-group>

    <van-cell-group inset class="mt-12">
      <van-cell title="修改密码" icon="lock" is-link @click="$router.push('/change-password')" />
      <van-cell title="核验记录" icon="shield-o" is-link @click="$router.push('/verify')" />
      <van-cell v-if="userStore.isTeacher" title="教务管理" icon="manager-o" is-link @click="$router.push('/teacher')" />
    </van-cell-group>

    <div class="logout-wrap mt-16">
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
  padding: 44px 20px 34px;
  text-align: center;
  border-radius: 0 0 20px 20px;
}
.avatar-wrap {
  position: relative;
  display: inline-block;
  cursor: pointer;
  padding: 4px;
  border: 1.5px solid rgba(216, 185, 108, 0.6);
  border-radius: 50%;
}
.avatar-fallback {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: rgba(245, 242, 234, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 700;
  color: #f5f2ea;
}
.avatar-edit {
  position: absolute;
  bottom: 0;
  right: 0;
  background: var(--paper-card);
  border-radius: 50%;
  padding: 3px;
  color: var(--ink-800);
  font-size: 12px;
  border: 1px solid var(--line);
}
.profile-name {
  font-family: var(--font-display);
  font-size: 21px;
  font-weight: 700;
  letter-spacing: 0.1em;
  margin-top: 14px;
  color: #f5f2ea;
}
.profile-role {
  margin-top: 10px;
}
.role-chip {
  display: inline-block;
  font-size: 11px;
  color: rgba(245, 242, 234, 0.8);
  border: 1px solid rgba(245, 242, 234, 0.3);
  border-radius: 99px;
  padding: 3px 12px;
  letter-spacing: 0.2em;
}
.profile-org {
  font-size: 12px;
  color: rgba(245, 242, 234, 0.6);
  margin-top: 8px;
}
.logout-wrap {
  padding: 0 16px;
}
.logout-wrap :deep(.van-button--danger) {
  color: var(--danger);
  border-color: rgba(194, 67, 46, 0.45);
  background: transparent;
}
</style>
