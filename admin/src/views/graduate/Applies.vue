<template>
  <div class="page-card">
    <div class="toolbar">
      <a-input v-model:value="query.keyword" placeholder="学员姓名 / 账号" allow-clear style="width:190px" @press-enter="reload" />
      <a-select v-model:value="query.projectId" placeholder="培训班" allow-clear show-search option-filter-prop="label" style="width:220px" :options="projectOptions" />
      <a-select v-model:value="query.auditStatus" placeholder="审批状态" allow-clear style="width:130px">
        <a-select-option :value="0">待审批</a-select-option>
        <a-select-option :value="1">已通过</a-select-option>
        <a-select-option :value="2">已驳回</a-select-option>
      </a-select>
      <a-select v-model:value="query.currentNode" placeholder="当前节点" allow-clear style="width:150px">
        <a-select-option value="TEACHER">教员初审</a-select-option>
        <a-select-option value="LEADER">领导复审</a-select-option>
        <a-select-option value="ADMIN">管理员终审</a-select-option>
        <a-select-option value="DONE">已完成</a-select-option>
      </a-select>
      <a-button type="primary" @click="reload">查询</a-button>
      <a-button @click="reset">重置</a-button>
    </div>

    <a-table :data-source="rows" :columns="columns" :loading="loading" :pagination="pagination" row-key="id" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'student'">
          <div style="font-weight:500">{{ record.userName }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.orgName || record.username }}</div>
        </template>
        <template v-else-if="column.key === 'project'">{{ record.projectName }}</template>
        <template v-else-if="column.key === 'credit'">
          <span style="font-weight:600">{{ record.creditSnapshot }}</span> <span class="muted">/ {{ record.needCredit }} 学分</span>
          <div style="font-size:12px;color:#94a3b8">学时 {{ record.studyHourSnapshot }} 小时</div>
        </template>
        <template v-else-if="column.key === 'node'">
          <a-steps size="small" :current="nodeStep(record)" style="width:260px" :items="stepItems" />
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.auditStatus === 1 ? 'green' : record.auditStatus === 2 ? 'red' : 'processing'">{{ record.auditStatusLabel }}</a-tag>
          <div v-if="record.rejectReason" style="font-size:12px;color:#dc2626;margin-top:3px;max-width:170px">{{ record.rejectReason }}</div>
        </template>
        <template v-else-if="column.key === 'time'">
          <div style="font-size:12px">{{ record.applyTime }}</div>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a @click="openDetail(record)">详情</a>
            <a v-if="record.auditStatus === 0" @click="openAudit(record, true)">审批</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="auditOpen" :title="approve ? '通过毕业申请' : '驳回毕业申请'" :confirm-loading="submitting" :ok-button-props="{ danger: !approve }" :width="500" @ok="submitAudit">
      <a-alert v-if="current" type="info" show-icon :message="`${current.userName} · ${current.projectName} · 当前节点：${nodeText(current.currentNode)}`" style="margin-bottom:16px" />
      <a-form layout="vertical">
        <a-form-item :label="approve ? '审批意见' : '驳回原因'" :required="!approve">
          <a-textarea v-model:value="opinion" :rows="3" :placeholder="approve ? '选填，如：学习记录完整，同意毕业' : '请填写驳回原因，学员补齐学分后可重新提交'" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailOpen" title="毕业申请详情" width="720">
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <div style="background:linear-gradient(120deg,#f0f9ff,#f8fafc);border-radius:12px;padding:18px;margin-bottom:18px">
            <div style="display:flex;align-items:center;gap:12px">
              <a-avatar :size="50" style="background:#2563eb">{{ String(detail.userName || '学').slice(0, 1) }}</a-avatar>
              <div style="flex:1">
                <div style="font-size:17px;font-weight:600">{{ detail.userName }}</div>
                <div style="font-size:12px;color:#64748b">{{ detail.orgName }} · {{ detail.projectName }}</div>
              </div>
              <a-tag :color="detail.auditStatus === 1 ? 'green' : detail.auditStatus === 2 ? 'red' : 'processing'">{{ detail.auditStatusLabel }}</a-tag>
            </div>
            <div style="display:flex;gap:34px;margin-top:16px">
              <div><div style="font-size:11px;color:#94a3b8">申请学分</div><div style="font-size:20px;font-weight:700">{{ detail.creditSnapshot }}</div></div>
              <div><div style="font-size:11px;color:#94a3b8">累计学时</div><div style="font-size:20px;font-weight:700">{{ detail.studyHourSnapshot }}h</div></div>
              <div><div style="font-size:11px;color:#94a3b8">申请时间</div><div style="font-size:13px;margin-top:4px">{{ detail.applyTime }}</div></div>
            </div>
          </div>

          <a-steps :current="nodeStep(detail)" size="small" :items="stepItems" style="margin-bottom:22px" />

          <div v-if="detail.certificateUrl" style="background:#ecfdf5;border:1px solid #a7f3d0;border-radius:10px;padding:14px;display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
            <div>
              <div style="font-weight:600;color:#047857">电子证书已生成</div>
              <div style="font-size:12px;color:#059669;margin-top:3px">证书编号：{{ detail.certificateNo }}</div>
            </div>
            <a-button type="primary" ghost :href="fileUrl(detail.certificateUrl)" target="_blank">查看证书</a-button>
          </div>

          <div style="font-size:14px;font-weight:600;margin-bottom:10px">审批记录</div>
          <a-empty v-if="!detail.approvalLogs?.length" description="暂无审批记录" />
          <a-timeline v-else>
            <a-timeline-item v-for="log in detail.approvalLogs" :key="log.id" :color="log.action === 'REJECT' ? 'red' : 'green'">
              <div style="font-weight:500">{{ nodeText(log.node || '') }} · {{ log.action === 'REJECT' ? '驳回' : '通过' }}</div>
              <div style="font-size:12px;color:#64748b;margin-top:2px">{{ log.opinion || '-' }}</div>
              <div style="font-size:11px;color:#94a3b8;margin-top:3px">{{ log.auditorName }} · {{ log.createTime }}</div>
            </a-timeline-item>
          </a-timeline>

          <div v-if="detail.auditStatus === 0" style="margin-top:20px;text-align:right">
            <a-button type="primary" style="margin-right:8px" @click="openAudit(detail, true)">通过</a-button>
            <a-button danger @click="openAudit(detail, false)">驳回</a-button>
          </div>
        </template>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, type TablePaginationConfig } from 'ant-design-vue'
import { graduateApi, projectApi } from '../../api'
import { fileUrl } from '../../api/http'
import type { GraduateApplyRow, ProjectRow } from '../../types'

const loading = ref(false)
const rows = ref<GraduateApplyRow[]>([])
const projects = ref<ProjectRow[]>([])
const query = reactive<{ pageNum: number; pageSize: number; keyword: string; projectId?: number; auditStatus?: number; currentNode?: string }>({ pageNum: 1, pageSize: 10, keyword: '' })
const pagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (n: number) => `共 ${n} 条申请` })
const auditOpen = ref(false)
const submitting = ref(false)
const approve = ref(true)
const current = ref<GraduateApplyRow | null>(null)
const opinion = ref('')
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<GraduateApplyRow | null>(null)
const projectOptions = computed(() => projects.value.map((p) => ({ value: p.id, label: p.projectName })))
const stepItems = [{ title: '教员初审' }, { title: '领导复审' }, { title: '管理员终审' }, { title: '完成' }]
const columns = [
  { title: '学员', key: 'student', width: 150 },
  { title: '培训班', key: 'project', width: 190, ellipsis: true },
  { title: '学分 / 学时', key: 'credit', width: 140 },
  { title: '审批进度', key: 'node', width: 290 },
  { title: '状态', key: 'status', width: 180 },
  { title: '申请时间', key: 'time', width: 170 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' as const }
]

function nodeText(node: string) {
  return ({ TEACHER: '教员初审', LEADER: '领导复审', ADMIN: '管理员终审', DONE: '已完成', REJECTED: '已驳回', VERIFY: '核验复核', CREDIT: '学分审批' } as Record<string, string>)[node] || node
}
function nodeStep(row: GraduateApplyRow) {
  if (row.auditStatus === 1 || row.currentNode === 'DONE') return 4
  if (row.currentNode === 'REJECTED') return 0
  return ({ TEACHER: 0, LEADER: 1, ADMIN: 2 } as Record<string, number>)[row.currentNode] ?? 0
}
async function loadMeta() { projects.value = (await projectApi.page({ pageNum: 1, pageSize: 200 })).records }
async function load() {
  loading.value = true
  try {
    const page = await graduateApi.page({ ...query, keyword: query.keyword || undefined })
    rows.value = page.records
    pagination.current = page.pageNum
    pagination.total = page.total
  } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
function reload() { query.pageNum = 1; void load() }
function reset() { query.keyword = ''; query.projectId = undefined; query.auditStatus = undefined; query.currentNode = undefined; reload() }
function onTableChange(pag: TablePaginationConfig) { query.pageNum = Number(pag.current || 1); query.pageSize = Number(pag.pageSize || 10); void load() }

function openAudit(row: GraduateApplyRow, pass: boolean) {
  current.value = row
  approve.value = pass
  opinion.value = ''
  auditOpen.value = true
}
async function submitAudit() {
  if (!current.value) return
  if (!approve.value && !opinion.value.trim()) { message.warning('驳回时必须填写原因'); return }
  submitting.value = true
  try {
    await graduateApi.audit(current.value.id, approve.value ? 'approve' : 'reject', opinion.value)
    message.success(approve.value ? '审批已通过' : '已驳回')
    auditOpen.value = false
    detailOpen.value = false
    await load()
  } catch (e) { message.error((e as Error).message) } finally { submitting.value = false }
}
async function openDetail(row: GraduateApplyRow) {
  detailOpen.value = true
  detailLoading.value = true
  try { detail.value = await graduateApi.detail(row.id) } catch (e) { message.error((e as Error).message) } finally { detailLoading.value = false }
}

onMounted(async () => {
  try { await loadMeta() } catch (e) { message.error((e as Error).message) }
  await load()
})
</script>
