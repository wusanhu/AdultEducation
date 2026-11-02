<template>
  <div class="page-card">
    <div class="toolbar">
      <div>
        <div style="font-size:17px;font-weight:600">学分补录审批</div>
        <div style="font-size:12px;color:#94a3b8;margin-top:3px">待审批 {{ rows.length }} 项 · 审批通过后学分立即入账并通知学员</div>
      </div>
      <div class="spacer" />
      <a-button @click="load"><ReloadOutlined /> 刷新</a-button>
    </div>

    <a-empty v-if="!loading && rows.length === 0" description="当前没有待审批的学分补录" />
    <a-spin :spinning="loading">
      <div v-for="item in rows" :key="item.id" style="border:1px solid #e8eef7;border-radius:12px;padding:18px;margin-bottom:12px">
        <div style="display:flex;gap:16px;align-items:flex-start;flex-wrap:wrap">
          <a-avatar :size="44" style="background:#e0f2fe;color:#0369a1">{{ String(item.userName || '学').slice(0, 1) }}</a-avatar>
          <div style="flex:1;min-width:260px">
            <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap">
              <span style="font-size:15px;font-weight:600">{{ item.userName }}</span>
              <span style="font-size:12px;color:#94a3b8">{{ item.orgName }}</span>
              <a-tag color="purple">人工补录</a-tag>
              <a-tag v-if="item.auditNode === 'LEADER'" color="orange">领导复核</a-tag>
              <a-tag v-else color="blue">管理员终审</a-tag>
            </div>
            <div style="font-size:13px;color:#475569;margin-top:8px">
              《{{ item.projectName }}》 · <b style="color:#16a34a">+{{ item.creditValue }} 学分</b>
            </div>
            <div style="font-size:12px;color:#64748b;margin-top:6px;background:#f8fafc;border-radius:7px;padding:9px 12px;line-height:1.7">
              <span class="muted">补录原因：</span>{{ item.remark || '-' }}
            </div>
            <div style="font-size:12px;color:#94a3b8;margin-top:8px">申请人：{{ item.applyUserName || '-' }} · 提交时间：{{ item.createTime }}</div>
          </div>
          <div style="display:flex;flex-direction:column;gap:8px;min-width:130px">
            <a-button type="primary" @click="openAction(item, true)">通过</a-button>
            <a-button danger @click="openAction(item, false)">驳回</a-button>
          </div>
        </div>
      </div>
    </a-spin>

    <a-modal v-model:open="actionOpen" :title="approve ? '通过学分补录' : '驳回学分补录'" :confirm-loading="submitting" :ok-button-props="{ danger: !approve }" width="500" @ok="submit">
      <a-alert v-if="current" :type="approve ? 'success' : 'warning'" show-icon :message="`${current.userName} · ${current.projectName} · ${current.creditValue} 学分`" style="margin-bottom:16px" />
      <a-form layout="vertical">
        <a-form-item :label="approve ? '审批意见' : '驳回原因'" :required="!approve">
          <a-textarea v-model:value="opinion" :rows="3" :placeholder="approve ? '选填，如：材料齐全，同意入账' : '请填写驳回原因，学员可据此补充材料'" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { creditApi } from '../../api'
import type { CreditRow } from '../../types'

const rows = ref<CreditRow[]>([])
const loading = ref(false)
const actionOpen = ref(false)
const submitting = ref(false)
const approve = ref(true)
const current = ref<CreditRow | null>(null)
const opinion = ref('')

async function load() {
  loading.value = true
  try { rows.value = await creditApi.pending() } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
function openAction(item: CreditRow, pass: boolean) {
  current.value = item
  approve.value = pass
  opinion.value = ''
  actionOpen.value = true
}
async function submit() {
  if (!current.value) return
  if (!approve.value && !opinion.value.trim()) { message.warning('驳回时必须填写原因'); return }
  submitting.value = true
  try {
    await creditApi.audit(current.value.id, approve.value, opinion.value)
    message.success(approve.value ? '已通过并发放学分' : '已驳回')
    actionOpen.value = false
    await load()
  } catch (e) { message.error((e as Error).message) } finally { submitting.value = false }
}

onMounted(load)
</script>
