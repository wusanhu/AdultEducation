<template>
  <div class="page-card">
    <div class="toolbar">
      <a-input v-model:value="query.keyword" placeholder="课程名称" allow-clear style="width:200px" @press-enter="reload" />
      <a-select v-model:value="query.projectId" placeholder="培训班" allow-clear show-search option-filter-prop="label" style="width:230px" :options="projectOptions" />
      <a-select v-model:value="query.status" placeholder="状态" allow-clear style="width:120px">
        <a-select-option :value="1">已上架</a-select-option>
        <a-select-option :value="0">草稿</a-select-option>
        <a-select-option :value="2">已下架</a-select-option>
      </a-select>
      <a-select v-model:value="query.verifyGearCode" placeholder="核验档位" allow-clear style="width:150px" :options="gearOptions" />
      <a-button type="primary" @click="reload">查询</a-button>
      <a-button @click="reset">重置</a-button>
      <div class="spacer" />
      <a-button v-if="auth.hasPerm('course:add')" type="primary" @click="$router.push('/course/edit/new')"><PlusOutlined /> 新建课程</a-button>
    </div>

    <a-table :data-source="rows" :columns="columns" :loading="loading" :pagination="pagination" row-key="id" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'course'">
          <div style="display:flex;gap:12px;align-items:center">
            <img :src="record.cover || defaultCover" class="table-cover" alt="封面" />
            <div style="min-width:0">
              <a v-if="auth.hasPerm('course:edit') && !record.projectArchived" @click="$router.push(`/course/edit/${record.id}`)" style="font-weight:600">{{ record.courseName }}</a>
              <span v-else style="font-weight:600">{{ record.courseName }}</span>
              <div style="font-size:12px;color:#94a3b8;margin-top:3px">
                {{ record.isRequired === 1 ? '必修课' : '选修课' }} · {{ record.studyHour }} 学时
                <span v-if="record.needExam === 1"> · 需测验</span>
              </div>
            </div>
          </div>
        </template>
        <template v-else-if="column.key === 'project'">
          <div style="font-size:13px">
            {{ record.projectName }}
            <a-tag v-if="record.projectArchived" color="purple" style="margin-left:4px">已归档</a-tag>
          </div>
          <div style="font-size:12px;color:#94a3b8">授课：{{ record.createUserName || '-' }}</div>
        </template>
        <template v-else-if="column.key === 'credit'">
          <span style="font-weight:600;color:#2563eb">{{ record.credit }}</span> <span class="muted">学分</span>
        </template>
        <template v-else-if="column.key === 'gear'">
          <a-tag color="geekblue">{{ record.verifyGearName || '不核验' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'study'">
          <div style="font-size:13px">{{ record.studyCount }} 人学习</div>
          <a-progress :percent="Number(record.avgProgress || 0)" size="small" :show-info="false" style="width:110px" />
        </template>
        <template v-else-if="column.key === 'question'">
          <span :style="{ color: record.questionCount ? '#1e293b' : '#f59e0b' }">{{ record.questionCount }} 题</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : record.status === 0 ? 'default' : 'orange'">
            {{ record.status === 1 ? '已上架' : record.status === 0 ? '草稿' : '已下架' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a v-if="auth.hasPerm('course:edit') && !record.projectArchived" @click="$router.push(`/course/edit/${record.id}`)">编辑</a>
            <a v-if="auth.hasPerm('course:publish') && !record.projectArchived" @click="toggleStatus(record)">{{ record.status === 1 ? '下架' : '上架' }}</a>
            <a-dropdown v-if="auth.hasPerm('study:monitor') || auth.hasPerm('verify:list') || (auth.hasPerm('course:delete') && !record.projectArchived)">
              <a>更多 <DownOutlined /></a>
              <template #overlay>
                <a-menu @click="(info: any) => moreAction(String(info.key), record)">
                  <a-menu-item v-if="auth.hasPerm('study:monitor')" key="monitor"><MonitorOutlined /> 学习监控</a-menu-item>
                  <a-menu-item v-if="auth.hasPerm('verify:list')" key="verify"><CameraOutlined /> 核验记录</a-menu-item>
                  <a-menu-divider v-if="auth.hasPerm('course:delete') && !record.projectArchived" />
                  <a-menu-item v-if="auth.hasPerm('course:delete') && !record.projectArchived" key="delete" danger>删除课程</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <span v-if="record.projectArchived" class="muted" style="font-size:12px">仅可查看</span>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal, type TablePaginationConfig } from 'ant-design-vue'
import { CameraOutlined, DownOutlined, MonitorOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { courseApi, projectApi } from '../../api'
import { useAuthStore } from '../../store/auth'
import type { CourseRow, ProjectRow, VerifyGear } from '../../types'

const auth = useAuthStore()
const router = useRouter()
const defaultCover = 'data:image/svg+xml;charset=utf-8,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%2280%22 height=%2246%22%3E%3Crect width=%2280%22 height=%2246%22 fill=%22%23e2e8f0%22/%3E%3Ctext x=%2240%22 y=%2227%22 font-size=%2211%22 fill=%22%2394a3b8%22 text-anchor=%22middle%22%3E课程%3C/text%3E%3C/svg%3E'
const loading = ref(false)
const rows = ref<CourseRow[]>([])
const projects = ref<ProjectRow[]>([])
const gears = ref<VerifyGear[]>([])
const query = reactive<{ pageNum: number; pageSize: number; keyword: string; projectId?: number; status?: number; verifyGearCode?: number }>({
  pageNum: 1, pageSize: 10, keyword: ''
})
const pagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (n: number) => `共 ${n} 门课程` })

const projectOptions = computed(() => projects.value.map((p) => ({ value: p.id, label: p.projectName })))
const gearOptions = computed(() => gears.value.map((g) => ({ value: g.gearCode, label: g.gearName })))
const columns = [
  { title: '课程', key: 'course', width: 320 },
  { title: '培训班 / 授课', key: 'project', width: 210 },
  { title: '学分', key: 'credit', width: 90, align: 'center' as const },
  { title: '核验档位', key: 'gear', width: 120, align: 'center' as const },
  { title: '学习情况', key: 'study', width: 150 },
  { title: '题库', key: 'question', width: 80, align: 'center' as const },
  { title: '状态', key: 'status', width: 90, align: 'center' as const },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const, align: 'right' as const }
]

async function loadMeta() {
  const [projectPage, gearList] = await Promise.all([
    projectApi.page({ pageNum: 1, pageSize: 200 }),
    courseApi.gears()
  ])
  projects.value = projectPage.records
  gears.value = gearList
}
async function load() {
  loading.value = true
  try {
    const page = await courseApi.page({ ...query, keyword: query.keyword || undefined })
    rows.value = page.records
    pagination.current = page.pageNum
    pagination.total = page.total
  } catch (e) {
    message.error((e as Error).message)
  } finally {
    loading.value = false
  }
}
function reload() { query.pageNum = 1; void load() }
function reset() { query.keyword = ''; query.projectId = undefined; query.status = undefined; query.verifyGearCode = undefined; reload() }
function onTableChange(pag: TablePaginationConfig) { query.pageNum = Number(pag.current || 1); query.pageSize = Number(pag.pageSize || 10); void load() }

function toggleStatus(row: CourseRow) {
  const next = row.status === 1 ? 2 : 1
  Modal.confirm({
    title: next === 1 ? '上架课程' : '下架课程',
    content: next === 1 ? '上架后培训班学员将收到课程上新通知，确定上架？' : '下架后学员无法继续学习，已有学习记录保留。',
    onOk: async () => {
      await courseApi.status(row.id, next)
      message.success('操作成功')
      await load()
    }
  })
}
function moreAction(key: string, row: CourseRow) {
  if (key === 'monitor') router.push({ path: '/study/monitor', query: { courseId: row.id } })
  if (key === 'verify') router.push({ path: '/verify/record', query: { courseId: row.id } })
  if (key === 'delete') {
    Modal.confirm({
      title: '删除课程', content: `确定删除《${row.courseName}》吗？已有学习记录的课程无法删除。`, okType: 'danger',
      onOk: async () => { await courseApi.remove(row.id); message.success('已删除'); await load() }
    })
  }
}

onMounted(async () => {
  try {
    await loadMeta()
  } catch (e) {
    message.error((e as Error).message)
  }
  await load()
})
</script>
