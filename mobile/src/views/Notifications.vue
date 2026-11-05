<template>
  <div class="page-container">
    <PageHeader title="消息通知">
      <template #right>
        <van-button size="mini" plain type="primary" @click="onReadAll">全部已读</van-button>
      </template>
    </PageHeader>

    <!-- Filter Tabs -->
    <van-tabs v-model:active="filter" sticky @change="loadData">
      <van-tab title="全部" :name="-1" />
      <van-tab title="未读" :name="0" />
      <van-tab title="已读" :name="1" />
    </van-tabs>

    <div class="cat-bar">
      <button
        v-for="c in catOptions"
        :key="c.value"
        type="button"
        class="cat-chip"
        :class="{ on: category === c.value }"
        @click="pickCategory(c.value)"
      >{{ c.label }}</button>
    </div>

    <van-list
      v-model:loading="listLoading"
      :finished="finished"
      finished-text="没有更多了"
      @load="loadMore"
    >
      <div
        v-for="item in list"
        :key="item.id"
        class="card notify-item"
        :class="{ unread: !item.isRead }"
        @click="onRead(item)"
      >
        <div class="flex-between mb-8">
          <div class="notify-head">
            <span
              class="cat-badge"
              :class="`cat-badge--${notifyCategory(item.type).tag}`"
            >{{ notifyCategory(item.type).label }}</span>
            <span v-if="!item.isRead" class="unread-dot"></span>
            <span class="notify-title">{{ item.title }}</span>
          </div>
          <span class="notify-time">{{ formatTime(item.createTime) }}</span>
        </div>
        <p class="notify-content">{{ item.content }}</p>
        <div v-if="item.link" class="notify-foot mt-8">
          <span class="notify-jump">查看详情 <van-icon name="arrow" size="11" /></span>
        </div>
      </div>
    </van-list>

    <div v-if="list.length === 0 && !listLoading" style="padding-top: 40px;">
      <EmptyState description="暂无该类别消息" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Button as VanButton, Tabs as VanTabs, Tab as VanTab, Icon as VanIcon,
  List as VanList, showToast,
} from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { notifications } from '@/api'
import { NOTICE_CATEGORIES, notifyCategory } from '@/config/notify'
import dayjs from 'dayjs'

const router = useRouter()
const filter = ref(-1)
const category = ref('')
const list = ref<any[]>([])
const listLoading = ref(false)
const finished = ref(false)
const pageNum = ref(1)
const pageSize = 10

const catOptions = computed(() => [{ value: '', label: '全部类别' }, ...NOTICE_CATEGORIES])

function formatTime(t: string) {
  return dayjs(t).format('MM-DD HH:mm')
}

async function loadData() {
  list.value = []
  pageNum.value = 1
  finished.value = false
  loadMore()
}

function pickCategory(value: string) {
  if (category.value === value) return
  category.value = value
  loadData()
}

async function loadMore() {
  if (finished.value) return
  listLoading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize,
    }
    if (filter.value >= 0) {
      params.isRead = filter.value
    }
    if (category.value) {
      params.type = category.value
    }
    const res = await notifications.list(params)
    const records = res?.records || []
    list.value.push(...records)
    if (list.value.length >= (res?.total || 0) || records.length < pageSize) {
      finished.value = true
    }
    pageNum.value++
  } catch {
    finished.value = true
  } finally {
    listLoading.value = false
  }
}

async function onRead(item: any) {
  if (!item.isRead) {
    try {
      await notifications.read(item.id)
      item.isRead = 1
    } catch {
      // ignore
    }
  }
  if (item.link) {
    router.push(item.link)
  }
}

async function onReadAll() {
  try {
    await notifications.readAll()
    showToast('已全部标记为已读')
    loadData()
  } catch {
    // ignore
  }
}
</script>

<style scoped>
.cat-bar {
  display: flex;
  gap: var(--sp-2);
  overflow-x: auto;
  padding: var(--sp-3) var(--page-x);
  background: var(--bg);
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}
.cat-bar::-webkit-scrollbar {
  display: none;
}
.cat-chip {
  flex: none;
  padding: 7px 13px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  font-family: inherit;
  font-size: 12px;
  line-height: 1.2;
  color: var(--ink-2);
  -webkit-appearance: none;
  appearance: none;
  transition: color 0.18s, background-color 0.18s, border-color 0.18s;
}
.cat-chip.on {
  border-color: var(--brand);
  background: var(--brand);
  font-weight: 600;
  color: #fff;
}
.notify-item {
  cursor: pointer;
  transition: transform 0.15s ease;
}
.notify-item:active {
  transform: scale(0.985);
}
.notify-item.unread {
  padding-left: 15px;
}
.notify-item.unread::before {
  content: '';
  position: absolute;
  top: 16px;
  bottom: 16px;
  left: 0;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--accent);
}
.notify-head {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
  margin-right: 8px;
}
.cat-badge {
  flex: none;
  padding: 2px 7px;
  border: 1px solid var(--line);
  border-radius: var(--r-tag);
  background: var(--surface-2);
  font-size: 10px;
  line-height: 1.6;
  color: var(--ink-2);
}
.cat-badge--primary { border-color: var(--brand-line); background: var(--brand-tint); color: var(--brand); }
.cat-badge--success { border-color: #cfe5da; background: var(--ok-tint); color: var(--ok); }
.cat-badge--warning { border-color: var(--accent-line); background: var(--warn-tint); color: var(--warn); }
.cat-badge--danger { border-color: #edcfcd; background: var(--bad-tint); color: var(--bad); }
.unread-dot {
  flex: none;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
}
.notify-title {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}
.notify-time {
  flex: none;
  font-size: 11px;
  color: var(--ink-4);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}
.notify-content {
  display: -webkit-box;
  overflow: hidden;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ink-2);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.notify-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.notify-jump {
  display: inline-flex;
  align-items: center;
  gap: 1px;
  font-size: 12px;
  font-weight: 500;
  color: var(--brand);
}
</style>
