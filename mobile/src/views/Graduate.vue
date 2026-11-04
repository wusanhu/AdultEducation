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
        <h4 class="card-title mb-8">毕业资格</h4>
        <van-steps direction="vertical" :active="activeStep">
          <van-step v-for="(step, i) in steps" :key="i">
            <h5>{{ step.title }}</h5>
            <p>{{ step.desc }}</p>
          </van-step>
        </van-steps>
      </div>

      <!-- Application Status -->
      <div v-if="myApplication" class="card">
        <h4 class="card-title mb-8">申请状态</h4>
        <div class="flex-between mb-8">
          <span class="info-label">状态</span>
          <StatusTag :status="myApplication.auditStatus" type="graduate" />
        </div>
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
      <div v-if="certificate" class="card">
        <h4 class="card-title mb-8">毕业证书</h4>
        <template v-if="certificate.certificateUrl">
          <div class="flex-between mb-8">
            <span class="info-label">证书编号</span>
            <span class="info-value">{{ certificate.certificateNo || '-' }}</span>
          </div>
          <div v-if="certificate.graduateTime" class="flex-between">
            <span class="info-label">毕业时间</span>
            <span class="info-value">{{ formatDate(certificate.graduateTime) }}</span>
          </div>
          <van-button type="primary" block round class="mt-12" @click="downloadCert">
            查看 / 下载证书
          </van-button>
        </template>
        <p v-else class="no-cert">证书生成中...</p>
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
  showToast,
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
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.5;
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
.no-cert {
  text-align: center;
  color: var(--ink-3);
  font-size: 13px;
  padding: var(--sp-5) 0;
}
.bottom-action {
  padding: var(--sp-3) var(--page-x) calc(var(--sp-3) + env(safe-area-inset-bottom));
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--surface);
  border-top: 1px solid var(--line);
}
.bottom-action :deep(.van-button) {
  height: 46px;
  font-size: 16px;
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
