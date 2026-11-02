<template>
  <div>
    <div style="background:linear-gradient(120deg,#0f2d52,#155e9c 55%,#0e7490);border-radius:14px;padding:26px 28px;color:#fff;display:flex;align-items:center;justify-content:space-between;gap:20px;margin-bottom:18px;overflow:hidden;position:relative">
      <div style="position:absolute;right:-40px;top:-80px;width:260px;height:260px;border-radius:50%;background:rgba(255,255,255,.06)"></div>
      <div>
        <div style="font-size:22px;font-weight:700;margin-bottom:6px">{{ greeting }}，{{ auth.displayName }}</div>
        <div style="font-size:13px;opacity:.72">今天是 {{ today }} · 当前角色：{{ auth.roleName }} · 让每一份学习成果都清晰可追溯</div>
      </div>
      <div style="display:flex;gap:10px;position:relative">
        <a-button v-if="auth.hasPerm('project:list')" ghost @click="$router.push('/project/list')">培训班管理</a-button>
        <a-button v-if="auth.hasPerm('report:org')" ghost @click="$router.push('/report/org')">查看报表</a-button>
        <a-button v-if="auth.hasPerm('verify:abnormal')" type="primary" ghost @click="$router.push('/verify/abnormal')">异常核验 {{ overview.abnormalCount || 0 }}</a-button>
      </div>
    </div>

    <a-row :gutter="[16, 16]">
      <a-col :xs="12" :md="6" v-for="card in stats" :key="card.label">
        <div class="stat-card" style="height:100%">
          <div style="display:flex;align-items:center;justify-content:space-between">
            <span class="label">{{ card.label }}</span>
            <component :is="card.icon" :style="{ color: card.color, fontSize: '18px' }" />
          </div>
          <div class="value">{{ card.value }}<small v-if="card.unit">{{ card.unit }}</small></div>
          <div style="font-size:12px;color:#94a3b8;margin-top:6px">{{ card.hint }}</div>
        </div>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" style="margin-top:16px">
      <a-col :xs="24" :lg="16">
        <div class="page-card" style="height:100%">
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px">
            <div>
              <div style="font-size:15px;font-weight:600">近 7 天学习趋势</div>
              <div style="font-size:12px;color:#94a3b8;margin-top:2px">学习分钟数与发放学分</div>
            </div>
            <a-tag color="blue">实时统计</a-tag>
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
            <div style="font-size:15px;font-weight:600">最近毕业学员</div>
            <a-button type="link" size="small" @click="$router.push('/graduate/file')">查看档案</a-button>
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
import { CreditCardOutlined, ReadOutlined, TeamOutlined, FileDoneOutlined } from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { dashboardApi } from '../api'
import { useAuthStore } from '../store/auth'
import type { OverviewData } from '../types'

const auth = useAuthStore()
const overview = ref<OverviewData>({} as OverviewData)
const chartEl = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const today = dayjs().format('YYYY年MM月DD日')
const greeting = computed(() => {
  const hour = dayjs().hour()
  return hour < 11 ? '上午好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好'
})
const percent = computed(() => `${Number(overview.value.verifyPassRate || 0)}%`)
const stats = computed(() => [
  { label: '学员总数', value: overview.value.studentCount || 0, unit: '人', hint: `在办培训班 ${overview.value.activeProjectCount || 0} 个`, icon: TeamOutlined, color: '#2563eb' },
  { label: '课程学习', value: overview.value.studyCount || 0, unit: '人次', hint: `已完成 ${overview.value.completedStudyCount || 0} 人次`, icon: ReadOutlined, color: '#0e7490' },
  { label: '累计发放学分', value: overview.value.creditTotal || 0, unit: '分', hint: `累计有效学时 ${overview.value.validHours || 0} 小时`, icon: CreditCardOutlined, color: '#16a34a' },
  { label: '待办审批', value: (overview.value.pendingCreditCount || 0) + (overview.value.pendingGraduateCount || 0), unit: '项', hint: `学分补录 ${overview.value.pendingCreditCount || 0} · 毕业申请 ${overview.value.pendingGraduateCount || 0}`, icon: FileDoneOutlined, color: '#d97706' }
])
const recentGraduates = computed(() => overview.value.recentGraduate || [])
const recentCredits = computed(() => overview.value.recentCredits || [])
const graduateColumns = [
  { title: '学员', key: 'user', width: 100 },
  { title: '培训班', dataIndex: 'projectName', key: 'project', ellipsis: true },
  { title: '毕业时间', key: 'time', width: 150 },
  { title: '证书编号', key: 'cert', width: 180 }
]

function renderChart() {
  if (!chartEl.value) return
  chart = echarts.init(chartEl.value)
  const trend = overview.value.trend || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['学习分钟', '发放学分'], right: 0, top: 0, textStyle: { color: '#64748b', fontSize: 12 } },
    grid: { left: 8, right: 12, bottom: 8, top: 42, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: trend.map((t) => dayjs(t.date).format('MM-DD')), axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#94a3b8' } },
    yAxis: [
      { type: 'value', name: '分钟', splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#94a3b8' }, nameTextStyle: { color: '#94a3b8' } },
      { type: 'value', name: '学分', splitLine: { show: false }, axisLabel: { color: '#94a3b8' }, nameTextStyle: { color: '#94a3b8' } }
    ],
    series: [
      { name: '学习分钟', type: 'line', smooth: true, symbolSize: 5, data: trend.map((t) => t.studyMinutes || 0), lineStyle: { width: 3, color: '#2563eb' }, itemStyle: { color: '#2563eb' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(37,99,235,.28)' }, { offset: 1, color: 'rgba(37,99,235,.01)' }]) } },
      { name: '发放学分', type: 'bar', yAxisIndex: 1, barWidth: 12, data: trend.map((t) => t.credits || 0), itemStyle: { color: '#22c55e', borderRadius: [4, 4, 0, 0] } }
    ]
  })
}

function resize() { chart?.resize() }

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
  chart?.dispose()
})
</script>
