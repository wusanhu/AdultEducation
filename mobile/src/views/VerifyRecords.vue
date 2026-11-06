<template>
  <div class="page-container">
    <PageHeader title="核验记录" />

    <!-- 学员：只看本人核验 -->
    <template v-if="!isOpsView">
      <van-loading v-if="loading" class="loading-center" />

      <div v-if="!loading && list.length === 0" class="empty-wrap">
        <EmptyState description="暂无核验记录" />
      </div>

      <div v-for="item in list" :key="item.id" class="card verify-item">
        <div class="flex-between mb-8">
          <span class="verify-course text-ellipsis">{{ item.courseName || '课程核验' }}</span>
          <span class="verify-result" :class="resultClass(item.verifyResult)">{{ resultText(item) }}</span>
        </div>
        <div class="verify-meta">
          <span>方式：{{ typeText(item) }}</span>
          <span>时间：{{ formatTime(item.verifyTime || item.createTime) }}</span>
        </div>
        <div v-if="similarityText(item)" class="verify-meta mt-4">
          <span>相似度：{{ similarityText(item) }}</span>
        </div>
        <p v-if="item.failReason" class="verify-fail mt-8">{{ item.failReason }}</p>
        <p v-if="item.remark" class="verify-remark mt-8">{{ item.remark }}</p>
      </div>
    </template>

    <!-- 教务：数据权限内的核验流水，分页 + 结果筛选交给服务端 -->
    <template v-else>
      <van-tabs v-model:active="resultFilter" sticky @change="resetOpsList">
        <van-tab title="全部" :name="-1" />
        <van-tab title="通过" :name="1" />
        <van-tab title="失败" :name="0" />
        <van-tab title="超时" :name="2" />
      </van-tabs>

      <div v-if="opsTotal > 0" class="list-count">
        <span class="kicker">核验记录</span>
        <span class="list-count-num">共 {{ opsTotal }} 条</span>
      </div>

      <van-pull-refresh v-model="refreshing" @refresh="resetOpsList">
        <van-list
          v-model:loading="opsLoading"
          :finished="opsFinished"
          finished-text="没有更多了"
          @load="loadOpsPage"
        >
          <div v-for="item in opsList" :key="item.id" class="card verify-item">
            <div class="verify-row">
              <span class="verify-thumb">
                <!-- leader/student 角色后端返回 imgUrl=null（抓拍照片受限），降级为描边占位 -->
                <img
                  v-if="item.imgUrl && !item._imgFailed"
                  :src="fileUrl(item.imgUrl)"
                  alt=""
                  @error="item._imgFailed = true"
                />
                <van-icon v-else name="photo-o" size="18" />
              </span>

              <div class="verify-body">
                <div class="flex-between mb-4">
                  <span class="verify-user text-ellipsis">{{ item.userName || item.username || '学员' }}</span>
                  <span class="verify-result" :class="resultClass(item.verifyResult)">{{ resultText(item) }}</span>
                </div>
                <div class="verify-line text-ellipsis">{{ item.courseName || '课程核验' }}</div>
                <div v-if="item.projectName" class="verify-line text-ellipsis">{{ item.projectName }}</div>

                <div class="verify-meta mt-4">
                  <span>{{ typeText(item) }}</span>
                  <span v-if="similarityText(item)">相似度 {{ similarityText(item) }}</span>
                  <span v-if="item.statusLabel">{{ item.statusLabel }}</span>
                </div>
                <div class="verify-meta mt-4">
                  <span>{{ formatTime(item.verifyTime) }}</span>
                  <span v-if="item.orgName" class="text-ellipsis">{{ item.orgName }}</span>
                </div>

                <p v-if="item.failReason" class="verify-fail mt-8">{{ item.failReason }}</p>
              </div>
            </div>
          </div>

          <div v-if="opsList.length === 0 && opsFinished" class="empty-wrap">
            <EmptyState description="暂无核验记录" />
          </div>
        </van-list>
      </van-pull-refresh>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import {
  Loading as VanLoading,
  Tabs as VanTabs,
  Tab as VanTab,
  List as VanList,
  PullRefresh as VanPullRefresh,
  Icon as VanIcon,
} from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { verify } from '@/api'
import { fileUrl } from '@/api/http'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const userStore = useUserStore()
/** /verify/records 后端要求 verify:list，能力位 canViewVerify 即该权限（或 verify:abnormal） */
const isOpsView = computed(() => userStore.caps.canViewVerify)

const loading = ref(true)
const list = ref<any[]>([])

const opsList = ref<any[]>([])
const opsTotal = ref(0)
const opsPage = ref(1)
const opsLoading = ref(false)
const opsFinished = ref(false)
const refreshing = ref(false)
const resultFilter = ref(-1)
const PAGE_SIZE = 10

function resultText(item: any) {
  return item.verifyResultLabel || resultLabel(item.verifyResult)
}

function resultLabel(r: number) {
  switch (r) {
    case 1: return '通过'
    case 0: return '失败'
    case 2: return '超时'
    default: return '待核验'
  }
}

function resultClass(r: number) {
  return r === 1 ? 'is-ok' : 'is-bad'
}

function typeText(item: any) {
  if (item.verifyTypeLabel) return item.verifyTypeLabel
  switch (item.verifyType) {
    case 1: case 'face': return '人脸比对'
    case 2: case 'sms': return '短信验证码'
    default: return '其他'
  }
}

// similarity 落库为 DECIMAL(6,4) 的小数（0.9312），统一按百分比展示
function similarityText(item: any) {
  const raw = item.similarity
  if (raw === null || raw === undefined || raw === '') return ''
  const num = Number(raw)
  if (Number.isNaN(num)) return ''
  const percent = num <= 1 ? num * 100 : num
  return `${percent.toFixed(1)}%`
}

function formatTime(t: string) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
}

async function loadData() {
  loading.value = true
  try {
    list.value = await verify.my() as any[]
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function loadOpsPage() {
  opsLoading.value = true
  try {
    const res: any = await verify.records({
      pageNum: opsPage.value,
      pageSize: PAGE_SIZE,
      verifyResult: resultFilter.value >= 0 ? resultFilter.value : undefined,
    })
    const records = res?.records || []
    opsList.value.push(...records)
    opsTotal.value = res?.total || 0
    if (records.length < PAGE_SIZE || opsList.value.length >= opsTotal.value) {
      opsFinished.value = true
    }
    opsPage.value += 1
  } catch {
    opsFinished.value = true
  } finally {
    opsLoading.value = false
    refreshing.value = false
  }
}

function resetOpsList() {
  opsList.value = []
  opsPage.value = 1
  opsTotal.value = 0
  opsFinished.value = false
  loadOpsPage()
}

// 教务分支首屏由 van-list 的 @load 触发，避免重复请求
onMounted(() => {
  if (!isOpsView.value) loadData()
})
</script>

<style scoped>
.empty-wrap {
  margin: var(--sp-4) var(--page-x) var(--sp-3);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-card);
  box-shadow: var(--shadow-card);
}
.list-count {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--sp-2);
  padding: var(--sp-4) var(--page-x) var(--sp-1);
}
.list-count-num {
  font-size: 11px;
  color: var(--ink-3);
}
.verify-item {
  cursor: default;
}
.verify-row {
  display: flex;
  gap: var(--sp-3);
}
.verify-thumb {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 48px;
  height: 48px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: var(--r-chip);
  background: var(--surface-2);
  color: var(--ink-3);
}
.verify-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.verify-body {
  flex: 1;
  min-width: 0;
}
.verify-course {
  flex: 1;
  min-width: 0;
  margin-right: var(--sp-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}
.verify-user {
  min-width: 0;
  margin-right: var(--sp-2);
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}
.verify-line {
  font-size: 13px;
  color: var(--ink-2);
  line-height: 1.6;
}
.verify-result {
  flex: none;
  padding: 2px 8px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface-2);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.5;
}
.verify-result.is-ok {
  border-color: var(--ok-line);
  background: var(--ok-tint);
  color: var(--ok);
}
.verify-result.is-bad {
  border-color: var(--bad-line);
  background: var(--bad-tint);
  color: var(--bad);
}
.verify-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-1) var(--sp-3);
  font-size: 12px;
  color: var(--ink-3);
}
.verify-remark {
  padding: var(--sp-2) var(--sp-3);
  border: 1px solid var(--line);
  border-radius: var(--r-tag);
  background: var(--surface-2);
  font-size: 11px;
  line-height: 1.6;
  color: var(--ink-2);
}
.verify-fail {
  padding: 6px 10px;
  border: 1px solid var(--bad-line);
  border-radius: var(--r-tag);
  background: var(--bad-tint);
  font-size: 11px;
  line-height: 1.6;
  color: var(--bad);
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: var(--sp-5);
}
</style>
