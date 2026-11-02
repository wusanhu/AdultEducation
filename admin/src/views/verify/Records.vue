<template>
  <div class="page-card">
    <div class="toolbar">
      <a-select v-model:value="query.projectId" placeholder="培训班" allow-clear show-search option-filter-prop="label" style="width:200px" :options="projectOptions" />
      <a-select v-model:value="query.courseId" placeholder="课程" allow-clear show-search option-filter-prop="label" style="width:200px" :options="courseOptions" />
      <a-select v-model:value="query.verifyResult" placeholder="核验结果" allow-clear style="width:130px">
        <a-select-option :value="1">通过</a-select-option>
        <a-select-option :value="0">失败</a-select-option>
        <a-select-option :value="2">超时</a-select-option>
      </a-select>
      <a-select v-model:value="query.status" placeholder="处理状态" allow-clear style="width:140px">
        <a-select-option :value="0">待核验</a-select-option>
        <a-select-option :value="1">已通过</a-select-option>
        <a-select-option :value="2">已失败</a-select-option>
        <a-select-option :value="3">已超时</a-select-option>
        <a-select-option :value="4">已复核</a-select-option>
      </a-select>
      <a-range-picker v-model:value="dateRange" style="width:240px" />
      <a-button type="primary" @click="reload">查询</a-button>
      <a-button @click="reset">重置</a-button>
    </div>

    <a-table :data-source="rows" :columns="columns" :loading="loading" :pagination="pagination" row-key="id" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'student'">
          <div style="font-weight:500">{{ record.userName }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.orgName || record.username }}</div>
        </template>
        <template v-else-if="column.key === 'course'">
          <div style="font-size:13px">{{ record.courseName }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.projectName }}</div>
        </template>
        <template v-else-if="column.key === 'type'">
          <a-tag :color="record.verifyType === 2 ? 'blue' : 'purple'">{{ record.verifyTypeLabel }}</a-tag>
          <div style="font-size:12px;color:#94a3b8">{{ record.triggerPoint || '-' }}</div>
        </template>
        <template v-else-if="column.key === 'result'">
          <a-tag :color="resultColor(record.verifyResult)">{{ record.verifyResultLabel || '待核验' }}</a-tag>
          <div v-if="record.similarity != null" style="font-size:12px;color:#94a3b8">相似度 {{ record.similarity }}</div>
        </template>
        <template v-else-if="column.key === 'image'">
          <a-image v-if="record.imgUrl" :src="record.imgUrl" :width="52" :height="38" style="object-fit:cover;border-radius:5px" />
          <span v-else class="muted" style="font-size:12px">无抓拍</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag>{{ record.statusLabel }}</a-tag>
          <div v-if="record.failReason" style="font-size:11px;color:#dc2626;margin-top:3px;max-width:140px">{{ record.failReason }}</div>
        </template>
        <template v-else-if="column.key === 'action'">
          <a v-if="record.status !== 0" @click="openReview(record)">复核</a>
          <span v-else class="muted">待核验</span>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="reviewOpen" title="核验记录复核" :confirm-loading="reviewing" width="520" @ok="submitReview">
      <a-descriptions v-if="current" :column="1" size="small" bordered style="margin-bottom:16px">
        <a-descriptions-item label="学员">{{ current.userName }}</a-descriptions-item>
        <a-descriptions-item label="课程">{{ current.courseName }}</a-descriptions-item>
        <a-descriptions-item label="核验结果">{{ current.verifyResultLabel }}{{ current.similarity != null ? `（相似度 ${current.similarity}）` : '' }}</a-descriptions-item>
        <a-descriptions-item label="失败原因">{{ current.failReason || '-' }}</a-descriptions-item>
      </a-descriptions>
      <a-form layout="vertical">
        <a-form-item label="复核结论" required>
          <a-radio-group v-model:value="reviewForm.action">
            <a-radio value="allow">允许本次学时有效</a-radio>
            <a-radio value="invalidate">作废本次学时</a-radio>
            <a-radio value="violate">标记违规并撤销学分</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="复核备注"><a-textarea v-model:value="reviewForm.remark" :rows="3" placeholder="填写复核依据与处理说明" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message, type TablePaginationConfig } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import { courseApi, projectApi, verifyApi } from '../../api'
import type { CourseRow, ProjectRow, VerifyLogRow } from '../../types'

const route = useRoute()
const loading = ref(false)
const rows = ref<VerifyLogRow[]>([])
const projects = ref<ProjectRow[]>([])
const courses = ref<CourseRow[]>([])
const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const query = reactive<{ pageNum: number; pageSize: number; projectId?: number; courseId?: number; verifyResult?: number; status?: number }>({
  pageNum: 1, pageSize: 10, courseId: route.query.courseId ? Number(route.query.courseId) : undefined
})
const pagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (n: number) => `共 ${n} 条核验记录` })
const reviewOpen = ref(false)
const reviewing = ref(false)
const current = ref<VerifyLogRow | null>(null)
const reviewForm = reactive({ action: 'allow', remark: '' })
const projectOptions = computed(() => projects.value.map((p) => ({ value: p.id, label: p.projectName })))
const courseOptions = computed(() => courses.value.filter((c) => !query.projectId || c.projectId === query.projectId).map((c) => ({ value: c.id, label: c.courseName })))
const columns = [
  { title: '学员', key: 'student', width: 150 },
  { title: '课程 / 培训班', key: 'course', width: 200 },
  { title: '核验时间', dataIndex: 'verifyTime', key: 'time', width: 170 },
  { title: '方式 / 触发点', key: 'type', width: 130 },
  { title: '结果', key: 'result', width: 120 },
  { title: '抓拍', key: 'image', width: 80 },
  { title: '处理状态', key: 'status', width: 160 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const }
]

function resultColor(result?: number) { return result === 1 ? 'green' : result === 2 ? 'orange' : result === 0 ? 'red' : 'default' }

async function loadMeta() {
  const [projectPage, coursePage] = await Promise.all([projectApi.page({ pageNum: 1, pageSize: 200 }), courseApi.page({ pageNum: 1, pageSize: 200 })])
  projects.value = projectPage.records
  courses.value = coursePage.records
}
async function load() {
  loading.value = true
  try {
    const page = await verifyApi.records({
      ...query,
      startDate: dateRange.value ? dateRange.value[0].format('YYYY-MM-DD') : undefined,
      endDate: dateRange.value ? dateRange.value[1].format('YYYY-MM-DD') : undefined
    })
    rows.value = page.records
    pagination.current = page.pageNum
    pagination.total = page.total
  } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
function reload() { query.pageNum = 1; void load() }
function reset() { query.projectId = undefined; query.courseId = undefined; query.verifyResult = undefined; query.status = undefined; dateRange.value = null; reload() }
function onTableChange(pag: TablePaginationConfig) { query.pageNum = Number(pag.current || 1); query.pageSize = Number(pag.pageSize || 10); void load() }

function openReview(row: VerifyLogRow) {
  current.value = row
  reviewForm.action = 'allow'
  reviewForm.remark = ''
  reviewOpen.value = true
}
async function submitReview() {
  if (!current.value) return
  reviewing.value = true
  try {
    await verifyApi.review(current.value.id, reviewForm.action, reviewForm.remark)
    message.success('复核完成')
    reviewOpen.value = false
    await load()
  } catch (e) { message.error((e as Error).message) } finally { reviewing.value = false }
}

onMounted(async () => {
  try { await loadMeta() } catch (e) { message.error((e as Error).message) }
  await load()
})
</script>
