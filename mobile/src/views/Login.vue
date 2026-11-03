<template>
  <div class="login-page">
    <div class="login-header">
      <h1 class="login-title">继续教育学分管理</h1>
      <p class="login-subtitle">移动端学习平台</p>
    </div>
    <van-form @submit="onSubmit" class="login-form">
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          name="username"
          label="账号"
          placeholder="请输入账号"
          :rules="[{ required: true, message: '请输入账号' }]"
          left-icon="manager-o"
        />
        <van-field
          v-model="form.password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
          left-icon="lock"
        />
      </van-cell-group>
      <div class="login-btn-wrap">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          登 录
        </van-button>
      </div>
    </van-form>
    <div class="demo-hints">
      <p class="demo-title">演示账号：</p>
      <p>学员：student / student123</p>
      <p>教师：teacher / teacher123</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Form as VanForm, Field as VanField, CellGroup as VanCellGroup, Button as VanButton, showToast } from 'vant'
import { auth } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

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
  background: linear-gradient(135deg, #1989fa 0%, #07c160 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 24px;
}
.login-header {
  text-align: center;
  margin-bottom: 40px;
}
.login-title {
  color: #fff;
  font-size: 26px;
  font-weight: 700;
}
.login-subtitle {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  margin-top: 8px;
}
.login-form {
  background: #fff;
  border-radius: 12px;
  padding: 24px 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.6);
}
.login-btn-wrap {
  padding: 24px 16px 8px;
}
.demo-hints {
  margin-top: 24px;
  text-align: center;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  line-height: 1.8;
}
.demo-title {
  font-weight: 600;
  margin-bottom: 4px;
}
</style>
