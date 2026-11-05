<template>
  <div class="login-page">
    <div class="login-hero">
      <span class="hero-ornament" aria-hidden="true">学</span>
      <div class="brand-row">
        <div class="brand-mark">继</div>
        <div class="brand-text">
          <p class="brand-name">继续教育学分管理</p>
          <p class="brand-en">ADULT EDUCATION CREDIT PLATFORM</p>
        </div>
      </div>

      <div class="page-title">
        <h1>登录学习平台</h1>
        <p class="page-desc">在线学习 · 学分记录 · 毕业申请</p>
      </div>

      <div class="hero-foot">
        <span class="foot-line"></span>
        <span>LEARN · RECORD · GRADUATE</span>
      </div>
    </div>

    <div class="login-sheet">
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
        <div class="demo-head">
          <span class="kicker">演示账号</span>
          <span class="demo-tip">点击即可填充</span>
        </div>
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

      <p class="login-note">账号由所属单位统一开通，如无法登录请联系教务管理员。</p>
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
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
  background: var(--bg);
}

/* ---------- 品牌区 ---------- */
.login-hero {
  position: relative;
  overflow: hidden;
  flex: none;
  padding: calc(env(safe-area-inset-top) + 32px) var(--sp-6) 60px;
  color: var(--ink);
  background: linear-gradient(180deg, #e9f1f8 0%, #f2f6fa 60%, var(--bg) 100%);
}

.hero-ornament {
  position: absolute;
  right: -14px;
  bottom: -30px;
  font-family: var(--font-serif);
  font-size: 150px;
  line-height: 1;
  color: rgba(79, 109, 143, 0.08);
  pointer-events: none;
  user-select: none;
}

.brand-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--sp-3);
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 42px;
  height: 42px;
  border: 1px solid var(--brand-line);
  border-radius: 12px;
  background: var(--surface);
  box-shadow: var(--shadow-card);
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 700;
  color: var(--brand);
}

.brand-name {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.3;
  letter-spacing: 0.01em;
  color: var(--ink);
}

.brand-en {
  margin-top: 3px;
  font-size: 9px;
  letter-spacing: 0.12em;
  color: var(--ink-4);
}

.page-title {
  position: relative;
  margin-top: 46px;
}

.page-title h1 {
  font-family: var(--font-serif);
  font-size: 30px;
  font-weight: 600;
  line-height: 1.3;
  letter-spacing: 0.02em;
  color: var(--ink);
}

.page-desc {
  margin-top: var(--sp-2);
  font-size: 13px;
  letter-spacing: 0.04em;
  color: var(--ink-3);
}

.hero-foot {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 30px;
  font-size: 9px;
  letter-spacing: 0.16em;
  color: var(--ink-4);
}

.foot-line {
  width: 26px;
  height: 2px;
  background: var(--accent);
}

/* ---------- 表单纸面 ---------- */
.login-sheet {
  position: relative;
  flex: 1;
  margin-top: -28px;
  padding: var(--sp-6) var(--sp-6) calc(env(safe-area-inset-bottom) + 32px);
  border-radius: 28px 28px 0 0;
  background: var(--surface);
  border-top: 1px solid var(--line);
}

.login-form {
  max-width: 440px;
  margin: 0 auto;
}

.field-box {
  border: 1px solid var(--line);
  border-radius: var(--r-control);
  background: var(--surface-2);
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.field-box + .field-box {
  margin-top: 14px;
}

.field-box:focus-within {
  border-color: var(--brand-line);
  background: var(--surface);
  box-shadow: 0 0 0 3px var(--brand-tint);
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
  margin-bottom: 6px;
  font-size: 12px;
  letter-spacing: 0.04em;
  color: var(--ink-3);
}

.field-box :deep(.van-field__body) {
  padding: 0;
}

.field-box :deep(.van-field__left-icon) {
  display: flex;
  align-items: center;
  margin-right: var(--sp-3);
  color: var(--ink-3);
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
  height: 50px;
  border-radius: var(--r-control);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

/* ---------- 演示账号 ---------- */
.demo-hints {
  max-width: 440px;
  margin: 34px auto 0;
  padding-top: var(--sp-4);
  border-top: 1px dashed var(--line-strong);
}

.demo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.demo-tip {
  font-size: 10px;
  color: var(--ink-4);
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
  padding: 9px 12px;
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
  border-color: var(--accent-line);
  background: var(--accent-tint);
  color: var(--accent-strong);
}

.chip-role {
  font-weight: 600;
}

.chip-name {
  color: var(--ink-3);
}

.demo-chip:active .chip-name,
.demo-chip:hover .chip-name {
  color: var(--accent-strong);
}

.login-note {
  max-width: 440px;
  margin: var(--sp-5) auto 0;
  font-size: 11px;
  line-height: 1.65;
  color: var(--ink-4);
}
</style>
