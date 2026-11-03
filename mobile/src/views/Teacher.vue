<template>
  <div class="page-container">
    <PageHeader title="教务管理" />

    <van-tabs v-model:active="activeTab" sticky>
      <van-tab v-if="isLeader" title="数据看板" name="dashboard">
        <van-loading v-if="dashboardLoading" class="loading-center" />
        <template v-else>
          <div class="card stat-grid">
            <div class="stat-item">
              <b>{{ overview.studentCount ?? '-' }}</b>
              <span>学员数</span>
            </div>
            <div class="stat-item">
              <b>{{ overview.studyCount ?? '-' }}</b>
              <span>学习中</span>
            </div>
            <div class="stat-item">
              <b>{{ overview.graduateCount ?? '-' }}</b>
              <span>已毕业</span>
            </div>
            <div class="stat-item">
              <b class="orange">{{ overview.pendingGraduateCount ?? '-' }}</b>
              <span>待审批</span>
            </div>
          </div>

          <div class="section-title">预警学员</div>
          <div v-if="warnings.length === 0" class="card empty-tip">暂无预警学员</div>
          <div v-for="item in warnings.slice(0, 20)" :key="item.type + '-' + item.userId + '-' + item.projectId" class="card audit-item">
            <div class="flex-between mb-8">
              <span class="audit-name">{{ item.userName }}（{{ item.orgName || '-' }}）</span>
              <van-tag type="warning">{{ item.typeLabel }}</van-tag>
            </div>
            <div class="audit-meta">
              <span>{{ item.projectName }}</span>
            </div>
            <p class="audit-remark mt-8">{{ item.message }}</p>
          </div>
        </template>
      </van-tab>

      <van-tab v-if="canMonitor" title="学习进度" name="study">
        <van-loading v-if="studyLoading" class="loading-center" />
        <div v-if="!studyLoading && studyList.length === 0" style="padding-top: 40px;">
          <EmptyState description="暂无学习记录" />
        </div>
        <div v-for="item in studyList" :key="item.id" class="card audit-item">
          <div class="flex-between mb-8">
            <span class="audit-name">{{ item.userName }} · {{ item.courseName }}</span>
            <span class="status-text">{{ item.statusLabel || '-' }}</span>
          </div>
          <div class="audit-meta">
            <span>{{ item.projectName }}</span>
            <span>有效学习 {{ formatDuration(item.validDuration) }}</span>
          </div>
          <van-progress
            class="mt-8"
            :percentage="Math.min(100, Math.round(item.progress || 0))"
            :show-pivot="false"
            color="#1989fa"
            stroke-width="4"
          />
        </div>
      </van-tab>

      <van-tab v-if="canMonitor" title="考试成绩" name="exam">
        <van-loading v-if="examLoading" class="loading-center" />
        <div v-if="!examLoading && examList.length === 0" style="padding-top: 40px;">
          <EmptyState description="暂无考试记录" />
        </div>
        <div v-for="item in examList" :key="item.id" class="card audit-item">
          <div class="flex-between mb-8">
            <span class="audit-name">{{ item.userName }} · {{ item.courseName }}</span>
            <van-tag :type="item.isPass === 1 ? 'success' : 'danger'">
              {{ item.isPass === 1 ? '已通过' : '未通过' }}
            </van-tag>
          </div>
          <div class="audit-meta">
            <span>{{ item.projectName }}</span>
            <span>第 {{ item.attemptNo }} 次</span>
            <span>得分 {{ item.score }}</span>
          </div>
          <div class="audit-meta mt-8">
            <span>提交时间：{{ formatTime(item.submitTime) }}</span>
          </div>
        </div>
      </van-tab>

      <van-tab v-if="canAuditCredit" title="学分审核" name="credit">
        <van-loading v-if="creditLoading" class="loading-center" />
        <div v-if="!creditLoading && creditList.length === 0" style="padding-top: 40px;">
          <EmptyState description="暂无待审核学分申请" />
        </div>
        <div v-for="item in creditList" :key="item.id" class="card audit-item">
          <div class="flex-between mb-8">
            <span class="audit-name">{{ item.realName || item.username }}</span>
            <span class="audit-credit">{{ item.creditValue }} 学分</span>
          </div>
          <div class="audit-meta">
            <span>{{ item.projectName }}</span>
            <span>{{ formatTime(item.createTime) }}</span>
          </div>
          <p v-if="item.remark" class="audit-remark mt-8">{{ item.remark }}</p>
          <div class="audit-actions mt-12">
            <van-button size="small" type="success" @click="auditCredit(item.id, true)">通过</van-button>
            <van-button size="small" type="danger" plain @click="auditCredit(item.id, false)">驳回</van-button>
          </div>
        </div>
      </van-tab>

      <van-tab v-if="canGraduate" title="毕业审批" name="graduate">
        <van-loading v-if="graduateLoading" class="loading-center" />
        <div v-if="!graduateLoading && graduateList.length === 0" style="padding-top: 40px;">
          <EmptyState description="暂无待审批毕业申请" />
        </div>
        <div v-for="item in graduateList" :key="item.id" class="card audit-item">
          <div class="flex-between mb-8">
            <span class="audit-name">{{ item.userName || item.realName || item.username }}</span>
            <StatusTag :status="item.auditStatus" type="graduate" />
          </div>
          <div class="audit-meta">
            <span>{{ item.projectName }}</span>
            <span>{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="audit-meta mt-8">
            <span>当前节点：{{ item.currentNode || '-' }}</span>
          </div>
          <div class="audit-actions mt-12">
            <van-button size="small" type="success" @click="auditGraduate(item.id, 'approve')">通过</van-button>
            <van-button size="small" type="danger" plain @click="auditGraduate(item.id, 'reject')">驳回</van-button>
          </div>
        </div>
      </van-tab>
    </van-tabs>

    <van-dialog
      v-model:show="rejectVisible"
      title="填写驳回原因"
      show-cancel-button
      :before-close="onRejectBeforeClose"
    >
      <van-field
        v-model="rejectReason"
        rows="3"
        autosize
        type="textarea"
        maxlength="200"
        show-word-limit
        placeholder="请填写驳回原因，将通知申请人"
        class="reject-field"
      />
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  Tabs as VanTabs, Tab as VanTab, Button as VanButton, Tag as VanTag,
  Progress as VanProgress, Loading as VanLoading, Dialog as VanDialog,
  Field as VanField, showToast, showConfirmDialog,
} from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { credits } from '@/api'
import { get, post } from '@/api/http'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const userStore = useUserStore()
const perms = computed(() => userStore.user?.perms || [])
const isLeader = computed(() => userStore.isLeader)
const canMonitor = computed(() => perms.value.includes('study:monitor'))
const canGraduate = computed(() => perms.value.includes('graduate:list'))
const canAuditCredit = computed(() => isLeader.value)

const activeTab = ref(
  isLeader.value ? 'dashboard' : canMonitor.value ? 'study' : canGraduate.value ? 'graduate' : 'credit'
)

const dashboardLoading = ref(false)
const overview = ref<any>({})
const warnings = ref<any[]>([])

const creditLoading = ref(false)
const graduateLoading = ref(false)
const studyLoading = ref(false)
const examLoading = ref(false)
const creditList = ref<any[]>([])
const graduateList = ref<any[]>([])
const studyList = ref<any[]>([])
const examList = ref<any[]>([])

function formatTime(t: string) {
  return t ? dayjs(t).format('MM-DD HH:mm') : '-'
}

function formatDuration(sec: number) {
  const s = Math.floor(sec || 0)
  return `${Math.floor(s / 3600)} 小时 ${Math.floor((s % 3600) / 60)} 分`
}

async function loadDashboard() {
  if (!isLeader.value) return
  dashboardLoading.value = true
  try {
    const [o, w] = await Promise.all([
      get<any>('/reports/overview'),
      get<any[]>('/reports/warnings'),
    ])
    overview.value = o || {}
    warnings.value = w || []
  } catch {
    overview.value = {}
    warnings.value = []
  } finally {
    dashboardLoading.value = false
  }
}

async function loadCredits() {
  if (!canAuditCredit.value) return
  creditLoading.value = true
  try {
    creditList.value = await credits.pending() as any[]
  } catch {
    creditList.value = []
  } finally {
    creditLoading.value = false
  }
}

async function loadGraduate() {
  if (!canGraduate.value) return
  graduateLoading.value = true
  try {
    const res = await get<any>('/graduate/applies', {
      pageNum: 1,
      pageSize: 50,
      auditStatus: 0,
    }) as any
    graduateList.value = res?.records || []
  } catch {
    graduateList.value = []
  } finally {
    graduateLoading.value = false
  }
}

async function loadStudy() {
  if (!canMonitor.value) return
  studyLoading.value = true
  try {
    const res = await get<any>('/verify/study-records', { pageNum: 1, pageSize: 50 }) as any
    studyList.value = res?.records || []
  } catch {
    studyList.value = []
  } finally {
    studyLoading.value = false
  }
}

async function loadExams() {
  if (!canMonitor.value) return
  examLoading.value = true
  try {
    const res = await get<any>('/exams', { pageNum: 1, pageSize: 50 }) as any
    examList.value = res?.records || []
  } catch {
    examList.value = []
  } finally {
    examLoading.value = false
  }
}

async function auditCredit(id: number, approve: boolean) {
  if (!approve) {
    openReject('credit', id)
    return
  }
  try {
    await showConfirmDialog({ title: '确认', message: '确定通过该学分申请？' })
    await credits.audit(id, { approve: true })
    showToast('通过成功')
    loadCredits()
  } catch {
    // cancelled or error
  }
}

async function auditGraduate(id: number, action: string) {
  if (action !== 'approve') {
    openReject('graduate', id)
    return
  }
  try {
    await showConfirmDialog({ title: '确认', message: '确定通过该毕业申请？' })
    await post(`/graduate/applies/${id}/audit`, { action, opinion: '同意' })
    showToast('通过成功')
    loadGraduate()
  } catch {
    // cancelled or error
  }
}

type RejectType = 'credit' | 'graduate'
const rejectVisible = ref(false)
const rejectType = ref<RejectType>('credit')
const rejectId = ref(0)
const rejectReason = ref('')
const rejectSubmitting = ref(false)

function openReject(type: RejectType, id: number) {
  rejectType.value = type
  rejectId.value = id
  rejectReason.value = ''
  rejectVisible.value = true
}

async function onRejectBeforeClose(action: string) {
  if (action !== 'confirm') return true
  const reason = rejectReason.value.trim()
  if (!reason) {
    showToast('请填写驳回原因')
    return false
  }
  if (rejectSubmitting.value) return false
  rejectSubmitting.value = true
  try {
    if (rejectType.value === 'credit') {
      await credits.audit(rejectId.value, { approve: false, remark: reason })
      loadCredits()
    } else {
      await post(`/graduate/applies/${rejectId.value}/audit`, { action: 'reject', opinion: reason })
      loadGraduate()
    }
    showToast('已驳回')
    return true
  } catch {
    return false
  } finally {
    rejectSubmitting.value = false
  }
}

onMounted(() => {
  loadDashboard()
  loadStudy()
  loadExams()
  loadCredits()
  loadGraduate()
})
</script>

<style scoped>
.audit-item {
  cursor: default;
}
.audit-name {
  font-size: 15px;
  font-weight: 500;
  flex: 1;
}
.audit-credit {
  font-size: 16px;
  font-weight: 700;
  color: #ff976a;
}
.audit-meta {
  font-size: 12px;
  color: #969799;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.audit-remark {
  font-size: 13px;
  color: #646566;
}
.audit-actions {
  display: flex;
  gap: 8px;
}
.status-text {
  font-size: 12px;
  color: #1989fa;
}
.section-title {
  margin: 18px 16px 8px;
  font-size: 15px;
  font-weight: 600;
  color: #323233;
}
.empty-tip {
  color: #969799;
  text-align: center;
  font-size: 13px;
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  text-align: center;
}
.stat-item b {
  display: block;
  font-size: 20px;
  color: #1989fa;
}
.stat-item b.orange {
  color: #ff976a;
}
.stat-item span {
  font-size: 12px;
  color: #969799;
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}
.reject-field {
  padding: 8px 0 16px;
}
</style>
