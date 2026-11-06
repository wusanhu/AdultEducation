<template>
  <div class="login-page">
    <!-- 品牌区：与管理端登录页同一套深蓝渐变语言 -->
    <section class="brand-zone">
      <div class="brand-decor" aria-hidden="true">
        <span class="decor-grid" />
        <span class="decor-beam" />
        <span class="decor-glow" />
        <span class="decor-ring decor-ring--xl" />
        <span class="decor-ring decor-ring--lg" />
      </div>

      <header class="brand-head">
        <span class="brand-mark">继</span>
        <div class="brand-name">
          <strong>继续教育学分管理平台</strong>
          <span>ADULT EDUCATION CREDIT PLATFORM</span>
        </div>
      </header>

      <div class="brand-body">
        <p class="brand-eyebrow">LEARNING PORTAL · 学员学习门户</p>
        <h1 class="brand-title">让每一次学习<br />都被真实记录</h1>
        <p class="brand-desc">学习留痕 · 学分自动统计 · 毕业在线申请</p>
      </div>

      <ul class="brand-stats">
        <li>
          <strong>4<span>类</span></strong>
          <span>身份核验</span>
        </li>
        <li>
          <strong>3<span>级</span></strong>
          <span>审批流程</span>
        </li>
        <li>
          <strong>1<span>套</span></strong>
          <span>学分体系</span>
        </li>
      </ul>
    </section>

    <!-- 表单区 -->
    <section class="form-sheet">
      <div class="form-inner">
        <div class="form-head">
          <p class="form-eyebrow">STUDENT SIGN IN</p>
          <h2 class="form-title">登录学习平台</h2>
          <p class="form-sub">请使用单位开通的学员账号登录</p>
        </div>

        <van-form class="login-form" @submit="onSubmit">
          <div class="field">
            <label class="field-label" for="login-username">账号</label>
            <div class="field-box">
              <van-field
                id="login-username"
                v-model="form.username"
                name="username"
                placeholder="请输入账号"
                clearable
                left-icon="manager-o"
                autocomplete="username"
                :rules="[{ required: true, message: '请输入账号' }]"
              />
            </div>
          </div>
          <div class="field">
            <label class="field-label" for="login-password">密码</label>
            <div class="field-box">
              <van-field
                id="login-password"
                v-model="form.password"
                name="password"
                type="password"
                placeholder="请输入密码"
                left-icon="lock"
                autocomplete="current-password"
                :rules="[{ required: true, message: '请输入密码' }]"
              />
            </div>
          </div>
          <div class="login-btn-wrap">
            <van-button block type="primary" native-type="submit" :loading="loading">
              登 录
            </van-button>
          </div>
        </van-form>

        <div class="demo-block">
          <div class="demo-head">
            <span class="kicker">演示账号</span>
            <span class="demo-hint">点击自动填充</span>
          </div>
          <div class="demo-list">
            <button
              v-for="item in demoAccounts"
              :key="item.username"
              type="button"
              class="demo-item"
              :class="{ 'is-active': form.username === item.username }"
              @click="fill(item)"
            >
              <span class="demo-label">{{ item.role }}</span>
              <span class="demo-user">{{ item.username }}</span>
            </button>
          </div>
        </div>

        <p class="login-note">账号由所属单位统一开通，如无法登录请联系教务管理员。</p>
      </div>
    </section>

    <!-- 底部信息带 -->
    <footer class="page-footer">
      <div class="footer-row">
        <span>© {{ year }} 继续教育学分管理平台</span>
        <span class="footer-sep" />
        <span>学员端 v2.0</span>
      </div>
      <div class="footer-row">
        <span class="footer-muted">账号由所属单位统一开通</span>
        <a :href="adminUrl" class="footer-link">切换至管理端</a>
      </div>
    </footer>
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

const adminUrl = `${window.location.origin}/admin/`
const year = new Date().getFullYear()

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
  /* 登录页局部配色，与管理端登录页保持一致，不影响全局 token */
  --lp-primary: #1d4ed8;
  --lp-primary-hover: #1e40af;
  --lp-primary-deep: #0a1733;
  --lp-ink: #0f172a;
  --lp-muted: #64748b;
  --lp-subtle: #64748b;
  --lp-line: #e2e8f0;
  --lp-line-strong: #cdd6e4;
  --lp-surface: #ffffff;
  --lp-canvas: #f7f9fc;
  --lp-footer: #08122b;
  --lp-font: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, sans-serif;

  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
  overflow-x: hidden;
  overflow-y: auto;
  background: var(--lp-canvas);
  color: var(--lp-ink);
  font-family: var(--lp-font);
  -webkit-font-smoothing: antialiased;
}

/* ---------- 品牌区 ---------- */
.brand-zone {
  position: relative;
  overflow: hidden;
  flex: none;
  min-height: 40dvh;
  display: flex;
  flex-direction: column;
  padding: calc(env(safe-area-inset-top) + 18px) 22px clamp(30px, 5vh, 52px);
  color: #ffffff;
  background: linear-gradient(135deg, #0a1733 0%, #10245c 46%, #143aa8 100%);
}

.brand-decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

/* 窄屏放大网格尺度、减少元素数量，避免碎花 */
.decor-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.055) 1px, transparent 1px);
  background-size: 68px 68px;
  mask-image: radial-gradient(130% 110% at 15% 8%, #000 32%, transparent 82%);
  opacity: 0.9;
}

.decor-beam {
  position: absolute;
  left: -42%;
  top: 22%;
  width: 184%;
  height: 46%;
  transform: rotate(-17deg);
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(96, 165, 250, 0.1) 36%,
    rgba(147, 197, 253, 0.18) 52%,
    rgba(96, 165, 250, 0.08) 68%,
    transparent 100%
  );
  filter: blur(9px);
}

.decor-glow {
  position: absolute;
  right: -34%;
  bottom: -46%;
  width: 124vw;
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
  width: 118vw;
  aspect-ratio: 1;
  right: -36%;
  bottom: -58%;
}

.decor-ring--lg {
  width: 78vw;
  aspect-ratio: 1;
  right: -10%;
  bottom: -32%;
  border-color: rgba(191, 219, 254, 0.16);
}

.brand-zone > *:not(.brand-decor) {
  position: relative;
  z-index: 1;
}

.brand-head {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.brand-mark {
  flex: none;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.12);
}

.brand-name {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.brand-name strong {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.3;
  letter-spacing: 0.01em;
}

.brand-name span {
  font-size: 9px;
  letter-spacing: 0.14em;
  line-height: 1.4;
  color: rgba(191, 219, 254, 0.58);
  overflow-wrap: anywhere;
}

.brand-body {
  margin-top: clamp(16px, 3vh, 40px);
}

.brand-eyebrow {
  margin: 0;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: rgba(191, 219, 254, 0.78);
}

.brand-title {
  margin: 12px 0 0;
  font-size: clamp(30px, 9vw, 40px);
  font-weight: 600;
  line-height: 1.14;
  letter-spacing: -0.02em;
  color: #ffffff;
}

.brand-desc {
  margin: 12px 0 0;
  max-width: 340px;
  font-size: 13px;
  line-height: 1.7;
  color: rgba(226, 232, 240, 0.72);
}

.brand-stats {
  margin: auto 0 0;
  padding: clamp(10px, 2vh, 16px) 0 0;
  list-style: none;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.brand-stats li {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.brand-stats strong {
  font-size: 20px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: -0.02em;
  color: #ffffff;
  font-variant-numeric: tabular-nums;
}

.brand-stats strong span {
  margin-left: 2px;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0;
  color: rgba(191, 219, 254, 0.7);
}

.brand-stats li > span {
  font-size: 11px;
  letter-spacing: 0.04em;
  color: rgba(191, 219, 254, 0.62);
}

/* ---------- 表单区 ---------- */
.form-sheet {
  position: relative;
  flex: 1;
  margin-top: -22px;
  padding: 26px 16px calc(env(safe-area-inset-bottom) + 20px);
  border-radius: 24px 24px 0 0;
  background: var(--lp-surface);
}

.form-inner {
  width: 100%;
  max-width: 440px;
  margin: 0 auto;
}

.form-eyebrow {
  margin: 0;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.16em;
  color: var(--lp-primary);
}

.form-title {
  margin: 10px 0 0;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.3;
  letter-spacing: -0.01em;
  color: var(--lp-ink);
}

.form-sub {
  margin: 8px 0 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--lp-muted);
}

.login-form {
  margin-top: 24px;
}

.field + .field {
  margin-top: 16px;
}

.field-label {
  display: block;
  margin: 0 0 7px 2px;
  font-size: 12px;
  font-weight: 500;
  color: #334155;
}

.field-box {
  box-sizing: border-box;
  min-height: 48px;
  border: 1px solid var(--lp-line-strong);
  border-radius: 12px;
  background: #fbfcfe;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, background 0.16s ease;
}

.field-box:focus-within {
  border-color: var(--lp-primary);
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.12);
}

.field-box :deep(.van-cell) {
  min-height: 46px;
  padding: 0 13px;
  background: transparent;
}

.field-box :deep(.van-cell::after) {
  display: none;
}

.field-box :deep(.van-field__body) {
  min-height: 44px;
  align-items: center;
}

.field-box :deep(.van-field__left-icon) {
  display: flex;
  align-items: center;
  margin-right: 10px;
  color: var(--lp-subtle);
}

.field-box :deep(.van-field__left-icon .van-icon) {
  font-size: 18px;
}

.field-box :deep(.van-field__control) {
  font-size: 15px;
  color: var(--lp-ink);
}

.field-box :deep(.van-field__control::placeholder) {
  color: var(--lp-subtle);
}

.field-box :deep(.van-field__error-message) {
  padding: 2px 0 6px;
  font-size: 11.5px;
  color: #b4453f;
}

.field-box :deep(.van-field__clear) {
  color: var(--lp-subtle);
}

.login-btn-wrap {
  margin-top: 22px;
  /* 覆盖全局青色主色，登录页按钮为纯色 #1D4ED8 */
  --van-button-primary-background-color: var(--lp-primary);
  --van-button-primary-border-color: var(--lp-primary);
  --van-button-primary-color: #ffffff;
}

.login-btn-wrap :deep(.van-button) {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.14em;
  box-shadow: 0 12px 24px -16px rgba(29, 78, 216, 0.8);
}

/* 覆盖全局 Vant 主色按钮规则，登录页使用纯色 #1D4ED8 */
.login-btn-wrap :deep(.van-button--primary:not(.van-button--plain)) {
  background: var(--lp-primary);
  border: 0;
}

.login-btn-wrap :deep(.van-button--primary:not(.van-button--plain):active) {
  background: var(--lp-primary-hover);
}

/* ---------- 演示账号 ---------- */
.demo-block {
  margin-top: 26px;
  padding-top: 18px;
  border-top: 1px solid var(--lp-line);
}

.demo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--lp-muted);
}

.kicker {
  font-weight: 600;
  color: #334155;
}

.demo-hint {
  font-size: 11px;
  color: var(--lp-subtle);
}

.demo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.demo-item {
  flex: 1 1 0;
  min-width: 120px;
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 9px 11px;
  border: 1px solid var(--lp-line);
  border-radius: 9px;
  background: #fbfcfe;
  font-family: inherit;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
  -webkit-appearance: none;
  appearance: none;
  transition: border-color 0.15s ease, background 0.15s ease, box-shadow 0.15s ease;
}

.demo-item:active,
.demo-item:hover {
  border-color: var(--lp-line-strong);
  background: #ffffff;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.05);
}

.demo-item.is-active {
  border-color: var(--lp-primary);
  background: #eff4ff;
}

.demo-item:focus-visible {
  outline: 2px solid rgba(29, 78, 216, 0.4);
  outline-offset: 1px;
}

.demo-label {
  font-size: 12px;
  font-weight: 600;
  color: #1e293b;
}

.demo-user {
  font-size: 11px;
  color: var(--lp-subtle);
  font-variant-numeric: tabular-nums;
  overflow-wrap: anywhere;
}

.login-note {
  margin: 18px 0 0;
  font-size: 11px;
  line-height: 1.65;
  color: var(--lp-subtle);
}

/* ---------- 底部信息带 ---------- */
.page-footer {
  flex: none;
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 12px 16px calc(env(safe-area-inset-bottom) + 12px);
  background: var(--lp-footer);
  font-size: 11.5px;
  letter-spacing: 0.02em;
  color: rgba(226, 232, 240, 0.58);
}

.footer-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 0;
}

.footer-sep {
  width: 1px;
  height: 11px;
  background: rgba(255, 255, 255, 0.18);
}

.footer-muted {
  color: rgba(148, 163, 184, 0.6);
}

.footer-link {
  color: #bfdbfe;
  text-decoration: none;
}

.footer-link:active {
  color: #ffffff;
  text-decoration: underline;
}

/* ---------- 窄屏微调：360px ---------- */
@media (max-width: 360px) {
  .brand-zone {
    padding-left: 18px;
    padding-right: 18px;
  }

  .brand-desc {
    font-size: 12.5px;
  }

  .brand-stats {
    gap: 6px;
  }

  .form-sheet {
    padding-left: 14px;
    padding-right: 14px;
  }

  .form-title {
    font-size: 21px;
  }
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
