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
      <p class="profile-role">{{ userStore.user?.roleName || '学员' }}</p>
      <p class="profile-org">{{ userStore.user?.orgName }}</p>
    </div>

    <!-- Menu Cells -->
    <van-cell-group inset class="mt-16">
      <van-cell title="姓名" :value="userStore.user?.realName" is-link @click="showEditName = true" />
      <van-cell title="手机号" :value="userStore.user?.phone || '未设置'" is-link @click="showEditPhone = true" />
      <van-cell title="账号" :value="userStore.user?.username" />
    </van-cell-group>

    <van-cell-group inset class="mt-12">
      <van-cell title="修改密码" is-link @click="$router.push('/change-password')" />
      <van-cell title="核验记录" is-link @click="$router.push('/verify')" />
      <van-cell v-if="userStore.isTeacher" title="教师管理" is-link @click="$router.push('/teacher')" />
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
  background: linear-gradient(135deg, #1989fa, #07c160);
  padding: 40px 20px 32px;
  text-align: center;
  color: #fff;
}
.avatar-wrap {
  position: relative;
  display: inline-block;
  cursor: pointer;
}
.avatar-fallback {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
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
  background: #fff;
  border-radius: 50%;
  padding: 2px;
}
.profile-name {
  font-size: 20px;
  font-weight: 600;
  margin-top: 12px;
}
.profile-role {
  font-size: 13px;
  opacity: 0.85;
  margin-top: 4px;
}
.profile-org {
  font-size: 12px;
  opacity: 0.7;
  margin-top: 2px;
}
.logout-wrap {
  padding: 0 16px;
}
</style>
