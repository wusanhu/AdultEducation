<template>
  <div class="page-container monitor-page">
    <van-tabs v-model:active="activeTab" sticky @change="onTabChange">
      <!-- Tab A 学习记录 -->
      <van-tab title="学习记录" name="study">
        <div class="search-bar">
          <van-search
            v-model="studyKeyword"
            placeholder="搜索学员姓名 / 账号"
            @search="reloadStudy"
            @clear="reloadStudy"
          />
        </div>

        <div class="filter-row">
          <div class="chip-group">
            <button
              v-for="opt in abnormalOptions"
              :key="'ab-' + opt.value"
              class="filter-chip"
              :class="{ 'filter-chip--on': studyAbnormal === opt.value }"
              @click="setStudyAbnormal(opt.value)"
            >{{ opt.text }}</button>
          </div>
          <div class="chip-group">
            <button
              v-for="opt in statusOptions"
              :key="'st-' + opt.value"
              class="filter-chip"
              :class="{ 'filter-chip--on': studyStatus === opt.value }"
              @click="setStudyStatus(opt.value)"
            >{{ opt.text }}</button>
          </div>
        </div>

        <div v-if="study.list.length > 0" class="list-count">
          <span class="kicker">学习记录</span>
          <span class="list-count-num">共 {{ studyTotal }} 条</span>
        </div>

        <van-pull-refresh v-model="studyRefreshing" @refresh="onRefreshStudy">
          <van-list
            v-model:loading="study.loading"
            :finished="study.finished"
            :finished-text="study.list.length ? '没有更多了' : ''"
            @load="loadStudy"
          >
            <div
              v-for="row in study.list"
              :key="row.id"
              class="card row-card"
              @click="goDetail(row.id)"
            >
              <div class="flex-between mb-8">
                <span class="row-title text-ellipsis">{{ row.userName || row.username || '学员' }}</span>
                <span class="chip chip--brand">{{ row.statusLabel || '学习中' }}</span>
              </div>

              <div class="row-course text-ellipsis">{{ row.courseName || '课程' }}</div>
              <div v-if="row.projectName" class="row-project text-ellipsis">{{ row.projectName }}</div>

              <div class="metric-grid mt-12">
                <div class="metric-cell">
                  <span class="metric-num">{{ fmtDuration(row.validDuration) }}</span>
                  <span class="metric-cap">有效时长</span>
                </div>
                <div class="metric-cell">
                  <span class="metric-num">{{ Math.round(Number(row.progress || 0)) }}%</span>
                  <span class="metric-cap">进度</span>
                </div>
                <div class="metric-cell">
                  <span class="metric-num">{{ row.screenSwitchCount || 0 }}</span>
                  <span class="metric-cap">切屏</span>
                </div>
                <div class="metric-cell">
                  <span class="metric-num" :class="{ 'is-bad': Number(row.verifyFail) > 0 }">{{ row.verifyFail || 0 }}</span>
                  <span class="metric-cap">核验失败</span>
                </div>
              </div>

              <hr class="hairline" />

              <div class="row-foot">
                <span
                  class="chip"
                  :class="Number(row.isAbnormal) ? 'chip--bad' : 'chip--ok'"
                >{{ Number(row.isAbnormal) ? '异常' : '正常' }}</span>
                <span class="foot-time">
                  <i v-if="row.online" class="live-dot" aria-hidden="true"></i>
                  {{ row.lastHeartbeat ? '心跳 ' + formatShort(row.lastHeartbeat) : '暂无心跳' }}
                </span>
              </div>
            </div>
          </van-list>
        </van-pull-refresh>

        <div v-if="study.finished && !study.loading && study.list.length === 0" class="empty-wrap">
          <EmptyState description="暂无学习记录" />
        </div>
      </van-tab>

      <!-- Tab B 考试成绩 -->
      <van-tab title="考试成绩" name="exam">
        <div v-if="exam.list.length > 0" class="list-count">
          <span class="kicker">考试成绩</span>
          <span class="list-count-num">共 {{ examTotal }} 条</span>
        </div>

        <van-list
          v-model:loading="exam.loading"
          :finished="exam.finished"
          :finished-text="exam.list.length ? '没有更多了' : ''"
          @load="loadExam"
        >
          <div v-for="row in exam.list" :key="row.id" class="card row-card exam-card">
            <div class="flex-between mb-8">
              <span class="row-title text-ellipsis">{{ row.userName || row.username || '学员' }}</span>
              <span
                class="chip"
                :class="Number(row.isPass) === 1 ? 'chip--ok' : 'chip--bad'"
              >{{ Number(row.isPass) === 1 ? '合格' : '不合格' }}</span>
            </div>

            <div class="row-course text-ellipsis">{{ row.courseName || '课程' }}</div>
            <div v-if="row.projectName" class="row-project text-ellipsis">{{ row.projectName }}</div>

            <div class="exam-score mt-12">
              <span class="exam-score-num">{{ formatScore(row.score) }}</span>
              <span class="exam-score-unit">分</span>
              <span class="exam-score-meta">第 {{ row.attemptNo || 1 }} 次作答</span>
            </div>

            <div class="row-foot mt-12">
              <span class="foot-time">交卷 {{ formatFull(row.submitTime) }}</span>
            </div>
          </div>
        </van-list>

        <div v-if="exam.finished && !exam.loading && exam.list.length === 0" class="empty-wrap">
          <EmptyState description="暂无考试成绩" />
        </div>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Tabs as VanTabs, Tab as VanTab, Search as VanSearch,
  List as VanList, PullRefresh as VanPullRefresh,
} from 'vant'
import EmptyState from '@/components/EmptyState.vue'
import { verify, exam as examApi } from '@/api'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const PAGE_SIZE = 10
// 首页宫格「考试成绩」以 /monitor?tab=exam 直达，其余一律落到学习记录
const activeTab = ref<any>(route.query.tab === 'exam' ? 'exam' : 'study')
const studyKeyword = ref('')
const studyAbnormal = ref(-1)
const studyStatus = ref(-1)
const studyRefreshing = ref(false)

const abnormalOptions = [
  { text: '全部', value: -1 },
  { text: '异常', value: 1 },
  { text: '正常', value: 0 },
]
const statusOptions = [
  { text: '全部状态', value: -1 },
  { text: '学习中', value: 1 },
  { text: '已完成', value: 2 },
]

const study = reactive({ list: [] as any[], loading: false, finished: false, pageNum: 1 })
const exam = reactive({ list: [] as any[], loading: false, finished: false, pageNum: 1 })
const studyTotal = ref(0)
const examTotal = ref(0)

function onTabChange(name: any) {
  activeTab.value = name
}

// 同一路由仅变化 query 时组件不会重新挂载，需手动跟随
watch(
  () => route.query.tab,
  (tab) => {
    if (route.path === '/monitor') activeTab.value = tab === 'exam' ? 'exam' : 'study'
  }
)

function fmtDuration(seconds: number) {
  const s = Math.max(0, Math.floor(Number(seconds || 0)))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  if (h > 0) return `${h}h ${m}m`
  if (m > 0) return `${m}m ${sec}s`
  return `${sec}s`
}
function formatShort(t: string) {
  return t ? dayjs(t).format('MM-DD HH:mm') : '-'
}
function formatFull(t: string) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
}
// 成绩为百分制小数，去掉多余的 0
function formatScore(v: any) {
  const n = Number(v || 0)
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}

async function loadStudy() {
  if (study.finished) return
  study.loading = true
  try {
    const params: any = { pageNum: study.pageNum, pageSize: PAGE_SIZE }
    if (studyKeyword.value) params.keyword = studyKeyword.value
    if (studyAbnormal.value >= 0) params.abnormal = studyAbnormal.value
    if (studyStatus.value >= 0) params.status = studyStatus.value
    const res: any = await verify.studyRecords(params)
    const records = res?.records || []
    study.list.push(...records)
    studyTotal.value = res?.total || 0
    if (study.list.length >= studyTotal.value || records.length < PAGE_SIZE) {
      study.finished = true
    }
    study.pageNum++
  } catch {
    study.finished = true
  } finally {
    study.loading = false
    studyRefreshing.value = false
  }
}

function resetStudy() {
  study.list = []
  study.pageNum = 1
  study.finished = false
  studyTotal.value = 0
}

function reloadStudy() {
  resetStudy()
  loadStudy()
}

async function onRefreshStudy() {
  resetStudy()
  await loadStudy()
}

function setStudyAbnormal(v: number) {
  if (studyAbnormal.value === v) return
  studyAbnormal.value = v
  reloadStudy()
}
function setStudyStatus(v: number) {
  if (studyStatus.value === v) return
  studyStatus.value = v
  reloadStudy()
}

async function loadExam() {
  if (exam.finished) return
  exam.loading = true
  try {
    const res: any = await examApi.page({ pageNum: exam.pageNum, pageSize: PAGE_SIZE })
    const records = res?.records || []
    exam.list.push(...records)
    examTotal.value = res?.total || 0
    if (exam.list.length >= examTotal.value || records.length < PAGE_SIZE) {
      exam.finished = true
    }
    exam.pageNum++
  } catch {
    exam.finished = true
  } finally {
    exam.loading = false
  }
}

function goDetail(id: number) {
  router.push(`/study-record/${id}`)
}
</script>

<style scoped>
.monitor-page :deep(.van-tabs__wrap) {
  border-bottom: 1px solid var(--line);
}
.search-bar {
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}
.search-bar :deep(.van-search__content) {
  border-radius: var(--r-control);
}

/* 移动端筛选：紧凑 chip 行，非桌面工具条 */
.filter-row {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
  padding: var(--sp-3) var(--page-x) var(--sp-2);
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}
.chip-group {
  display: flex;
  gap: var(--sp-2);
}
.filter-chip {
  flex: 1;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 var(--sp-2);
  font-size: 13px;
  line-height: 1.2;
  color: var(--ink-2);
  background: var(--bg);
  border: 1px solid var(--line);
  border-radius: var(--r-control);
  cursor: pointer;
}
.filter-chip--on {
  color: var(--brand);
  background: var(--brand-tint);
  border-color: var(--brand-line);
  font-weight: 600;
}

.list-count {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--sp-2);
  padding: var(--sp-4) var(--page-x) var(--sp-2);
}
.list-count-num {
  font-size: 12px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

.row-card {
  cursor: pointer;
  transition: opacity 0.15s;
}
.row-card:active {
  opacity: 0.7;
}
.row-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
  flex: 1;
  min-width: 0;
  margin-right: var(--sp-2);
}
.row-course {
  font-size: 13px;
  color: var(--ink-2);
}
.row-project {
  font-size: 12px;
  color: var(--ink-3);
  margin-top: 2px;
}

.metric-grid {
  display: flex;
  flex-wrap: wrap;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: var(--r-chip);
  padding: var(--sp-3) var(--sp-2);
}
.metric-cell {
  flex: 1 0 25%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.metric-num {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}
.metric-num.is-bad {
  color: var(--bad);
}
.metric-cap {
  font-size: 11px;
  color: var(--ink-3);
}

.row-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-2);
}
.foot-time {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-1);
  font-size: 12px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}
.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--ok);
}

/* 语义 chip：底色浅调 + 同系文字，无阴影 */
.chip {
  flex: none;
  padding: 3px 9px;
  font-size: 11px;
  font-weight: 500;
  border-radius: var(--r-chip);
  line-height: 1.4;
  white-space: nowrap;
}
.chip--brand {
  color: var(--brand);
  background: var(--brand-tint);
}
.chip--ok {
  color: var(--ok);
  background: var(--ok-tint);
}
.chip--bad {
  color: var(--bad);
  background: var(--bad-tint);
}

.exam-card {
  cursor: default;
}
.exam-score {
  display: flex;
  align-items: baseline;
  gap: var(--sp-2);
}
.exam-score-num {
  font-size: 26px;
  font-weight: 700;
  color: var(--brand);
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}
.exam-score-unit {
  font-size: 13px;
  color: var(--ink-2);
}
.exam-score-meta {
  margin-left: auto;
  font-size: 12px;
  color: var(--ink-3);
}

.empty-wrap {
  padding-top: var(--sp-5);
}
</style>
