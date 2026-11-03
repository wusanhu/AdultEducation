<template>
  <div class="login-bg" style="display:flex;align-items:center;justify-content:center;padding:24px">
    <div style="width:1080px;max-width:100%;display:grid;grid-template-columns:1.15fr .85fr;gap:0;border-radius:20px;overflow:hidden;box-shadow:0 30px 80px rgba(0,0,0,.35);background:#fff">
      <div style="padding:58px 54px;background:linear-gradient(150deg,#0d1b2e,#143f6b 60%,#0e7490);color:#fff;position:relative;overflow:hidden">
        <div style="position:absolute;right:-90px;top:-90px;width:300px;height:300px;border-radius:50%;background:rgba(255,255,255,.07)"></div>
        <div style="position:absolute;right:40px;bottom:-120px;width:260px;height:260px;border-radius:50%;background:rgba(56,189,248,.12)"></div>
        <div style="display:flex;align-items:center;gap:12px;margin-bottom:42px">
          <div style="width:42px;height:42px;border-radius:12px;background:#38bdf8;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:20px;color:#082f49">继</div>
          <div>
            <div style="font-size:17px;font-weight:600;letter-spacing:1px">继续教育学分管理平台</div>
            <div style="font-size:12px;opacity:.65;margin-top:2px">ADULT EDUCATION CREDIT PLATFORM</div>
          </div>
        </div>
        <h1 style="font-size:38px;line-height:1.3;margin:0 0 18px;font-weight:700">让每一次学习<br />都被真实记录</h1>
        <p style="font-size:14px;line-height:1.9;opacity:.78;margin:0 0 40px;max-width:400px">
          视频学习过程留痕、身份核验防替学、学分自动统计、毕业审批与电子证书归档，构建完整的继续教育学习闭环。
        </p>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;max-width:440px">
          <div v-for="item in features" :key="item.title" style="padding:14px 16px;border:1px solid rgba(255,255,255,.14);border-radius:10px;background:rgba(255,255,255,.05)">
            <div style="font-size:14px;font-weight:600;margin-bottom:4px">{{ item.title }}</div>
            <div style="font-size:12px;opacity:.65">{{ item.desc }}</div>
          </div>
        </div>
      </div>

      <div style="padding:64px 52px;display:flex;flex-direction:column;justify-content:center">
        <div style="font-size:24px;font-weight:700;color:#0f172a;margin-bottom:6px">登录管理端</div>
        <div style="font-size:13px;color:#64748b;margin-bottom:34px">使用管理员、领导或教员账号登录</div>

        <a-form :model="form" layout="vertical" @finish="submit">
          <a-form-item label="账号" name="username" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input v-model:value="form.username" size="large" placeholder="请输入账号" allow-clear>
              <template #prefix><UserOutlined style="color:#94a3b8" /></template>
            </a-input>
          </a-form-item>
          <a-form-item label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
            <a-input-password v-model:value="form.password" size="large" placeholder="请输入密码">
              <template #prefix><LockOutlined style="color:#94a3b8" /></template>
            </a-input-password>
          </a-form-item>
          <a-button type="primary" html-type="submit" size="large" block :loading="loading" style="height:46px;font-size:15px;margin-top:8px">
            登 录
          </a-button>
        </a-form>

        <div style="margin-top:30px;padding-top:20px;border-top:1px dashed #e2e8f0">
          <div style="font-size:12px;color:#94a3b8;margin-bottom:10px">演示账号（点击填充）</div>
          <div style="display:flex;flex-wrap:wrap;gap:8px">
            <a-tag v-for="account in demoAccounts" :key="account.username" style="cursor:pointer;margin:0" @click="fill(account)">
              {{ account.label }} {{ account.username }}
            </a-tag>
          </div>
        </div>
        <div style="margin-top:22px;font-size:12px;color:#94a3b8">
          学员请使用移动端 H5：<a :href="mobileUrl" target="_blank">{{ mobileUrl }}</a>
        </div>
      </div>
    </div>

    <a-modal v-model:open="pwdOpen" title="首次登录需修改初始密码" :width="420" :closable="false" :mask-closable="false" :confirm-loading="pwdLoading" @ok="changePwd">
      <a-alert type="info" show-icon message="为保障账号安全，请设置包含字母与数字、长度 8-32 位的新密码。" style="margin-bottom:16px" />
      <a-form layout="vertical">
        <a-form-item label="新密码" required>
          <a-input-password v-model:value="pwdForm.newPassword" placeholder="至少 8 位，包含字母和数字" />
        </a-form-item>
        <a-form-item label="确认新密码" required>
          <a-input-password v-model:value="pwdForm.confirm" placeholder="请再次输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import { authApi } from '../api'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const pwdOpen = ref(false)
const pwdLoading = ref(false)
const pwdForm = reactive({ newPassword: '', confirm: '' })
const form = reactive({ username: 'admin', password: 'admin123' })
const mobileUrl = `${window.location.origin}/m/`

const features = [
  { title: '视频学习监管', desc: '心跳计时 · 切屏检测 · 防拖拽' },
  { title: '身份核验', desc: '人脸比对 · 短信降级 · 异常复核' },
  { title: '学分闭环', desc: '自动发放 · 补录审批 · 撤销留痕' },
  { title: '毕业归档', desc: '三级审批 · 电子证书 · 档案导出' }
]
const demoAccounts = [
  { label: '管理员', username: 'admin', password: 'admin123' },
  { label: '领导', username: 'leader', password: 'leader123' },
  { label: '教员', username: 'teacher', password: 'teacher123' }
]

function fill(account: { username: string; password: string }) {
  form.username = account.username
  form.password = account.password
}

async function submit() {
  loading.value = true
  try {
    await auth.login(form.username.trim(), form.password)
    if (auth.mustChangePwd) {
      pwdOpen.value = true
      return
    }
    message.success(`欢迎回来，${auth.displayName}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (e) {
    message.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

async function changePwd() {
  if (pwdForm.newPassword.length < 8) {
    message.warning('新密码至少 8 位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    message.warning('两次输入的新密码不一致')
    return
  }
  pwdLoading.value = true
  try {
    await authApi.changePassword(form.password, pwdForm.newPassword)
    auth.mustChangePwd = false
    localStorage.setItem('admin_must_change', '0')
    pwdOpen.value = false
    message.success('密码修改成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (e) {
    message.error((e as Error).message)
  } finally {
    pwdLoading.value = false
  }
}
</script>
