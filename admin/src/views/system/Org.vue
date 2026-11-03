<template>
  <div class="page-card">
    <div class="toolbar">
      <a-input v-model:value="keyword" placeholder="搜索组织 / 部门名称" allow-clear style="width:260px" />
      <a-button type="primary" @click="load">查询</a-button>
      <div class="spacer" />
      <a-button v-if="auth.hasPerm('system:org:edit')" type="primary" @click="openCreate()"><PlusOutlined /> 新增组织</a-button>
    </div>
    <a-table :data-source="filteredTree" :columns="columns" :loading="loading" row-key="id" :pagination="false" size="middle"
             :default-expand-all-rows="true">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <span style="font-weight:500">{{ record.orgName }}</span>
          <a-tag v-if="record.orgType === 1" color="blue" style="margin-left:8px">组织</a-tag>
          <a-tag v-else style="margin-left:8px">部门</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-switch :checked="record.status === 1" size="small" :disabled="!auth.hasPerm('system:org:edit')" @change="(v: boolean) => toggleStatus(record, v)" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space v-if="auth.hasPerm('system:org:edit')" size="small">
            <a @click="openCreate(record)">新增下级</a>
            <a @click="openEdit(record)">编辑</a>
            <a-popconfirm title="确认删除该组织/部门？存在下级或账号时会被拒绝" @confirm="remove(record)">
              <a style="color:#dc2626">删除</a>
            </a-popconfirm>
          </a-space>
          <span v-else class="muted">-</span>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="open" :title="form.id ? '编辑组织' : '新增组织'" :width="560" :confirm-loading="saving" @ok="save">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="上级" name="parentId">
          <a-tree-select v-model:value="form.parentId" :tree-data="tree" allow-clear placeholder="不选则为顶级"
                         :field-names="{ label: 'orgName', value: 'id', children: 'children' }" tree-default-expand-all />
        </a-form-item>
        <a-form-item label="名称" name="orgName"><a-input v-model:value="form.orgName" placeholder="如：XX市教育局 / 培训科" /></a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="类型" name="orgType">
              <a-radio-group v-model:value="form.orgType">
                <a-radio :value="1">组织</a-radio>
                <a-radio :value="2">部门</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="编码" name="orgCode"><a-input v-model:value="form.orgCode" placeholder="唯一编码" /></a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="负责人"><a-select v-model:value="form.leaderUserId" allow-clear show-search option-filter-prop="label" :options="leaderOptions" /></a-form-item>
          </a-col>
          <a-col :span="12"><a-form-item label="排序"><a-input-number v-model:value="form.sort" :min="0" style="width:100%" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="状态"><a-switch v-model:checked="statusOn" checked-children="启用" un-checked-children="停用" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, type FormInstance } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { systemApi } from '../../api'
import { useAuthStore } from '../../store/auth'
import type { OrgNode } from '../../types'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const open = ref(false)
const keyword = ref('')
const tree = ref<OrgNode[]>([])
const users = ref<{ id: number; realName: string; username: string }[]>([])
const formRef = ref<FormInstance>()
const form = reactive<Record<string, any>>({ id: undefined, parentId: undefined, orgName: '', orgType: 1, orgCode: '', leaderUserId: undefined, sort: 0, status: 1 })
const statusOn = computed({ get: () => form.status === 1, set: (v) => (form.status = v ? 1 : 0) })
const leaderOptions = computed(() => users.value.map((u) => ({ value: u.id, label: `${u.realName}（${u.username}）` })))
const filteredTree = computed<OrgNode[]>(() => {
  const kw = keyword.value.trim()
  if (!kw) return tree.value
  const filter = (nodes: OrgNode[]): OrgNode[] => nodes
    .map((n) => ({ ...n, children: filter(n.children || []) }))
    .filter((n) => n.orgName.includes(kw) || n.orgCode?.includes(kw) || (n.children && n.children.length > 0))
  return filter(tree.value)
})
const rules = {
  orgName: [{ required: true, message: '请填写名称' }],
  orgType: [{ required: true, message: '请选择类型' }],
  orgCode: [{ required: true, message: '请填写编码' }]
}
const columns = [
  { title: '名称', key: 'name', width: 360 },
  { title: '编码', dataIndex: 'orgCode', key: 'orgCode', width: 160 },
  { title: '负责人', dataIndex: 'leaderName', key: 'leaderName', width: 120 },
  { title: '账号数', dataIndex: 'userCount', key: 'userCount', width: 100 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 190, align: 'right' as const }
]

async function load() {
  loading.value = true
  try { tree.value = await systemApi.orgTree() } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
async function loadUsers() {
  try {
    const page = await systemApi.users({ pageNum: 1, pageSize: 500 })
    users.value = page.records
  } catch { /* ignore */ }
}
function openCreate(parent?: OrgNode) {
  Object.assign(form, { id: undefined, parentId: parent?.id, orgName: '', orgType: parent ? 2 : 1, orgCode: '', leaderUserId: undefined, sort: 0, status: 1 })
  open.value = true
}
function openEdit(row: OrgNode) {
  Object.assign(form, { id: row.id, parentId: row.parentId || undefined, orgName: row.orgName, orgType: row.orgType, orgCode: row.orgCode, leaderUserId: row.leaderUserId, sort: row.sort, status: row.status })
  open.value = true
}
async function save() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    await systemApi.saveOrg({ ...form })
    message.success('保存成功')
    open.value = false
    await load()
  } catch (e) { message.error((e as Error).message) } finally { saving.value = false }
}
async function remove(row: OrgNode) {
  try { await systemApi.removeOrg(row.id); message.success('已删除'); await load() } catch (e) { message.error((e as Error).message) }
}
async function toggleStatus(row: OrgNode, v: boolean) {
  try { await systemApi.orgStatus(row.id, v ? 1 : 0); row.status = v ? 1 : 0; message.success('已更新') } catch (e) { message.error((e as Error).message) }
}

onMounted(() => { void load(); void loadUsers() })
</script>
