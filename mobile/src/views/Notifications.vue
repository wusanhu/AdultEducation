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
          <span class="notify-title">{{ item.title }}</span>
          <span class="notify-time">{{ formatTime(item.createTime) }}</span>
        </div>
        <p class="notify-content">{{ item.content }}</p>
        <div class="notify-actions mt-8" v-if="!item.isRead">
          <van-tag type="primary" plain size="medium">未读</van-tag>
        </div>
      </div>
    </van-list>

    <div v-if="list.length === 0 && !listLoading" style="padding-top: 40px;">
      <EmptyState description="暂无通知" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  Button as VanButton, Tabs as VanTabs, Tab as VanTab, Tag as VanTag,
  List as VanList, showToast,
} from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { notifications } from '@/api'
import dayjs from 'dayjs'

const filter = ref(-1)
const list = ref<any[]>([])
const listLoading = ref(false)
const finished = ref(false)
const pageNum = ref(1)
const pageSize = 10

function formatTime(t: string) {
  return dayjs(t).format('MM-DD HH:mm')
}

async function loadData() {
  list.value = []
  pageNum.value = 1
  finished.value = false
  loadMore()
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
.notify-item {
  cursor: pointer;
}
.notify-item.unread {
  border-left: 3px solid #1989fa;
}
.notify-title {
  font-size: 15px;
  font-weight: 500;
  flex: 1;
  margin-right: 8px;
}
.notify-time {
  font-size: 12px;
  color: #c8c9cc;
  white-space: nowrap;
}
.notify-content {
  font-size: 13px;
  color: #646566;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.notify-actions {
  display: flex;
  gap: 8px;
}
</style>
