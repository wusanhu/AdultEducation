<template>
  <div class="page-card">
    <div class="toolbar">
      <a-select v-model:value="projectId" placeholder="全部培训班" allow-clear show-search option-filter-prop="label"
                style="width:240px" :options="projectOptions" @change="load" />
      <a-select v-if="mode === 'warning'" v-model:value="warningType" style="width:150px" @change="load">
        <a-select-option value="ALL">全部预警</a-select-option>
        <a-select-option value="CREDIT">学分不足</a-select-option>
        <a-select-option value="EXPIRE">即将到期</a-select-option>
        <a-select-option value="INACTIVE">长期未学习</a-select-option>
      </a-select>
      <a-button type="primary" @click="load">刷新</a-button>
      <div class="spacer" />
      <a-button v-if="mode === 'warning' && auth.hasPerm('report:warning')" :loading="notifying" @click="notifyAll">
        <BellOutlined /> 一键提醒
      </a-button>
      <a-button v-if="auth.hasPerm('report:export')" @click="exportData"><DownloadOutlined /> 导出 Excel</a-button>
    </div>

    <a-alert v-if="mode === 'warning'" type="info" show-icon style="margin-bottom:14px"
             message="预警口径：学分未达标、培训班 30 天内到期、超过 30 天未登录或未学习。可勾选学员后发送站内提醒。" />

    <a-table :data-source="rows" :columns="columns" :loading="loading" :pagination="{ pageSize: 20, showSizeChanger: false }"
             :row-key="rowKey" :row-selection="mode === 'warning' ? selection : undefined" size="middle" :scroll="{ x: 1000 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          <a-tag :color="record.type === 'CREDIT' ? 'orange' : record.type === 'EXPIRE' ? 'red' : 'default'">{{ record.typeLabel }}</a-tag>
        </template>
        <template v-else-if="column.key === 'student'">
          <div style="font-weight:500">{{ record.userName || '—' }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.phone || record.orgName || '' }}</div>
        </template>
        <template v-else-if="column.key === 'needExam'">
          <a-tag :color="record.needExam ? 'blue' : 'default'">{{ record.needExam ? '需测验' : '免测验' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'message'">
          <span style="color:#64748b">{{ record.message }}</span>
        </template>
        <template v-else-if="column.key === 'progress'">
          <a-progress :percent="Number(record.avgProgress || 0)" size="small" />
        </template>
        <template v-else-if="column.key === 'rate'">
          <span :style="{ color: Number(record[column.dataIndex] || 0) >= 60 ? '#16a34a' : '#dc2626', fontWeight: 600 }">
            {{ record[column.dataIndex] ?? 0 }}%
          </span>
        </template>
        <template v-else>{{ record[column.dataIndex] ?? '—' }}</template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { BellOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { projectApi, reportApi } from '../../api'
import { useAuthStore } from '../../store/auth'

type Row = Record<string, any>

const route = useRoute()
const auth = useAuthStore()
const mode = computed(() => {
  const seg = route.path.split('/').pop() || 'org'
  return ['org', 'credit', 'course', 'warning'].includes(seg) ? seg : 'org'
})
const titles: Record<string, string> = { org: '单位汇总报表', credit: '培训班学分统计', course: '课程学习报表', warning: '学习预警清单' }
const loading = ref(false)
const notifying = ref(false)
const rows = ref<Row[]>([])
const projects = ref<{ id: number; projectName: string }[]>([])
const projectId = ref<number>()
const warningType = ref('ALL')
const selectedUserIds = ref<number[]>([])
const projectOptions = computed(() => projects.value.map((p) => ({ value: p.id, label: p.projectName })))

const columns = computed(() => {
  if (mode.value === 'org') {
    return [
      { title: '单位名称', dataIndex: 'orgName', key: 'orgName', width: 220, fixed: 'left' },
      { title: '学员数', dataIndex: 'studentCount', key: 'studentCount', width: 90 },
      { title: '学习记录', dataIndex: 'studyCount', key: 'studyCount', width: 100 },
      { title: '完成数', dataIndex: 'completedCount', key: 'completedCount', width: 90 },
      { title: '学习学时', dataIndex: 'studyHours', key: 'studyHours', width: 110 },
      { title: '学分合计', dataIndex: 'creditTotal', key: 'creditTotal', width: 110 },
      { title: '人均学分', dataIndex: 'avgCredit', key: 'avgCredit', width: 110 },
      { title: '毕业人数', dataIndex: 'graduateCount', key: 'graduateCount', width: 100 },
      { title: '完成率', dataIndex: 'completionRate', key: 'rate', width: 110 }
    ]
  }
  if (mode.value === 'credit') {
    return [
      { title: '培训班', dataIndex: 'projectName', key: 'projectName', width: 240, fixed: 'left' },
      { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
      { title: '人数', dataIndex: 'memberCount', key: 'memberCount', width: 80 },
      { title: '所需学分', dataIndex: 'needCredit', key: 'needCredit', width: 110 },
      { title: '学分合计', dataIndex: 'creditTotal', key: 'creditTotal', width: 110 },
      { title: '人均学分', dataIndex: 'avgCredit', key: 'avgCredit', width: 110 },
      { title: '已发放人数', dataIndex: 'issuedCount', key: 'issuedCount', width: 110 },
      { title: '毕业人数', dataIndex: 'graduateCount', key: 'graduateCount', width: 100 },
      { title: '自动录入', dataIndex: 'autoCreditTotal', key: 'autoCreditTotal', width: 110 },
      { title: '手工补录', dataIndex: 'manualCreditTotal', key: 'manualCreditTotal', width: 110 }
    ]
  }
  if (mode.value === 'course') {
    return [
      { title: '课程名称', dataIndex: 'courseName', key: 'courseName', width: 260, fixed: 'left' },
      { title: '测验', dataIndex: 'needExam', key: 'needExam', width: 100 },
      { title: '学习人数', dataIndex: 'studyCount', key: 'studyCount', width: 100 },
      { title: '完成人数', dataIndex: 'completedCount', key: 'completedCount', width: 100 },
      { title: '完成率', dataIndex: 'completionRate', key: 'rate', width: 100 },
      { title: '测验次数', dataIndex: 'examCount', key: 'examCount', width: 100 },
      { title: '通过人数', dataIndex: 'examPassCount', key: 'examPassCount', width: 100 },
      { title: '通过率', dataIndex: 'examPassRate', key: 'rate', width: 100 },
      { title: '平均分', dataIndex: 'avgScore', key: 'avgScore', width: 100 },
      { title: '平均进度', dataIndex: 'avgProgress', key: 'progress', width: 160 }
    ]
  }
  return [
    { title: '预警类型', dataIndex: 'typeLabel', key: 'type', width: 120, fixed: 'left' },
    { title: '学员 / 联系电话', key: 'student', width: 180 },
    { title: '培训班', dataIndex: 'projectName', key: 'projectName', width: 220 },
    { title: '所需学分', dataIndex: 'needCredit', key: 'needCredit', width: 100 },
    { title: '当前学分', dataIndex: 'currentCredit', key: 'currentCredit', width: 100 },
    { title: '差额', dataIndex: 'deficit', key: 'deficit', width: 90 },
    { title: '到期 / 未学习', dataIndex: 'lastLoginTime', key: 'lastLoginTime', width: 160 },
    { title: '预警说明', dataIndex: 'message', key: 'message', width: 280 }
  ]
})

const rowKey = (row: Row) => `${row.type || 'row'}-${row.userId || row.projectId}-${row.projectId || ''}`
const selection = computed(() => ({
  selectedRowKeys: selectedUserIds.value,
  onChange: (keys: (string | number)[], selected: Row[]) => {
    selectedUserIds.value = selected.filter((r) => r.userId).map((r) => Number(r.userId))
  },
  getCheckboxProps: (row: Row) => ({ disabled: !row.userId })
}))

async function load() {
  loading.value = true
  try {
    if (mode.value === 'org') rows.value = await reportApi.org(projectId.value)
    else if (mode.value === 'credit') rows.value = await reportApi.credit(projectId.value)
    else if (mode.value === 'course') rows.value = await reportApi.course(projectId.value)
    else rows.value = await reportApi.warnings({ projectId: projectId.value, type: warningType.value })
  } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}

async function exportData() {
  const type = mode.value === 'warning' ? 'warning' : mode.value === 'credit' ? 'creditReport' : mode.value
  try { await reportApi.export({ type, projectId: projectId.value }) } catch (e) { message.error((e as Error).message) }
}

async function notifyAll() {
  notifying.value = true
  try {
    const count = await reportApi.notifyWarnings({
      projectId: projectId.value, type: warningType.value,
      userIds: selectedUserIds.value.length ? selectedUserIds.value.join(',') : undefined
    })
    message.success(`已发送 ${count} 条提醒`)
  } catch (e) { message.error((e as Error).message) } finally { notifying.value = false }
}

watch(() => route.path, () => {
  selectedUserIds.value = []
  warningType.value = 'ALL'
  void load()
})

onMounted(async () => {
  document.title = `${titles[mode.value]} · ${import.meta.env.VITE_APP_TITLE || '继续教育学分管理平台'}`
  try {
    projects.value = (await projectApi.page({ pageNum: 1, pageSize: 200 })).records as any
    if (route.query.projectId) projectId.value = Number(route.query.projectId)
  } catch (e) { message.error((e as Error).message) }
  await load()
})
</script>
