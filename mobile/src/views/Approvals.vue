<template>
  <div class="page-container">
    <template v-if="visibleTabs.length">
      <van-tabs v-model:active="activeTab" class="approval-tabs" sticky>
        <van-tab v-for="t in visibleTabs" :key="t.key" :title="t.label" :name="t.key" />
      </van-tabs>

      <!-- 学分审核：/credits/pending 返回平铺数组，无分页，故不使用 van-list -->
      <van-pull-refresh
        v-if="activeTab === 'credit'"
        v-model="credit.refreshing"
        @refresh="loadCredit"
      >
        <div v-if="credit.list.length" class="list-count">
          <span class="kicker">学分补录待审</span>
          <span class="list-count-num">{{ credit.list.length }} 条</span>
        </div>

        <div v-for="row in credit.list" :key="row.id" class="card audit-card">
          <div class="flex-between mb-8">
            <span class="row-title text-ellipsis">{{ row.userName || row.username || '学员' }}</span>
            <StatusTag v-if="row.auditStatus != null" :status="row.auditStatus" type="credit" />
          </div>
          <div class="row-sub text-ellipsis">{{ row.projectName || '—' }}</div>

          <div class="tag-row mt-8">
            <span v-if="row.sourceLabel" class="chip chip-ink">{{ row.sourceLabel }}</span>
            <span v-if="row.courseName" class="chip chip-ink text-ellipsis">{{ row.courseName }}</span>
          </div>

          <div class="strip mt-12">
            <div class="strip-cell">
              <span class="strip-label">补录学分</span>
              <span class="strip-num">+{{ toNum(row.creditValue) }}</span>
            </div>
            <div class="strip-cell strip-cell--end">
              <span class="strip-label">申请时间</span>
              <span class="strip-time">{{ formatTime(row.createTime) }}</span>
            </div>
          </div>

          <div v-if="row.remark" class="reason-box mt-8">
            <span class="reason-label">补录原因</span>
            <span class="reason-plain">{{ row.remark }}</span>
          </div>

          <div class="row-actions">
            <van-button
              size="small"
              type="success"
              plain
              class="btn-pass"
              :loading="creditBusy === row.id"
              @click.stop="approveCredit(row)"
            >
              通过
            </van-button>
            <van-button
              size="small"
              type="danger"
              plain
              class="btn-reject"
              @click.stop="openCreditReject(row)"
            >
              驳回
            </van-button>
          </div>
        </div>

        <van-loading v-if="credit.loading && !credit.list.length" class="loading-center" />
        <div v-else-if="!credit.list.length && !credit.loading" class="empty-wrap">
          <EmptyState description="暂无待审核的学分申请" />
        </div>
      </van-pull-refresh>

      <van-pull-refresh
        v-else-if="activeTab === 'graduate'"
        v-model="grad.refreshing"
        @refresh="grad.refresh"
      >
        <van-list
          v-model:loading="grad.loading"
          :finished="grad.finished"
          :finished-text="grad.list.length ? '没有更多了' : ''"
          @load="grad.loadMore"
        >
          <div v-if="grad.list.length" class="list-count">
            <span class="kicker">待审批毕业申请</span>
            <span class="list-count-num">{{ grad.total }} 条</span>
          </div>

          <div
            v-for="row in grad.list"
            :key="row.id"
            class="card audit-card audit-card--tap"
            @click="toggleExpand(row.id)"
          >
            <div class="flex-between mb-8">
              <span class="row-title text-ellipsis">{{ row.userName || row.username || '学员' }}</span>
              <span class="expand-hint">{{ expandedId === row.id ? '收起' : '详情' }}</span>
            </div>
            <div class="row-sub text-ellipsis">{{ row.projectName || '—' }}</div>

            <!-- 节点与状态文案由后端翻译，前端原样展示 -->
            <div class="tag-row mt-8">
              <span v-if="row.currentNodeLabel" class="chip chip-brand">{{ row.currentNodeLabel }}</span>
              <span v-if="row.auditStatusLabel" class="chip chip-warn">{{ row.auditStatusLabel }}</span>
              <span class="row-time">{{ formatTime(row.applyTime) }}</span>
            </div>

            <div v-if="expandedId === row.id" class="detail-panel">
              <div class="kv-row">
                <span class="kv-label">学员单位</span>
                <span class="kv-value">{{ row.orgName || '—' }}</span>
              </div>
              <hr class="kv-sep" />
              <div class="kv-row">
                <span class="kv-label">所需学分</span>
                <span class="kv-value">{{ toNum(row.needCredit) }}</span>
              </div>
              <hr class="kv-sep" />
              <div class="kv-row">
                <span class="kv-label">学分快照</span>
                <span class="kv-value">{{ toNum(row.creditSnapshot) }}</span>
              </div>
              <hr class="kv-sep" />
              <div class="kv-row">
                <span class="kv-label">学时快照</span>
                <span class="kv-value">{{ toNum(row.studyHourSnapshot) }}</span>
              </div>
              <hr class="kv-sep" />
              <div class="kv-row">
                <span class="kv-label">申请时间</span>
                <span class="kv-value">{{ formatFullTime(row.applyTime) }}</span>
              </div>
              <p v-if="row.projectArchived" class="archive-note">该培训班已归档，仅可查看，不能审批</p>
            </div>

            <div class="row-actions">
              <van-button
                size="small"
                type="success"
                plain
                class="btn-pass"
                :loading="gradBusy === row.id"
                @click.stop="approveGraduate(row)"
              >
                通过
              </van-button>
              <van-button
                size="small"
                type="danger"
                plain
                class="btn-reject"
                @click.stop="openGraduateReject(row)"
              >
                驳回
              </van-button>
            </div>
          </div>

          <div v-if="!grad.list.length && !grad.loading" class="empty-wrap">
            <EmptyState description="暂无待审批的毕业申请" />
          </div>
        </van-list>
      </van-pull-refresh>

      <van-pull-refresh
        v-else
        v-model="abnormal.refreshing"
        @refresh="abnormal.refresh"
      >
        <van-list
          v-model:loading="abnormal.loading"
          :finished="abnormal.finished"
          :finished-text="abnormal.list.length ? '没有更多了' : ''"
          @load="abnormal.loadMore"
        >
          <div v-if="abnormal.list.length" class="list-count">
            <span class="kicker">核验异常记录</span>
            <span class="list-count-num">{{ abnormal.total }} 条</span>
          </div>

          <div v-for="row in abnormal.list" :key="row.id" class="card audit-card">
            <div class="flex-between mb-8">
              <span class="row-title text-ellipsis">{{ row.userName || row.username || '学员' }}</span>
              <span v-if="row.locked" class="chip chip-bad">已锁定</span>
            </div>
            <div class="row-sub text-ellipsis">
              {{ row.courseName || '课程' }} · {{ row.projectName || '—' }}
            </div>

            <div class="reason-box mt-8">
              <span class="reason-label">异常原因</span>
              <span class="reason-text">{{ row.abnormalReason || '学习过程异常' }}</span>
            </div>

            <template v-if="row.latestVerify">
              <div class="tag-row mt-8">
                <span class="chip" :class="verifyStatusClass(row.latestVerify.status)">
                  {{ verifyStatusLabel(row.latestVerify.status) }}
                </span>
                <span v-if="row.verifyFail" class="chip chip-ink">核验失败 {{ row.verifyFail }} 次</span>
              </div>
              <div class="kv-row mt-8">
                <span class="kv-label">最近核验</span>
                <span class="kv-value">{{ formatFullTime(row.latestVerify.verifyTime) }}</span>
              </div>
              <div v-if="row.latestVerify.failReason" class="kv-row mt-4">
                <span class="kv-label">失败原因</span>
                <span class="kv-value">{{ row.latestVerify.failReason }}</span>
              </div>
              <div class="row-actions">
                <van-button
                  size="small"
                  plain
                  class="btn-review"
                  @click.stop="openReview(row)"
                >
                  复核处理
                </van-button>
              </div>
            </template>
            <div v-else class="no-verify mt-12">暂无核验记录</div>
          </div>

          <div v-if="!abnormal.list.length && !abnormal.loading" class="empty-wrap">
            <EmptyState description="暂无异常核验记录" />
          </div>
        </van-list>
      </van-pull-refresh>
    </template>

    <div v-else class="empty-wrap">
      <EmptyState description="暂无可用的审批入口" />
    </div>

    <van-popup v-model:show="creditSheet.show" position="bottom" round safe-area-inset-bottom>
      <div class="sheet">
        <div class="sheet-head">
          <span class="sheet-title">驳回学分补录</span>
          <span class="sheet-close" @click="creditSheet.show = false">关闭</span>
        </div>
        <div class="sheet-body">
          <p class="sheet-context">
            {{ creditSheet.row?.userName || '学员' }} · {{ creditSheet.row?.projectName || '—' }}
            （+{{ toNum(creditSheet.row?.creditValue) }} 学分）
          </p>
          <div class="sheet-field">
            <van-field
              v-model="creditSheet.remark"
              type="textarea"
              rows="3"
              maxlength="200"
              show-word-limit
              placeholder="请填写驳回原因，将同步给学员"
            />
          </div>
        </div>
        <div class="sheet-foot">
          <van-button
            block
            round
            type="danger"
            class="btn-reject-solid"
            :loading="creditSheet.submitting"
            @click="submitCreditReject"
          >
            确认驳回
          </van-button>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="gradSheet.show" position="bottom" round safe-area-inset-bottom>
      <div class="sheet">
        <div class="sheet-head">
          <span class="sheet-title">驳回毕业申请</span>
          <span class="sheet-close" @click="gradSheet.show = false">关闭</span>
        </div>
        <div class="sheet-body">
          <p class="sheet-context">
            {{ gradSheet.row?.userName || '学员' }} · {{ gradSheet.row?.projectName || '—' }}
            <span v-if="gradSheet.row?.currentNodeLabel">（{{ gradSheet.row.currentNodeLabel }}）</span>
          </p>
          <div class="sheet-field">
            <van-field
              v-model="gradSheet.remark"
              type="textarea"
              rows="3"
              maxlength="200"
              show-word-limit
              placeholder="请填写驳回原因，必填且将同步给学员"
            />
          </div>
        </div>
        <div class="sheet-foot">
          <van-button
            block
            round
            type="danger"
            class="btn-reject-solid"
            :loading="gradSheet.submitting"
            @click="submitGraduateReject"
          >
            确认驳回
          </van-button>
        </div>
      </div>
    </van-popup>

    <van-action-sheet
      v-model:show="actionSheet.show"
      :actions="reviewActions"
      title="异常复核"
      description="复核结论将同步给学员并影响其学时与学分"
      cancel-text="取消"
      close-on-click-action
      safe-area-inset-bottom
      @select="onReviewSelect"
    />

    <van-popup v-model:show="remarkSheet.show" position="bottom" round safe-area-inset-bottom>
      <div class="sheet">
        <div class="sheet-head">
          <span class="sheet-title">{{ remarkSheet.actionName }}</span>
          <span class="sheet-close" @click="remarkSheet.show = false">关闭</span>
        </div>
        <div class="sheet-body">
          <p class="sheet-context">
            {{ remarkSheet.row?.userName || '学员' }} · {{ remarkSheet.row?.courseName || '课程' }}
          </p>
          <div class="sheet-field">
            <van-field
              v-model="remarkSheet.remark"
              type="textarea"
              rows="3"
              maxlength="200"
              show-word-limit
              :placeholder="remarkSheet.action === 'violate'
                ? '请填写违规判定依据，必填'
                : '补充说明（选填）'"
            />
          </div>
        </div>
        <div class="sheet-foot">
          <van-button
            block
            round
            type="primary"
            class="btn-confirm"
            :loading="remarkSheet.submitting"
            @click="submitReview"
          >
            确认{{ remarkSheet.actionName }}
          </van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Tabs as VanTabs, Tab as VanTab, PullRefresh as VanPullRefresh, List as VanList,
  Button as VanButton, Popup as VanPopup, Field as VanField,
  ActionSheet as VanActionSheet, Loading as VanLoading,
  showToast, showSuccessToast,
} from 'vant'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { credits, graduate, verify } from '@/api'
import { useUserStore } from '@/stores/user'
import { useTodoStore } from '@/stores/todo'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const todoStore = useTodoStore()

const PAGE_SIZE = 10

const TAB_DEFS = [
  { key: 'credit', label: '学分审核' },
  { key: 'graduate', label: '毕业审批' },
  { key: 'verify', label: '异常复核' },
] as const
type TabKey = (typeof TAB_DEFS)[number]['key']

const visibleTabs = computed(() =>
  TAB_DEFS.filter((t) => {
    const caps = userStore.caps
    if (t.key === 'credit') return caps.canApproveCredit
    if (t.key === 'graduate') return caps.canAuditGraduate
    return caps.canReviewVerify
  })
)

function queryTab(): TabKey | '' {
  const wanted = String(route.query.tab || '')
  const keys = visibleTabs.value.map((t) => t.key as string)
  // 深链指定的 tab 无权限时静默回落到第一个可见 tab
  if (keys.includes(wanted)) return wanted as TabKey
  return (keys[0] as TabKey) || ''
}

const activeTab = ref<TabKey | ''>(queryTab())

const grad = reactive(makeFeed((pageNum) =>
  graduate.applies({ pageNum, pageSize: PAGE_SIZE, auditStatus: 0 })
    .then((res: any) => ({ records: res?.records || [], total: Number(res?.total || 0) }))
))
const abnormal = reactive(makeFeed((pageNum) =>
  verify.abnormal({ pageNum, pageSize: PAGE_SIZE })
    .then((res: any) => ({ records: res?.records || [], total: Number(res?.total || 0) }))
))

const credit = reactive({ list: [] as any[], loading: false, refreshing: false, loaded: false })
const creditBusy = ref<number | null>(null)
const gradBusy = ref<number | null>(null)
const expandedId = ref<number | null>(null)

const creditSheet = reactive({ show: false, row: null as any, remark: '', submitting: false })
const gradSheet = reactive({ show: false, row: null as any, remark: '', submitting: false })
const actionSheet = reactive({ show: false, row: null as any })
const remarkSheet = reactive({
  show: false, row: null as any, action: '', actionName: '', remark: '', submitting: false,
})

const reviewActions = [
  { name: '准予通过', color: 'var(--ok)', action: 'allow' },
  { name: '核验无效', color: 'var(--ink-2)', action: 'invalidate' },
  { name: '判定违规', color: 'var(--bad)', action: 'violate' },
]

function makeFeed(loader: (pageNum: number) => Promise<{ records: any[]; total: number }>) {
  const list = ref<any[]>([])
  const loading = ref(false)
  const finished = ref(false)
  const refreshing = ref(false)
  const total = ref(0)
  const pageNum = ref(1)

  async function loadMore() {
    if (finished.value || loading.value) return
    loading.value = true
    try {
      const res = await loader(pageNum.value)
      list.value.push(...res.records)
      total.value = res.total
      if (!res.records.length || res.records.length < PAGE_SIZE || list.value.length >= res.total) {
        finished.value = true
      } else {
        pageNum.value += 1
      }
    } catch {
      finished.value = true
    } finally {
      loading.value = false
    }
  }

  async function reload() {
    if (loading.value) return
    list.value = []
    pageNum.value = 1
    finished.value = false
    await loadMore()
  }

  async function refresh() {
    await reload()
    refreshing.value = false
  }

  return { list, loading, finished, refreshing, total, loadMore, reload, refresh }
}

function toNum(v: unknown) {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

function formatTime(t?: string) {
  return t ? dayjs(t).format('MM-DD HH:mm') : '-'
}

function formatFullTime(t?: string) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
}

/** 后端只翻译学习记录本身的字段，latestVerify 是原始 UserVerifyLog，需前端映射 */
function verifyStatusLabel(s: number) {
  if (s === 0) return '核验中'
  if (s === 2) return '核验未通过'
  if (s === 3) return '核验超时'
  return '待复核'
}

function verifyStatusClass(s: number) {
  if (s === 0) return 'chip-warn'
  if (s === 2 || s === 3) return 'chip-bad'
  return 'chip-ink'
}

async function loadCredit() {
  credit.loading = true
  try {
    const data = await credits.pending()
    credit.list = Array.isArray(data) ? data : []
    credit.loaded = true
  } catch {
    // handled by interceptor
  } finally {
    credit.loading = false
    credit.refreshing = false
  }
}

/** 任一审批动作成功后刷新当前 tab 清单与底部角标待办数 */
async function afterAction(key: TabKey) {
  if (key === 'credit') await loadCredit()
  else if (key === 'graduate') await grad.reload()
  else await abnormal.reload()
  todoStore.refresh()
}

function approveCredit(row: any) {
  creditBusy.value = row.id
  credits.audit(row.id, { approve: true })
    .then(() => {
      showSuccessToast('已通过')
      return afterAction('credit')
    })
    .catch(() => undefined)
    .finally(() => { creditBusy.value = null })
}

function openCreditReject(row: any) {
  creditSheet.row = row
  creditSheet.remark = ''
  creditSheet.show = true
}

async function submitCreditReject() {
  const reason = creditSheet.remark.trim()
  if (!reason) {
    showToast('驳回时必须填写驳回原因')
    return
  }
  creditSheet.submitting = true
  try {
    await credits.audit(creditSheet.row.id, { approve: false, remark: reason })
    showSuccessToast('已驳回')
    creditSheet.show = false
    await afterAction('credit')
  } catch {
    // handled
  } finally {
    creditSheet.submitting = false
  }
}

function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

function approveGraduate(row: any) {
  gradBusy.value = row.id
  graduate.audit(row.id, { action: 'approve' })
    .then(() => {
      showSuccessToast('已通过')
      expandedId.value = null
      return afterAction('graduate')
    })
    .catch(() => undefined)
    .finally(() => { gradBusy.value = null })
}

function openGraduateReject(row: any) {
  gradSheet.row = row
  gradSheet.remark = ''
  gradSheet.show = true
}

async function submitGraduateReject() {
  const opinion = gradSheet.remark.trim()
  if (!opinion) {
    showToast('驳回时必须填写驳回原因')
    return
  }
  gradSheet.submitting = true
  try {
    await graduate.audit(gradSheet.row.id, { action: 'reject', opinion })
    showSuccessToast('已驳回')
    gradSheet.show = false
    expandedId.value = null
    await afterAction('graduate')
  } catch {
    // handled
  } finally {
    gradSheet.submitting = false
  }
}

function openReview(row: any) {
  actionSheet.row = row
  actionSheet.show = true
}

function onReviewSelect(item: any) {
  remarkSheet.row = actionSheet.row
  remarkSheet.action = item.action
  remarkSheet.actionName = item.name
  remarkSheet.remark = ''
  remarkSheet.show = true
}

async function submitReview() {
  // /verify/{id}/review 的路径 id 是 UserVerifyLog 主键，即 latestVerify.id，不是学习记录 id
  const logId = remarkSheet.row?.latestVerify?.id
  if (!logId) {
    showToast('该记录暂无可复核的核验记录')
    return
  }
  const remark = remarkSheet.remark.trim()
  if (remarkSheet.action === 'violate' && !remark) {
    showToast('判定违规时必须填写说明')
    return
  }
  remarkSheet.submitting = true
  try {
    await verify.review(logId, {
      action: remarkSheet.action as 'allow' | 'invalidate' | 'violate',
      remark: remark || undefined,
    })
    showSuccessToast('复核完成')
    remarkSheet.show = false
    await afterAction('verify')
  } catch {
    // handled
  } finally {
    remarkSheet.submitting = false
  }
}

watch(activeTab, (key) => {
  if (key && String(route.query.tab || '') !== key) {
    router.replace({ query: { ...route.query, tab: key } })
  }
  if (key === 'credit' && !credit.loaded) loadCredit()
})

watch(() => route.query.tab, () => {
  const next = queryTab()
  if (next && next !== activeTab.value) activeTab.value = next
})

onMounted(() => {
  if (activeTab.value === 'credit') loadCredit()
})
</script>

<style scoped>
.approval-tabs :deep(.van-tabs__wrap) {
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}
.approval-tabs :deep(.van-tabs__nav) {
  background: var(--surface);
  box-shadow: none;
}
.approval-tabs :deep(.van-tab) {
  color: var(--ink-3);
  font-weight: 500;
}
.approval-tabs :deep(.van-tab--active) {
  color: var(--brand);
  font-weight: 600;
}
.approval-tabs :deep(.van-tabs__line) {
  background: var(--brand);
  border-radius: var(--r-tag);
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
}
.empty-wrap {
  padding-top: var(--sp-5);
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: var(--sp-8) 0;
}

.audit-card {
  cursor: default;
}
.audit-card--tap {
  cursor: pointer;
  transition: opacity 0.15s;
}
.audit-card--tap:active {
  opacity: 0.7;
}
.row-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.4;
  flex: 1;
  min-width: 0;
  margin-right: var(--sp-2);
}
.row-sub {
  font-size: 13px;
  color: var(--ink-2);
  line-height: 1.5;
}
.row-time {
  margin-left: auto;
  flex: none;
  font-size: 12px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}
.expand-hint {
  flex: none;
  font-size: 12px;
  color: var(--brand);
  padding: 2px var(--sp-2);
  border: 1px solid var(--brand-line);
  border-radius: var(--r-tag);
  background: var(--brand-tint);
}

.tag-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--sp-2);
}
.chip {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  height: 22px;
  padding: 0 var(--sp-2);
  border: 1px solid var(--line);
  border-radius: var(--r-tag);
  font-size: 11px;
  line-height: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.chip-brand {
  background: var(--brand-tint);
  border-color: var(--brand-line);
  color: var(--brand);
}
.chip-warn {
  background: var(--warn-tint);
  border-color: var(--warn-tint);
  color: var(--warn);
}
.chip-bad {
  background: var(--bad-tint);
  border-color: var(--bad-tint);
  color: var(--bad);
  font-weight: 600;
}
.chip-ink {
  background: var(--surface-2);
  color: var(--ink-2);
}

.strip {
  display: flex;
  align-items: stretch;
  gap: var(--sp-3);
  padding: var(--sp-3);
  border: 1px solid var(--line);
  border-radius: var(--r-chip);
  background: var(--surface-2);
}
.strip-cell {
  display: flex;
  flex-direction: column;
  gap: var(--sp-1);
  min-width: 0;
}
.strip-cell--end {
  margin-left: auto;
  text-align: right;
  align-items: flex-end;
}
.strip-label {
  font-size: 11px;
  color: var(--ink-3);
}
.strip-num {
  font-size: 18px;
  font-weight: 700;
  color: var(--brand);
  line-height: 1.15;
  font-variant-numeric: tabular-nums;
}
.strip-time {
  font-size: 12px;
  color: var(--ink-2);
  font-variant-numeric: tabular-nums;
}

.reason-box {
  font-size: 12px;
  color: var(--ink-2);
  line-height: 1.6;
  padding: var(--sp-2) var(--sp-3);
  border: 1px solid var(--line);
  border-radius: var(--r-chip);
  background: var(--surface-2);
}
.reason-label {
  display: block;
  font-size: 11px;
  color: var(--ink-3);
}
.reason-text {
  display: block;
  margin-top: var(--sp-1);
  font-size: 12px;
  color: var(--bad);
  line-height: 1.55;
}
.reason-plain {
  display: block;
  margin-top: var(--sp-1);
  font-size: 12px;
  color: var(--ink-2);
  line-height: 1.55;
}

.detail-panel {
  margin-top: var(--sp-3);
  padding: var(--sp-3);
  border: 1px solid var(--line);
  border-radius: var(--r-control);
  background: var(--surface-2);
}
.kv-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--sp-3);
}
.kv-label {
  flex: none;
  font-size: 12px;
  color: var(--ink-3);
}
.kv-value {
  font-size: 12px;
  color: var(--ink);
  text-align: right;
  min-width: 0;
  line-height: 1.6;
  font-variant-numeric: tabular-nums;
}
.kv-sep {
  height: 1px;
  border: 0;
  background: var(--line);
  margin: var(--sp-2) 0;
}
.archive-note {
  margin-top: var(--sp-2);
  padding: var(--sp-2) var(--sp-3);
  border-radius: var(--r-chip);
  background: var(--warn-tint);
  color: var(--warn);
  font-size: 12px;
  line-height: 1.5;
}
.no-verify {
  font-size: 12px;
  color: var(--ink-3);
  text-align: center;
  padding: var(--sp-3);
  border: 1px dashed var(--line-strong);
  border-radius: var(--r-chip);
}

.row-actions {
  display: flex;
  gap: var(--sp-3);
  margin-top: var(--sp-4);
}
/* 审批动作是主路径，触控区放大到 44px 并铺满半宽 */
.row-actions :deep(.van-button) {
  flex: 1;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--r-control);
}
/* Vant 的 plain 语义色已指向 token，这里再补一层浅底补足平面层级 */
.row-actions :deep(.btn-pass) {
  color: var(--ok);
  border-color: var(--ok);
  background: var(--ok-tint);
}
.row-actions :deep(.btn-reject) {
  color: var(--bad);
  border-color: var(--bad);
  background: var(--bad-tint);
}
.row-actions :deep(.btn-review) {
  color: var(--brand);
  border-color: var(--brand-line);
  background: var(--brand-tint);
}

.sheet {
  background: var(--surface);
}
.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-2);
  padding: var(--sp-4) var(--page-x);
  border-bottom: 1px solid var(--line);
}
.sheet-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
}
.sheet-close {
  flex: none;
  font-size: 13px;
  color: var(--ink-3);
  padding: var(--sp-2);
}
.sheet-body {
  padding: var(--sp-4);
}
.sheet-context {
  font-size: 13px;
  color: var(--ink-2);
  line-height: 1.6;
  padding: var(--sp-3);
  border: 1px solid var(--line);
  border-radius: var(--r-chip);
  background: var(--brand-tint);
}
.sheet-field {
  margin-top: var(--sp-3);
  border: 1px solid var(--line);
  border-radius: var(--r-control);
  background: var(--surface-2);
  overflow: hidden;
}
.sheet-field :deep(.van-cell) {
  background: transparent;
}
.sheet-field :deep(.van-cell::after) {
  display: none;
}
.sheet-field :deep(.van-field__body) {
  font-size: 14px;
}
.sheet-foot {
  padding: 0 var(--sp-4) var(--sp-4);
}
.sheet-foot :deep(.van-button) {
  height: 46px;
  font-size: 16px;
  font-weight: 600;
}
.btn-reject-solid {
  background: var(--bad);
  border-color: var(--bad);
  color: #fff;
}
.btn-confirm {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}
</style>
