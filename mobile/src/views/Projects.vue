<template>
  <div class="page-container">
    <header class="page-head">
      <div>
        <span class="kicker">PROGRAMS</span>
        <h1>培训班</h1>
      </div>
      <span class="head-count">{{ isOps ? opsTotal : list.length }} 个</span>
    </header>

    <!-- 学员：我的培训班，接口返回全量，搜索在前端过滤 -->
    <template v-if="!isOps">
      <!-- 课程聚合：口径与首页学分卡一致（按课程学习状态统计） -->
      <div v-if="courseStats.total > 0" class="course-stats">
        <div class="course-stat">
          <b>{{ courseStats.learning }}<i>门</i></b>
          <span>学习中</span>
        </div>
        <div class="course-stat">
          <b>{{ courseStats.completed }}<i>门</i></b>
          <span>已完成</span>
        </div>
        <div class="course-stat">
          <b>{{ courseStats.total }}<i>门</i></b>
          <span>全部课程</span>
        </div>
      </div>

      <div class="search-bar">
        <van-search v-model="keyword" placeholder="搜索培训班名称或编号" @search="loadData" />
      </div>

      <div v-if="list.length === 0 && !loading" class="empty-wrap">
        <EmptyState icon="cluster-o" :description="keyword ? '没有匹配的培训班' : '暂无培训班'" />
      </div>

      <div
        v-for="item in list"
        :key="item.id"
        class="card project-card"
        :class="{ 'project-card--active': item.status === 1 }"
        @click="$router.push(`/projects/${item.id}`)"
      >
        <div class="flex-between mb-8">
          <span class="project-name">{{ item.projectName }}</span>
          <StatusTag :status="item.status" type="project" />
        </div>
        <div class="project-meta">
          <span class="meta-code">{{ item.projectCode }}</span>
          <span v-if="item.startTime" class="meta-date">
            {{ formatDate(item.startTime) }} ~ {{ formatDate(item.endTime) }}
          </span>
        </div>

        <hr class="hairline" />

        <div class="progress-head">
          <span class="progress-text">学习进度</span>
          <span class="progress-num">
            {{ item.completedCourse || 0 }}/{{ item.courseTotal || 0 }}
            <span class="progress-unit">课程</span>
          </span>
        </div>
        <van-progress
          :percentage="item.courseTotal ? Math.round(((item.completedCourse || 0) / item.courseTotal) * 100) : 0"
          :show-pivot="false"
          color="var(--brand)"
          track-color="var(--surface-3)"
          stroke-width="5"
        />

        <div class="credit-box mt-12">
          <span class="credit-box-label">已获学分 / 所需学分</span>
          <span class="credit-box-value">
            <b class="stat-value">{{ item.credit || 0 }}</b>
            <i>/ {{ item.needCredit || 0 }}</i>
          </span>
        </div>
      </div>

      <van-loading v-if="loading" class="loading-center" />
    </template>

    <!-- 教务：数据权限内的全部培训班，检索与翻页全部交给服务端 -->
    <template v-else>
      <div class="search-bar">
        <van-search
          v-model="keyword"
          placeholder="搜索名称或编号"
          @search="resetOpsList"
          @clear="resetOpsList"
        />
      </div>



      <van-pull-refresh v-model="refreshing" @refresh="resetOpsList">
        <van-list
          v-model:loading="opsLoading"
          :finished="opsFinished"
          finished-text="没有更多了"
          @load="loadOpsPage"
        >
          <div
            v-for="item in opsList"
            :key="item.id"
            class="card project-card"
            :class="{ 'project-card--active': item.status === 1 }"
            @click="$router.push(`/projects/${item.id}`)"
          >
            <div class="flex-between mb-8">
              <span class="project-name">{{ item.projectName }}</span>
              <StatusTag :status="item.status" type="project" />
            </div>
            <div class="project-meta">
              <span v-if="item.projectCode" class="meta-code">{{ item.projectCode }}</span>
              <span v-if="item.startTime" class="meta-date">
                {{ formatDate(item.startTime) }} ~ {{ formatDate(item.endTime) }}
              </span>
            </div>

            <hr class="hairline" />

            <div class="ops-stat">
              <div class="ops-stat-item">
                <span class="stat-value ops-stat-num">{{ item.memberCount || 0 }}</span>
                <span class="stat-label">学员</span>
              </div>
              <div class="ops-stat-item">
                <span class="stat-value ops-stat-num">{{ item.courseCount || 0 }}</span>
                <span class="stat-label">课程</span>
              </div>
              <div class="ops-stat-item">
                <span class="stat-value ops-stat-num">{{ item.needCredit || 0 }}</span>
                <span class="stat-label">所需学分</span>
              </div>
            </div>

            <p v-if="teacherText(item.teachers)" class="ops-teachers mt-12">
              <van-icon name="manager-o" size="13" />
              <span class="text-ellipsis">师资：{{ teacherText(item.teachers) }}</span>
            </p>
          </div>

          <div v-if="opsList.length === 0 && opsFinished" class="empty-wrap">
            <EmptyState :description="keyword ? '没有匹配的培训班' : '暂无培训班'" />
          </div>
        </van-list>
      </van-pull-refresh>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import {
  Search as VanSearch,
  Progress as VanProgress,
  Loading as VanLoading,
  List as VanList,
  PullRefresh as VanPullRefresh,
  Icon as VanIcon,
} from 'vant'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { projects } from '@/api'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const userStore = useUserStore()
/** 列表分支只看能力，不看角色名 */
const isOps = computed(() => userStore.caps.canListProjects)

const keyword = ref('')
const list = ref<any[]>([])
const loading = ref(false)
/** 课程聚合：直接累加 /projects/my 下发的 learningCourse/completedCourse/courseTotal，
 *  与首页学分卡同口径，无需逐班请求详情 */
const courseStats = ref({ learning: 0, completed: 0, total: 0 })

function computeCourseStats(all: any[]) {
  courseStats.value = {
    learning: all.reduce((s, p) => s + (p.learningCourse || 0), 0),
    completed: all.reduce((s, p) => s + (p.completedCourse || 0), 0),
    total: all.reduce((s, p) => s + (p.courseTotal || 0), 0),
  }
}

/** 旧版后端未下发 learningCourse 时，回退为逐班详情统计 */
async function loadCourseStatsFallback(all: any[]) {
  const details = await Promise.all(
    all.map((p) => projects.myDetail(p.id).catch(() => null))
  )
  let learning = 0
  let completed = 0
  let total = 0
  for (const d of details) {
    for (const c of (d?.courses || []) as any[]) {
      total++
      if (c.studyStatus === 1) learning++
      else if (c.studyStatus === 2) completed++
    }
  }
  courseStats.value = { learning, completed, total }
}

const opsList = ref<any[]>([])
const opsTotal = ref(0)
const opsPage = ref(1)
const opsLoading = ref(false)
const opsFinished = ref(false)
const refreshing = ref(false)
const PAGE_SIZE = 10

function formatDate(d: string) {
  return dayjs(d).format('YYYY-MM-DD')
}

function teacherText(teachers: any) {
  const names = ((teachers || []) as any[])
    .map((t: any) => t.realName)
    .filter((n: any) => !!n)
  if (names.length === 0) return ''
  if (names.length <= 2) return names.join('、')
  return `${names.slice(0, 2).join('、')} 等${names.length}人`
}

async function loadData() {
  loading.value = true
  try {
    const data = await projects.my()
    const all = data as any[]
    if (keyword.value) {
      list.value = all.filter((p: any) =>
        (p.projectName || '').includes(keyword.value) ||
        (p.projectCode || '').includes(keyword.value)
      )
    } else {
      list.value = all
    }
    // 聚合始终基于全量培训班，保证与首页数字一致，不受搜索过滤影响
    if (all.length > 0 && all[0].learningCourse != null) {
      computeCourseStats(all)
    } else {
      loadCourseStatsFallback(all)
    }
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

async function loadOpsPage() {
  opsLoading.value = true
  try {
    const res: any = await projects.page({
      pageNum: opsPage.value,
      pageSize: PAGE_SIZE,
      keyword: keyword.value || undefined,
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

// 教务分支首屏交给 van-list 的 @load，避免和 onMounted 重复请求
onMounted(() => {
  if (!isOps.value) loadData()
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--sp-3);
  padding: calc(env(safe-area-inset-top) + 24px) var(--page-x) var(--sp-3);
}
.page-head h1 {
  margin-top: 5px;
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0.02em;
  color: var(--ink);
}
.head-count {
  flex: none;
  margin-bottom: 3px;
  padding: 3px 10px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  font-size: 11px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}
/* 课程聚合条：与首页学分卡同一口径 */
.course-stats {
  display: flex;
  margin: 0 var(--page-x) var(--sp-4);
  padding: var(--sp-3) 0;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-card);
}
.course-stat {
  flex: 1;
  min-width: 0;
  text-align: center;
}
.course-stat + .course-stat {
  border-left: 1px solid var(--line);
}
.course-stat b {
  display: block;
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
.course-stat b i {
  font-style: normal;
  font-size: 11px;
  margin-left: 2px;
  color: var(--ink-3);
  font-family: var(--font-sans, inherit);
}
.course-stat span {
  display: block;
  margin-top: 3px;
  font-size: 11px;
  color: var(--ink-3);
}

.search-bar {
  padding: 0 var(--page-x) var(--sp-3);
  background: transparent;
}
.search-bar :deep(.van-search) {
  padding: 0;
}
.search-bar :deep(.van-search__content) {
  border-radius: var(--r-control);
  background: var(--surface);
  border: 1px solid var(--line);
}
.search-bar :deep(.van-field__control) {
  font-size: 14px;
}
.empty-wrap {
  margin: 0 var(--page-x) var(--sp-3);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-card);
  box-shadow: var(--shadow-card);
}
.project-card {
  position: relative;
  overflow: hidden;
  padding: var(--sp-4);
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease;
}
.project-card:active {
  background: var(--surface-2);
}
.project-card--active::before {
  content: '';
  position: absolute;
  top: var(--sp-4);
  bottom: var(--sp-4);
  left: 0;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--accent);
}
.project-name {
  flex: 1;
  min-width: 0;
  margin-right: var(--sp-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--ink);
  letter-spacing: 0.01em;
}
.project-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px var(--sp-3);
  font-size: 12px;
  color: var(--ink-3);
}
.meta-code {
  padding: 2px 7px;
  border: 1px solid var(--brand-line);
  border-radius: var(--r-tag);
  background: var(--brand-tint);
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}
.meta-date {
  color: var(--ink-3);
}
.progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-2);
  margin-bottom: var(--sp-2);
}
.progress-text {
  font-size: 12px;
  color: var(--ink-3);
}
.progress-num {
  font-family: var(--font-serif);
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
.progress-unit {
  font-size: 11px;
  font-weight: 400;
  color: var(--ink-3);
  margin-left: 2px;
}
.credit-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-2);
  padding: 10px var(--sp-3);
  border: 1px solid var(--accent-line);
  border-radius: var(--r-chip);
  background: var(--accent-tint);
}
.credit-box-label {
  font-size: 11px;
  color: var(--accent-strong);
  opacity: 0.8;
}
.credit-box-value {
  display: flex;
  align-items: baseline;
  gap: var(--sp-2);
}
.credit-box-value b.stat-value {
  font-size: 22px;
  color: var(--accent-strong);
}
.credit-box-value i {
  font-style: normal;
  font-size: 13px;
  color: var(--ink-2);
  font-variant-numeric: tabular-nums;
}
.ops-stat {
  display: flex;
}
.ops-stat-item {
  flex: 1;
  min-width: 0;
  text-align: center;
}
.ops-stat-item + .ops-stat-item {
  border-left: 1px solid var(--line);
}
.ops-stat-num {
  display: block;
  font-size: 21px;
  color: var(--brand);
}
.ops-teachers {
  display: flex;
  align-items: center;
  gap: var(--sp-1);
  min-width: 0;
  font-size: 12px;
  color: var(--ink-2);
}
.ops-teachers span {
  min-width: 0;
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: var(--sp-5);
}
</style>
