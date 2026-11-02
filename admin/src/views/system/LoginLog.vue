<template>
  <div class="page-card">
    <div class="toolbar">
      <a-input v-model:value="query.keyword" placeholder="账号 / IP" allow-clear style="width:230px" @press-enter="reload" />
      <a-range-picker v-model:value="range" show-time value-format="YYYY-MM-DD HH:mm:ss" style="width:340px" />
      <a-button type="primary" @click="reload">查询</a-button>
      <a-button @click="reset">重置</a-button>
    </div>

    <a-table :data-source="rows" :columns="columns" :loading="loading" :pagination="pagination" row-key="id" size="middle" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'user'">
          <div style="font-weight:500">{{ record.realName || record.username }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.username }}</div>
        </template>
        <template v-else-if="column.key === 'device'">
          <div>{{ record.ip }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.device }}</div>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'success' : 'error'">{{ record.status === 1 ? '成功' : '失败' }}</a-tag>
          <span style="margin-left:6px;font-size:12px;color:#64748b">{{ record.msg }}</span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, type TablePaginationConfig } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import { systemApi } from '../../api'
import type { LogRow } from '../../types'

const loading = ref(false)
const rows = ref<LogRow[]>([])
const range = ref<[Dayjs, Dayjs] | null>(null)
const query = reactive<Record<string, any>>({ pageNum: 1, pageSize: 10, keyword: '' })
const pagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (n: number) => `共 ${n} 条记录` })
const columns = [
  { title: '用户', key: 'user', width: 180 },
  { title: 'IP / 终端', key: 'device', width: 220 },
  { title: '结果', key: 'status', width: 260 },
  { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime', width: 180 }
]
async function load() {
  loading.value = true
  try {
    const page = await systemApi.loginLogs({ ...query, start: range.value?.[0], end: range.value?.[1] })
    rows.value = page.records
    pagination.current = page.pageNum
    pagination.total = page.total
  } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
function reload() { query.pageNum = 1; void load() }
function reset() { query.keyword = ''; range.value = null; reload() }
function onTableChange(pag: TablePaginationConfig) { query.pageNum = Number(pag.current || 1); query.pageSize = Number(pag.pageSize || 10); void load() }
onMounted(load)
</script>
