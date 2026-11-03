<template>
  <div class="page-card">
    <div class="toolbar">
      <a-input v-model:value="query.keyword" placeholder="学员姓名 / 证书编号" allow-clear style="width:210px" @press-enter="reload" />
      <a-select v-model:value="query.projectId" placeholder="培训班" allow-clear show-search option-filter-prop="label" style="width:220px" :options="projectOptions" />
      <a-date-picker v-model:value="yearDate" picker="year" placeholder="毕业年份" style="width:140px" />
      <a-button type="primary" @click="reload">查询</a-button>
      <a-button @click="reset">重置</a-button>
      <div class="spacer" />
      <a-button v-if="auth.hasPerm('report:export')" @click="exportData"><DownloadOutlined /> 导出档案</a-button>
    </div>

    <a-table :data-source="rows" :columns="columns" :loading="loading" :pagination="pagination" row-key="id" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'student'">
          <div style="font-weight:500">{{ record.userName }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.orgName || record.username }}</div>
        </template>
        <template v-else-if="column.key === 'project'">{{ record.projectName }}</template>
        <template v-else-if="column.key === 'cert'"><a-typography-text copyable style="font-size:13px">{{ record.certificateNo }}</a-typography-text></template>
        <template v-else-if="column.key === 'credit'"><span style="font-weight:600;color:#16a34a">{{ record.creditValue }}</span></template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a v-if="record.certificateUrl" :href="fileUrl(record.certificateUrl)" target="_blank">查看</a>
            <a v-if="record.certificateUrl" :href="fileUrl(record.certificateUrl)" :download="`${record.certificateNo}.pdf`">下载</a>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, type TablePaginationConfig } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import type { Dayjs } from 'dayjs'
import { graduateApi, projectApi, reportApi } from '../../api'
import { fileUrl } from '../../api/http'
import { useAuthStore } from '../../store/auth'
import type { GraduateFileRow, ProjectRow } from '../../types'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref<GraduateFileRow[]>([])
const projects = ref<ProjectRow[]>([])
const yearDate = ref<Dayjs | null>(null)
const query = reactive<{ pageNum: number; pageSize: number; keyword: string; projectId?: number }>({ pageNum: 1, pageSize: 10, keyword: '' })
const pagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (n: number) => `共 ${n} 份毕业档案` })
const projectOptions = computed(() => projects.value.map((p) => ({ value: p.id, label: p.projectName })))
const columns = [
  { title: '学员', key: 'student', width: 170 },
  { title: '培训班', key: 'project', width: 230 },
  { title: '证书编号', key: 'cert', width: 230 },
  { title: '毕业学分', key: 'credit', width: 110 },
  { title: '毕业时间', dataIndex: 'graduateTime', key: 'time', width: 170 },
  { title: '操作', key: 'action', width: 120 }
]
async function loadMeta() { projects.value = (await projectApi.page({ pageNum: 1, pageSize: 200 })).records }
async function load() {
  loading.value = true
  try {
    const page = await graduateApi.files({ ...query, keyword: query.keyword || undefined, year: yearDate.value ? yearDate.value.format('YYYY') : undefined })
    rows.value = page.records
    pagination.current = page.pageNum
    pagination.total = page.total
  } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
function reload() { query.pageNum = 1; void load() }
function reset() { query.keyword = ''; query.projectId = undefined; yearDate.value = null; reload() }
function onTableChange(pag: TablePaginationConfig) { query.pageNum = Number(pag.current || 1); query.pageSize = Number(pag.pageSize || 10); void load() }
async function exportData() {
  try { await reportApi.export({ type: 'graduate', projectId: query.projectId }) } catch (e) { message.error((e as Error).message) }
}

onMounted(async () => {
  try { await loadMeta() } catch (e) { message.error((e as Error).message) }
  await load()
})
</script>
