<template>
  <div class="page-card">
    <div class="toolbar">
      <span style="color:#64748b">角色决定功能权限与数据范围，预置角色可调整权限但不可删除。</span>
      <div class="spacer" />
      <a-button v-if="auth.hasPerm('system:role:edit')" type="primary" @click="openCreate"><PlusOutlined /> 新增角色</a-button>
    </div>
    <a-table :data-source="rows" :columns="columns" :loading="loading" row-key="id" size="middle" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'role'">
          <div style="font-weight:500">{{ record.roleName }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.roleCode }}</div>
        </template>
        <template v-else-if="column.key === 'scope'">
          <a-tag>{{ scopeLabel(record.dataScope) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-switch :checked="record.status === 1" size="small" :disabled="!!record.builtIn || !auth.hasPerm('system:role:edit')"
                    @change="(v: boolean) => toggleStatus(record, v)" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space v-if="auth.hasPerm('system:role:edit')" size="small">
            <a @click="openEdit(record)">配置权限</a>
            <a-popconfirm v-if="!record.builtIn" title="确认删除该角色？" @confirm="remove(record)">
              <a style="color:#dc2626">删除</a>
            </a-popconfirm>
          </a-space>
          <span v-else class="muted">-</span>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="open" :title="form.id ? '编辑角色' : '新增角色'" :width="800" :confirm-loading="saving" @ok="save">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8"><a-form-item label="角色名称" name="roleName"><a-input v-model:value="form.roleName" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="角色编码" name="roleCode"><a-input v-model:value="form.roleCode" :disabled="!!form.builtIn" /></a-form-item></a-col>
          <a-col :span="8">
            <a-form-item label="数据范围" name="dataScope">
              <a-select v-model:value="form.dataScope" :options="scopeOptions" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="角色说明"><a-input v-model:value="form.roleDesc" /></a-form-item>
        <a-row :gutter="20">
          <a-col :span="14">
            <div style="font-weight:500;margin-bottom:8px">功能权限</div>
            <div style="border:1px solid #e2e8f0;border-radius:8px;padding:10px;max-height:380px;overflow:auto">
              <a-tree v-model:checked-keys="checkedMenus" checkable :tree-data="menuTree" :field-names="{ title: 'menuName', key: 'id', children: 'children' }"
                      :default-expand-all="true" />
            </div>
          </a-col>
          <a-col :span="10">
            <div style="font-weight:500;margin-bottom:8px">
              指定组织
              <span style="font-size:12px;color:#94a3b8;font-weight:400">（数据范围=指定组织时生效）</span>
            </div>
            <div style="border:1px solid #e2e8f0;border-radius:8px;padding:10px;max-height:380px;overflow:auto">
              <a-tree v-model:checked-keys="checkedOrgs" checkable :tree-data="orgTree" :field-names="{ title: 'orgName', key: 'id', children: 'children' }"
                      :default-expand-all="true" :disabled="form.dataScope !== 5" />
            </div>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, type FormInstance } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { systemApi } from '../../api'
import { useAuthStore } from '../../store/auth'
import type { MenuNode, OrgNode, RoleRow } from '../../types'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const open = ref(false)
const rows = ref<RoleRow[]>([])
const menuTree = ref<MenuNode[]>([])
const orgTree = ref<OrgNode[]>([])
const checkedMenus = ref<number[]>([])
const checkedOrgs = ref<number[]>([])
const formRef = ref<FormInstance>()
const form = reactive<Record<string, any>>({ id: undefined, roleName: '', roleCode: '', roleDesc: '', dataScope: 4, status: 1, builtIn: 0 })
const scopeOptions = [
  { value: 1, label: '全部数据' },
  { value: 2, label: '本组织及下级' },
  { value: 3, label: '仅本部门' },
  { value: 4, label: '仅本人数据' },
  { value: 5, label: '指定组织' }
]
const rules = {
  roleName: [{ required: true, message: '请填写角色名称' }],
  roleCode: [{ required: true, message: '请填写角色编码' }],
  dataScope: [{ required: true, message: '请选择数据范围' }]
}
const columns = [
  { title: '角色', key: 'role', width: 220 },
  { title: '说明', dataIndex: 'roleDesc', key: 'roleDesc' },
  { title: '数据范围', key: 'scope', width: 140 },
  { title: '账号数', dataIndex: 'userCount', key: 'userCount', width: 100 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 170, align: 'right' as const }
]

function scopeLabel(v: number) { return scopeOptions.find((s) => s.value === v)?.label || '未知' }
async function load() {
  loading.value = true
  try { rows.value = await systemApi.roles() } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
async function loadMeta() {
  const [menus, orgs] = await Promise.all([systemApi.menuTree(), systemApi.orgTree()])
  menuTree.value = menus
  orgTree.value = orgs
}
function openCreate() {
  Object.assign(form, { id: undefined, roleName: '', roleCode: '', roleDesc: '', dataScope: 4, status: 1, builtIn: 0 })
  checkedMenus.value = []
  checkedOrgs.value = []
  open.value = true
}
async function openEdit(row: RoleRow) {
  try {
    const detail = await systemApi.roleDetail(row.id)
    Object.assign(form, detail.role)
    checkedMenus.value = detail.menuIds
    checkedOrgs.value = detail.orgIds
    open.value = true
  } catch (e) { message.error((e as Error).message) }
}
async function save() {
  try { await formRef.value?.validate() } catch { return }
  if (form.dataScope === 5 && !checkedOrgs.value.length) {
    message.warning('数据范围为指定组织时，请至少选择一个组织')
    return
  }
  saving.value = true
  try {
    await systemApi.saveRole({ ...form, menuIds: checkedMenus.value, orgIds: checkedOrgs.value })
    message.success('保存成功')
    open.value = false
    await load()
  } catch (e) { message.error((e as Error).message) } finally { saving.value = false }
}
async function toggleStatus(row: RoleRow, v: boolean) {
  try { await systemApi.roleStatus(row.id, v ? 1 : 0); row.status = v ? 1 : 0; message.success('已更新') } catch (e) { message.error((e as Error).message) }
}
async function remove(row: RoleRow) {
  try { await systemApi.removeRole(row.id); message.success('已删除'); await load() } catch (e) { message.error((e as Error).message) }
}

onMounted(async () => {
  try { await loadMeta() } catch (e) { message.error((e as Error).message) }
  await load()
})
</script>
