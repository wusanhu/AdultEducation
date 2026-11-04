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
            <van-tag :type="notifyCategory(item.type).tag" plain size="medium" class="cat-tag">
              {{ notifyCategory(item.type).label }}
            </van-tag>
            <span class="notify-title">{{ item.title }}</span>
          </div>
          <span class="notify-time">{{ formatTime(item.createTime) }}</span>
        </div>
        <p class="notify-content">{{ item.content }}</p>
        <div class="notify-foot mt-8">
          <van-tag v-if="!item.isRead" type="primary" size="medium">未读</van-tag>
          <span v-if="item.link" class="notify-jump">查看详情 ›</span>
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
  Button as VanButton, Tabs as VanTabs, Tab as VanTab, Tag as VanTag,
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
  background: var(--surface);
  border-bottom: 1px solid var(--line);
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}
.cat-bar::-webkit-scrollbar {
  display: none;
}
.cat-chip {
  flex: none;
  padding: 6px 12px;
  font-size: 13px;
  line-height: 1;
  color: var(--ink-2);
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 999px;
}
.cat-chip.on {
  color: #fff;
  font-weight: 500;
  background: var(--brand);
  border-color: var(--brand);
}
.notify-item {
  cursor: pointer;
}
.notify-item.unread {
  border-left: 3px solid var(--brand);
}
.notify-head {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  min-width: 0;
  flex: 1;
  margin-right: 8px;
}
.cat-tag {
  flex: none;
  padding: 3px 7px;
  font-weight: 500;
}
.cat-tag.van-tag--primary { background: var(--brand-tint); }
.cat-tag.van-tag--success { background: var(--ok-tint); }
.cat-tag.van-tag--warning { background: var(--warn-tint); }
.cat-tag.van-tag--danger { background: var(--bad-tint); }
.notify-title {
  font-size: 15px;
  font-weight: 500;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.notify-time {
  font-size: 12px;
  color: var(--ink-4);
  white-space: nowrap;
}
.notify-content {
  font-size: 13px;
  color: var(--ink-2);
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.notify-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-2);
}
.notify-jump {
  font-size: 13px;
  color: var(--brand);
}
</style>
