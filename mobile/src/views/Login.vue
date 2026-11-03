<template>
  <div class="login-page ink-hero">
    <span class="hero-watermark">学无止境</span>

    <!-- 品牌标识 -->
    <div class="brand">
      <div class="brand-mark">继</div>
      <h1 class="brand-title">继续教育学分管理</h1>
      <p class="brand-sub">学籍在册 · 笃学不倦</p>
    </div>

    <!-- 卷宗式表单 -->
    <div class="dossier">
      <div class="dossier-head">
        <span class="dossier-tab">登录卷宗</span>
        <span class="dossier-no num">NO. JXJY</span>
      </div>
      <van-form @submit="onSubmit" class="login-form">
        <van-field
          v-model="form.username"
          name="username"
          label="账号"
          placeholder="请输入账号"
          :rules="[{ required: true, message: '请输入账号' }]"
          left-icon="manager-o"
        />
        <div class="rule-dashed"></div>
        <van-field
          v-model="form.password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
          left-icon="lock"
        />
        <div class="login-btn-wrap">
          <van-button round block type="primary" native-type="submit" :loading="loading">
            入 学
          </van-button>
        </div>
      </van-form>
    </div>

    <!-- 演示账号 -->
    <div class="demo-hints">
      <p class="demo-title">演示账号</p>
      <div class="demo-chips">
        <span class="demo-chip" @click="fill('student', 'student123')">学员 student</span>
        <span class="demo-chip" @click="fill('teacher', 'teacher123')">教员 teacher</span>
        <span class="demo-chip" @click="fill('leader', 'leader123')">领导 leader</span>
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

function fill(u: string, p: string) {
  form.username = u
  form.password = p
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
  display: flex;
  flex-direction: column;
  padding: 0 24px calc(32px + env(safe-area-inset-bottom));
}

.brand {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 68px 0 40px;
}
.brand-mark {
  width: 64px;
  height: 64px;
  margin: 0 auto 18px;
  border-radius: 18px;
  background: linear-gradient(150deg, #1f6feb, #0e7490);
  color: #fff;
  font-size: 28px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 20px rgba(14, 116, 144, 0.35);
}
.brand-title {
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #fff;
}
.brand-sub {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 0.4em;
  margin-top: 10px;
}

.dossier {
  position: relative;
  z-index: 1;
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 12px 32px rgba(5, 15, 40, 0.25);
}
.dossier-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #eef2f7;
  background: #f6f9fd;
}
.dossier-tab {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  letter-spacing: 0.15em;
}
.dossier-no {
  font-size: 11px;
  color: #94a3b8;
  letter-spacing: 0.1em;
}

.login-form {
  padding: 20px 16px 4px;
}
.login-form :deep(.van-field) {
  padding: 10px 4px;
}
.login-form :deep(.van-field__label) {
  width: 44px;
}
.login-form :deep(.van-cell::after) {
  display: none;
}
.login-btn-wrap {
  padding: 26px 4px 20px;
}

.demo-hints {
  position: relative;
  z-index: 1;
  margin-top: 30px;
  text-align: center;
}
.demo-title {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 0.35em;
  margin-bottom: 12px;
}
.demo-chips {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}
.demo-chip {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 99px;
  padding: 5px 12px;
  cursor: pointer;
  transition: background 0.15s;
}
.demo-chip:active {
  background: rgba(255, 255, 255, 0.14);
}
</style>
