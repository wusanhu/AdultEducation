<template>
  <div class="page-container">
    <!-- Summary Cards -->
    <div class="summary-cards">
      <div class="summary-item">
        <div class="stat-value">{{ summary.issued || 0 }}</div>
        <div class="stat-label">已发放学分</div>
      </div>
      <div class="summary-item">
        <div class="stat-value" style="color: #ffd8a8">{{ summary.pending || 0 }}</div>
        <div class="stat-label">待发放学分</div>
      </div>
      <div class="summary-item">
        <div class="stat-value" style="color: #fff">{{ summary.count || 0 }}</div>
        <div class="stat-label">学分记录</div>
      </div>
    </div>

    <!-- Apply Button -->
    <div v-if="canApply" class="card" style="text-align: center;">
      <van-button type="primary" plain size="small" round @click="openApply">
        学分补录申请
      </van-button>
    </div>

    <!-- Ledger List -->
    <div class="section-title">学分明细</div>
    <div v-if="list.length === 0" style="padding-top: 20px;">
      <EmptyState description="暂无学分记录" />
    </div>
    <div v-for="item in list" :key="item.id" class="card credit-item">
      <div class="flex-between mb-8">
        <span class="credit-name">{{ item.courseName || item.projectName || '学分记录' }}</span>
        <span class="credit-value">+{{ item.creditValue }}</span>
      </div>
      <div class="flex-between">
        <div class="credit-meta">
          <van-tag v-if="item.sourceType" size="medium" plain>{{ sourceLabel(item.sourceType) }}</van-tag>
          <StatusTag v-if="item.auditStatus != null" :status="item.auditStatus" type="credit" />
        </div>
        <span class="credit-time">{{ formatDate(item.createTime) }}</span>
      </div>
      <p v-if="item.remark" class="credit-remark mt-8">{{ item.remark }}</p>
    </div>

    <!-- Apply Dialog -->
    <van-popup v-model:show="showApply" position="bottom" round :style="{ maxHeight: '80vh' }">
      <div class="apply-form">
        <h3 class="apply-title">学分补录申请</h3>
        <van-form @submit="onApply">
          <van-field
            v-model="projectName"
            label="培训班"
            readonly
            is-link
            placeholder="请选择培训班"
            @click="showProjectPicker = true"
          />
          <van-field
            v-model="studentName"
            label="学员"
            readonly
            is-link
            placeholder="请选择学员"
            :disabled="!applyForm.projectId"
            @click="openStudentPicker"
          />
          <van-field
            v-model="applyForm.creditValue"
            label="补录学分"
            type="number"
            placeholder="请输入补录学分"
            :rules="[{ required: true, message: '请输入补录学分' }]"
          />
          <van-field
            v-model="applyForm.remark"
            label="补录原因"
            type="textarea"
            rows="3"
            placeholder="请输入补录原因"
            :rules="[{ required: true, message: '请输入补录原因' }]"
          />
          <div class="apply-btns">
            <van-button block round type="primary" native-type="submit" :loading="applying">提交申请</van-button>
            <van-button block round plain class="mt-8" @click="showApply = false">取消</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <van-popup v-model:show="showProjectPicker" position="bottom" round>
      <van-picker :columns="projectColumns" @confirm="onProjectConfirm" @cancel="showProjectPicker = false" />
    </van-popup>
    <van-popup v-model:show="showStudentPicker" position="bottom" round>
      <van-picker :columns="studentColumns" @confirm="onStudentConfirm" @cancel="showStudentPicker = false" />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  Button as VanButton, Tag as VanTag, Popup as VanPopup,
  Form as VanForm, Field as VanField, Picker as VanPicker, showToast,
} from 'vant'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { credits, projects } from '@/api'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const userStore = useUserStore()
const summary = ref<any>({})
const list = ref<any[]>([])
const showApply = ref(false)
const applying = ref(false)
const projectOptions = ref<any[]>([])
const studentOptions = ref<any[]>([])
const showProjectPicker = ref(false)
const showStudentPicker = ref(false)
const projectName = ref('')
const studentName = ref('')
const applyForm = reactive({
  projectId: null as number | null,
  userId: null as number | null,
  creditValue: '',
  remark: '',
})

const canApply = computed(() => (userStore.user?.perms || []).includes('credit:apply'))
const projectColumns = computed(() =>
  projectOptions.value.map((p: any) => ({ text: p.projectName, value: p.id }))
)
const studentColumns = computed(() =>
  studentOptions.value.map((u: any) => ({
    text: `${u.realName || u.username}${u.orgName ? `（${u.orgName}）` : ''}`,
    value: u.id,
  }))
)

function formatDate(d: string) {
  return d ? dayjs(d).format('YYYY-MM-DD') : '-'
}

function sourceLabel(t: number) {
  switch (t) {
    case 1: return '课程自动发放'
    case 2: return '人工补录'
    default: return '其他'
  }
}

async function loadData() {
  try {
    const [sum, items] = await Promise.all([
      credits.summary(),
      credits.my(),
    ])
    summary.value = sum || {}
    list.value = (items as any[]) || []
  } catch {
    // ignore
  }
}

async function openApply() {
  if (projectOptions.value.length === 0) {
    try {
      projectOptions.value = (await projects.options()) || []
    } catch {
      // ignore
    }
  }
  if (projectOptions.value.length === 0) {
    showToast('暂无可补录的培训班')
    return
  }
  showApply.value = true
}

function onProjectConfirm({ selectedOptions }: any) {
  const opt = selectedOptions?.[0]
  showProjectPicker.value = false
  if (!opt) return
  applyForm.projectId = opt.value
  applyForm.userId = null
  projectName.value = opt.text
  studentName.value = ''
  loadStudents(opt.value)
}

async function loadStudents(projectId: number) {
  try {
    studentOptions.value = (await projects.studentOptions(projectId)) || []
    if (studentOptions.value.length === 0) {
      showToast('该培训班暂无学员')
    }
  } catch {
    studentOptions.value = []
  }
}

function openStudentPicker() {
  if (!applyForm.projectId) {
    showToast('请先选择培训班')
    return
  }
  if (studentOptions.value.length === 0) {
    showToast('该培训班暂无可补录学员')
    return
  }
  showStudentPicker.value = true
}

function onStudentConfirm({ selectedOptions }: any) {
  const opt = selectedOptions?.[0]
  showStudentPicker.value = false
  if (!opt) return
  applyForm.userId = opt.value
  studentName.value = opt.text
}

async function onApply() {
  if (!applyForm.projectId || !applyForm.userId) {
    showToast('请选择培训班和学员')
    return
  }
  const value = Number(applyForm.creditValue)
  if (!value || value <= 0) {
    showToast('请输入正确的补录学分')
    return
  }
  applying.value = true
  try {
    await credits.apply({
      userId: applyForm.userId,
      projectId: applyForm.projectId,
      creditValue: value,
      remark: applyForm.remark,
    })
    showToast('申请已提交')
    showApply.value = false
    applyForm.creditValue = ''
    applyForm.remark = ''
    loadData()
  } catch {
    // handled
  } finally {
    applying.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.summary-cards {
  display: flex;
  background: linear-gradient(135deg, #1989fa, #07c160);
  padding: 24px 16px;
  margin-bottom: 12px;
}
.summary-item {
  flex: 1;
  text-align: center;
  color: #fff;
}
.summary-item .stat-value {
  color: #fff;
}
.summary-item .stat-label {
  color: rgba(255, 255, 255, 0.8);
}
.credit-item {
  cursor: default;
}
.credit-name {
  font-size: 14px;
  font-weight: 500;
  flex: 1;
  margin-right: 8px;
}
.credit-value {
  font-size: 18px;
  font-weight: 700;
  color: #07c160;
}
.credit-meta {
  display: flex;
  gap: 6px;
}
.credit-time {
  font-size: 12px;
  color: #c8c9cc;
}
.credit-remark {
  font-size: 12px;
  color: #969799;
}
.apply-form {
  padding: 20px 16px;
}
.apply-title {
  text-align: center;
  font-size: 16px;
  margin-bottom: 16px;
}
.apply-btns {
  padding: 16px 0;
}
</style>
