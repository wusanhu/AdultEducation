<template>
  <div>
    <div class="page-card" style="margin-bottom:16px">
      <div style="display:flex;align-items:flex-start;gap:16px;flex-wrap:wrap">
        <a-button shape="circle" @click="$router.push('/project/list')"><ArrowLeftOutlined /></a-button>
        <div style="flex:1;min-width:240px">
          <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
            <h2 class="page-title" style="margin:0">{{ project.projectName }}</h2>
            <a-tag :color="statusColor(project.status)">{{ statusText(project.status) }}</a-tag>
            <a-tag>{{ project.projectCode }}</a-tag>
          </div>
          <div style="color:#64748b;font-size:13px;margin-top:8px;line-height:1.7">{{ project.description || '暂无项目简介' }}</div>
          <div style="color:#94a3b8;font-size:12px;margin-top:8px">
            培训周期：{{ formatDate(project.startTime) }} 至 {{ formatDate(project.endTime) }} · 创建人：{{ project.createUserName || '-' }}
          </div>
        </div>
        <div style="display:flex;gap:26px;text-align:center">
          <div><div style="font-size:24px;font-weight:700">{{ project.memberCount || 0 }}</div><div style="font-size:12px;color:#94a3b8">学员</div></div>
          <div><div style="font-size:24px;font-weight:700">{{ project.courseCount || 0 }}</div><div style="font-size:12px;color:#94a3b8">课程</div></div>
          <div><div style="font-size:24px;font-weight:700;color:#2563eb">{{ project.needCredit }}</div><div style="font-size:12px;color:#94a3b8">毕业学分</div></div>
        </div>
      </div>
    </div>

    <div class="page-card">
      <a-alert v-if="archived" type="warning" show-icon message="该培训班已归档，所有关联内容仅可查看，不能新增、编辑或删除。" style="margin-bottom:16px" />
      <a-tabs v-model:activeKey="tab">
        <a-tab-pane key="members" tab="培训班学员">
          <div class="toolbar">
            <a-input v-model:value="memberQuery.keyword" placeholder="姓名 / 账号 / 手机号" allow-clear style="width:220px" @press-enter="loadMembers(true)" />
            <a-button type="primary" @click="loadMembers(true)">查询</a-button>
            <div class="spacer" />
            <a-button v-if="auth.hasPerm('project:member') && !archived" type="primary" @click="openAddMembers"><PlusOutlined /> 添加学员</a-button>
          </div>
          <a-table :data-source="members" :columns="memberColumns" :loading="memberLoading" :pagination="memberPagination" row-key="userId" @change="onMemberPage">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'student'">
                <div style="font-weight:500">{{ record.realName }}</div>
                <div style="font-size:12px;color:#94a3b8">{{ record.username }}</div>
              </template>
              <template v-else-if="column.key === 'progress'">
                <a-progress :percent="Number(record.progress || 0)" size="small" :stroke-color="progressColor(Number(record.progress || 0))" />
                <div style="font-size:12px;color:#94a3b8">{{ record.credit }} / {{ record.needCredit }} 学分 · {{ record.completedCourse }}/{{ record.courseTotal }} 课程</div>
              </template>
              <template v-else-if="column.key === 'graduate'">
                <a-tag v-if="record.graduateStatus === 1" color="green">已毕业</a-tag>
                <a-tag v-else-if="record.graduateStatus === 0" color="processing">{{ nodeText(record.graduateNode) }}</a-tag>
                <a-tag v-else-if="record.graduateStatus === 2" color="red">已驳回</a-tag>
                <span v-else class="muted">未申请</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-popconfirm v-if="auth.hasPerm('project:member') && !archived" title="确定将该学员移出培训班？" @confirm="removeMember(record.userId)">
                  <a style="color:#dc2626">移除</a>
                </a-popconfirm>
                <span v-else class="muted">-</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="teachers" tab="培训班教员">
          <div class="toolbar">
            <span class="muted">教员只能查看并管理自己创建的课程</span>
            <div class="spacer" />
            <a-button v-if="auth.hasPerm('project:teacher') && !archived" type="primary" @click="openAssignTeachers"><TeamOutlined /> 分配教员</a-button>
          </div>
          <a-table :data-source="teachers" :columns="teacherColumns" row-key="id" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'"><a-avatar style="background:#e0f2fe;color:#0369a1;margin-right:8px">{{ record.realName?.slice(0,1) }}</a-avatar>{{ record.realName }}</template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="courses" tab="项目课程">
          <a-table :data-source="project.courses || []" :columns="courseColumns" row-key="id" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <a v-if="auth.hasPerm('course:list')" @click="$router.push('/course/list')">{{ record.courseName }}</a>
                <span v-else>{{ record.courseName }}</span>
              </template>
              <template v-else-if="column.key === 'status'"><a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.statusName }}</a-tag></template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </div>

    <a-modal v-model:open="addOpen" title="添加培训班学员" :confirm-loading="adding" :width="520" @ok="addMembers">
      <a-alert type="info" show-icon message="可多选学员，已在本项目中的学员将自动忽略。" style="margin-bottom:14px" />
      <a-select v-model:value="selectedUserIds" mode="multiple" show-search option-filter-prop="label" placeholder="搜索姓名、账号或手机号" style="width:100%" :options="studentSelectOptions" :max-tag-count="6" />
    </a-modal>

    <a-modal v-model:open="teacherOpen" title="分配培训班教员" :confirm-loading="assigning" :width="520" @ok="assignTeachers">
      <a-select v-model:value="selectedTeacherIds" mode="multiple" show-search option-filter-prop="label" placeholder="选择教员" style="width:100%" :options="teacherSelectOptions" />
      <div class="muted" style="font-size:12px;margin-top:10px">重新分配将覆盖当前教员列表。</div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, PlusOutlined, TeamOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { projectApi } from '../../api'
import { useAuthStore } from '../../store/auth'
import type { Option } from '../../api'
import type { ProjectMemberRow, ProjectRow } from '../../types'

interface ProjectDetail extends ProjectRow {
  courses: { id: number; courseName: string; credit: number; status: number; statusName: string }[]
  members: { userId: number; realName: string; phone?: string; orgName?: string; credit: number }[]
}

const auth = useAuthStore()
const route = useRoute()
const projectId = Number(route.params.id)
const project = ref<ProjectDetail>({} as ProjectDetail)
const archived = computed(() => project.value.status === 3)
const tab = ref('members')
const members = ref<ProjectMemberRow[]>([])
const teachers = ref<{ id: number; realName: string; phone?: string; orgName?: string; courseCount?: number }[]>([])
const memberLoading = ref(false)
const memberQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const memberPagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (n: number) => `共 ${n} 名学员` })
const addOpen = ref(false)
const adding = ref(false)
const studentOptions = ref<Option[]>([])
const selectedUserIds = ref<number[]>([])
const teacherOpen = ref(false)
const assigning = ref(false)
const teacherOptions = ref<Option[]>([])
const selectedTeacherIds = ref<number[]>([])

const memberColumns = [
  { title: '学员', key: 'student', width: 160 },
  { title: '所属单位', dataIndex: 'orgName', key: 'org', width: 190 },
  { title: '学习进度', key: 'progress', width: 280 },
  { title: '毕业状态', key: 'graduate', width: 150, align: 'center' as const },
  { title: '加入时间', dataIndex: 'joinTime', key: 'join', width: 160 },
  { title: '操作', key: 'action', width: 80, align: 'right' as const }
]
const teacherColumns = [
  { title: '教员', key: 'name', width: 200 },
  { title: '联系电话', dataIndex: 'phone', key: 'phone', width: 150 },
  { title: '所属单位', dataIndex: 'orgName', key: 'org' },
  { title: '负责课程数', dataIndex: 'courseCount', key: 'courseCount', width: 120 }
]
const courseColumns = [
  { title: '课程名称', key: 'name' },
  { title: '学分', dataIndex: 'credit', key: 'credit', width: 100, align: 'center' as const },
  { title: '状态', key: 'status', width: 110, align: 'center' as const }
]

const studentSelectOptions = computed(() => studentOptions.value.map((s) => ({
  value: s.id,
  label: `${String(s.realName || s.label || '')}（${String(s.username || '')} · ${String(s.phone || '无手机号')}）`
})))
const teacherSelectOptions = computed(() => teacherOptions.value.map((t) => ({
  value: t.id,
  label: `${String(t.realName || t.label || '')}（${String(t.username || '')} · ${String(t.orgName || '')}）`
})))

function formatDate(value?: string) { return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-' }
function statusText(status: number) { return ({ 0: '未开始', 1: '进行中', 2: '已结束', 3: '已归档' } as Record<number, string>)[status] || '未知' }
function statusColor(status: number) { return ({ 0: 'default', 1: 'processing', 2: 'orange', 3: 'purple' } as Record<number, string>)[status] || 'default' }
function nodeText(node?: string) { return ({ TEACHER: '教员初审中', LEADER: '领导复审中', ADMIN: '管理员终审中', DONE: '已通过', REJECTED: '已驳回' } as Record<string, string>)[node || ''] || '审批中' }
function progressColor(percent: number) { return percent >= 100 ? '#16a34a' : percent >= 60 ? '#2563eb' : '#f59e0b' }

async function loadProject() {
  project.value = await projectApi.detail(projectId) as unknown as ProjectDetail
}
async function loadMembers(reset = false) {
  if (reset) memberQuery.pageNum = 1
  memberLoading.value = true
  try {
    const page = await projectApi.members(projectId, { ...memberQuery, keyword: memberQuery.keyword || undefined })
    members.value = page.records
    memberPagination.current = page.pageNum
    memberPagination.total = page.total
  } catch (e) {
    message.error((e as Error).message)
  } finally {
    memberLoading.value = false
  }
}
function onMemberPage(pag: { current?: number; pageSize?: number }) {
  memberQuery.pageNum = Number(pag.current || 1)
  memberQuery.pageSize = Number(pag.pageSize || 10)
  void loadMembers()
}
async function loadTeachers() { teachers.value = await projectApi.teachers(projectId) }

async function openAddMembers() {
  try {
    studentOptions.value = await projectApi.studentOptions()
    selectedUserIds.value = []
    addOpen.value = true
  } catch (e) {
    message.error((e as Error).message)
  }
}
async function addMembers() {
  if (!selectedUserIds.value.length) { message.warning('请选择学员'); return }
  adding.value = true
  try {
    const result = await projectApi.addMembers(projectId, selectedUserIds.value)
    message.success(`已添加 ${result.added} 名学员`)
    addOpen.value = false
    await Promise.all([loadProject(), loadMembers()])
  } catch (e) {
    message.error((e as Error).message)
  } finally {
    adding.value = false
  }
}
async function removeMember(userId: number) {
  try {
    await projectApi.removeMember(projectId, userId)
    message.success('已移除')
    await Promise.all([loadProject(), loadMembers()])
  } catch (e) {
    message.error((e as Error).message)
  }
}
async function openAssignTeachers() {
  try {
    teacherOptions.value = await projectApi.teacherOptions()
    selectedTeacherIds.value = teachers.value.map((t) => t.id)
    teacherOpen.value = true
  } catch (e) {
    message.error((e as Error).message)
  }
}
async function assignTeachers() {
  assigning.value = true
  try {
    await projectApi.assignTeachers(projectId, selectedTeacherIds.value)
    message.success('教员分配已更新')
    teacherOpen.value = false
    await Promise.all([loadTeachers(), loadProject()])
  } catch (e) {
    message.error((e as Error).message)
  } finally {
    assigning.value = false
  }
}

onMounted(async () => {
  try {
    await loadProject()
    await Promise.all([loadMembers(), loadTeachers()])
  } catch (e) {
    message.error((e as Error).message)
  }
})
</script>
