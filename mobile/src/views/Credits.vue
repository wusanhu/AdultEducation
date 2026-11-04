<template>
  <div class="page-container">
    <!-- Summary Cards -->
    <div v-if="isStudentView" class="card summary-cards">
      <div class="summary-item">
        <div class="stat-value metric-brand">{{ summary.issued || 0 }}</div>
        <div class="stat-label">已发放学分</div>
      </div>
      <div class="summary-item">
        <div class="stat-value metric-warn">{{ summary.pending || 0 }}</div>
        <div class="stat-label">待发放学分</div>
      </div>
      <div class="summary-item">
        <div class="stat-value">{{ summary.count || 0 }}</div>
        <div class="stat-label">学分记录</div>
      </div>
    </div>

    <!-- Apply Button -->
    <template v-if="canApply">
      <div v-if="isStudentView" class="card apply-entry">
        <van-button type="primary" plain block round @click="openApply">
          学分补录申请
        </van-button>
      </div>
      <template v-else>
        <div class="section-title">学分补录</div>
        <div class="card apply-entry">
          <p class="apply-hint">为培训班学员补录学分，提交后进入待审核队列，可在「审批」中跟踪。</p>
          <van-button type="primary" plain block round @click="openApply">
            新建补录申请
          </van-button>
        </div>
      </template>
    </template>

    <!-- Ledger List -->
    <template v-if="isStudentView">
      <div class="section-title">学分明细</div>
      <div v-if="list.length === 0" class="empty-wrap">
        <EmptyState description="暂无学分记录" />
      </div>
      <div v-for="item in list" :key="item.id" class="card credit-item">
        <div class="flex-between mb-8">
          <span class="credit-name text-ellipsis">{{ item.courseName || item.projectName || '学分记录' }}</span>
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
    </template>

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
import { useTodoStore } from '@/stores/todo'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'

const userStore = useUserStore()
const todoStore = useTodoStore()
const route = useRoute()
const router = useRouter()
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

const canApply = computed(() => userStore.caps.canApplyCredit)
/** 学分台账是学员视角数据，教务从宫格进来只为补录 */
const isStudentView = computed(() => !userStore.caps.isOps)
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
  if (!isStudentView.value) return
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
    todoStore.refresh()
  } catch {
    // handled
  } finally {
    applying.value = false
  }
}

onMounted(async () => {
  await loadData()
  // 首页宫格「学分补录」直达：消费掉 query 避免再次切回 tab 时重复弹窗
  if (route.query.apply === '1') {
    router.replace({ query: { ...route.query, apply: undefined } })
    if (canApply.value) openApply()
  }
})
</script>

<style scoped>
.summary-cards {
  display: flex;
  margin-top: var(--sp-4);
  padding: var(--sp-5) var(--sp-2);
}
.summary-item {
  flex: 1;
  min-width: 0;
  text-align: center;
}
.summary-item + .summary-item {
  border-left: 1px solid var(--line);
}
.apply-entry {
  padding: var(--sp-3) var(--sp-4);
}
.apply-entry :deep(.van-button) {
  height: 44px;
  font-size: 15px;
  font-weight: 600;
}
.apply-hint {
  font-size: 12px;
  color: var(--ink-3);
  line-height: 1.6;
  margin-bottom: var(--sp-3);
}
.empty-wrap {
  padding-top: var(--sp-5);
}
.credit-item {
  cursor: default;
}
.credit-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  flex: 1;
  min-width: 0;
  margin-right: var(--sp-2);
}
.credit-value {
  flex: none;
  font-size: 18px;
  font-weight: 700;
  color: var(--ok);
  font-variant-numeric: tabular-nums;
}
.credit-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}
.credit-time {
  flex: none;
  font-size: 12px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}
.credit-remark {
  font-size: 12px;
  color: var(--ink-2);
  line-height: 1.6;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: var(--r-chip);
  padding: var(--sp-2) var(--sp-3);
}
.apply-form {
  padding: var(--sp-5) var(--page-x) calc(var(--sp-5) + env(safe-area-inset-bottom));
}
.apply-title {
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: var(--sp-4);
}
.apply-btns {
  padding: var(--sp-4) 0 0;
}
.apply-btns :deep(.van-button) {
  height: 44px;
  font-size: 15px;
  font-weight: 600;
}
</style>
