<template>
  <div class="page-container">
    <PageHeader title="毕业申请" />

    <van-loading v-if="loading" class="loading-center" />

    <template v-if="!loading">
      <!-- Project Selector -->
      <div class="card picker-card">
        <van-field
          v-model="selectedProjectName"
          is-link
          readonly
          label="培训班"
          placeholder="请选择培训班"
          @click="showProjectPicker = true"
        />
      </div>

      <van-popup v-model:show="showProjectPicker" round position="bottom">
        <van-picker
          :columns="projectColumns"
          @confirm="onProjectConfirm"
          @cancel="showProjectPicker = false"
        />
      </van-popup>

      <!-- Eligibility -->
      <div v-if="eligibility" class="card">
        <div class="card-head">
          <span class="kicker">GRADUATION CHECK</span>
          <span class="card-tag">{{ activeStep }}/3 已完成</span>
        </div>
        <h4 class="card-title">毕业资格</h4>

        <div class="credit-progress">
          <div class="credit-progress-top">
            <span>学分进度</span>
            <span class="credit-progress-num">
              <b>{{ eligibility.credits || 0 }}</b> / {{ eligibility.needCredit || 0 }}
            </span>
          </div>
          <van-progress
            :percentage="creditPercent"
            :show-pivot="false"
            color="var(--accent)"
            track-color="var(--surface-3)"
            stroke-width="6"
          />
        </div>

        <van-steps direction="vertical" :active="activeStep">
          <van-step v-for="(step, i) in steps" :key="i">
            <h5>{{ step.title }}</h5>
            <p>{{ step.desc }}</p>
          </van-step>
        </van-steps>
      </div>

      <!-- Application Status -->
      <div v-if="myApplication" class="card">
        <div class="card-head">
          <span class="kicker">APPLICATION</span>
          <StatusTag :status="myApplication.auditStatus" type="graduate" />
        </div>
        <h4 class="card-title mb-8">申请状态</h4>
        <div class="flex-between mb-8">
          <span class="info-label">提交时间</span>
          <span class="info-value">{{ formatDate(myApplication.applyTime || myApplication.createTime) }}</span>
        </div>
        <div v-if="myApplication.currentNodeLabel || myApplication.currentNode" class="flex-between">
          <span class="info-label">当前节点</span>
          <span class="info-value">{{ myApplication.currentNodeLabel || myApplication.currentNode }}</span>
        </div>
        <div v-if="myApplication.rejectReason" class="reject-note">
          <span class="reject-label">驳回原因</span>
          <span class="reject-text">{{ myApplication.rejectReason }}</span>
        </div>
      </div>

      <!-- Certificate -->
      <div v-if="certificate" class="cert-card">
        <span class="cert-watermark" aria-hidden="true">证</span>
        <div class="cert-head">
          <span class="cert-kicker">CERTIFICATE OF COMPLETION</span>
          <span class="cert-seal">验</span>
        </div>
        <h4 class="cert-title">毕业证书</h4>
        <template v-if="certificate.certificateUrl">
          <div class="cert-rule"></div>
          <div class="cert-meta">
            <div class="cert-meta-row">
              <span class="info-label">证书编号</span>
              <span class="info-value cert-no">{{ certificate.certificateNo || '-' }}</span>
            </div>
            <div v-if="certificate.graduateTime" class="cert-meta-row">
              <span class="info-label">毕业时间</span>
              <span class="info-value">{{ formatDate(certificate.graduateTime) }}</span>
            </div>
          </div>
          <van-button type="primary" block class="cert-btn" @click="downloadCert">
            查看 / 下载证书
          </van-button>
        </template>
        <p v-else class="no-cert">证书生成中，请稍后查看</p>
      </div>

      <!-- Apply Button -->
      <div v-if="selectedProjectId && eligibility?.canApply" class="bottom-action">
        <van-button type="primary" block round :loading="applying" @click="onApply">
          {{ myApplication?.auditStatus === 2 ? '重新提交毕业申请' : '提交毕业申请' }}
        </van-button>
      </div>
      <div v-else-if="eligibility && !eligibility.canApply && eligibility.reason" class="bottom-action tip-action">
        {{ eligibility.reason }}
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import {
  Button as VanButton, Field as VanField, Loading as VanLoading,
  Popup as VanPopup, Picker as VanPicker, Steps as VanSteps, Step as VanStep,
  Progress as VanProgress, showToast,
} from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { graduate, projects } from '@/api'
import { fileUrl } from '@/api/http'
import dayjs from 'dayjs'

const loading = ref(true)
const applying = ref(false)
const projectOptions = ref<any[]>([])
const selectedProjectId = ref<number | null>(null)
const selectedProjectName = ref('')
const showProjectPicker = ref(false)
const eligibility = ref<any>(null)
const myApplication = ref<any>(null)
const certificate = ref<any>(null)

const projectColumns = computed(() =>
  projectOptions.value.map((p: any) => ({ text: p.projectName, value: p.id }))
)

const steps = computed(() => {
  const e = eligibility.value
  if (!e) return []
  const creditOk = Number(e.credits || 0) >= Number(e.needCredit || 0)
  const app = myApplication.value
  return [
    {
      title: '学分要求',
      desc: creditOk
        ? `已获得 ${e.credits || 0} / ${e.needCredit || 0} 学分，已达标`
        : `已获得 ${e.credits || 0} / ${e.needCredit || 0} 学分，未达标`,
      done: creditOk,
    },
    {
      title: '提交申请',
      desc: app
        ? (app.auditStatus === 2 ? '申请已驳回，可重新提交' : '毕业申请已提交')
        : (e.canApply ? '当前可提交毕业申请' : '暂未提交'),
      done: Boolean(app),
    },
    {
      title: '审核结果',
      desc: app ? (app.auditStatusLabel || '审批中') : '待审核',
      done: app?.auditStatus === 1,
    },
  ]
})

const activeStep = computed(() => steps.value.filter((s) => s.done).length)

const creditPercent = computed(() => {
  const need = Number(eligibility.value?.needCredit || 0)
  const got = Number(eligibility.value?.credits || 0)
  if (!need) return 0
  return Math.min(100, Math.round((got / need) * 100))
})

function formatDate(d: string) {
  return d ? dayjs(d).format('YYYY-MM-DD HH:mm') : '-'
}

function onProjectConfirm({ selectedOptions }: any) {
  const opt = selectedOptions?.[0]
  if (opt) {
    selectedProjectId.value = opt.value
    selectedProjectName.value = opt.text
  }
  showProjectPicker.value = false
}

async function loadEligibility() {
  if (!selectedProjectId.value) return
  try {
    const [elig, app, cert] = await Promise.all([
      graduate.eligibility(selectedProjectId.value).catch(() => null),
      graduate.my(selectedProjectId.value).catch(() => null),
      graduate.myCertificate(selectedProjectId.value).catch(() => null),
    ])
    eligibility.value = elig
    myApplication.value = app
    certificate.value = cert
  } catch {
    // ignore
  }
}

async function onApply() {
  if (!selectedProjectId.value) return
  applying.value = true
  try {
    await graduate.apply(selectedProjectId.value)
    showToast('申请已提交')
    loadEligibility()
  } catch {
    // handled
  } finally {
    applying.value = false
  }
}

function downloadCert() {
  const url = fileUrl(certificate.value?.certificateUrl)
  if (url) {
    window.open(url, '_blank')
  }
}

watch(selectedProjectId, () => {
  loadEligibility()
})

onMounted(async () => {
  try {
    projectOptions.value = await projects.options()
    if (projectOptions.value.length > 0) {
      selectedProjectId.value = projectOptions.value[0].id
      selectedProjectName.value = projectOptions.value[0].projectName
    }
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.picker-card {
  margin-top: var(--sp-4);
  padding: 0;
}
.picker-card :deep(.van-cell) {
  background: transparent;
}
.picker-card :deep(.van-cell::after) {
  display: none;
}
.picker-card :deep(.van-field__label) {
  font-weight: 500;
  color: var(--ink-2);
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-2);
  margin-bottom: 8px;
}
.card-tag {
  flex: none;
  padding: 2px 8px;
  border: 1px solid var(--brand-line);
  border-radius: 999px;
  background: var(--brand-tint);
  font-size: 10px;
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}
.card-title {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.4;
  letter-spacing: 0.02em;
}

.credit-progress {
  margin: var(--sp-4) 0 var(--sp-3);
  padding: var(--sp-3);
  border: 1px solid var(--line);
  border-radius: var(--r-chip);
  background: var(--surface-2);
}
.credit-progress-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 9px;
  font-size: 12px;
  color: var(--ink-3);
}
.credit-progress-num {
  color: var(--ink-2);
  font-variant-numeric: tabular-nums;
}
.credit-progress-num b {
  font-family: var(--font-serif);
  font-size: 16px;
  color: var(--accent-strong);
}
.card h5 {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.5;
}
.card h5 + p {
  font-size: 12px;
  color: var(--ink-3);
  line-height: 1.6;
  margin-top: var(--sp-1);
}
.info-label {
  flex: none;
  font-size: 13px;
  color: var(--ink-3);
  margin-right: var(--sp-3);
}
.info-value {
  font-size: 13px;
  color: var(--ink);
  text-align: right;
  min-width: 0;
  line-height: 1.5;
  font-variant-numeric: tabular-nums;
}
.reject-note {
  margin-top: var(--sp-3);
  padding: var(--sp-3);
  background: var(--bad-tint);
  border-left: 3px solid var(--bad);
  border-radius: 0 var(--r-chip) var(--r-chip) 0;
}
.reject-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--bad);
}
.reject-text {
  display: block;
  margin-top: var(--sp-1);
  font-size: 13px;
  color: var(--bad);
  line-height: 1.55;
}
/* ---------- 证书 ---------- */
.cert-card {
  position: relative;
  overflow: hidden;
  margin: 0 var(--page-x) var(--sp-4);
  padding: 22px var(--sp-5) var(--sp-4);
  border: 1px solid var(--accent-line);
  border-radius: var(--r-card);
  background:
    radial-gradient(120% 100% at 0% 0%, rgba(173, 122, 53, 0.1) 0%, transparent 52%),
    #fcf9f1;
  box-shadow: inset 0 0 0 4px #fcf9f1, inset 0 0 0 5px var(--accent-line);
}

.cert-watermark {
  position: absolute;
  top: -12px;
  right: -4px;
  font-family: var(--font-serif);
  font-size: 88px;
  line-height: 1;
  color: rgba(173, 122, 53, 0.08);
  pointer-events: none;
  user-select: none;
}

.cert-head {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.cert-kicker {
  font-size: 8px;
  font-weight: 600;
  letter-spacing: 0.16em;
  color: var(--accent-strong);
  opacity: 0.72;
}

.cert-seal {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  margin-top: -4px;
  border: 1px solid var(--accent);
  border-radius: 50%;
  font-family: var(--font-serif);
  font-size: 13px;
  font-weight: 700;
  color: var(--accent-strong);
  opacity: 0.8;
  transform: rotate(-8deg);
}

.cert-title {
  margin-top: 6px;
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--brand-deep);
}

.cert-rule {
  height: 1px;
  margin: var(--sp-3) 0;
  background: linear-gradient(90deg, var(--accent) 0%, var(--accent-line) 38%, transparent 100%);
}

.cert-meta {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.cert-meta-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--sp-3);
}

.cert-no {
  font-family: var(--font-serif);
  font-size: 13px;
  letter-spacing: 0.03em;
  color: var(--accent-strong);
}

.cert-btn {
  height: 44px;
  margin-top: var(--sp-4);
  font-size: 14px;
  letter-spacing: 0.06em;
}

.no-cert {
  padding: var(--sp-5) 0 6px;
  font-size: 12px;
  text-align: center;
  color: var(--ink-3);
}
.bottom-action {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  padding: var(--sp-3) var(--page-x) calc(var(--sp-3) + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.94);
  border-top: 1px solid var(--line);
  backdrop-filter: saturate(180%) blur(12px);
  -webkit-backdrop-filter: saturate(180%) blur(12px);
}
.bottom-action :deep(.van-button) {
  height: 46px;
  font-size: 15px;
  font-weight: 600;
}
.tip-action {
  color: var(--warn);
  font-size: 13px;
  text-align: center;
  line-height: 1.5;
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: var(--sp-8) 0;
}
</style>
