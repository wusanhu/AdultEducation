<template>
  <div class="page-card">
    <div class="toolbar">
      <a-select v-model:value="dictType" placeholder="全部字典类型" allow-clear show-search style="width:240px" :options="typeOptions" @change="load" />
      <a-input v-model:value="keyword" placeholder="字典标签 / 值" allow-clear style="width:200px" />
      <a-button type="primary" @click="load">查询</a-button>
      <div class="spacer" />
      <a-button type="primary" @click="openCreate"><PlusOutlined /> 新增字典项</a-button>
    </div>

    <a-table :data-source="filtered" :columns="columns" :loading="loading" row-key="id" size="middle" :pagination="{ pageSize: 30 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'"><code>{{ record.dictType }}</code></template>
        <template v-else-if="column.key === 'label'">
          <span style="font-weight:500">{{ record.dictLabel }}</span>
          <a-tag style="margin-left:8px">{{ record.dictValue }}</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-switch :checked="record.status === 1" size="small" @change="(v: boolean) => toggle(record, v)" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a @click="openEdit(record)">编辑</a>
            <a-popconfirm title="确认删除该字典项？" @confirm="remove(record)">
              <a style="color:#dc2626">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="open" :title="form.id ? '编辑字典项' : '新增字典项'" :width="500" :confirm-loading="saving" @ok="save">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="字典名称" name="dictName"><a-input v-model:value="form.dictName" placeholder="如：学分来源" /></a-form-item>
        <a-form-item label="字典类型" name="dictType"><a-input v-model:value="form.dictType" placeholder="如：credit_source" /></a-form-item>
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="字典值" name="dictValue"><a-input v-model:value="form.dictValue" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="字典标签" name="dictLabel"><a-input v-model:value="form.dictLabel" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="排序"><a-input-number v-model:value="form.sort" :min="0" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="状态"><a-switch v-model:checked="statusOn" checked-children="启用" un-checked-children="停用" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, type FormInstance } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { systemApi } from '../../api'
import type { DictRow } from '../../types'

const loading = ref(false)
const saving = ref(false)
const open = ref(false)
const dictType = ref<string>()
const keyword = ref('')
const rows = ref<DictRow[]>([])
const formRef = ref<FormInstance>()
const form = reactive<Record<string, any>>({ id: undefined, dictName: '', dictType: '', dictValue: '', dictLabel: '', sort: 0, status: 1 })
const statusOn = computed({ get: () => form.status === 1, set: (v) => (form.status = v ? 1 : 0) })
const rules = {
  dictName: [{ required: true, message: '请填写字典名称' }],
  dictType: [{ required: true, message: '请填写字典类型' }],
  dictValue: [{ required: true, message: '请填写字典值' }],
  dictLabel: [{ required: true, message: '请填写字典标签' }]
}
const typeOptions = computed(() => Array.from(new Set(rows.value.map((r) => r.dictType))).map((t) => ({ value: t, label: t })))
const filtered = computed(() => {
  const kw = keyword.value.trim()
  if (!kw) return rows.value
  return rows.value.filter((r) => r.dictLabel.includes(kw) || r.dictValue.includes(kw))
})
const columns = [
  { title: '字典类型', key: 'type', width: 190 },
  { title: '字典项', key: 'label', width: 280 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 90 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 140 }
]

async function load() {
  loading.value = true
  try { rows.value = await systemApi.dicts(dictType.value) } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
function openCreate() {
  Object.assign(form, { id: undefined, dictName: '', dictType: dictType.value || '', dictValue: '', dictLabel: '', sort: 0, status: 1 })
  open.value = true
}
function openEdit(row: DictRow) {
  Object.assign(form, row)
  open.value = true
}
async function save() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    await systemApi.saveDict({ ...form })
    message.success('保存成功')
    open.value = false
    await load()
  } catch (e) { message.error((e as Error).message) } finally { saving.value = false }
}
async function toggle(row: DictRow, v: boolean) {
  try {
    await systemApi.saveDict({ ...row, status: v ? 1 : 0 })
    row.status = v ? 1 : 0
    message.success('已更新')
  } catch (e) { message.error((e as Error).message) }
}
async function remove(row: DictRow) {
  try { await systemApi.removeDict(row.id); message.success('已删除'); await load() } catch (e) { message.error((e as Error).message) }
}

onMounted(load)
</script>
