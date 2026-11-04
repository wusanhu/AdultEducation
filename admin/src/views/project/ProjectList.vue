<template>
  <div class="page-card">
    <div class="toolbar">
      <a-input v-model:value="query.keyword" placeholder="项目名称 / 编码" allow-clear style="width:220px" @press-enter="reload" />
      <a-select v-model:value="query.status" placeholder="项目状态" allow-clear style="width:140px">
        <a-select-option :value="0">未开始</a-select-option>
        <a-select-option :value="1">进行中</a-select-option>
        <a-select-option :value="2">已结束</a-select-option>
        <a-select-option :value="3">已归档</a-select-option>
      </a-select>
      <a-button type="primary" @click="reload">查询</a-button>
      <a-button @click="reset">重置</a-button>
      <div class="spacer" />
      <a-button v-if="auth.hasPerm('project:add')" type="primary" @click="openCreate">
        <PlusOutlined /> 新建培训班
      </a-button>
    </div>

    <a-table :data-source="rows" :columns="columns" :loading="loading" :pagination="pagination" row-key="id" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'project'">
          <a @click="$router.push(`/project/${record.id}`)" style="font-weight:600">{{ record.projectName }}</a>
          <div style="font-size:12px;color:#94a3b8">{{ record.projectCode }}</div>
        </template>
        <template v-else-if="column.key === 'period'">
          <div style="font-size:13px">{{ formatDate(record.startTime) }}</div>
          <div style="font-size:12px;color:#94a3b8">至 {{ formatDate(record.endTime) }}</div>
        </template>
        <template v-else-if="column.key === 'credit'">
          <span style="font-weight:600">{{ record.needCredit }}</span> <span class="muted">学分</span>
        </template>
        <template v-else-if="column.key === 'scale'">
          <div style="font-size:13px">{{ record.memberCount }} 名学员</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.courseCount }} 门课程 · {{ (record.teachers || []).length }} 名教员</div>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a @click="$router.push(`/project/${record.id}`)">学员</a>
            <a v-if="auth.hasPerm('project:edit') && record.status !== 3" @click="openEdit(record)">编辑</a>
            <a-dropdown v-if="(auth.hasPerm('project:edit') || auth.hasPerm('project:delete')) && record.status !== 3">
              <a>更多 <DownOutlined /></a>
              <template #overlay>
                <a-menu @click="(info: any) => onAction(String(info.key), record)">
                  <a-menu-item v-if="auth.hasPerm('project:edit') && record.status === 0" key="start">开始培训</a-menu-item>
                  <a-menu-item v-if="auth.hasPerm('project:edit') && record.status === 1" key="end">结束培训</a-menu-item>
                  <a-menu-item v-if="auth.hasPerm('project:edit') && record.status !== 3" key="archive">归档项目</a-menu-item>
                  <a-menu-item v-if="auth.hasPerm('project:edit') && record.status === 2" key="restart">重新开放</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item v-if="auth.hasPerm('project:delete')" key="delete" danger>删除</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <span v-if="record.status === 3" class="muted" style="font-size:12px">仅可查看</span>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-drawer v-model:open="drawerOpen" :title="form.id ? '编辑培训班' : '新建培训班'" width="620" :body-style="{ paddingBottom: '80px' }">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="14">
            <a-form-item label="项目名称" name="projectName"><a-input v-model:value="form.projectName" placeholder="如：2026年度专业技术人员继续教育" /></a-form-item>
          </a-col>
          <a-col :span="10">
            <a-form-item label="项目编码" name="projectCode"><a-input v-model:value="form.projectCode" placeholder="如：P2026-01" /></a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="培训周期" name="period">
          <a-range-picker v-model:value="form.period" show-time style="width:100%" :placeholder="['开始时间', '结束时间']" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="毕业所需总学分" name="needCredit"><a-input-number v-model:value="form.needCredit" :min="0.5" :step="0.5" style="width:100%" addon-after="学分" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="项目状态" name="status">
              <a-select v-model:value="form.status">
                <a-select-option :value="0">未开始</a-select-option>
                <a-select-option :value="1">进行中</a-select-option>
                <a-select-option :value="2">已结束</a-select-option>
                <a-select-option :value="3">已归档</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="项目简介"><a-textarea v-model:value="form.description" :rows="3" placeholder="培训目标、对象与学习要求" /></a-form-item>
        <a-form-item label="毕业审批流程">
          <div style="display:flex;gap:10px;flex-wrap:wrap">
            <a-checkbox v-model:checked="form.needTeacherAudit">教员初审</a-checkbox>
            <a-checkbox v-model:checked="form.needLeaderAudit">单位领导复审</a-checkbox>
          </div>
          <div class="muted" style="font-size:12px;margin-top:6px">至少保留一个节点；关闭某节点后，审批将自动流转至下一节点，末节点通过后生成电子证书。</div>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="学员选项"><a-checkbox v-model:checked="form.allowOverdue">允许逾期学习</a-checkbox></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="结业选项"><a-checkbox v-model:checked="form.autoGraduate">符合条件自动结业</a-checkbox></a-form-item></a-col>
        </a-row>
        <a-form-item label="证书模板">
          <div style="display:flex;gap:10px;align-items:center">
            <a-upload :show-upload-list="false" :before-upload="uploadTemplate" accept=".png,.jpg,.jpeg,.pdf">
              <a-button><UploadOutlined /> 上传模板</a-button>
            </a-upload>
            <span v-if="form.certificateTemplateUrl" class="muted" style="font-size:12px">已上传：{{ form.certificateTemplateUrl }}</span>
            <span v-else class="muted" style="font-size:12px">未上传时使用系统默认证书样式</span>
          </div>
        </a-form-item>
      </a-form>
      <template #footer>
        <div style="text-align:right">
          <a-button style="margin-right:8px" @click="drawerOpen = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="save">保存</a-button>
        </div>
      </template>
    </a-drawer>

    <a-modal v-model:open="periodOpen" title="设置启用周期" :confirm-loading="periodSaving" :width="480" @ok="submitPeriod">
      <a-alert type="warning" show-icon :message="`《${periodTarget?.projectName || ''}》已过期，重新启用需设置新的启用周期。`" style="margin-bottom:16px" />
      <a-form layout="vertical">
        <a-form-item label="启用周期" required>
          <a-range-picker v-model:value="periodRange" show-time style="width:100%" :placeholder="['启用开始时间', '启用结束时间']" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal, type FormInstance, type TablePaginationConfig } from 'ant-design-vue'
import { DownOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'
import dayjs, { type Dayjs } from 'dayjs'
import { projectApi } from '../../api'
import { upload } from '../../api/http'
import { useAuthStore } from '../../store/auth'
import type { ProjectRow } from '../../types'

interface ProjectForm {
  id?: number
  projectName: string
  projectCode: string
  description: string
  period: [Dayjs, Dayjs] | null
  needCredit: number
  status: number
  certificateTemplateUrl: string
  allowOverdue: boolean
  autoGraduate: boolean
  needTeacherAudit: boolean
  needLeaderAudit: boolean
}

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const rows = ref<ProjectRow[]>([])
const total = ref(0)
const drawerOpen = ref(false)
const periodOpen = ref(false)
const periodSaving = ref(false)
const periodRange = ref<[Dayjs, Dayjs] | null>(null)
const periodTarget = ref<ProjectRow | null>(null)
const formRef = ref<FormInstance>()
const query = reactive<{ pageNum: number; pageSize: number; keyword: string; status?: number }>({ pageNum: 1, pageSize: 10, keyword: '' })
const form = reactive<ProjectForm>(emptyForm())

const columns = [
  { title: '培训班', key: 'project', width: 260 },
  { title: '培训周期', key: 'period', width: 190 },
  { title: '毕业学分', key: 'credit', width: 110, align: 'center' as const },
  { title: '规模', key: 'scale', width: 180 },
  { title: '状态', key: 'status', width: 100, align: 'center' as const },
  { title: '创建人', dataIndex: 'createUserName', key: 'creator', width: 100 },
  { title: '操作', key: 'action', width: 190, fixed: 'right' as const, align: 'right' as const }
]
const rules = {
  projectName: [{ required: true, message: '请填写项目名称' }],
  projectCode: [{ required: true, message: '请填写项目编码' }],
  period: [{ required: true, message: '请选择培训周期' }],
  needCredit: [{ required: true, message: '请填写所需学分' }]
}

function emptyForm(): ProjectForm {
  return {
    projectName: '', projectCode: '', description: '', period: null, needCredit: 10, status: 0,
    certificateTemplateUrl: '', allowOverdue: false, autoGraduate: false,
    needTeacherAudit: true, needLeaderAudit: true
  }
}

const pagination = reactive<TablePaginationConfig>({
  current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (n: number) => `共 ${n} 条`
})

function formatDate(value?: string) { return value ? dayjs(value).format('YYYY-MM-DD') : '-' }
function statusText(status: number) { return ({ 0: '未开始', 1: '进行中', 2: '已结束', 3: '已归档' } as Record<number, string>)[status] || '未知' }
function statusColor(status: number) { return ({ 0: 'default', 1: 'processing', 2: 'orange', 3: 'purple' } as Record<number, string>)[status] || 'default' }

async function load() {
  loading.value = true
  try {
    const page = await projectApi.page({ pageNum: query.pageNum, pageSize: query.pageSize, keyword: query.keyword || undefined, status: query.status })
    rows.value = page.records
    total.value = page.total
    pagination.current = page.pageNum
    pagination.total = page.total
  } catch (e) {
    message.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

function reload() { query.pageNum = 1; void load() }
function reset() { query.keyword = ''; query.status = undefined; reload() }
function onTableChange(pag: TablePaginationConfig) { query.pageNum = Number(pag.current || 1); query.pageSize = Number(pag.pageSize || 10); void load() }

async function openCreate() {
  Object.assign(form, emptyForm())
  drawerOpen.value = true
  try {
    form.projectCode = await projectApi.nextCode()
  } catch {
    // 取号失败时留空，由必填校验兜住
  }
}

function openEdit(row: ProjectRow) {
  Object.assign(form, {
    id: row.id, projectName: row.projectName, projectCode: row.projectCode, description: row.description || '',
    period: [dayjs(row.startTime), dayjs(row.endTime)], needCredit: Number(row.needCredit), status: row.status,
    certificateTemplateUrl: row.certificateTemplateUrl || '',
    allowOverdue: row.allowOverdue === 1, autoGraduate: row.autoGraduate === 1,
    needTeacherAudit: row.needTeacherAudit === 1, needLeaderAudit: row.needLeaderAudit === 1
  })
  drawerOpen.value = true
}

async function uploadTemplate(file: File) {
  try {
    const result = await upload<{ url: string }>('/files/upload', file, { type: 'template' })
    form.certificateTemplateUrl = result.url
    message.success('模板上传成功')
  } catch (e) {
    message.error((e as Error).message)
  }
  return false
}

async function save() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (!form.period) return
  if (!form.needTeacherAudit && !form.needLeaderAudit) {
    message.warning('毕业审批至少需要保留一个节点')
    return
  }
  saving.value = true
  const payload = {
    projectName: form.projectName,
    projectCode: form.projectCode,
    description: form.description,
    startTime: form.period[0].format('YYYY-MM-DDTHH:mm:ss'),
    endTime: form.period[1].format('YYYY-MM-DDTHH:mm:ss'),
    needCredit: form.needCredit,
    status: form.status,
    certificateTemplateUrl: form.certificateTemplateUrl || null,
    allowOverdue: form.allowOverdue ? 1 : 0,
    autoGraduate: form.autoGraduate ? 1 : 0,
    needTeacherAudit: form.needTeacherAudit ? 1 : 0,
    needLeaderAudit: form.needLeaderAudit ? 1 : 0
  }
  try {
    if (form.id) await projectApi.update(form.id, payload)
    else await projectApi.save(payload)
    message.success('保存成功')
    drawerOpen.value = false
    await load()
  } catch (e) {
    message.error((e as Error).message)
  } finally {
    saving.value = false
  }
}

function onAction(key: string, row: ProjectRow) {
  if (key === 'delete') {
    Modal.confirm({
      title: '删除培训班', content: `确定删除《${row.projectName}》吗？项目下已有课程或学员时无法删除。`, okType: 'danger',
      onOk: async () => { await projectApi.remove(row.id); message.success('已删除'); await load() }
    })
    return
  }
  const statusMap: Record<string, number> = { start: 1, end: 2, archive: 3, restart: 1 }
  const labelMap: Record<string, string> = { start: '开始培训', end: '结束培训', archive: '归档项目', restart: '重新开放' }
  const expired = row.endTime ? dayjs(row.endTime).isBefore(dayjs()) : true
  if (key === 'restart' || (key === 'start' && expired)) {
    periodTarget.value = row
    periodRange.value = [dayjs(), dayjs().add(30, 'day')]
    periodOpen.value = true
    return
  }
  Modal.confirm({
    title: labelMap[key], content: `确定对《${row.projectName}》执行该操作吗？`,
    onOk: async () => {
      await projectApi.status(row.id, statusMap[key])
      message.success('操作成功')
      await load()
    }
  })
}

async function submitPeriod() {
  if (!periodTarget.value || !periodRange.value) {
    message.warning('请设置启用周期')
    return
  }
  periodSaving.value = true
  try {
    await projectApi.status(periodTarget.value.id, 1, {
      startTime: periodRange.value[0].format('YYYY-MM-DDTHH:mm:ss'),
      endTime: periodRange.value[1].format('YYYY-MM-DDTHH:mm:ss')
    })
    message.success('已重新启用')
    periodOpen.value = false
    await load()
  } catch (e) {
    message.error((e as Error).message)
  } finally {
    periodSaving.value = false
  }
}

onMounted(load)
</script>
