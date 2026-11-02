<template>
  <div class="page-container">
    <!-- Summary Cards -->
    <div class="summary-cards">
      <div class="summary-item">
        <div class="stat-value">{{ summary.totalCredits || 0 }}</div>
        <div class="stat-label">总学分</div>
      </div>
      <div class="summary-item">
        <div class="stat-value" style="color: #07c160">{{ summary.earnedCredits || 0 }}</div>
        <div class="stat-label">已获得</div>
      </div>
      <div class="summary-item">
        <div class="stat-value" style="color: #ff976a">{{ summary.pendingCredits || 0 }}</div>
        <div class="stat-label">待审核</div>
      </div>
      <div class="summary-item">
        <div class="stat-value" style="color: #ee0a24">{{ summary.requiredCredits || 0 }}</div>
        <div class="stat-label">需完成</div>
      </div>
    </div>

    <!-- Apply Button -->
    <div class="card" style="text-align: center;">
      <van-button type="primary" plain size="small" round @click="showApply = true">
        学分补录申请
      </van-button>
    </div>

    <!-- Ledger List -->
    <div class="section-title">学分明细</div>
    <div v-if="list.length === 0" style="padding-top: 20px;">
      <EmptyState description="暂无学分记录" />
    </div>
    <div v-for="item in list" :key="item.id" class="card credit-item">
      <div class="flex-between mb-8">
        <span class="credit-name">{{ item.courseName || item.projectName || '学分记录' }}</span>
        <span class="credit-value">+{{ item.creditValue }}</span>
      </div>
      <div class="flex-between">
        <div class="credit-meta">
          <van-tag v-if="item.sourceType" size="medium" plain>{{ sourceLabel(item.sourceType) }}</van-tag>
          <StatusTag v-if="item.auditStatus != null" :status="item.auditStatus" type="credit" />
        </div>
        <span class="credit-time">{{ formatDate(item.createTime) }}</span>
      </div>
      <p v-if="item.remark" class="credit-remark mt-8">{{ item.remark }}</p>
    </div>

    <!-- Apply Dialog -->
    <van-popup v-model:show="showApply" position="bottom" round :style="{ maxHeight: '70vh' }">
      <div class="apply-form">
        <h3 class="apply-title">学分补录申请</h3>
        <van-form @submit="onApply">
          <van-field
            v-model="applyForm.creditValue"
            label="补录学分"
            type="digit"
            placeholder="请输入补录学分"
            :rules="[{ required: true, message: '请输入补录学分' }]"
          />
          <van-field
            v-model="applyForm.remark"
            label="补录原因"
            type="textarea"
            rows="3"
            placeholder="请输入补录原因"
            :rules="[{ required: true, message: '请输入补录原因' }]"
          />
          <div class="apply-btns">
            <van-button block round type="primary" native-type="submit" :loading="applying">提交申请</van-button>
            <van-button block round plain class="mt-8" @click="showApply = false">取消</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  Button as VanButton, Tag as VanTag, Popup as VanPopup,
  Form as VanForm, Field as VanField, showToast,
} from 'vant'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { credits, projects } from '@/api'
import dayjs from 'dayjs'

const summary = ref<any>({})
const list = ref<any[]>([])
const showApply = ref(false)
const applying = ref(false)
const applyForm = reactive({
  creditValue: '',
  remark: '',
})

function formatDate(d: string) {
  return d ? dayjs(d).format('YYYY-MM-DD') : '-'
}

function sourceLabel(t: number) {
  switch (t) {
    case 1: return '课程学习'
    case 2: return '考试通过'
    case 3: return '手动补录'
    case 4: return '毕业认定'
    default: return '其他'
  }
}

async function loadData() {
  try {
    const [sum, items] = await Promise.all([
      credits.summary(),
      credits.my(),
    ])
    summary.value = sum
    list.value = items as any[]
  } catch {
    // ignore
  }
}

async function onApply() {
  applying.value = true
  try {
    // Get first project for apply
    const opts = await projects.options()
    const projectId = opts?.[0]?.id
    if (!projectId) {
      showToast('暂无可用培训班')
      return
    }
    const user = JSON.parse(localStorage.getItem('mobile_user') || '{}')
    await credits.apply({
      userId: user.id,
      projectId,
      creditValue: Number(applyForm.creditValue),
      remark: applyForm.remark,
    })
    showToast('申请已提交')
    showApply.value = false
    applyForm.creditValue = ''
    applyForm.remark = ''
    loadData()
  } catch {
    // handled
  } finally {
    applying.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.summary-cards {
  display: flex;
  background: linear-gradient(135deg, #1989fa, #07c160);
  padding: 24px 16px;
  margin-bottom: 12px;
}
.summary-item {
  flex: 1;
  text-align: center;
  color: #fff;
}
.summary-item .stat-value {
  color: #fff;
}
.summary-item .stat-label {
  color: rgba(255, 255, 255, 0.8);
}
.credit-item {
  cursor: default;
}
.credit-name {
  font-size: 14px;
  font-weight: 500;
  flex: 1;
  margin-right: 8px;
}
.credit-value {
  font-size: 18px;
  font-weight: 700;
  color: #07c160;
}
.credit-meta {
  display: flex;
  gap: 6px;
}
.credit-time {
  font-size: 12px;
  color: #c8c9cc;
}
.credit-remark {
  font-size: 12px;
  color: #969799;
}
.apply-form {
  padding: 20px 16px;
}
.apply-title {
  text-align: center;
  font-size: 16px;
  margin-bottom: 16px;
}
.apply-btns {
  padding: 16px 0;
}
</style>
