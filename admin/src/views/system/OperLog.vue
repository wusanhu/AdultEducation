<template>
  <div class="page-card">
    <div class="toolbar">
      <a-input v-model:value="query.keyword" placeholder="操作人 / 操作内容" allow-clear style="width:230px" @press-enter="reload" />
      <a-select v-model:value="query.module" placeholder="模块" allow-clear style="width:150px" :options="moduleOptions" />
      <a-range-picker v-model:value="range" show-time value-format="YYYY-MM-DD HH:mm:ss" style="width:340px" />
      <a-button type="primary" @click="reload">查询</a-button>
      <a-button @click="reset">重置</a-button>
    </div>

    <a-table :data-source="rows" :columns="columns" :loading="loading" :pagination="pagination" row-key="id" size="middle" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'user'">
          <div style="font-weight:500">{{ record.userName || '—' }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.orgName || '' }}</div>
        </template>
        <template v-else-if="column.key === 'module'"><a-tag color="blue">{{ record.operModule }}</a-tag></template>
        <template v-else-if="column.key === 'content'">
          <span style="color:#334155">{{ record.operContent }}</span>
        </template>
        <template v-else-if="column.key === 'result'">
          <a-tag :color="record.result === 1 ? 'green' : 'red'">{{ record.result === 1 ? '成功' : '失败' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'device'">
          <span style="font-size:12px;color:#64748b">{{ record.ip }}</span>
          <div style="font-size:12px;color:#94a3b8">{{ record.device }}</div>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, type TablePaginationConfig } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import { systemApi } from '../../api'
import type { LogRow } from '../../types'

const loading = ref(false)
const rows = ref<LogRow[]>([])
const range = ref<[Dayjs, Dayjs] | null>(null)
const query = reactive<Record<string, any>>({ pageNum: 1, pageSize: 10, keyword: '', module: undefined })
const pagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (n: number) => `共 ${n} 条日志` })
const moduleOptions = computed(() => Array.from(new Set(rows.value.map((r) => String(r.operModule || '')).filter(Boolean))).map((m) => ({ value: m, label: m })))
const columns = [
  { title: '操作人', key: 'user', width: 150 },
  { title: '模块', key: 'module', width: 130 },
  { title: '操作内容', key: 'content', width: 300 },
  { title: '结果', key: 'result', width: 90, align: 'center' as const },
  { title: 'IP / 终端', key: 'device', width: 200 },
  { title: '耗时', dataIndex: 'duration', key: 'duration', width: 90, align: 'center' as const },
  { title: '操作时间', dataIndex: 'operTime', key: 'operTime', width: 170 }
]
async function load() {
  loading.value = true
  try {
    const page = await systemApi.operLogs({ ...query, start: range.value?.[0], end: range.value?.[1] })
    rows.value = page.records
    pagination.current = page.pageNum
    pagination.total = page.total
  } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
function reload() { query.pageNum = 1; void load() }
function reset() { query.keyword = ''; query.module = undefined; range.value = null; reload() }
function onTableChange(pag: TablePaginationConfig) { query.pageNum = Number(pag.current || 1); query.pageSize = Number(pag.pageSize || 10); void load() }
onMounted(load)
</script>
