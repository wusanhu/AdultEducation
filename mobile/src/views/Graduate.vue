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
        <h4 class="mb-8">毕业资格</h4>
        <van-steps direction="vertical" :active="activeStep">
          <van-step v-for="(step, i) in steps" :key="i">
            <h5>{{ step.title }}</h5>
            <p>{{ step.desc }}</p>
          </van-step>
        </van-steps>
      </div>

      <!-- Application Status -->
      <div v-if="myApplication" class="card">
        <h4 class="mb-8">申请状态</h4>
        <div class="flex-between mb-8">
          <span>状态</span>
          <StatusTag :status="myApplication.auditStatus" type="graduate" />
        </div>
        <div class="flex-between mb-8">
          <span>提交时间</span>
          <span>{{ formatDate(myApplication.createTime) }}</span>
        </div>
        <div v-if="myApplication.currentNode" class="flex-between mb-8">
          <span>当前节点</span>
          <span>{{ myApplication.currentNode }}</span>
        </div>
        <div v-if="myApplication.opinion" class="flex-between">
          <span>审批意见</span>
          <span>{{ myApplication.opinion }}</span>
        </div>
      </div>

      <!-- Certificate -->
      <div v-if="certificate" class="card">
        <h4 class="mb-8">毕业证书</h4>
        <div v-if="certificate.certificateUrl" class="cert-preview">
          <img :src="certificate.certificateUrl" class="cert-img" />
          <van-button type="primary" plain size="small" round class="mt-8" @click="downloadCert">
            下载证书
          </van-button>
        </div>
        <p v-else class="no-cert">证书生成中...</p>
      </div>

      <!-- Apply Button -->
      <div v-if="!myApplication && selectedProjectId && eligibility?.eligible" class="bottom-action">
        <van-button type="primary" block round :loading="applying" @click="onApply">
          提交毕业申请
        </van-button>
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
  return [
    { title: '课程学习', desc: e.coursesCompleted !== false ? '已完成' : '未完成' },
    { title: '考试通过', desc: e.examsPassed !== false ? '已完成' : '未完成' },
    { title: '学分达标', desc: `${e.earnedCredits || 0}/${e.requiredCredits || 0}` },
    { title: '身份核验', desc: e.verified !== false ? '已通过' : '待核验' },
  ]
})

const activeStep = computed(() => {
  return steps.value.filter((s) => s.desc.includes('已完成') || s.desc.includes('已通过') || s.desc.includes('达标')).length
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
  if (certificate.value?.certificateUrl) {
    window.open(certificate.value.certificateUrl, '_blank')
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
.cert-preview {
  text-align: center;
}
.cert-img {
  width: 100%;
  max-width: 300px;
  border-radius: 8px;
}
.no-cert {
  text-align: center;
  color: #969799;
  padding: 20px 0;
}
.bottom-action {
  padding: 16px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}
</style>
