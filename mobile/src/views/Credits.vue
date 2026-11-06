<template>
  <div class="page-container credits-page">
    <header class="page-head">
      <div>
        <span class="kicker">CREDITS</span>
        <h1>学分</h1>
      </div>
      <span class="head-count">{{ list.length }} 条记录</span>
    </header>

    <!-- 学分护照 -->
    <div v-if="isStudentView" class="credit-pass">
      <div class="pass-head">
        <span class="pass-kicker">CREDIT TRANSCRIPT</span>
        <span class="pass-badge">学分台账</span>
      </div>
      <div class="pass-body">
        <div class="pass-main">
          <span class="pass-label">已发放学分</span>
          <span class="pass-value">{{ summary.issued || 0 }}<i>分</i></span>
        </div>
        <div class="pass-side">
          <div class="pass-side-item">
            <span class="pass-side-value">{{ summary.pending || 0 }}</span>
            <span class="pass-side-label">待发放</span>
          </div>
          <div class="pass-side-item">
            <span class="pass-side-value">{{ summary.count || 0 }}</span>
            <span class="pass-side-label">记录</span>
          </div>
        </div>
      </div>
      <div class="pass-foot">
        <van-icon name="shield-o" size="12" />
        <span>学分记录可用于毕业资格审核</span>
      </div>
    </div>

    <!-- 补录入口 -->
    <div v-if="canApply" class="card apply-entry">
      <div class="apply-copy">
        <span class="apply-title">学分补录</span>
        <p class="apply-hint">
          {{ isStudentView ? '遗漏的学分可提交补录申请，审核通过后计入台账。' : '为培训班学员补录学分，提交后进入审批队列。' }}
        </p>
      </div>
      <van-button size="small" type="primary" plain @click="openApply">去申请</van-button>
    </div>

    <!-- 明细 -->
    <template v-if="isStudentView">
      <div class="section-title flex-between">
        <span>学分明细</span>
        <span v-if="list.length" class="more-link">{{ list.length }} 条</span>
      </div>

      <div v-if="list.length === 0" class="empty-wrap">
        <EmptyState icon="gold-coin-o" description="暂无学分记录" />
      </div>

      <div v-else class="ledger">
        <div v-for="item in list" :key="item.id" class="ledger-row">
          <div class="ledger-date">
            <span class="ledger-d">{{ formatDay(item.createTime) }}</span>
            <span class="ledger-y">{{ formatYear(item.createTime) }}</span>
          </div>
          <div class="ledger-rail"><i class="ledger-dot"></i></div>
          <div class="ledger-card">
            <div class="ledger-top">
              <span class="credit-name text-ellipsis">{{ item.courseName || item.projectName || '学分记录' }}</span>
              <span class="credit-value">+{{ item.creditValue }}</span>
            </div>
            <div class="ledger-meta">
              <span v-if="item.sourceType" class="pill">{{ sourceLabel(item.sourceType) }}</span>
              <StatusTag v-if="item.auditStatus != null" :status="item.auditStatus" type="credit" />
            </div>
            <p v-if="item.remark" class="credit-remark">{{ item.remark }}</p>
          </div>
        </div>
      </div>
    </template>

    <!-- 补录弹层 -->
    <van-popup v-model:show="showApply" position="bottom" round :style="{ maxHeight: '80vh' }">
      <div class="apply-form">
        <h3 class="apply-form-title">学分补录申请</h3>
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
            <van-button block type="primary" native-type="submit" :loading="applying">提交申请</van-button>
            <van-button block plain class="mt-8" @click="showApply = false">取消</van-button>
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
  Button as VanButton, Popup as VanPopup, Icon as VanIcon,
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

function formatDay(d: string) {
  return d ? dayjs(d).format('MM-DD') : '-'
}

function formatYear(d: string) {
  return d ? dayjs(d).format('YYYY') : ''
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
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--sp-3);
  padding: calc(env(safe-area-inset-top) + 24px) var(--page-x) var(--sp-3);
}
.page-head h1 {
  margin-top: 5px;
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0.02em;
  color: var(--ink);
}
.head-count {
  flex: none;
  margin-bottom: 3px;
  padding: 3px 10px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  font-size: 11px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

/* ---------- 学分护照 ---------- */
.credit-pass {
  position: relative;
  overflow: hidden;
  margin: 0 var(--page-x) var(--sp-4);
  padding: var(--sp-5);
  border: 1px solid var(--line);
  border-radius: var(--r-card);
  background: var(--surface);
  box-shadow: var(--shadow-card);
  color: var(--ink);
}

.credit-pass::before {
  content: '';
  position: absolute;
  top: 0;
  left: var(--sp-5);
  width: 28px;
  height: 2px;
  background: var(--accent);
}

.credit-pass::after {
  content: '分';
  position: absolute;
  top: -16px;
  right: 2px;
  font-family: var(--font-serif);
  font-size: 92px;
  line-height: 1;
  color: rgba(29, 78, 216, 0.07);
  pointer-events: none;
}

.pass-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pass-kicker {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: var(--ink-3);
}

.pass-badge {
  padding: 2px 8px;
  border: 1px solid var(--brand-line);
  border-radius: 999px;
  background: var(--brand-tint);
  font-size: 10px;
  color: var(--brand);
}

.pass-body {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--sp-4);
  margin-top: var(--sp-4);
}

.pass-main {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.pass-label {
  font-size: 12px;
  color: var(--ink-3);
}

.pass-value {
  font-family: var(--font-serif);
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.01em;
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}

.pass-value i {
  margin-left: 4px;
  font-size: 12px;
  font-weight: 400;
  font-style: normal;
  color: var(--ink-3);
}

.pass-side {
  display: flex;
  gap: var(--sp-5);
  padding-bottom: 3px;
}

.pass-side-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.pass-side-value {
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.pass-side-label {
  font-size: 10px;
  color: var(--ink-3);
}

.pass-foot {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: var(--sp-4);
  padding-top: var(--sp-3);
  border-top: 1px solid var(--line);
  font-size: 11px;
  color: var(--ink-3);
}

/* ---------- 补录 ---------- */
.apply-entry {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
}

.apply-copy {
  flex: 1;
  min-width: 0;
}

.apply-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.apply-hint {
  margin-top: 3px;
  font-size: 11px;
  line-height: 1.55;
  color: var(--ink-3);
}

.apply-entry :deep(.van-button) {
  flex: none;
  border-radius: 999px;
  padding: 0 var(--sp-4);
}

/* ---------- 流水时间线 ---------- */
.ledger {
  padding: 0 var(--page-x);
}

.ledger-row {
  display: flex;
  gap: 0;
  align-items: flex-start;
}

.ledger-date {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex: none;
  width: 46px;
  padding-top: 15px;
}

.ledger-d {
  font-family: var(--font-serif);
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-2);
  font-variant-numeric: tabular-nums;
}

.ledger-y {
  margin-top: 2px;
  font-size: 9px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

.ledger-rail {
  position: relative;
  flex: none;
  width: 24px;
  align-self: stretch;
  margin: 0 6px;
}

.ledger-rail::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: -12px;
  left: 50%;
  width: 1px;
  background: var(--line);
  transform: translateX(-50%);
}

.ledger-row:first-child .ledger-rail::before {
  top: 18px;
}

.ledger-row:last-child .ledger-rail::before {
  bottom: calc(100% - 18px);
}

.ledger-dot {
  position: absolute;
  top: 16px;
  left: 50%;
  width: 7px;
  height: 7px;
  border: 2px solid var(--surface);
  border-radius: 50%;
  background: var(--accent);
  outline: 1px solid var(--accent-line);
  transform: translate(-50%, 0);
}

.ledger-card {
  flex: 1;
  min-width: 0;
  margin-bottom: var(--sp-3);
  padding: var(--sp-3) var(--sp-4);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-chip);
  box-shadow: var(--shadow-card);
}

.ledger-top {
  display: flex;
  align-items: baseline;
  gap: var(--sp-2);
}

.credit-name {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.credit-value {
  flex: none;
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 700;
  color: var(--accent-strong);
  font-variant-numeric: tabular-nums;
}

.ledger-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: var(--sp-2);
}

.ledger-card .pill {
  padding: 2px 7px;
  font-size: 10px;
}

.credit-remark {
  margin-top: var(--sp-2);
  padding: var(--sp-2) var(--sp-3);
  border: 1px solid var(--line);
  border-radius: var(--r-tag);
  background: var(--surface-2);
  font-size: 11px;
  line-height: 1.6;
  color: var(--ink-2);
}

.empty-wrap {
  margin: 0 var(--page-x) var(--sp-3);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-card);
}

/* ---------- 申请弹层 ---------- */
.apply-form {
  padding: var(--sp-5) var(--page-x) calc(var(--sp-5) + env(safe-area-inset-bottom));
}

.apply-form-title {
  margin-bottom: var(--sp-4);
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 600;
  text-align: center;
  color: var(--ink);
}

.apply-btns {
  padding: var(--sp-4) 0 0;
}

.apply-btns :deep(.van-button) {
  height: 46px;
  font-size: 15px;
  font-weight: 600;
}
</style>
