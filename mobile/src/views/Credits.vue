<template>
  <div class="page-container">
    <!-- 成绩单式总览 -->
    <div class="ledger-hero ink-hero">
      <span class="hero-watermark">学分</span>
      <p class="ledger-title">学 分 册</p>
      <div class="ledger-summary">
        <div class="ledger-item">
          <div class="ledger-value num">{{ summary.issued || 0 }}</div>
          <div class="ledger-label">已发放</div>
        </div>
        <div class="ledger-divider"></div>
        <div class="ledger-item">
          <div class="ledger-value num ledger-value--gold">{{ summary.pending || 0 }}</div>
          <div class="ledger-label">待发放</div>
        </div>
        <div class="ledger-divider"></div>
        <div class="ledger-item">
          <div class="ledger-value num">{{ summary.count || 0 }}</div>
          <div class="ledger-label">记录数</div>
        </div>
      </div>
      <div class="ticket-edge"></div>
    </div>

    <!-- Apply Button -->
    <div v-if="canApply" class="apply-entry">
      <van-button type="primary" plain size="small" round icon="edit" @click="openApply">
        学分补录申请
      </van-button>
    </div>

    <!-- Ledger List -->
    <div class="section-title">
      学分明细
      <span class="section-title-en">LEDGER</span>
    </div>
    <div v-if="list.length === 0" style="padding-top: 10px;">
      <EmptyState description="暂无学分记录" glyph="分" />
    </div>
    <div v-for="item in list" :key="item.id" class="card credit-item">
      <div class="credit-row">
        <div class="credit-main">
          <span class="credit-name">{{ item.courseName || item.projectName || '学分记录' }}</span>
          <div class="credit-meta">
            <span v-if="item.sourceType" class="source-chip">{{ sourceLabel(item.sourceType) }}</span>
            <StatusTag v-if="item.auditStatus != null" :status="item.auditStatus" type="credit" />
          </div>
        </div>
        <div class="credit-amount">
          <span class="amount-plus">+</span>
          <span class="amount-num num">{{ item.creditValue }}</span>
        </div>
      </div>
      <div class="credit-foot">
        <span class="credit-time num">{{ formatDate(item.createTime) }}</span>
        <span v-if="item.remark" class="credit-remark">{{ item.remark }}</span>
      </div>
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
  Button as VanButton, Popup as VanPopup,
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
.ledger-hero {
  padding: 30px 20px 22px;
}
.ledger-title {
  position: relative;
  z-index: 1;
  font-family: var(--font-body);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.5em;
  color: rgba(245, 242, 234, 0.85);
  text-align: center;
}
.ledger-summary {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  margin-top: 18px;
}
.ledger-item {
  flex: 1;
  text-align: center;
}
.ledger-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
}
.ledger-value--gold {
  color: #1f6feb;
}
.ledger-label {
  font-size: 11px;
  color: rgba(245, 242, 234, 0.55);
  letter-spacing: 0.2em;
  margin-top: 5px;
}
.ledger-divider {
  width: 1px;
  height: 30px;
  background: rgba(245, 242, 234, 0.18);
}

.apply-entry {
  display: flex;
  justify-content: center;
  padding: 14px 16px 0;
}
.apply-entry :deep(.van-button--plain) {
  border-color: rgba(31, 111, 235, 0.45);
  color: var(--text-strong);
}

.credit-item {
  cursor: default;
}
.credit-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.credit-main {
  flex: 1;
  min-width: 0;
}
.credit-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-strong);
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.credit-meta {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}
.source-chip {
  font-size: 11px;
  color: var(--text-sub);
  background: var(--bg-soft);
  border: 1px solid var(--line);
  border-radius: 4px;
  padding: 3px 7px;
  white-space: nowrap;
}
.credit-amount {
  flex: none;
  display: flex;
  align-items: baseline;
  color: var(--primary);
}
.amount-plus {
  font-size: 14px;
  font-weight: 600;
}
.amount-num {
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
}
.credit-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  border-top: 1px dashed var(--line);
  padding-top: 9px;
}
.credit-time {
  flex: none;
  font-size: 11px;
  color: var(--text-faint);
  letter-spacing: 0.06em;
}
.credit-remark {
  font-size: 11px;
  color: var(--text-faint);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.apply-form {
  padding: 20px 16px;
}
.apply-title {
  text-align: center;
  font-family: var(--font-body);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.2em;
  margin-bottom: 16px;
  color: var(--text-strong);
}
.apply-btns {
  padding: 16px 0;
}
</style>
