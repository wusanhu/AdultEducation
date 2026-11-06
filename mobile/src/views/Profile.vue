<template>
  <div class="page-container profile-page">
    <!-- 身份卡 -->
    <header class="profile-hero">
      <span class="hero-ornament" aria-hidden="true">学</span>
      <div class="id-card">
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
          <span class="avatar-edit"><van-icon name="photo-o" size="12" /></span>
          <input
            ref="fileInput"
            type="file"
            accept="image/*"
            style="display: none"
            @change="onAvatarChange"
          />
        </div>
        <div class="id-main">
          <span class="id-kicker">LEARNING PROFILE</span>
          <h2 class="profile-name">{{ userStore.user?.realName }}</h2>
          <div class="id-meta">
            <span class="role-chip">{{ userStore.user?.roleName || '学员' }}</span>
            <p v-if="userStore.user?.orgName" class="profile-org">
              <van-icon name="cluster-o" size="12" />
              <span class="text-ellipsis">{{ userStore.user?.orgName }}</span>
            </p>
          </div>
        </div>
      </div>
    </header>

    <!-- 账号信息 -->
    <div class="section-title">账号资料</div>
    <van-cell-group inset>
      <van-cell title="姓名" icon="contact" :value="userStore.user?.realName" is-link @click="openNameEdit" />
      <van-cell title="手机号" icon="phone-o" :value="userStore.user?.phone || '未设置'" is-link @click="openPhoneEdit" />
      <van-cell title="账号" icon="user-o" :value="userStore.user?.username" />
    </van-cell-group>

    <!-- 学习服务 -->
    <div class="section-title">学习服务</div>
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

    <div class="logout-wrap">
      <van-button plain type="danger" block @click="onLogout">退出登录</van-button>
    </div>

    <!-- 修改姓名：before-close 异步校验，失败时保持弹窗不丢输入 -->
    <van-dialog
      v-model:show="showEditName"
      title="修改姓名"
      show-cancel-button
      :before-close="onNameBeforeClose"
    >
      <van-field v-model="editName" label="姓名" placeholder="请输入姓名" maxlength="30" />
    </van-dialog>

    <!-- 修改手机号 -->
    <van-dialog
      v-model:show="showEditPhone"
      title="修改手机号"
      show-cancel-button
      :before-close="onPhoneBeforeClose"
    >
      <van-field v-model="editPhone" label="手机号" placeholder="请输入手机号" type="tel" maxlength="11" />
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
const editName = ref('')
const editPhone = ref('')
const profileSaving = ref(false)

function openNameEdit() {
  // 每次打开都以当前资料回填，避免上次的残留输入
  editName.value = userStore.user?.realName || ''
  showEditName.value = true
}

function openPhoneEdit() {
  editPhone.value = userStore.user?.phone || ''
  showEditPhone.value = true
}

/** 后端 /profile 为整体提交语义（realName 必填、未传字段会被清空），
 *  因此姓名/手机号/头像始终一起发送，避免改 A 丢 B */
async function saveProfile(patch: { realName?: string; phone?: string; avatar?: string }) {
  const payload = {
    realName: patch.realName ?? userStore.user?.realName ?? '',
    phone: patch.phone ?? userStore.user?.phone ?? '',
    avatar: patch.avatar ?? userStore.user?.avatar ?? '',
  }
  const fresh = await profile.update(payload)
  if (fresh && typeof fresh === 'object') {
    Object.assign(userStore.user!, fresh)
  }
  localStorage.setItem('mobile_user', JSON.stringify(userStore.user))
}

async function onNameBeforeClose(action: string) {
  if (action !== 'confirm') return true
  const name = editName.value.trim()
  if (!name) {
    showToast('请输入姓名')
    return false
  }
  profileSaving.value = true
  try {
    await saveProfile({ realName: name })
    showToast('修改成功')
    return true
  } catch {
    return false
  } finally {
    profileSaving.value = false
  }
}

async function onPhoneBeforeClose(action: string) {
  if (action !== 'confirm') return true
  const phone = editPhone.value.trim()
  if (phone && !/^1[3-9]\d{9}$/.test(phone)) {
    showToast('手机号格式不正确')
    return false
  }
  profileSaving.value = true
  try {
    await saveProfile({ phone })
    showToast(phone ? '修改成功' : '已清除手机号')
    return true
  } catch {
    return false
  } finally {
    profileSaving.value = false
  }
}

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
    await saveProfile({ avatar: avatarUrl })
    showToast('头像更新成功')
  } catch {
    showToast('上传失败')
  }
  target.value = ''
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
/* ---------- 身份卡 ---------- */
.profile-hero {
  position: relative;
  overflow: hidden;
  padding: calc(env(safe-area-inset-top) + 20px) var(--page-x) var(--sp-2);
  background: linear-gradient(180deg, #e8eefc 0%, #f1f5fe 66%, var(--bg) 100%);
}

.hero-ornament {
  position: absolute;
  top: -4px;
  right: -6px;
  font-family: var(--font-serif);
  font-size: 96px;
  line-height: 1;
  color: rgba(29, 78, 216, 0.09);
  pointer-events: none;
  user-select: none;
}

.id-card {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: var(--sp-4);
  padding: var(--sp-5) var(--sp-4);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-card);
  box-shadow: var(--shadow-float);
}

.avatar-wrap {
  position: relative;
  display: inline-block;
  flex: none;
  cursor: pointer;
}

.avatar-wrap :deep(.van-image) {
  border: 3px solid var(--brand-line);
  border-radius: 50%;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: var(--brand-tint);
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 700;
  color: var(--brand);
}

.avatar-edit {
  position: absolute;
  right: -2px;
  bottom: -2px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 1px solid var(--brand-line);
  border-radius: 50%;
  background: var(--brand-tint);
  color: var(--brand);
}

.id-main {
  flex: 1;
  min-width: 0;
}

.id-kicker {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.16em;
  color: var(--ink-3);
}

.profile-name {
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--ink);
}

.id-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px var(--sp-2);
  margin-top: 9px;
}

.role-chip {
  flex: none;
  padding: 2px 9px;
  border: 1px solid var(--brand-line);
  border-radius: 999px;
  background: var(--brand-tint);
  font-size: 11px;
  line-height: 1.6;
  color: var(--brand);
}

.profile-org {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  font-size: 11px;
  color: var(--ink-3);
}

/* ---------- 菜单 ---------- */
:deep(.van-cell-group--inset) {
  border-radius: var(--r-card);
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
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  margin-right: var(--sp-3);
  border: 1px solid var(--brand-line);
  border-radius: var(--r-tag);
  background: var(--brand-tint);
  color: var(--brand);
  font-size: 16px;
}

:deep(.van-cell--clickable:active) {
  background: var(--surface-2);
}

.logout-wrap {
  padding: var(--sp-6) var(--page-x) 0;
}

.logout-wrap :deep(.van-button) {
  height: 46px;
  border-radius: var(--r-control);
  font-size: 14px;
  font-weight: 500;
  background: var(--surface);
  border-color: var(--bad-line);
}
</style>
