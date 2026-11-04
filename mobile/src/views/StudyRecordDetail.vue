<template>
  <div class="page-container detail-page">
    <PageHeader title="学习详情" />

    <van-loading v-if="loading" class="loading-center" />

    <!-- 请求失败或记录不存在：优雅降级为占位，而非破损页面 -->
    <div v-else-if="!detail" class="empty-wrap">
      <EmptyState description="学习记录不存在或无权查看">
        <van-button type="primary" plain class="back-btn" @click="goBack">返回</van-button>
      </EmptyState>
    </div>

    <template v-else>
      <!-- 概要 -->
      <div class="card summary-card">
        <div class="flex-between mb-12">
          <span class="summary-name text-ellipsis">{{ detail.userName || detail.username || '学员' }}</span>
          <span
            class="chip"
            :class="Number(detail.isAbnormal) ? 'chip--bad' : 'chip--ok'"
          >{{ Number(detail.isAbnormal) ? '异常' : '正常' }}</span>
        </div>
        <div class="kv-list">
          <div class="kv-row">
            <span class="kv-label">学员</span>
            <span class="kv-value">{{ studentName }}<em v-if="studentPhone" class="kv-sub">{{ studentPhone }}</em></span>
          </div>
          <hr class="hairline kv-hr" />
          <div class="kv-row">
            <span class="kv-label">课程</span>
            <span class="kv-value text-ellipsis">{{ courseName }}<em v-if="courseCredit" class="kv-sub">{{ courseCredit }} 学分</em></span>
          </div>
          <hr class="hairline kv-hr" />
          <div class="kv-row">
            <span class="kv-label">培训班</span>
            <span class="kv-value text-ellipsis">{{ projectName }}</span>
          </div>
        </div>
      </div>

      <!-- 学习数据 -->
      <div class="section-title">学习数据</div>
      <div class="card">
        <div class="stat-block">
          <div class="stat-row">
            <span class="stat-value">{{ fmtDuration(detail.validDuration) }}</span>
            <span class="stat-label">有效时长</span>
          </div>
          <div class="stat-row">
            <span class="stat-value">{{ Math.round(Number(detail.progress || 0)) }}%</span>
            <span class="stat-label">学习进度</span>
          </div>
        </div>

        <div class="progress-line mt-12">
          <van-progress
            :percentage="Math.min(100, Math.round(Number(detail.progress || 0)))"
            :show-pivot="false"
            color="var(--brand)"
            track-color="var(--line)"
            stroke-width="5"
          />
        </div>

        <hr class="hairline" />

        <div class="kv-list">
          <div class="kv-row">
            <span class="kv-label">切屏次数</span>
            <span class="kv-value">{{ detail.screenSwitchCount || 0 }}</span>
          </div>
          <hr class="hairline kv-hr" />
          <div class="kv-row">
            <span class="kv-label">核验失败</span>
            <span class="kv-value">
              <span :class="{ 'is-bad': Number(detail.verifyFail) > 0 }">{{ detail.verifyFail || 0 }}</span>
              <em v-if="detail.verifySuccess != null || detail.verifyTotal != null" class="kv-sub">
                成功 {{ detail.verifySuccess || 0 }} / 共 {{ detail.verifyTotal || 0 }}
              </em>
            </span>
          </div>
          <hr class="hairline kv-hr" />
          <div class="kv-row">
            <span class="kv-label">学习状态</span>
            <span class="kv-value">{{ detail.statusLabel || '—' }}</span>
          </div>
          <hr class="hairline kv-hr" />
          <div class="kv-row">
            <span class="kv-label">最近心跳</span>
            <span class="kv-value">
              {{ detail.lastHeartbeat ? formatFull(detail.lastHeartbeat) : '—' }}
              <em v-if="detail.online" class="kv-sub is-live">在线</em>
            </span>
          </div>
        </div>
      </div>

      <!-- 核验记录 -->
      <template v-if="verifyLogs.length">
        <div class="section-title">核验记录</div>
        <div class="card log-list">
          <div v-for="(log, idx) in verifyLogs" :key="log.id">
            <div v-if="idx > 0" class="hairline kv-hr"></div>
            <div class="log-row">
              <div class="log-head">
                <span class="log-type">{{ log.verifyTypeLabel || '身份核验' }}</span>
                <span class="chip" :class="verifyChipClass(log.status)">{{ verifyStatusLabel(log) }}</span>
              </div>
              <div class="log-sub">
                <span>结果：{{ log.verifyResultLabel || '—' }}</span>
                <span>{{ formatFull(log.verifyTime) }}</span>
              </div>
              <p v-if="log.failReason" class="log-fail">{{ log.failReason }}</p>
            </div>
          </div>
        </div>
      </template>

      <!-- 学分记录 -->
      <template v-if="creditRecords.length">
        <div class="section-title">学分记录</div>
        <div class="card log-list">
          <div v-for="(c, idx) in creditRecords" :key="c.id">
            <div v-if="idx > 0" class="hairline kv-hr"></div>
            <div class="log-row">
              <div class="log-head">
                <span class="credit-val">+{{ c.creditValue }}</span>
                <StatusTag :status="c.auditStatus" type="credit" />
              </div>
              <div class="log-sub">
                <span>{{ creditSourceLabel(c.sourceType) }}</span>
                <span>{{ formatDate(c.createTime) }}</span>
              </div>
              <p v-if="c.remark" class="log-note">{{ c.remark }}</p>
            </div>
          </div>
        </div>
      </template>

      <!-- 心跳明细：诊断数据，视觉次要 -->
      <div v-if="heartbeats.length" class="hb-wrap">
        <van-collapse v-model="hbActive">
          <van-collapse-item :title="hbTitle" name="hb">
            <div class="hb-list">
              <div v-for="h in shownHeartbeats" :key="h.id" class="hb-item">
                <div class="hb-time">{{ formatFull(h.reportTime) }}</div>
                <div class="hb-metrics">
                  <span>进度 {{ Math.round(Number(h.progress || 0)) }}%</span>
                  <span>增量 +{{ Math.max(0, Math.round(Number(h.deltaSeconds || 0))) }}s</span>
                  <span :class="{ 'hb-flag': Number(h.inForeground) === 0 }">{{ Number(h.inForeground) === 1 ? '前台' : '后台' }}</span>
                  <span :class="{ 'hb-flag': Number(h.screenSwitch) === 1 }">{{ Number(h.screenSwitch) === 1 ? '切屏' : '正常' }}</span>
                </div>
              </div>
              <p v-if="heartbeats.length > shownHeartbeats.length" class="hb-more">仅展示最近 {{ shownHeartbeats.length }} 条</p>
            </div>
          </van-collapse-item>
        </van-collapse>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Loading as VanLoading, Button as VanButton, Progress as VanProgress,
  Collapse as VanCollapse, CollapseItem as VanCollapseItem,
} from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { verify } from '@/api'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const detail = ref<any>(null)
const hbActive = ref<string[]>([])

const HB_SHOW = 20

const verifyLogs = computed<any[]>(() => detail.value?.verifyLogs || [])
const creditRecords = computed<any[]>(() => detail.value?.creditRecords || [])
const heartbeats = computed<any[]>(() => detail.value?.heartbeats || [])
const shownHeartbeats = computed(() => heartbeats.value.slice(0, HB_SHOW))
const hbTitle = computed(
  () => `心跳明细 · 共 ${heartbeats.value.length} 条`
)

const studentName = computed(() => {
  const s = detail.value?.student
  return s?.realName || s?.username || detail.value?.userName || '—'
})
const studentPhone = computed(() => detail.value?.student?.phone || '')
const courseName = computed(() => detail.value?.course?.courseName || detail.value?.courseName || '—')
const courseCredit = computed(() => detail.value?.course?.credit ?? '')
const projectName = computed(() => detail.value?.project?.projectName || detail.value?.projectName || '—')

function fmtDuration(seconds: number) {
  const s = Math.max(0, Math.floor(Number(seconds || 0)))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  if (h > 0) return `${h}h ${m}m`
  if (m > 0) return `${m}m ${sec}s`
  return `${sec}s`
}
function formatFull(t: string) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '—'
}
function formatDate(t: string) {
  return t ? dayjs(t).format('YYYY-MM-DD') : '—'
}

// verifyLogs 的 statusLabel 由后端翻译，仅在缺失时回退到前端映射
function verifyStatusLabel(log: any) {
  if (log?.statusLabel) return log.statusLabel
  switch (Number(log?.status)) {
    case 0: return '核验中'
    case 1: return '已通过'
    case 2: return '核验未通过'
    case 3: return '核验超时'
    default: return '待核验'
  }
}
function verifyChipClass(status: number) {
  switch (Number(status)) {
    case 1: return 'chip--ok'
    case 2: return 'chip--bad'
    case 3: return 'chip--warn'
    case 4: return 'chip--brand'
    default: return 'chip--neutral'
  }
}
function creditSourceLabel(t: number) {
  switch (Number(t)) {
    case 1: return '课程自动发放'
    case 2: return '人工补录'
    default: return '学分记录'
  }
}

function goBack() {
  if (router.options.history.state.back) router.back()
  else router.push('/monitor')
}

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    const res = await verify.studyRecordDetail(id)
    detail.value = res || null
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail-page {
  padding-bottom: calc(var(--sp-8) + env(safe-area-inset-bottom));
}

.summary-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--ink);
  flex: 1;
  min-width: 0;
  margin-right: var(--sp-2);
}

.kv-list {
  display: flex;
  flex-direction: column;
}
.kv-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--sp-3);
  font-size: 14px;
  padding: var(--sp-1) 0;
}
.kv-label {
  flex: none;
  color: var(--ink-3);
}
.kv-value {
  color: var(--ink);
  text-align: right;
  min-width: 0;
  font-variant-numeric: tabular-nums;
}
.kv-sub {
  font-style: normal;
  font-size: 12px;
  color: var(--ink-3);
  margin-left: var(--sp-2);
}
.kv-sub.is-live {
  color: var(--ok);
}
/* hairline 作行间分隔：去掉其上下外边距，交由行高控制 */
.kv-hr {
  margin: 0;
}

.stat-block {
  display: flex;
  align-items: flex-end;
  gap: var(--sp-8);
}
.stat-row {
  display: flex;
  flex-direction: column;
  gap: var(--sp-1);
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.15;
  color: var(--ink);
  letter-spacing: -0.01em;
  font-variant-numeric: tabular-nums;
}
.stat-label {
  font-size: 12px;
  color: var(--ink-3);
}
.progress-line :deep(.van-progress) {
  margin: 0;
}
.is-bad {
  color: var(--bad);
  font-weight: 600;
}

.log-list {
  padding: var(--sp-2) var(--sp-4);
}
.log-row {
  padding: var(--sp-3) 0;
}
.log-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-2);
}
.log-type {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  flex: 1;
  min-width: 0;
}
.credit-val {
  font-size: 18px;
  font-weight: 700;
  color: var(--ok);
  font-variant-numeric: tabular-nums;
}
.log-sub {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-2);
  margin-top: 2px;
  font-size: 12px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}
.log-fail {
  margin-top: var(--sp-2);
  font-size: 12px;
  color: var(--bad);
  line-height: 1.5;
}
.log-note {
  margin-top: var(--sp-2);
  font-size: 12px;
  color: var(--ink-2);
  line-height: 1.5;
}

/* 心跳明细：次要诊断样式，小字 + surface-2 填充 */
.hb-wrap {
  margin: var(--sp-3) var(--page-x) 0;
  border: 1px solid var(--line);
  border-radius: var(--r-card);
  overflow: hidden;
}
.hb-wrap :deep(.van-cell) {
  background: var(--surface-2);
  color: var(--ink-2);
  min-height: 44px;
  align-items: center;
}
.hb-wrap :deep(.van-collapse-item__content) {
  background: var(--surface);
  padding: var(--sp-2) var(--sp-4) var(--sp-3);
}
.hb-list {
  display: flex;
  flex-direction: column;
}
.hb-item {
  padding: var(--sp-2) 0;
  border-bottom: 1px solid var(--line);
}
.hb-item:last-child {
  border-bottom: 0;
}
.hb-time {
  font-size: 12px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}
.hb-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-1) var(--sp-3);
  margin-top: 2px;
  font-size: 12px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}
.hb-metrics .hb-flag {
  color: var(--warn);
}
.hb-more {
  margin-top: var(--sp-2);
  font-size: 11px;
  color: var(--ink-4);
  text-align: center;
}

.chip {
  flex: none;
  padding: 3px 9px;
  font-size: 11px;
  font-weight: 500;
  border-radius: var(--r-chip);
  line-height: 1.4;
  white-space: nowrap;
}
.chip--ok { color: var(--ok); background: var(--ok-tint); }
.chip--bad { color: var(--bad); background: var(--bad-tint); }
.chip--warn { color: var(--warn); background: var(--warn-tint); }
.chip--brand { color: var(--brand); background: var(--brand-tint); }
.chip--neutral { color: var(--ink-2); background: var(--surface-2); }

.back-btn {
  display: block;
  margin: var(--sp-2) auto 0;
  min-width: 120px;
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: var(--sp-8);
}
.empty-wrap {
  padding-top: var(--sp-8);
}
</style>
