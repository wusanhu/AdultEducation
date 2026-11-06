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
        <template v-else-if="column.key === 'face'">
          <a-tag v-if="record.faceImg" color="blue">已设置</a-tag>
          <a-tag v-else color="warning">未设置</a-tag>
          <a @click="openFaceUpload(record)">上传</a>
        </template>
        <template v-else-if="column.key === 'lastStudy'">
          <span style="font-size:12px;color:#64748b">{{ record.lastStudyTime || '暂未学习' }}</span>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="faceModal" :title="`上传人脸基准照 - ${faceTarget?.realName || ''}`" :footer="null" :width="420">
      <div style="display:flex;flex-direction:column;align-items:center;gap:14px;padding:6px 0 2px">
        <img v-if="facePreview" :src="facePreview" alt="基准照" style="width:150px;height:150px;object-fit:cover;border-radius:12px;border:1px solid #e2e8f0" />
        <div v-else style="width:150px;height:150px;border-radius:12px;border:1px dashed #cbd5e1;display:flex;align-items:center;justify-content:center;color:#94a3b8;font-size:12px">
          暂无基准照
        </div>
        <div style="font-size:12px;color:#64748b;text-align:center;line-height:1.7">
          用于学员核验时与人脸采集照进行 1:1 比对，<br />请上传清晰的本人正脸免冠照（≤8MB）。
        </div>
        <a-upload :show-upload-list="false" :before-upload="onFaceFile" accept="image/*">
          <a-button type="primary" :loading="faceUploading">{{ facePreview ? '重新上传' : '选择图片上传' }}</a-button>
        </a-upload>
      </div>
    </a-modal>
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
  { title: '人脸基准照', key: 'face', width: 130, align: 'center' as const },
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

const faceModal = ref(false)
const faceUploading = ref(false)
const faceTarget = ref<StudentRow | null>(null)
const facePreview = ref('')

function openFaceUpload(record: StudentRow) {
  faceTarget.value = record
  facePreview.value = record.faceImg || ''
  faceModal.value = true
}
async function onFaceFile(file: File) {
  if (!faceTarget.value) return false
  if (!/^image\//.test(file.type)) { message.warning('请选择图片文件'); return false }
  if (file.size > 8 * 1024 * 1024) { message.warning('图片不能超过 8MB'); return false }
  faceUploading.value = true
  try {
    const res = await systemApi.upload(file)
    const url = (res as { url?: string }).url || (res as unknown as string)
    await studentApi.updateFaceImg(faceTarget.value.id, String(url))
    facePreview.value = String(url)
    message.success('基准照已保存')
    faceModal.value = false
    await load()
  } catch (e) { message.error((e as Error).message) } finally { faceUploading.value = false }
  return false
}

onMounted(async () => {
  try { await loadMeta() } catch (e) { message.error((e as Error).message) }
  await load()
})
</script>

<style scoped>
.summary { font-size: 12px; color: #64748b; }
</style>
