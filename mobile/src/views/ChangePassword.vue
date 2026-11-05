<template>
  <div class="page-container">
    <PageHeader title="修改密码" />

    <div class="pwd-intro">
      <span class="kicker">ACCOUNT SECURITY</span>
      <h2>修改登录密码</h2>
      <p>为保障账号安全，新密码需为 8-32 位且同时包含字母与数字。</p>
    </div>

    <van-form @submit="onSubmit" class="pwd-form">
      <van-cell-group inset>
        <van-field
          v-model="form.oldPassword"
          type="password"
          label="原密码"
          placeholder="请输入原密码"
          :rules="[{ required: true, message: '请输入原密码' }]"
        />
        <van-field
          v-model="form.newPassword"
          type="password"
          label="新密码"
          placeholder="8-32位，含字母和数字"
          :rules="[
            { required: true, message: '请输入新密码' },
            { validator: validatePwd, message: '密码需8-32位，含字母和数字' },
          ]"
        />
        <van-field
          v-model="form.confirmPassword"
          type="password"
          label="确认密码"
          placeholder="请再次输入新密码"
          :rules="[
            { required: true, message: '请确认新密码' },
            { validator: validateConfirm, message: '两次密码不一致' },
          ]"
        />
      </van-cell-group>
      <div class="submit-wrap">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          确认修改
        </van-button>
      </div>
    </van-form>

    <p v-if="userStore.mustChangePwd" class="hint-text">
      首次登录需要修改密码后才能继续使用
    </p>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Form as VanForm, Field as VanField, CellGroup as VanCellGroup,
  Button as VanButton, showToast,
} from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import { auth } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

function validatePwd(val: string) {
  return /^(?=.*[A-Za-z])(?=.*[0-9]).{8,32}$/.test(val)
}

function validateConfirm(val: string) {
  return (val === form.newPassword)
}

async function onSubmit() {
  loading.value = true
  try {
    await auth.changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    })
    // 后端修改密码后会作废旧 token，需重新登录
    userStore.clearUser()
    showToast('密码修改成功，请重新登录')
    setTimeout(() => {
      router.replace('/login')
    }, 500)
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.pwd-intro {
  margin: var(--sp-4) var(--page-x) var(--sp-4);
  padding: var(--sp-5) var(--sp-4);
  border: 1px solid var(--line);
  border-radius: var(--r-card);
  background:
    radial-gradient(100% 120% at 100% 0%, rgba(173, 122, 53, 0.1) 0%, transparent 58%),
    var(--surface);
  box-shadow: var(--shadow-card);
}
.pwd-intro h2 {
  margin-top: 6px;
  font-family: var(--font-serif);
  font-size: 19px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--ink);
}
.pwd-intro p {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.65;
  color: var(--ink-3);
}
.pwd-form :deep(.van-cell-group--inset) {
  margin-top: 0;
}
.pwd-form :deep(.van-field__label) {
  width: 74px;
  color: var(--ink-2);
  font-size: 14px;
}
.pwd-form :deep(.van-cell) {
  padding-top: 15px;
  padding-bottom: 15px;
}
.submit-wrap {
  padding: var(--sp-6) var(--page-x);
}
.submit-wrap :deep(.van-button) {
  height: 48px;
  font-size: 15px;
  font-weight: 600;
}
.hint-text {
  padding: 0 var(--page-x);
  font-size: 12px;
  line-height: 1.6;
  text-align: center;
  color: var(--warn);
}
</style>
