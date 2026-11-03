<template>
  <div class="page-card">
    <div class="toolbar">
      <a-input v-model:value="query.keyword" placeholder="学员姓名 / 账号" allow-clear style="width:190px" @press-enter="reload" />
      <a-select v-model:value="query.projectId" placeholder="培训班" allow-clear show-search option-filter-prop="label" style="width:210px" :options="projectOptions" />
      <a-select v-model:value="query.courseId" placeholder="课程" allow-clear show-search option-filter-prop="label" style="width:210px" :options="courseOptions" />
      <a-select v-model:value="query.abnormal" placeholder="异常状态" allow-clear style="width:130px">
        <a-select-option :value="1">仅异常记录</a-select-option>
        <a-select-option :value="0">正常记录</a-select-option>
      </a-select>
      <a-select v-model:value="query.status" placeholder="学习状态" allow-clear style="width:120px">
        <a-select-option :value="1">学习中</a-select-option>
        <a-select-option :value="2">已完成</a-select-option>
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
        <template v-else-if="column.key === 'course'">
          <div style="font-size:13px">{{ record.courseName }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.projectName }}</div>
        </template>
        <template v-else-if="column.key === 'progress'">
          <a-progress :percent="Number(record.progress || 0)" size="small" :stroke-color="record.isAbnormal === 1 ? '#dc2626' : '#2563eb'" />
          <div style="font-size:12px;color:#94a3b8">有效 {{ formatDuration(record.validDuration) }}</div>
        </template>
        <template v-else-if="column.key === 'verify'">
          <div style="font-size:13px">核验 {{ record.verifyTotal || record.verifyCount || 0 }} 次</div>
          <div style="font-size:12px;color:#16a34a">通过 {{ record.verifySuccess }} · <span style="color:#dc2626">失败 {{ record.verifyFail }}</span></div>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 2 ? 'green' : 'processing'">{{ record.statusLabel || (record.status === 2 ? '已完成' : '学习中') }}</a-tag>
          <a-tag v-if="record.isAbnormal === 1" color="red">异常</a-tag>
          <a-tag v-if="record.online" color="cyan">在线</a-tag>
        </template>
        <template v-else-if="column.key === 'heartbeat'">
          <div style="font-size:12px">{{ record.lastHeartbeat || '-' }}</div>
          <div style="font-size:12px;color:#94a3b8">切屏 {{ record.screenSwitchCount }} 次</div>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a @click="openDetail(record.id)">学习明细</a>
            <a v-if="record.isAbnormal === 1" style="color:#16a34a" @click="unlock(record)">解除限制</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-drawer v-model:open="detailOpen" title="学习明细" width="900">
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <div style="display:flex;gap:20px;flex-wrap:wrap;margin-bottom:18px">
            <div style="flex:1;min-width:280px;background:#f8fafc;border-radius:10px;padding:16px">
              <div style="font-size:12px;color:#94a3b8;margin-bottom:4px">学员</div>
              <div style="font-size:17px;font-weight:600">{{ detail.userName }}</div>
              <div style="font-size:12px;color:#64748b;margin-top:4px">{{ detail.orgName }} · {{ detail.username }}</div>
            </div>
            <div style="flex:1;min-width:280px;background:#f8fafc;border-radius:10px;padding:16px">
              <div style="font-size:12px;color:#94a3b8;margin-bottom:4px">课程</div>
              <div style="font-size:15px;font-weight:600">{{ detail.courseName }}</div>
              <div style="font-size:12px;color:#64748b;margin-top:4px">{{ detail.projectName }}</div>
            </div>
            <div style="flex:1;min-width:240px;background:#f8fafc;border-radius:10px;padding:16px">
              <div style="font-size:12px;color:#94a3b8;margin-bottom:6px">学习进度</div>
              <a-progress :percent="Number(detail.progress || 0)" :stroke-color="detail.isAbnormal === 1 ? '#dc2626' : '#2563eb'" />
              <div style="font-size:12px;color:#64748b">有效时长 {{ formatDuration(detail.validDuration) }} · 核验 {{ detail.verifySuccess }}/{{ (detail.verifySuccess || 0) + (detail.verifyFail || 0) }} 通过</div>
            </div>
          </div>
          <a-alert v-if="detail.isAbnormal === 1" type="error" show-icon :message="`异常学习：${detail.abnormalReason || '触发防作弊规则'}`" style="margin-bottom:16px" />
          <a-tabs>
            <a-tab-pane key="heartbeat" :tab="`心跳记录 (${detail.heartbeats.length})`">
              <a-table :data-source="detail.heartbeats" :columns="heartbeatColumns" size="small" :pagination="{ pageSize: 8 }" row-key="id">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'fg'"><a-tag :color="record.inForeground === 1 ? 'green' : 'orange'">{{ record.inForeground === 1 ? '前台' : '后台' }}</a-tag></template>
                  <template v-else-if="column.key === 'switch'"><span :style="{ color: record.screenSwitch ? '#dc2626' : '#64748b' }">{{ record.screenSwitch ? '切屏' : '正常' }}</span></template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="verify" :tab="`核验记录 (${detail.verifyLogs.length})`">
              <a-table :data-source="detail.verifyLogs" :columns="verifyColumns" size="small" :pagination="{ pageSize: 8 }" row-key="id">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'result'">
                    <a-tag :color="record.verifyResult === 1 ? 'green' : record.verifyResult === 2 ? 'orange' : 'red'">{{ record.verifyResultLabel }}</a-tag>
                  </template>
                  <template v-else-if="column.key === 'image'">
                    <a-image v-if="record.imgUrl" :src="fileUrl(record.imgUrl)" :width="48" :height="36" style="object-fit:cover;border-radius:4px" />
                    <span v-else class="muted">无</span>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="credit" :tab="`学分记录 (${detail.creditRecords.length})`">
              <a-table :data-source="detail.creditRecords" :columns="creditColumns" size="small" :pagination="false" row-key="id">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'value'"><b style="color:#16a34a">+{{ record.creditValue }}</b></template>
                  <template v-else-if="column.key === 'status'">
                    <a-tag :color="record.auditStatus === 2 ? 'green' : record.auditStatus === 1 ? 'processing' : 'red'">
                      {{ record.auditStatus === 2 ? '已发放' : record.auditStatus === 1 ? '待审批' : record.auditStatus === 3 ? '已驳回' : '已撤销' }}
                    </a-tag>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
          </a-tabs>
        </template>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message, Modal, type TablePaginationConfig } from 'ant-design-vue'
import { courseApi, projectApi, studyApi } from '../../api'
import { fileUrl } from '../../api/http'
import type { CourseRow, CreditRow, ProjectRow, StudyRecordRow, VerifyLogRow } from '../../types'

interface HeartbeatRow { id: number; reportTime: string; deltaSeconds: number; progress: number; inForeground: number; screenSwitch: number; ip?: string }
interface StudyDetail extends StudyRecordRow { heartbeats: HeartbeatRow[]; verifyLogs: VerifyLogRow[]; creditRecords: CreditRow[] }

const route = useRoute()
const loading = ref(false)
const rows = ref<StudyRecordRow[]>([])
const projects = ref<ProjectRow[]>([])
const courses = ref<CourseRow[]>([])
const query = reactive<{ pageNum: number; pageSize: number; keyword: string; projectId?: number; courseId?: number; abnormal?: number; status?: number }>({
  pageNum: 1, pageSize: 10, keyword: '', courseId: route.query.courseId ? Number(route.query.courseId) : undefined
})
const pagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (n: number) => `共 ${n} 条学习记录` })
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<StudyDetail | null>(null)
const projectOptions = computed(() => projects.value.map((p) => ({ value: p.id, label: p.projectName })))
const courseOptions = computed(() => courses.value.filter((c) => !query.projectId || c.projectId === query.projectId).map((c) => ({ value: c.id, label: c.courseName })))

const columns = [
  { title: '学员', key: 'student', width: 170 },
  { title: '课程 / 培训班', key: 'course', width: 220 },
  { title: '学习进度', key: 'progress', width: 190 },
  { title: '核验情况', key: 'verify', width: 160 },
  { title: '状态', key: 'status', width: 150 },
  { title: '最近心跳', key: 'heartbeat', width: 150 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const, align: 'right' as const }
]
const heartbeatColumns = [
  { title: '上报时间', dataIndex: 'reportTime', width: 170 },
  { title: '增量(秒)', dataIndex: 'deltaSeconds', width: 100 },
  { title: '进度(%)', dataIndex: 'progress', width: 100 },
  { title: '前后台', key: 'fg', width: 90 },
  { title: '切屏', key: 'switch', width: 90 },
  { title: 'IP', dataIndex: 'ip' }
]
const verifyColumns = [
  { title: '核验时间', dataIndex: 'verifyTime', width: 170 },
  { title: '类型', dataIndex: 'verifyTypeLabel', width: 110 },
  { title: '触发点', dataIndex: 'triggerPoint', width: 100 },
  { title: '结果', key: 'result', width: 90 },
  { title: '相似度', dataIndex: 'similarity', width: 90 },
  { title: '抓拍', key: 'image', width: 70 }
]
const creditColumns = [
  { title: '学分', key: 'value', width: 90 },
  { title: '来源', dataIndex: 'sourceLabel', width: 110 },
  { title: '状态', key: 'status', width: 100 },
  { title: '时间', dataIndex: 'createTime' }
]

function formatDuration(seconds: number) {
  const s = Number(seconds || 0)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  return h > 0 ? `${h} 小时 ${m} 分` : `${m} 分钟`
}

async function loadMeta() {
  const [projectPage, coursePage] = await Promise.all([
    projectApi.page({ pageNum: 1, pageSize: 200 }),
    courseApi.page({ pageNum: 1, pageSize: 200 })
  ])
  projects.value = projectPage.records
  courses.value = coursePage.records
}
async function load() {
  loading.value = true
  try {
    const page = await studyApi.monitor({ ...query, keyword: query.keyword || undefined })
    rows.value = page.records
    pagination.current = page.pageNum
    pagination.total = page.total
  } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
function reload() { query.pageNum = 1; void load() }
function reset() { query.keyword = ''; query.projectId = undefined; query.courseId = undefined; query.abnormal = undefined; query.status = undefined; reload() }
function onTableChange(pag: TablePaginationConfig) { query.pageNum = Number(pag.current || 1); query.pageSize = Number(pag.pageSize || 10); void load() }

async function openDetail(id: number) {
  detailOpen.value = true
  detailLoading.value = true
  try {
    detail.value = await studyApi.detail(id) as unknown as StudyDetail
  } catch (e) { message.error((e as Error).message) } finally { detailLoading.value = false }
}
function unlock(record: StudyRecordRow) {
  Modal.confirm({
    title: '解除学习限制', content: `确定解除 ${record.userName} 在《${record.courseName}》的学习限制并清除异常标记？`,
    onOk: async () => {
      await studyApi.unlock(record.id, true)
      message.success('已解除限制')
      await load()
    }
  })
}

onMounted(async () => {
  try { await loadMeta() } catch (e) { message.error((e as Error).message) }
  await load()
})
</script>
