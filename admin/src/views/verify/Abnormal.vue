<template>
  <div class="page-card">
    <div class="toolbar">
      <a-input v-model:value="query.keyword" placeholder="学员姓名 / 账号" allow-clear style="width:200px" @press-enter="reload" />
      <a-select v-model:value="query.projectId" placeholder="培训班" allow-clear show-search option-filter-prop="label" style="width:210px" :options="projectOptions" />
      <a-select v-model:value="query.courseId" placeholder="课程" allow-clear show-search option-filter-prop="label" style="width:210px" :options="courseOptions" />
      <a-button type="primary" @click="reload">查询</a-button>
      <a-button @click="reset">重置</a-button>
      <div class="spacer" />
      <a-alert type="warning" show-icon message="达到防刷阈值的记录会自动进入异常清单，复核结论将写入审计日志。" style="padding:4px 12px" />
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
        <template v-else-if="column.key === 'abnormal'">
          <a-tag color="red">异常学习</a-tag>
          <div style="font-size:12px;color:#b91c1c;margin-top:4px;max-width:280px">{{ record.abnormalReason || '-' }}</div>
        </template>
        <template v-else-if="column.key === 'latest'">
          <template v-if="record.latestVerify">
            <a-tag :color="record.latestVerify.status === 3 ? 'orange' : 'red'">{{ record.latestVerify.status === 3 ? '核验超时' : record.latestVerify.verifyResultLabel || '核验失败' }}</a-tag>
            <div style="font-size:12px;color:#94a3b8;margin-top:3px">{{ record.latestVerify.verifyTime }}</div>
          </template>
          <span v-else class="muted" style="font-size:12px">防刷规则触发</span>
        </template>
        <template v-else-if="column.key === 'progress'">
          <a-progress :percent="Number(record.progress || 0)" size="small" status="exception" />
          <div style="font-size:12px;color:#94a3b8">切屏 {{ record.screenSwitchCount }} 次 / {{ record.screenSwitchDuration }} 秒</div>
        </template>
        <template v-else-if="column.key === 'locked'">
          <a-tag :color="record.locked ? 'red' : 'default'">{{ record.locked ? '学习已锁定' : '可继续学习' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a @click="openReview(record)">复核处理</a>
            <a v-if="record.latestVerify" @click="openDetail(record)">学习明细</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="reviewOpen" title="异常学习复核" :confirm-loading="reviewing" width="560" @ok="submitReview">
      <a-alert type="error" show-icon :message="current?.abnormalReason || '触发防刷规则'" style="margin-bottom:16px" />
      <a-form layout="vertical">
        <a-form-item label="处理结论" required>
          <a-radio-group v-model:value="reviewForm.action">
            <a-radio value="allow">允许本次学时有效并解除限制</a-radio>
            <a-radio value="invalidate">作废本次学时</a-radio>
            <a-radio value="violate">标记违规并撤销课程学分</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="处理说明"><a-textarea v-model:value="reviewForm.remark" :rows="3" placeholder="填写复核依据与处理说明" /></a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailOpen" title="学习明细" width="860">
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions :column="2" size="small" bordered style="margin-bottom:16px">
            <a-descriptions-item label="学员">{{ detail.userName }}</a-descriptions-item>
            <a-descriptions-item label="课程">{{ detail.courseName }}</a-descriptions-item>
            <a-descriptions-item label="有效时长">{{ formatDuration(detail.validDuration) }}</a-descriptions-item>
            <a-descriptions-item label="学习进度">{{ detail.progress }}%</a-descriptions-item>
            <a-descriptions-item label="核验通过/失败">{{ detail.verifySuccess }} / {{ detail.verifyFail }}</a-descriptions-item>
            <a-descriptions-item label="切屏次数">{{ detail.screenSwitchCount }} 次 / {{ detail.screenSwitchDuration }} 秒</a-descriptions-item>
          </a-descriptions>
          <div style="font-weight:600;margin-bottom:8px">核验记录</div>
          <a-table :data-source="detail.verifyLogs" size="small" :pagination="{ pageSize: 6 }" row-key="id" :columns="verifyColumns">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'result'"><a-tag :color="record.verifyResult === 1 ? 'green' : record.verifyResult === 2 ? 'orange' : 'red'">{{ record.verifyResultLabel }}</a-tag></template>
              <template v-else-if="column.key === 'image'"><a-image v-if="record.imgUrl" :src="record.imgUrl" :width="50" :height="36" style="object-fit:cover;border-radius:4px" /></template>
            </template>
          </a-table>
        </template>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, type TablePaginationConfig } from 'ant-design-vue'
import { courseApi, projectApi, studyApi, verifyApi } from '../../api'
import type { AbnormalRow, CourseRow, ProjectRow, StudyRecordRow, VerifyLogRow } from '../../types'

interface StudyDetail extends StudyRecordRow { verifyLogs: VerifyLogRow[] }

const loading = ref(false)
const rows = ref<AbnormalRow[]>([])
const projects = ref<ProjectRow[]>([])
const courses = ref<CourseRow[]>([])
const query = reactive<{ pageNum: number; pageSize: number; keyword: string; projectId?: number; courseId?: number }>({ pageNum: 1, pageSize: 10, keyword: '' })
const pagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (n: number) => `共 ${n} 条异常记录` })
const reviewOpen = ref(false)
const reviewing = ref(false)
const current = ref<AbnormalRow | null>(null)
const reviewForm = reactive({ action: 'allow', remark: '' })
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<StudyDetail | null>(null)
const projectOptions = computed(() => projects.value.map((p) => ({ value: p.id, label: p.projectName })))
const courseOptions = computed(() => courses.value.filter((c) => !query.projectId || c.projectId === query.projectId).map((c) => ({ value: c.id, label: c.courseName })))
const columns = [
  { title: '学员', key: 'student', width: 150 },
  { title: '课程 / 培训班', key: 'course', width: 190 },
  { title: '异常原因', key: 'abnormal', width: 300 },
  { title: '最近核验', key: 'latest', width: 170 },
  { title: '学习进度', key: 'progress', width: 180 },
  { title: '锁定状态', key: 'locked', width: 120 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const }
]
const verifyColumns = [
  { title: '时间', dataIndex: 'verifyTime', width: 165 },
  { title: '方式', dataIndex: 'verifyTypeLabel', width: 110 },
  { title: '结果', key: 'result', width: 90 },
  { title: '相似度', dataIndex: 'similarity', width: 90 },
  { title: '抓拍', key: 'image', width: 70 }
]

function formatDuration(seconds: number) {
  const s = Number(seconds || 0)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  return h > 0 ? `${h} 小时 ${m} 分` : `${m} 分钟`
}
async function loadMeta() {
  const [projectPage, coursePage] = await Promise.all([projectApi.page({ pageNum: 1, pageSize: 200 }), courseApi.page({ pageNum: 1, pageSize: 200 })])
  projects.value = projectPage.records
  courses.value = coursePage.records
}
async function load() {
  loading.value = true
  try {
    const page = await verifyApi.abnormal({ ...query, keyword: query.keyword || undefined })
    rows.value = page.records
    pagination.current = page.pageNum
    pagination.total = page.total
  } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
function reload() { query.pageNum = 1; void load() }
function reset() { query.keyword = ''; query.projectId = undefined; query.courseId = undefined; reload() }
function onTableChange(pag: TablePaginationConfig) { query.pageNum = Number(pag.current || 1); query.pageSize = Number(pag.pageSize || 10); void load() }

function openReview(row: AbnormalRow) {
  current.value = row
  reviewForm.action = 'allow'
  reviewForm.remark = ''
  reviewOpen.value = true
}
async function submitReview() {
  if (!current.value) return
  reviewing.value = true
  try {
    if (current.value.latestVerify) {
      await verifyApi.review(current.value.latestVerify.id, reviewForm.action, reviewForm.remark)
    } else if (reviewForm.action === 'allow') {
      await studyApi.unlock(current.value.id, true)
    } else {
      message.warning('该记录没有核验流水，仅支持解除学习限制')
      return
    }
    message.success('复核完成')
    reviewOpen.value = false
    await load()
  } catch (e) { message.error((e as Error).message) } finally { reviewing.value = false }
}
async function openDetail(row: AbnormalRow) {
  detailOpen.value = true
  detailLoading.value = true
  try { detail.value = await studyApi.detail(row.id) as unknown as StudyDetail } catch (e) { message.error((e as Error).message) } finally { detailLoading.value = false }
}

onMounted(async () => {
  try { await loadMeta() } catch (e) { message.error((e as Error).message) }
  await load()
})
</script>
