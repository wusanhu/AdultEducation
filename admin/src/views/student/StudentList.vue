<template>
  <div class="page-card">
    <div class="toolbar">
      <a-input v-model:value="query.keyword" placeholder="姓名 / 账号 / 手机号" allow-clear style="width:190px" @press-enter="reload" />
      <a-select v-model:value="query.projectId" placeholder="培训班" allow-clear show-search option-filter-prop="label" style="width:220px" :options="projectOptions" />
      <a-select v-model:value="query.orgId" placeholder="单位 / 部门" allow-clear show-search option-filter-prop="label" style="width:200px" :options="orgOptions" />
      <a-select v-model:value="query.status" placeholder="状态" allow-clear style="width:110px">
        <a-select-option :value="1">正常</a-select-option>
        <a-select-option :value="0">已停用</a-select-option>
      </a-select>
      <a-button type="primary" @click="reload">查询</a-button>
      <a-button @click="reset">重置</a-button>
      <div class="spacer" />
      <span class="summary">共 {{ pagination.total }} 名学员 · 在读 {{ learningTotal }} 门课程</span>
    </div>

    <a-table :data-source="rows" :columns="columns" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1120 }" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'student'">
          <div style="font-weight:500">{{ record.realName }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.username }}</div>
        </template>
        <template v-else-if="column.key === 'org'">
          <div style="font-size:13px">{{ record.orgName || '-' }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.deptName || '' }}</div>
        </template>
        <template v-else-if="column.key === 'progress'">
          <div style="font-size:12px"><span style="color:#2563eb;font-weight:600">{{ record.learningCourses }}</span> 学习中 / <span style="color:#16a34a;font-weight:600">{{ record.completedCourses }}</span> 已完成</div>
        </template>
        <template v-else-if="column.key === 'credits'">
          <span style="font-size:15px;font-weight:700;color:#b3864b">{{ record.totalCredits ?? 0 }}</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? '正常' : '已停用' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'lastStudy'">
          <span style="font-size:12px;color:#64748b">{{ record.lastStudyTime || '暂未学习' }}</span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, type TablePaginationConfig } from 'ant-design-vue'
import { studentApi, projectApi, systemApi } from '../../api'
import type { OrgNode, ProjectRow, StudentRow } from '../../types'

const loading = ref(false)
const rows = ref<StudentRow[]>([])
const projects = ref<ProjectRow[]>([])
const orgs = ref<OrgNode[]>([])
const query = reactive<{ pageNum: number; pageSize: number; keyword: string; projectId?: number; orgId?: number; status?: number }>({ pageNum: 1, pageSize: 10, keyword: '' })
const pagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (n: number) => `共 ${n} 名学员` })
const learningTotal = computed(() => rows.value.reduce((sum, r) => sum + (r.learningCourses || 0), 0))

const projectOptions = computed(() => projects.value.map((p) => ({ value: p.id, label: p.projectName })))
const orgOptions = computed(() => {
  const list: { value: number; label: string }[] = []
  const walk = (nodes: OrgNode[], depth = 0) => {
    for (const node of nodes || []) {
      list.push({ value: node.id, label: `${'　'.repeat(depth)}${node.orgName}` })
      walk((node.children || []) as OrgNode[], depth + 1)
    }
  }
  walk(orgs.value)
  return list
})

const columns = [
  { title: '学员', key: 'student', width: 130, fixed: 'left' as const },
  { title: '单位 / 部门', key: 'org', width: 160 },
  { title: '手机号', dataIndex: 'phone', width: 130 },
  { title: '在读培训班', dataIndex: 'enrolledProjects', width: 100, align: 'center' as const },
  { title: '课程进度', key: 'progress', width: 140, align: 'center' as const },
  { title: '累计学分', key: 'credits', width: 100, align: 'center' as const },
  { title: '最近学习', key: 'lastStudy', width: 170 },
  { title: '状态', key: 'status', width: 90, align: 'center' as const },
  { title: '创建时间', dataIndex: 'createTime', width: 110 }
]

async function loadMeta() {
  const [projectsPage, orgTree] = await Promise.all([
    projectApi.page({ pageNum: 1, pageSize: 200 }),
    systemApi.orgTree()
  ])
  projects.value = projectsPage.records
  orgs.value = orgTree
}
async function load() {
  loading.value = true
  try {
    const page = await studentApi.page({ ...query, keyword: query.keyword || undefined })
    rows.value = page.records
    pagination.current = page.pageNum
    pagination.total = page.total
  } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
function reload() { query.pageNum = 1; void load() }
function reset() { query.keyword = ''; query.projectId = undefined; query.orgId = undefined; query.status = undefined; reload() }
function onTableChange(pag: TablePaginationConfig) { query.pageNum = Number(pag.current || 1); query.pageSize = Number(pag.pageSize || 10); void load() }

onMounted(async () => {
  try { await loadMeta() } catch (e) { message.error((e as Error).message) }
  await load()
})
</script>

<style scoped>
.summary { font-size: 12px; color: #64748b; }
</style>
