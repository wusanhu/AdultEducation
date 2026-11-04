<template>
  <div class="login-page">
    <div class="login-shell">
      <div class="brand-row">
        <div class="brand-mark">继</div>
        <div class="brand-text">
          <p class="brand-name">继续教育学分管理</p>
          <p class="brand-en">ADULT EDUCATION CREDIT PLATFORM</p>
        </div>
      </div>

      <div class="page-title">
        <span class="title-accent"></span>
        <h1>登录学习平台</h1>
        <p class="page-desc">在线学习 · 学分记录 · 毕业申请</p>
      </div>

      <van-form class="login-form" @submit="onSubmit">
        <div class="field-box">
          <van-field
            v-model="form.username"
            name="username"
            label="账号"
            label-align="top"
            placeholder="请输入账号"
            clearable
            left-icon="manager-o"
            :rules="[{ required: true, message: '请输入账号' }]"
          />
        </div>
        <div class="field-box">
          <van-field
            v-model="form.password"
            name="password"
            label="密码"
            label-align="top"
            type="password"
            placeholder="请输入密码"
            left-icon="lock"
            :rules="[{ required: true, message: '请输入密码' }]"
          />
        </div>
        <div class="login-btn-wrap">
          <van-button block type="primary" native-type="submit" :loading="loading">
            登 录
          </van-button>
        </div>
      </van-form>

      <div class="demo-hints">
        <p class="demo-title">演示账号（点击填充）</p>
        <div class="demo-chips">
          <button
            v-for="item in demoAccounts"
            :key="item.username"
            type="button"
            class="demo-chip"
            @click="fill(item)"
          >
            <span class="chip-role">{{ item.role }}</span>
            <span class="chip-name">{{ item.username }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Form as VanForm, Field as VanField, Button as VanButton, showToast } from 'vant'
import { auth } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const demoAccounts = [
  { role: '学员', username: 'student', password: 'student123' },
  { role: '教师', username: 'teacher', password: 'teacher123' },
]

function fill(account: { username: string; password: string }) {
  form.username = account.username
  form.password = account.password
}

async function onSubmit() {
  loading.value = true
  try {
    const data = await auth.login({
      username: form.username,
      password: form.password,
    })
    userStore.setLogin(data)
    showToast('登录成功')
    if (data.mustChangePwd) {
      userStore.mustChangePwd = true
      router.replace('/change-password')
    } else {
      router.replace('/')
    }
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  min-height: 100dvh;
  background: var(--surface);
  padding: calc(env(safe-area-inset-top) + 44px) var(--sp-6) calc(env(safe-area-inset-bottom) + 40px);
}
.login-shell {
  max-width: 440px;
  margin: 0 auto;
}

/* 品牌区 */
.brand-row {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
}
.brand-mark {
  width: 44px;
  height: 44px;
  flex: none;
  border-radius: var(--r-control);
  background: var(--brand);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.3;
}
.brand-en {
  font-size: 10px;
  color: var(--ink-3);
  letter-spacing: 0.08em;
  margin-top: 3px;
}

/* 标题区 */
.page-title {
  margin-top: 48px;
}
.title-accent {
  display: block;
  width: 28px;
  height: 3px;
  background: var(--brand);
  border-radius: 2px;
}
.page-title h1 {
  font-size: 27px;
  font-weight: 700;
  color: var(--ink);
  line-height: 1.35;
  margin-top: var(--sp-4);
  letter-spacing: -0.01em;
}
.page-desc {
  font-size: 14px;
  color: var(--ink-3);
  margin-top: var(--sp-2);
}

/* 表单 */
.login-form {
  margin-top: var(--sp-6);
}
.field-box + .field-box {
  margin-top: 14px;
}
.field-box {
  border: 1px solid var(--line);
  border-radius: var(--r-control);
  background: var(--surface);
  transition: border-color 0.2s;
}
.field-box:focus-within {
  border-color: var(--brand);
}
.field-box :deep(.van-cell) {
  padding: 11px 14px 12px;
  background: transparent;
}
.field-box :deep(.van-cell::after) {
  display: none;
}
/* 保留 Vant label-align="top" 的整行宽度，仅调整排印 */
.field-box :deep(.van-field__label) {
  font-size: 13px;
  color: var(--ink-2);
  margin-bottom: 6px;
}
.field-box :deep(.van-field__body) {
  padding: 0;
}
.field-box :deep(.van-field__left-icon) {
  color: var(--ink-4);
  margin-right: var(--sp-2);
}
.field-box :deep(.van-field__left-icon .van-icon) {
  font-size: 18px;
}
.field-box :deep(.van-field__control) {
  font-size: 16px;
  color: var(--ink);
}
.field-box :deep(.van-field__control::placeholder) {
  color: var(--ink-4);
}
.login-btn-wrap {
  margin-top: var(--sp-6);
}
.login-btn-wrap :deep(.van-button) {
  width: 100%;
  height: 48px;
  border-radius: var(--r-control);
  border-color: var(--brand);
  background: var(--brand);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}
.login-btn-wrap :deep(.van-button:active) {
  background: var(--brand-strong);
  border-color: var(--brand-strong);
}

/* 演示账号 */
.demo-hints {
  margin-top: 36px;
  padding-top: var(--sp-5);
  border-top: 1px dashed var(--line);
}
.demo-title {
  font-size: 12px;
  color: var(--ink-3);
}
.demo-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: var(--sp-3);
}
.demo-chip {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  padding: var(--sp-2) var(--sp-3);
  border: 1px solid var(--line);
  border-radius: var(--r-chip);
  background: var(--surface-2);
  font-size: 13px;
  color: var(--ink-2);
  cursor: pointer;
  -webkit-appearance: none;
  appearance: none;
  font-family: inherit;
  transition: border-color 0.2s, color 0.2s, background-color 0.2s;
}
.demo-chip:active,
.demo-chip:hover {
  border-color: var(--brand-line);
  background: var(--brand-tint);
  color: var(--brand);
}
.chip-role {
  font-weight: 600;
}
.chip-name {
  color: var(--ink-3);
}
.demo-chip:active .chip-name,
.demo-chip:hover .chip-name {
  color: var(--brand);
}
</style>
