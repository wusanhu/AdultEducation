<template>
  <div class="page-card">
    <div class="toolbar">
      <a-input v-model:value="query.keyword" placeholder="学员姓名 / 账号" allow-clear style="width:190px" @press-enter="reload" />
      <a-select v-model:value="query.projectId" placeholder="培训班" allow-clear show-search option-filter-prop="label" style="width:220px" :options="projectOptions" />
      <a-select v-model:value="query.auditStatus" placeholder="学分状态" allow-clear style="width:130px">
        <a-select-option :value="2">已发放</a-select-option>
        <a-select-option :value="1">待审批</a-select-option>
        <a-select-option :value="3">已驳回</a-select-option>
        <a-select-option :value="4">已撤销</a-select-option>
      </a-select>
      <a-select v-model:value="query.sourceType" placeholder="学分来源" allow-clear style="width:130px">
        <a-select-option :value="1">课程自动</a-select-option>
        <a-select-option :value="2">人工补录</a-select-option>
      </a-select>
      <a-range-picker v-model:value="dateRange" style="width:240px" />
      <a-button type="primary" @click="reload">查询</a-button>
      <a-button @click="reset">重置</a-button>
      <div class="spacer" />
      <a-button v-if="auth.hasPerm('credit:apply')" type="primary" @click="$router.push('/credit/apply')"><PlusOutlined /> 学分补录</a-button>
      <a-button v-if="auth.hasPerm('report:export')" @click="exportData"><DownloadOutlined /> 导出</a-button>
    </div>

    <a-table :data-source="rows" :columns="columns" :loading="loading" :pagination="pagination" row-key="id" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'student'">
          <div style="font-weight:500">{{ record.userName }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.orgName || record.username }}</div>
        </template>
        <template v-else-if="column.key === 'project'">
          <div style="font-size:13px">
            {{ record.projectName }}
            <a-tag v-if="record.projectArchived" color="purple" style="margin-left:4px">已归档</a-tag>
          </div>
          <div style="font-size:12px;color:#94a3b8">{{ record.courseName || '线下/补录' }}</div>
        </template>
        <template v-else-if="column.key === 'credit'">
          <span style="font-size:16px;font-weight:700;color:#16a34a">+{{ record.creditValue }}</span>
        </template>
        <template v-else-if="column.key === 'source'">
          <a-tag :color="record.sourceType === 1 ? 'blue' : 'purple'">{{ record.sourceLabel }}</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.auditStatus)">{{ record.auditStatusLabel }}</a-tag>
          <div v-if="record.auditStatus === 1 && record.auditNode" style="font-size:12px;color:#94a3b8;margin-top:2px">领导审核中</div>
        </template>
        <template v-else-if="column.key === 'audit'">
          <div style="font-size:12px">{{ record.auditUserName || record.applyUserName || '-' }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.auditTime || record.createTime }}</div>
        </template>
        <template v-else-if="column.key === 'remark'">
          <a-tooltip :title="record.remark || record.revokeReason || record.auditRemark || '-'">
            <span style="font-size:12px;color:#64748b;display:inline-block;max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
              {{ record.revokeReason || record.remark || '-' }}
            </span>
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'action'">
          <a v-if="auth.hasPerm('credit:revoke') && record.auditStatus === 2 && !record.projectArchived" style="color:#dc2626" @click="openRevoke(record)">撤销</a>
          <span v-else-if="record.projectArchived && record.auditStatus === 2" class="muted" style="font-size:12px">已归档</span>
          <span v-else class="muted">-</span>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="revokeOpen" title="撤销学分" :confirm-loading="revoking" :width="480" @ok="submitRevoke">
      <a-alert type="warning" show-icon message="撤销后将扣除该笔学分并生成审计日志，操作不可恢复。" style="margin-bottom:14px" />
      <a-descriptions v-if="current" :column="2" size="small" bordered style="margin-bottom:14px">
        <a-descriptions-item label="学员">{{ current.userName }}</a-descriptions-item>
        <a-descriptions-item label="学分">{{ current.creditValue }}</a-descriptions-item>
        <a-descriptions-item label="培训班" :span="2">{{ current.projectName }}</a-descriptions-item>
      </a-descriptions>
      <a-form layout="vertical">
        <a-form-item label="撤销原因" required><a-textarea v-model:value="revokeReason" :rows="3" placeholder="如：违规刷课，经复核取消学分" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, type TablePaginationConfig } from 'ant-design-vue'
import { DownloadOutlined, PlusOutlined } from '@ant-design/icons-vue'
import type { Dayjs } from 'dayjs'
import { creditApi, projectApi, reportApi } from '../../api'
import { useAuthStore } from '../../store/auth'
import type { CreditRow, ProjectRow } from '../../types'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref<CreditRow[]>([])
const projects = ref<ProjectRow[]>([])
const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const query = reactive<{ pageNum: number; pageSize: number; keyword: string; projectId?: number; auditStatus?: number; sourceType?: number }>({ pageNum: 1, pageSize: 10, keyword: '' })
const pagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (n: number) => `共 ${n} 条学分记录` })
const revokeOpen = ref(false)
const revoking = ref(false)
const current = ref<CreditRow | null>(null)
const revokeReason = ref('')
const projectOptions = computed(() => projects.value.map((p) => ({ value: p.id, label: p.projectName })))
const columns = [
  { title: '学员', key: 'student', width: 160 },
  { title: '培训班 / 课程', key: 'project', width: 220 },
  { title: '学分', key: 'credit', width: 90, align: 'center' as const },
  { title: '来源', key: 'source', width: 110, align: 'center' as const },
  { title: '状态', key: 'status', width: 130, align: 'center' as const },
  { title: '审批人 / 时间', key: 'audit', width: 170 },
  { title: '备注', key: 'remark', width: 190 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const, align: 'right' as const }
]
function statusColor(status: number) { return ({ 1: 'processing', 2: 'green', 3: 'red', 4: 'default' } as Record<number, string>)[status] || 'default' }

async function loadMeta() { projects.value = (await projectApi.page({ pageNum: 1, pageSize: 200 })).records }
async function load() {
  loading.value = true
  try {
    const page = await creditApi.page({
      ...query, keyword: query.keyword || undefined,
      startDate: dateRange.value ? dateRange.value[0].format('YYYY-MM-DD') : undefined,
      endDate: dateRange.value ? dateRange.value[1].format('YYYY-MM-DD') : undefined
    })
    rows.value = page.records
    pagination.current = page.pageNum
    pagination.total = page.total
  } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
function reload() { query.pageNum = 1; void load() }
function reset() { query.keyword = ''; query.projectId = undefined; query.auditStatus = undefined; query.sourceType = undefined; dateRange.value = null; reload() }
function onTableChange(pag: TablePaginationConfig) { query.pageNum = Number(pag.current || 1); query.pageSize = Number(pag.pageSize || 10); void load() }
function openRevoke(row: CreditRow) { current.value = row; revokeReason.value = ''; revokeOpen.value = true }
async function submitRevoke() {
  if (!current.value || !revokeReason.value.trim()) { message.warning('请填写撤销原因'); return }
  revoking.value = true
  try {
    await creditApi.revoke(current.value.id, revokeReason.value)
    message.success('学分已撤销')
    revokeOpen.value = false
    await load()
  } catch (e) { message.error((e as Error).message) } finally { revoking.value = false }
}
async function exportData() {
  try {
    await reportApi.export({ type: 'credit', projectId: query.projectId })
  } catch (e) { message.error((e as Error).message) }
}

onMounted(async () => {
  try { await loadMeta() } catch (e) { message.error((e as Error).message) }
  await load()
})
</script>
