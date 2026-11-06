<template>
  <div class="login-page">
    <div class="login-main">
      <!-- 品牌区：满高出血 Hero -->
      <section class="brand-panel">
        <div class="brand-decor" aria-hidden="true">
          <span class="decor-grid" />
          <span class="decor-beam" />
          <span class="decor-glow" />
          <span class="decor-ring decor-ring--xl" />
          <span class="decor-ring decor-ring--lg" />
          <span class="decor-ring decor-ring--sm" />
        </div>

        <header class="brand-head">
          <span class="brand-mark">继</span>
          <div class="brand-name">
            <strong>继续教育学分管理平台</strong>
            <span>ADULT EDUCATION CREDIT PLATFORM</span>
          </div>
        </header>

        <div class="brand-body">
          <p class="brand-eyebrow">MANAGEMENT CONSOLE · 继续教育管理控制台</p>
          <h1 class="brand-title">让每一次学习<br />都被真实记录</h1>
          <p class="brand-desc">
            视频学习过程留痕、身份核验防替学、学分自动统计、毕业审批与电子证书归档，
            构建完整的继续教育学习闭环。
          </p>
          <p class="brand-desc-mobile">构建完整的继续教育学习闭环</p>
        </div>

        <ul class="brand-stats">
          <li>
            <strong>4<span>类</span></strong>
            <span>身份核验档位</span>
          </li>
          <li>
            <strong>3<span>级</span></strong>
            <span>毕业审批流程</span>
          </li>
          <li>
            <strong>1<span>套</span></strong>
            <span>学分全生命周期</span>
          </li>
        </ul>
      </section>

      <!-- 表单区 -->
      <section class="form-panel">
        <div class="form-inner">
          <div class="form-head">
            <p class="form-eyebrow">ADMIN CONSOLE</p>
            <h2 class="form-title">登录管理端</h2>
            <p class="form-sub">请使用管理员、单位领导或教员账号登录</p>
          </div>

          <a-form :model="form" layout="vertical" class="login-form" @finish="submit">
            <a-form-item label="账号" name="username" :rules="[{ required: true, message: '请输入账号' }]">
              <a-input
                ref="usernameRef"
                v-model:value="form.username"
                size="large"
                class="login-input"
                placeholder="请输入账号"
                allow-clear
                autocomplete="username"
              >
                <template #prefix><UserOutlined class="input-icon" /></template>
              </a-input>
            </a-form-item>
            <a-form-item label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
              <a-input-password
                v-model:value="form.password"
                size="large"
                class="login-input"
                placeholder="请输入密码"
                autocomplete="current-password"
              >
                <template #prefix><LockOutlined class="input-icon" /></template>
              </a-input-password>
            </a-form-item>
            <a-button type="primary" html-type="submit" size="large" block class="login-btn" :loading="loading">
              登 录
            </a-button>
          </a-form>

          <div class="demo-block">
            <div class="demo-head">
              <span>演示账号</span>
              <span class="demo-hint">点击自动填充</span>
            </div>
            <div class="demo-list">
              <button
                v-for="account in demoAccounts"
                :key="account.username"
                type="button"
                class="demo-item"
                :class="{ 'is-active': form.username === account.username }"
                @click="fill(account)"
              >
                <span class="demo-label">{{ account.label }}</span>
                <span class="demo-user">{{ account.username }}</span>
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 底部信息带 -->
    <footer class="page-footer">
      <div class="footer-left">
        <span>© {{ year }} 继续教育学分管理平台</span>
        <span class="footer-sep" />
        <span>管理控制台 v2.0</span>
      </div>
      <div class="footer-right">
        <span class="footer-placeholder">备案号：待补充</span>
        <span class="footer-placeholder">服务电话：400-000-0000</span>
        <a :href="mobileUrl" target="_blank" class="footer-link">学员移动端 H5</a>
      </div>
    </footer>

    <a-modal
      v-model:open="pwdOpen"
      title="首次登录需修改初始密码"
      :width="420"
      :closable="false"
      :mask-closable="false"
      :confirm-loading="pwdLoading"
      @ok="changePwd"
    >
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
import { onMounted, reactive, ref } from 'vue'
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
const usernameRef = ref<{ focus: () => void } | null>(null)
const year = new Date().getFullYear()

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

onMounted(() => {
  usernameRef.value?.focus()
})
</script>

<style scoped>
.login-page {
  /* 登录页局部配色，不影响全局 token */
  --lp-primary: #1d4ed8;
  --lp-primary-hover: #1e40af;
  --lp-primary-deep: #0a1733;
  --lp-ink: #0f172a;
  --lp-muted: #64748b;
  --lp-subtle: #94a3b8;
  --lp-line: #e2e8f0;
  --lp-line-strong: #cdd6e4;
  --lp-surface: #ffffff;
  --lp-canvas: #f7f9fc;
  --lp-footer: #08122b;
  --lp-font: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', system-ui, sans-serif;

  height: 100vh;
  height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  background: var(--lp-canvas);
  font-family: var(--lp-font);
  color: var(--lp-ink);
  -webkit-font-smoothing: antialiased;
}

.login-main {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 56fr 44fr;
}

/* ---------- 左侧品牌区 ---------- */
.brand-panel {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: clamp(36px, 4.4vw, 80px) clamp(36px, 5.4vw, 96px) clamp(32px, 3.4vw, 56px);
  color: #ffffff;
  background: linear-gradient(135deg, #0a1733 0%, #10245c 46%, #143aa8 100%);
}
.brand-decor { position: absolute; inset: 0; pointer-events: none; }
.decor-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: clamp(56px, 6vw, 88px) clamp(56px, 6vw, 88px);
  mask-image: radial-gradient(120% 100% at 20% 20%, #000 30%, transparent 82%);
  opacity: 0.9;
}
.decor-beam {
  position: absolute;
  left: -30%;
  top: 18%;
  width: 150%;
  height: 34%;
  transform: rotate(-16deg);
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(96, 165, 250, 0.12) 36%,
    rgba(147, 197, 253, 0.2) 52%,
    rgba(96, 165, 250, 0.1) 68%,
    transparent 100%
  );
  filter: blur(10px);
}
.decor-glow {
  position: absolute;
  right: -18%;
  bottom: -38%;
  width: 78vw;
  max-width: 1040px;
  aspect-ratio: 1;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.42) 0%, rgba(37, 99, 235, 0.14) 44%, transparent 72%);
}
.decor-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.decor-ring--xl {
  width: 78vw;
  max-width: 1120px;
  aspect-ratio: 1;
  right: -26%;
  bottom: -52%;
}
.decor-ring--lg {
  width: 52vw;
  max-width: 760px;
  aspect-ratio: 1;
  right: -8%;
  bottom: -32%;
  border-color: rgba(191, 219, 254, 0.16);
}
.decor-ring--sm {
  width: 26vw;
  max-width: 380px;
  aspect-ratio: 1;
  right: 14%;
  bottom: -8%;
  border-color: rgba(191, 219, 254, 0.2);
}
.brand-panel > *:not(.brand-decor) { position: relative; z-index: 1; }

.brand-head { display: flex; align-items: center; gap: 14px; }
.brand-mark {
  flex: none;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 21px;
  font-weight: 700;
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.12);
}
.brand-name { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.brand-name strong { font-size: 17px; font-weight: 600; letter-spacing: 0.01em; }
.brand-name span { font-size: 10px; letter-spacing: 0.2em; color: rgba(191, 219, 254, 0.58); }

.brand-body { margin-top: auto; padding-top: clamp(40px, 7vh, 100px); }
.brand-eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.16em;
  color: rgba(191, 219, 254, 0.78);
}
.brand-title {
  margin: clamp(16px, 2vh, 26px) 0 0;
  font-size: clamp(44px, 4.6vw, 72px);
  font-weight: 600;
  line-height: 1.1;
  letter-spacing: -0.03em;
  color: #ffffff;
}
.brand-desc {
  margin: clamp(20px, 2.6vh, 32px) 0 0;
  max-width: 560px;
  font-size: clamp(15px, 1.05vw, 18px);
  line-height: 1.85;
  color: rgba(226, 232, 240, 0.72);
}
.brand-desc-mobile { display: none; }

.brand-stats {
  margin: clamp(28px, 4.4vh, 52px) 0 0;
  padding: clamp(20px, 2.6vh, 30px) 0 0;
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: clamp(28px, 4vw, 64px);
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}
.brand-stats li { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.brand-stats strong {
  font-size: clamp(28px, 2.1vw, 34px);
  font-weight: 600;
  line-height: 1;
  letter-spacing: -0.02em;
  color: #ffffff;
  font-variant-numeric: tabular-nums;
}
.brand-stats strong span {
  margin-left: 3px;
  font-size: 13px;
  font-weight: 500;
  color: rgba(191, 219, 254, 0.7);
  letter-spacing: 0;
}
.brand-stats li > span { font-size: 12px; color: rgba(191, 219, 254, 0.62); letter-spacing: 0.04em; }

/* ---------- 右侧表单区 ---------- */
.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px clamp(32px, 4vw, 72px);
  background: var(--lp-surface);
  overflow-y: auto;
}
.form-inner { width: 100%; max-width: 404px; }

.form-eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.18em;
  color: var(--lp-primary);
}
.form-title {
  margin: 14px 0 0;
  font-size: 30px;
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1.3;
  color: var(--lp-ink);
}
.form-sub { margin: 10px 0 0; font-size: 14px; line-height: 1.6; color: var(--lp-muted); }

.login-form { margin-top: 36px; }
.login-form :deep(.ant-form-item) { margin-bottom: 20px; }
.login-form :deep(.ant-form-item-label > label) {
  font-size: 12.5px;
  font-weight: 500;
  color: #334155;
  height: auto;
  margin-bottom: 7px;
}
.login-form :deep(.ant-input-affix-wrapper),
.login-form :deep(.ant-input) {
  height: 48px;
  padding: 0 14px;
  border-radius: 12px;
  border-color: var(--lp-line-strong);
  background: #fbfcfe;
  font-size: 14px;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, background 0.16s ease;
}
.login-form :deep(.ant-input-affix-wrapper:hover),
.login-form :deep(.ant-input:hover) { border-color: #94a3b8; }
.login-form :deep(.ant-input-affix-wrapper-focused),
.login-form :deep(.ant-input-affix-wrapper:focus-within),
.login-form :deep(.ant-input:focus) {
  border-color: var(--lp-primary);
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.12);
}
.login-form :deep(.ant-input-affix-wrapper .ant-input) {
  height: auto;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}
.login-form :deep(.ant-form-item-explain-error) { padding-top: 4px; font-size: 12px; }
.input-icon { color: #94a3b8; }

.login-btn {
  height: 50px !important;
  margin-top: 6px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.12em;
  border: 1px solid transparent !important;
  border-radius: 12px !important;
  background: var(--lp-primary) !important;
  box-shadow: 0 14px 30px -18px rgba(29, 78, 216, 0.7) !important;
  transition: background 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}
.login-btn:hover { background: var(--lp-primary-hover) !important; }
.login-btn:active { background: var(--lp-primary-deep) !important; transform: translateY(1px); }
.login-btn:focus-visible { outline: 3px solid rgba(29, 78, 216, 0.28); outline-offset: 2px; }
.login-btn[disabled] { opacity: 0.65; }
.login-btn.ant-btn-loading { opacity: 0.85; }

.demo-block { margin-top: 34px; padding-top: 22px; border-top: 1px solid var(--lp-line); }
.demo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--lp-muted);
}
.demo-hint { font-size: 11.5px; color: var(--lp-subtle); }
.demo-list { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; margin-top: 10px; }
.demo-item {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 9px 11px;
  border: 1px solid var(--lp-line);
  border-radius: 9px;
  background: #fbfcfe;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease, box-shadow 0.15s ease;
}
.demo-item:hover { border-color: var(--lp-line-strong); background: #ffffff; box-shadow: 0 2px 6px rgba(15, 23, 42, 0.05); }
.demo-item.is-active { border-color: var(--lp-primary); background: #eff4ff; }
.demo-item:focus-visible { outline: 2px solid rgba(29, 78, 216, 0.4); outline-offset: 1px; }
.demo-label { font-size: 12px; font-weight: 600; color: #1e293b; }
.demo-user { font-size: 11px; color: var(--lp-subtle); font-variant-numeric: tabular-nums; }

/* ---------- 底部信息带 ---------- */
.page-footer {
  flex: none;
  min-height: 52px;
  padding: 0 clamp(20px, 3.4vw, 64px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: var(--lp-footer);
  color: rgba(226, 232, 240, 0.58);
  font-size: 12px;
  letter-spacing: 0.02em;
}
.footer-left, .footer-right { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; min-width: 0; }
.footer-sep { width: 1px; height: 12px; background: rgba(255, 255, 255, 0.18); }
.footer-placeholder { color: rgba(148, 163, 184, 0.6); }
.footer-link { color: #bfdbfe; text-decoration: none; transition: color 0.15s ease; }
.footer-link:hover { color: #ffffff; text-decoration: underline; }

/* ---------- 响应式 ---------- */
@media (max-width: 1100px) {
  .brand-panel { padding: 40px 40px 36px; }
  .brand-title { font-size: clamp(40px, 5vw, 54px); }
  .brand-desc { font-size: 15px; }
  .form-panel { padding: 40px 32px; }
}

@media (max-width: 900px) {
  .login-page { height: auto; min-height: 100dvh; overflow-y: auto; }
  .login-main { flex: none; grid-template-columns: 1fr; grid-template-rows: auto 1fr; min-height: 0; }
  .brand-panel { min-height: 240px; padding: 28px 26px 26px; }
  .brand-body { padding-top: 28px; }
  .brand-eyebrow { font-size: 10px; letter-spacing: 0.12em; }
  .brand-title { margin-top: 12px; font-size: 34px; line-height: 1.14; }
  .brand-desc { display: none; }
  .brand-desc-mobile { display: block; margin: 12px 0 0; font-size: 13px; color: rgba(226, 232, 240, 0.7); }
  .brand-stats { display: none; }
  .decor-ring--sm { display: none; }
  .form-panel {
    align-items: flex-start;
    padding: 34px 26px 40px;
    overflow: visible;
  }
  .form-inner { max-width: none; margin: 0 auto; max-width: 480px; }
  .form-title { font-size: 26px; }
  .page-footer {
    min-height: 0;
    padding: 14px 26px;
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    font-size: 11.5px;
  }
  .footer-placeholder { display: none; }
  .footer-right { gap: 10px; }
}

@media (max-width: 420px) {
  .login-page { min-height: 100dvh; }
  .brand-panel { min-height: 214px; padding: 22px 22px 20px; }
  .brand-title { font-size: 32px; }
  .brand-mark { width: 38px; height: 38px; font-size: 18px; }
  .brand-name strong { font-size: 15.5px; }
  .brand-name span { font-size: 9px; letter-spacing: 0.14em; }
  .form-panel { padding: 24px 22px 24px; }
  .form-title { font-size: 24px; }
  .form-sub { font-size: 13px; }
  .login-form { margin-top: 26px; }
  .page-footer { padding: 12px 22px; }
}

@media (max-width: 360px) {
  .demo-list { grid-template-columns: 1fr; }
  .demo-item { flex-direction: row; align-items: baseline; gap: 8px; }
}

@media (prefers-reduced-motion: reduce) {
  .login-page *,
  .login-page *::before,
  .login-page *::after {
    transition: none !important;
    animation: none !important;
  }
}
</style>
