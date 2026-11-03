<template>
  <a-row :gutter="16">
    <a-col :xs="24" :lg="13">
      <div class="page-card">
        <div style="font-size:16px;font-weight:600">发起学分补录</div>
        <div style="font-size:12px;color:#94a3b8;margin:4px 0 22px">用于线下培训、学时折算等场景。提交后按系统配置进入领导复核或管理员终审，审批通过自动入账。</div>
        <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
          <a-form-item label="学员" name="userId">
            <a-select v-model:value="form.userId" show-search option-filter-prop="label" placeholder="搜索姓名、账号或手机号" :options="studentSelectOptions" />
          </a-form-item>
          <a-form-item label="培训班" name="projectId">
            <a-select v-model:value="form.projectId" show-search option-filter-prop="label" placeholder="选择学分归属培训班" :options="projectOptions" />
          </a-form-item>
          <a-form-item label="补录学分" name="creditValue">
            <a-input-number v-model:value="form.creditValue" :min="0.5" :step="0.5" style="width:100%" addon-after="学分" />
          </a-form-item>
          <a-form-item label="补录原因" name="remark">
            <a-textarea v-model:value="form.remark" :rows="4" placeholder="请说明线下培训名称、时间、学时及折算依据，便于审批留痕。" />
          </a-form-item>
          <a-alert type="info" show-icon :message="auditHint" style="margin-bottom:16px" />
          <a-button type="primary" size="large" block :loading="saving" @click="submit">提交审批</a-button>
        </a-form>
      </div>
    </a-col>
    <a-col :xs="24" :lg="11">
      <div class="page-card">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:14px">
          <div style="font-size:15px;font-weight:600">最近补录记录</div>
          <a-button v-if="canViewLedger" type="link" size="small" @click="$router.push('/credit/list')">查看台账</a-button>
        </div>
        <a-empty v-if="recent.length === 0" description="暂无补录记录" />
        <div v-for="item in recent" :key="item.id" style="display:flex;align-items:center;gap:12px;padding:12px 0;border-bottom:1px dashed #f1f5f9">
          <a-avatar style="background:#fef3c7;color:#b45309">{{ String(item.userName || '学').slice(0, 1) }}</a-avatar>
          <div style="flex:1;min-width:0">
            <div style="font-size:13px;font-weight:500">{{ item.userName }} · {{ item.projectName }}</div>
            <div style="font-size:12px;color:#94a3b8;margin-top:2px">{{ item.remark || '学分补录' }}</div>
          </div>
          <div style="text-align:right">
            <div style="font-weight:700;color:#16a34a">+{{ item.creditValue }}</div>
            <a-tag :color="statusColor(item.auditStatus)" style="margin-top:3px">{{ item.auditStatusLabel }}</a-tag>
          </div>
        </div>
      </div>
      <div class="page-card" style="margin-top:16px;background:linear-gradient(135deg,#f0f9ff,#f8fafc)">
        <div style="font-size:14px;font-weight:600;margin-bottom:10px">审批说明</div>
        <div class="markdown-note">
          <p style="margin:0 0 6px">1. 默认由管理员终审；系统参数开启“学分补录需领导复核”后，先由领导审批再进入管理员终审。</p>
          <p style="margin:0 0 6px">2. 审批通过后学分立即入账，学员可在移动端查看明细。</p>
          <p style="margin:0">3. 申请、审批与入账结果均写入操作日志，可在系统日志中追溯。</p>
        </div>
      </div>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, type FormInstance } from 'ant-design-vue'
import { creditApi, projectApi } from '../../api'
import type { Option } from '../../api'
import { useAuthStore } from '../../store/auth'
import type { CreditRow, ProjectRow } from '../../types'

const auth = useAuthStore()
const canViewLedger = computed(() => auth.hasPerm('credit:list'))

const formRef = ref<FormInstance>()
const saving = ref(false)
const students = ref<Option[]>([])
const projects = ref<ProjectRow[]>([])
const recent = ref<CreditRow[]>([])
const form = reactive({ userId: undefined as number | undefined, projectId: undefined as number | undefined, creditValue: 1, remark: '' })
const rules = {
  userId: [{ required: true, message: '请选择学员' }],
  projectId: [{ required: true, message: '请选择培训班' }],
  creditValue: [{ required: true, message: '请填写补录学分' }],
  remark: [{ required: true, message: '请填写补录原因' }]
}
const studentSelectOptions = computed(() => students.value.map((s) => ({
  value: s.id,
  label: `${String(s.realName || s.label || '')}（${String(s.username || '')} · ${String(s.phone || '无手机号')}）`
})))
const projectOptions = computed(() => projects.value.map((p) => ({ value: p.id, label: p.projectName })))
const auditHint = computed(() => '提交后进入审批流：默认管理员终审，可在平台设置中开启领导复核。')

function statusColor(status: number) { return ({ 1: 'processing', 2: 'green', 3: 'red', 4: 'default' } as Record<number, string>)[status] || 'default' }

async function loadMeta() {
  const [studentList, projectPage] = await Promise.all([
    projectApi.studentOptions(),
    projectApi.page({ pageNum: 1, pageSize: 200 })
  ])
  students.value = studentList
  projects.value = projectPage.records
}
async function loadRecent() {
  if (!canViewLedger.value) {
    recent.value = []
    return
  }
  try {
    const page = await creditApi.page({ pageNum: 1, pageSize: 8, sourceType: 2 })
    recent.value = page.records
  } catch { recent.value = [] }
}
async function submit() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    await creditApi.apply({ userId: form.userId, projectId: form.projectId, creditValue: form.creditValue, remark: form.remark })
    message.success('补录申请已提交，等待审批')
    form.creditValue = 1
    form.remark = ''
    await loadRecent()
  } catch (e) { message.error((e as Error).message) } finally { saving.value = false }
}

onMounted(async () => {
  try { await loadMeta() } catch (e) { message.error((e as Error).message) }
  await loadRecent()
})
</script>
