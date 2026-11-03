<template>
  <div class="page-container">
    <PageHeader title="毕业申请" />

    <van-loading v-if="loading" class="loading-center" />

    <template v-if="!loading">
      <!-- Project Selector -->
      <div class="card">
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
        <h4 class="block-title">毕业资格</h4>
        <van-steps direction="vertical" :active="activeStep">
          <van-step v-for="(step, i) in steps" :key="i">
            <h5>{{ step.title }}</h5>
            <p>{{ step.desc }}</p>
          </van-step>
        </van-steps>
      </div>

      <!-- Application Status -->
      <div v-if="myApplication" class="card">
        <h4 class="block-title">申请状态</h4>
        <div class="flex-between mb-8">
          <span>状态</span>
          <StatusTag :status="myApplication.auditStatus" type="graduate" />
        </div>
        <div class="flex-between mb-8">
          <span>提交时间</span>
          <span>{{ formatDate(myApplication.applyTime || myApplication.createTime) }}</span>
        </div>
        <div v-if="myApplication.currentNodeLabel || myApplication.currentNode" class="flex-between mb-8">
          <span>当前节点</span>
          <span>{{ myApplication.currentNodeLabel || myApplication.currentNode }}</span>
        </div>
        <div v-if="myApplication.rejectReason" class="flex-between reject-row">
          <span>驳回原因</span>
          <span class="reject-text">{{ myApplication.rejectReason }}</span>
        </div>
      </div>

      <!-- Certificate -->
      <div v-if="certificate" class="card card--gilded cert-card">
        <h4 class="block-title">毕业证书</h4>
        <template v-if="certificate.certificateUrl">
          <div class="cert-plate">
            <span class="cert-seal seal seal--round">证</span>
            <p class="cert-meta">证书编号 <b class="num">{{ certificate.certificateNo || '-' }}</b></p>
            <p v-if="certificate.graduateTime" class="cert-meta">毕业时间 <b class="num">{{ formatDate(certificate.graduateTime) }}</b></p>
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
.block-title {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 700;
  color: var(--text-strong);
  letter-spacing: 0.08em;
  padding-left: 10px;
  position: relative;
  margin-bottom: 12px;
}
.block-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%) rotate(45deg);
  width: 6px;
  height: 6px;
  background: var(--seal);
  border-radius: 1px;
}
.cert-plate {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--paper-dim);
  border: 1px dashed var(--line-strong);
  border-radius: 10px;
  padding: 16px;
}
.cert-seal {
  width: 52px;
  height: 52px;
  font-size: 18px;
  flex: none;
}
.cert-meta {
  font-size: 13px;
  color: var(--text-sub);
  margin: 4px 0;
}
.cert-meta b {
  color: var(--text-strong);
  font-weight: 600;
  margin-left: 4px;
}
.reject-row {
  align-items: flex-start;
}
.reject-text {
  color: var(--danger);
  max-width: 65%;
  text-align: right;
}
.tip-action {
  color: var(--text-sub);
  font-size: 13px;
  text-align: center;
}
.no-cert {
  text-align: center;
  color: var(--text-sub);
  padding: 20px 0;
}
.bottom-action {
  padding: 12px 16px calc(16px + env(safe-area-inset-bottom));
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--paper-card);
  border-top: 1px solid var(--line);
  z-index: 5;
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}
</style>
