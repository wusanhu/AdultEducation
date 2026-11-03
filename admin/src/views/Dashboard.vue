<template>
  <div>
    <div class="hero-panel">
      <div class="hero-main">
        <div class="hero-greeting">{{ greeting }}，{{ auth.displayName }}</div>
        <div class="hero-meta">今天是 {{ today }} · 当前角色：{{ auth.roleName }} · {{ roleTip }}</div>
      </div>
      <div class="hero-actions">
        <a-button v-if="auth.hasPerm('project:list')" @click="$router.push('/project/list')">培训班管理</a-button>
        <a-button v-if="auth.hasPerm('course:list')" @click="$router.push('/course/list')">课程管理</a-button>
        <a-button v-if="auth.hasPerm('study:monitor')" @click="$router.push('/study/monitor')">学习监控</a-button>
        <a-button v-if="auth.hasPerm('verify:abnormal')" type="primary" @click="$router.push('/verify/abnormal')">异常核验 {{ overview.abnormalCount || 0 }}</a-button>
      </div>
    </div>

    <div class="stat-strip">
      <div v-for="card in stats" :key="card.label" class="stat-cell">
        <div class="stat-cell-head">
          <span class="label">{{ card.label }}</span>
          <component :is="card.icon" :style="{ color: card.color, fontSize: '18px' }" />
        </div>
        <div class="value">{{ card.value }}<small v-if="card.unit">{{ card.unit }}</small></div>
        <div class="stat-cell-hint">{{ card.hint }}</div>
      </div>
    </div>

    <a-row :gutter="[16, 16]" style="margin-top:16px">
      <a-col :xs="24" :lg="16">
        <div class="page-card" style="height:100%">
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px">
            <div>
              <div style="font-size:15px;font-weight:600">{{ trendTitle }}</div>
              <div style="font-size:12px;color:#94a3b8;margin-top:2px">学习分钟数与发放学分</div>
            </div>
            <div class="chart-legend">
              <span><i class="dot dot-blue"></i>学习分钟</span>
              <span><i class="dot dot-green"></i>发放学分</span>
              <a-tag color="blue">实时统计</a-tag>
            </div>
          </div>
          <div ref="chartEl" style="height:290px;width:100%"></div>
        </div>
      </a-col>
      <a-col :xs="24" :lg="8">
        <div class="page-card" style="height:100%">
          <div style="font-size:15px;font-weight:600;margin-bottom:16px">学习质量概览</div>
          <div style="text-align:center;padding:8px 0 18px">
            <a-progress type="dashboard" :percent="Number(overview.verifyPassRate || 0)" :size="148" :stroke-color="{ '0%': '#2563eb', '100%': '#22c55e' }">
              <template #format="percent"><span style="font-size:24px;font-weight:700">{{ percent }}</span><div style="font-size:11px;color:#94a3b8">核验通过率</div></template>
            </a-progress>
          </div>
          <div style="display:flex;justify-content:space-between;padding:10px 4px 0;border-top:1px solid #f1f5f9">
            <div style="text-align:center;flex:1">
              <div style="font-size:20px;font-weight:700;color:#0f172a">{{ overview.verifyTotal || 0 }}</div>
              <div style="font-size:12px;color:#94a3b8">核验次数</div>
            </div>
            <div style="width:1px;background:#f1f5f9"></div>
            <div style="text-align:center;flex:1">
              <div style="font-size:20px;font-weight:700;color:#dc2626">{{ overview.abnormalCount || 0 }}</div>
              <div style="font-size:12px;color:#94a3b8">异常学习</div>
            </div>
            <div style="width:1px;background:#f1f5f9"></div>
            <div style="text-align:center;flex:1">
              <div style="font-size:20px;font-weight:700;color:#16a34a">{{ overview.graduateCount || 0 }}</div>
              <div style="font-size:12px;color:#94a3b8">已毕业</div>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" style="margin-top:16px">
      <a-col :xs="24" :lg="14">
        <div class="page-card">
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px">
            <div>
              <div style="font-size:15px;font-weight:600">{{ isTeacher ? '我负责的课程' : '培训班学习进度' }}</div>
              <div style="font-size:12px;color:#94a3b8;margin-top:2px">{{ isTeacher ? '学习人数、完成人数与平均进度' : '按进行中优先展示，结业率 = 已毕业 / 学员数' }}</div>
            </div>
            <a-button v-if="isTeacher && auth.hasPerm('course:list')" type="link" size="small" @click="$router.push('/course/list')">课程管理</a-button>
            <a-button v-else-if="auth.hasPerm('project:list')" type="link" size="small" @click="$router.push('/project/list')">全部培训班</a-button>
          </div>
          <a-table v-if="isTeacher" :data-source="overview.myCourses || []" :columns="courseColumns" :pagination="false" size="small" row-key="id">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <a v-if="auth.hasPerm('course:edit')" @click="$router.push(`/course/edit/${record.id}`)">{{ record.courseName }}</a>
                <span v-else>{{ record.courseName }}</span>
                <div class="muted" style="font-size:12px">{{ record.projectName || '未归属培训班' }}</div>
              </template>
              <template v-else-if="column.key === 'progress'">
                <a-progress :percent="Number(record.avgProgress || 0)" size="small" />
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? '已上架' : '未上架' }}</a-tag>
              </template>
            </template>
          </a-table>
          <a-table v-else :data-source="overview.projectProgress || []" :columns="projectColumns" :pagination="false" size="small" row-key="id">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <a v-if="auth.hasPerm('project:list')" @click="$router.push(`/project/${record.id}`)">{{ record.projectName }}</a>
                <span v-else>{{ record.projectName }}</span>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="projectStatusColor(record.status)">{{ projectStatusText(record.status) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'gp'">{{ record.graduateCount }} / {{ record.memberCount }}</template>
              <template v-else-if="column.key === 'rate'">
                <a-progress :percent="Number(record.rate || 0)" size="small" />
              </template>
            </template>
          </a-table>
        </div>
      </a-col>
      <a-col :xs="24" :lg="10">
        <div class="page-card" style="height:100%">
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px">
            <div>
              <div style="font-size:15px;font-weight:600">{{ todoTitle }}</div>
              <div style="font-size:12px;color:#94a3b8;margin-top:2px">{{ todoTip }}</div>
            </div>
            <a-button v-if="todoLink" type="link" size="small" @click="$router.push(todoLink)">去处理</a-button>
          </div>
          <div v-if="todos.length === 0" style="padding:34px 0;text-align:center;color:#94a3b8">暂无待办事项</div>
          <div v-for="item in todos" :key="`${item.type}-${item.id}`" class="todo-row" @click="$router.push(item.link)">
            <a-tag :color="item.type === 'CREDIT' ? 'orange' : 'blue'">{{ item.type === 'CREDIT' ? '学分补录' : '毕业申请' }}</a-tag>
            <div class="todo-main">
              <div class="todo-title">{{ item.userName || '学员' }}<span class="muted"> · {{ item.projectName || '未关联培训班' }}</span></div>
              <div class="todo-sub">{{ item.node }} · {{ formatTime(item.time) }}</div>
            </div>
            <div v-if="item.type === 'CREDIT'" class="todo-value">+{{ item.value }} 分</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" style="margin-top:16px">
      <a-col :xs="24" :lg="14">
        <div class="page-card">
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px">
            <div style="font-size:15px;font-weight:600">最近毕业学员</div>
            <a-button v-if="auth.hasPerm('graduate:file') || auth.hasPerm('graduate:list')" type="link" size="small" @click="$router.push('/graduate/file')">查看档案</a-button>
          </div>
          <a-table :data-source="recentGraduates" :columns="graduateColumns" :pagination="false" size="small" row-key="certificateNo">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'user'">
                <span style="font-weight:500">{{ record.userName }}</span>
              </template>
              <template v-else-if="column.key === 'time'">
                <span class="muted">{{ record.graduateTime }}</span>
              </template>
              <template v-else-if="column.key === 'cert'">
                <a-typography-text copyable style="font-size:12px">{{ record.certificateNo }}</a-typography-text>
              </template>
            </template>
          </a-table>
        </div>
      </a-col>
      <a-col :xs="24" :lg="10">
        <div class="page-card">
          <div style="font-size:15px;font-weight:600;margin-bottom:12px">最近学分入账</div>
          <div v-if="recentCredits.length === 0" style="padding:30px 0;text-align:center;color:#94a3b8">暂无数据</div>
          <div v-for="(item, index) in recentCredits" :key="index" style="display:flex;align-items:center;gap:12px;padding:11px 0;border-bottom:1px dashed #f1f5f9">
            <a-avatar style="background:#e0f2fe;color:#0369a1;font-size:12px">{{ String(item.userName || '学').slice(0, 1) }}</a-avatar>
            <div style="flex:1;min-width:0">
              <div style="font-size:13px;font-weight:500">{{ item.userName }}</div>
              <div style="font-size:12px;color:#94a3b8">{{ item.sourceLabel }} · {{ item.auditTime }}</div>
            </div>
            <div style="color:#16a34a;font-weight:700">+{{ item.creditValue }}</div>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { CreditCardOutlined, ReadOutlined, TeamOutlined, FileDoneOutlined, ProjectOutlined } from '@ant-design/icons-vue'
import { Chart } from '@antv/g2'
import dayjs from 'dayjs'
import { dashboardApi } from '../api'
import { useAuthStore } from '../store/auth'
import type { OverviewData, DashboardTodo } from '../types'

const auth = useAuthStore()
const overview = ref<OverviewData>({} as OverviewData)
const chartEl = ref<HTMLDivElement>()
let chart: Chart | null = null
const today = dayjs().format('YYYY年MM月DD日')
const greeting = computed(() => {
  const hour = dayjs().hour()
  return hour < 11 ? '上午好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好'
})
const percent = computed(() => `${Number(overview.value.verifyPassRate || 0)}%`)

const role = computed(() => overview.value.role || (auth.roleCode === 'leader' ? 'leader' : auth.roleCode === 'teacher' ? 'teacher' : 'admin'))
const isLeader = computed(() => role.value === 'leader')
const isTeacher = computed(() => role.value === 'teacher')

const roleTip = computed(() => {
  if (isTeacher.value) return '关注我负责的课程与学员学习情况'
  if (isLeader.value) return '聚焦本单位培训进度与待复核事项'
  return '全局掌握平台学习、学分与结业情况'
})

const trendTitle = computed(() => {
  if (isTeacher.value) return '我的学员近 7 天学习趋势'
  if (isLeader.value) return '本单位近 7 天学习趋势'
  return '平台近 7 天学习趋势'
})

const todoTitle = computed(() => {
  if (isTeacher.value) return '待我初审'
  if (isLeader.value) return '待我复核'
  return '待办审批'
})
const todoTip = computed(() => {
  if (isTeacher.value) return '毕业申请教员初审'
  if (isLeader.value) return '学分补录复核 · 毕业申请复审'
  return '学分补录 · 毕业申请'
})

const stats = computed(() => {
  const o = overview.value
  const pending = (o.pendingCreditCount || 0) + (o.pendingGraduateCount || 0)
  if (isTeacher.value) {
    return [
      { label: '我的培训班', value: o.projectCount || 0, unit: '个', hint: `进行中 ${o.activeProjectCount || 0} 个`, icon: ProjectOutlined, color: '#2563eb' },
      { label: '我的课程', value: o.courseCount || 0, unit: '门', hint: `覆盖 ${o.activeProjectCount || 0} 个培训班`, icon: ReadOutlined, color: '#0e7490' },
      { label: '我的学员', value: o.studentCount || 0, unit: '人', hint: `学习完成 ${o.completedStudyCount || 0} 人次`, icon: TeamOutlined, color: '#16a34a' },
      { label: '待我初审', value: o.pendingGraduateCount || 0, unit: '项', hint: `累计核验 ${o.verifyTotal || 0} 次`, icon: FileDoneOutlined, color: '#d97706' }
    ]
  }
  if (isLeader.value) {
    return [
      { label: '本单位学员', value: o.studentCount || 0, unit: '人', hint: `已毕业 ${o.graduateCount || 0} 人`, icon: TeamOutlined, color: '#2563eb' },
      { label: '本单位培训', value: o.projectCount || 0, unit: '个', hint: `进行中 ${o.activeProjectCount || 0} 个`, icon: ProjectOutlined, color: '#0e7490' },
      { label: '本单位发放学分', value: o.creditTotal || 0, unit: '分', hint: `有效学时 ${o.validHours || 0} 小时`, icon: CreditCardOutlined, color: '#16a34a' },
      { label: '待我复核', value: pending, unit: '项', hint: `学分 ${o.pendingCreditCount || 0} · 毕业 ${o.pendingGraduateCount || 0}`, icon: FileDoneOutlined, color: '#d97706' }
    ]
  }
  return [
    { label: '培训班', value: o.projectCount || 0, unit: '个', hint: `进行中 ${o.activeProjectCount || 0} 个`, icon: ProjectOutlined, color: '#2563eb' },
    { label: '学员总数', value: o.studentCount || 0, unit: '人', hint: `已毕业 ${o.graduateCount || 0} 人`, icon: TeamOutlined, color: '#0e7490' },
    { label: '累计发放学分', value: o.creditTotal || 0, unit: '分', hint: `有效学时 ${o.validHours || 0} 小时`, icon: CreditCardOutlined, color: '#16a34a' },
    { label: '待办审批', value: pending, unit: '项', hint: `学分 ${o.pendingCreditCount || 0} · 毕业 ${o.pendingGraduateCount || 0}`, icon: FileDoneOutlined, color: '#d97706' }
  ]
})

const todos = computed<DashboardTodo[]>(() => {
  const o = overview.value
  return [...(o.pendingCreditList || []), ...(o.pendingGraduateList || [])]
    .sort((a, b) => (b.time || '').localeCompare(a.time || ''))
    .slice(0, 6)
})
const todoLink = computed(() => {
  if (todos.value.length > 0) return todos.value[0].link
  if (isTeacher.value) return auth.hasPerm('graduate:list') ? '/graduate/apply' : ''
  return auth.hasPerm('credit:audit') ? '/credit/audit' : ''
})

const recentGraduates = computed(() => overview.value.recentGraduate || [])
const recentCredits = computed(() => overview.value.recentCredits || [])
const graduateColumns = [
  { title: '学员', key: 'user', width: 90 },
  { title: '培训班', dataIndex: 'projectName', key: 'project', ellipsis: true },
  { title: '毕业时间', key: 'time', width: 120 },
  { title: '证书编号', key: 'cert', width: 160 }
]
const projectColumns = [
  { title: '培训班', key: 'name', ellipsis: true },
  { title: '状态', key: 'status', width: 82, align: 'center' as const },
  { title: '毕业 / 学员', key: 'gp', width: 100, align: 'center' as const },
  { title: '结业率', key: 'rate', width: 96 },
  { title: '发放学分', dataIndex: 'creditTotal', key: 'credit', width: 84, align: 'center' as const }
]
const courseColumns = [
  { title: '课程', key: 'name', ellipsis: true },
  { title: '学习人数', dataIndex: 'studentCount', key: 'students', width: 84, align: 'center' as const },
  { title: '完成人数', dataIndex: 'completedCount', key: 'completed', width: 84, align: 'center' as const },
  { title: '平均进度', key: 'progress', width: 108 },
  { title: '状态', key: 'status', width: 82, align: 'center' as const }
]

function projectStatusText(status?: number) {
  return ({ 0: '未开始', 1: '进行中', 2: '已结束', 3: '已归档' } as Record<number, string>)[status || 0] || '未知'
}
function projectStatusColor(status?: number) {
  return ({ 0: 'default', 1: 'green', 2: 'blue', 3: 'purple' } as Record<number, string>)[status || 0] || 'default'
}
function formatTime(value?: string) {
  return value ? dayjs(value).format('MM-DD HH:mm') : '-'
}

function renderChart() {
  if (!chartEl.value) return
  chart?.destroy()
  chart = new Chart({
    container: chartEl.value,
    autoFit: true
  })

  const trend = overview.value.trend || []
  const data = trend.map((t) => ({
    date: dayjs(t.date).format('MM-DD'),
    minutes: Number(t.studyMinutes || 0),
    credits: Number(t.credits || 0)
  }))

  chart.legend(false)
  chart.scale('x', { type: 'band', paddingInner: 0.55, paddingOuter: 0.25 })
  chart.axis('x', {
    title: false,
    labelFill: '#94a3b8',
    lineStroke: '#e2e8f0',
    tickStroke: '#e2e8f0'
  })

  // 学习分钟：左侧面积 + 折线
  chart
    .area()
    .data(data)
    .encode('x', 'date')
    .encode('y', 'minutes')
    .scale('y', { nice: true })
    .style('fill', 'rgba(37, 99, 235, 0.12)')
    .style('lineWidth', 0)
    .tooltip(false)

  chart
    .line()
    .data(data)
    .encode('x', 'date')
    .encode('y', 'minutes')
    .scale('y', { nice: true })
    .axis('y', {
      position: 'left',
      title: '分钟',
      titleFill: '#94a3b8',
      labelFill: '#94a3b8',
      lineStroke: '#e2e8f0',
      tickStroke: '#e2e8f0',
      gridStroke: '#f1f5f9'
    })
    .style('stroke', '#2563eb')
    .style('lineWidth', 3)
    .tooltip('minutes')

  // 发放学分：右侧独立坐标轴（最后绘制，避免被面积遮挡）
  chart
    .interval()
    .data(data)
    .encode('x', 'date')
    .encode('y', 'credits')
    .scale('y', { independent: true, nice: true })
    .axis('y', {
      position: 'right',
      title: '学分',
      titleFill: '#94a3b8',
      labelFill: '#94a3b8',
      lineStroke: '#e2e8f0',
      tickStroke: '#e2e8f0',
      grid: false
    })
    .style('fill', '#22c55e')
    .style('maxWidth', 22)
    .style('radiusTopLeft', 4)
    .style('radiusTopRight', 4)
    .tooltip('credits')

  chart.interaction('tooltip', { shared: true })
  chart.render()
}

function resize() {
  chart?.forceFit()
}

onMounted(async () => {
  try {
    overview.value = await dashboardApi.overview()
    renderChart()
    window.addEventListener('resize', resize)
  } catch (e) {
    message.error((e as Error).message)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.destroy()
  chart = null
})
</script>

<style scoped>
.hero-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 26px;
  margin-bottom: 16px;
  background: var(--admin-canvas-bg, #fff);
  border: 1px solid var(--admin-line, #eef2f7);
  border-left: 3px solid var(--admin-primary, #1f6feb);
  border-radius: var(--admin-radius-md, 12px);
}
.hero-main { min-width: 0; }
.hero-greeting {
  font-size: 21px;
  font-weight: 700;
  line-height: 1.3;
  color: #0f172a;
}
.hero-meta {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}
.hero-actions {
  flex: none;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.stat-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  background: var(--admin-canvas-bg, #fff);
  border: 1px solid var(--admin-line, #eef2f7);
  border-radius: var(--admin-radius-md, 12px);
}
.stat-cell { padding: 18px 22px; min-width: 0; }
.stat-cell + .stat-cell { border-left: 1px solid var(--admin-line, #eef2f7); }
.stat-cell-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.stat-cell .label { color: #64748b; font-size: 13px; }
.stat-cell .value {
  margin-top: 6px;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  color: #0f172a;
}
.stat-cell .value small {
  margin-left: 4px;
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
}
.stat-cell-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.todo-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 0;
  border-bottom: 1px dashed #f1f5f9;
  cursor: pointer;
}
.todo-row:last-child { border-bottom: none; }
.todo-row:hover .todo-title { color: var(--admin-primary, #1f6feb); }
.todo-main { flex: 1; min-width: 0; }
.todo-title {
  font-size: 13px;
  font-weight: 500;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.todo-sub { margin-top: 2px; font-size: 12px; color: #94a3b8; }
.todo-value { flex: none; font-size: 13px; font-weight: 700; color: #16a34a; }
@media (max-width: 1100px) {
  .stat-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .stat-cell:nth-child(3) { border-left: none; }
  .stat-cell:nth-child(n + 3) { border-top: 1px solid var(--admin-line, #eef2f7); }
}
@media (max-width: 720px) {
  .hero-panel { flex-direction: column; align-items: flex-start; }
  .stat-strip { grid-template-columns: minmax(0, 1fr); }
  .stat-cell + .stat-cell { border-left: none; border-top: 1px solid var(--admin-line, #eef2f7); }
}
.chart-legend {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: #64748b;
}
.chart-legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.chart-legend .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.dot-blue {
  background: #2563eb;
}
.dot-green {
  background: #22c55e;
}
</style>
