<template>
  <div class="page-card">
    <div class="toolbar">
      <span style="color:#64748b">菜单与按钮权限：目录仅用于分组，菜单对应前端页面，按钮对应操作权限标识；开启隐藏后，菜单不显示在导航，按钮不显示在页面。</span>
      <div class="spacer" />
      <a-button v-if="auth.hasPerm('system:menu:edit')" type="primary" @click="openCreate()"><PlusOutlined /> 新增菜单</a-button>
    </div>
    <a-table :data-source="tree" :columns="columns" :loading="loading" row-key="id" :pagination="false" size="middle" :default-expand-all-rows="true">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'menuName'">
          <a-tag :color="record.menuType === 1 ? 'blue' : record.menuType === 2 ? 'green' : 'default'">
            {{ record.menuType === 1 ? '目录' : record.menuType === 2 ? '菜单' : '按钮' }}
          </a-tag>
          <span style="margin-left:6px;font-weight:500">{{ record.menuName }}</span>
        </template>
        <template v-else-if="column.key === 'perms'"><code v-if="record.perms">{{ record.perms }}</code><span v-else>—</span></template>
        <template v-else-if="column.key === 'status'">
          <a-switch :checked="record.status === 1" size="small" :disabled="!auth.hasPerm('system:menu:edit')" @change="(v: boolean) => toggleStatus(record, v)" />
        </template>
        <template v-else-if="column.key === 'visible'">
          <a-tag :color="record.hidden === 1 ? 'default' : 'green'">{{ record.hidden === 1 ? '隐藏' : '显示' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'mobile'">
          <a-tag :color="record.mobileVisible === 1 ? 'green' : 'default'">{{ record.mobileVisible === 1 ? '显示' : '隐藏' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space v-if="auth.hasPerm('system:menu:edit')" size="small">
            <a @click="openCreate(record)">新增下级</a>
            <a @click="openEdit(record)">编辑</a>
            <a-popconfirm title="确认删除该菜单？" @confirm="remove(record)">
              <a style="color:#dc2626">删除</a>
            </a-popconfirm>
          </a-space>
          <span v-else class="muted">-</span>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="open" :title="form.id ? '编辑菜单' : '新增菜单'" :width="620" :confirm-loading="saving" @ok="save">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="上级菜单">
              <a-tree-select v-model:value="form.parentId" :tree-data="parentTree" allow-clear placeholder="不选则为顶级"
                             :field-names="{ label: 'menuName', value: 'id', children: 'children' }" tree-default-expand-all />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="类型" name="menuType">
              <a-radio-group v-model:value="form.menuType">
                <a-radio :value="1">目录</a-radio>
                <a-radio :value="2">菜单</a-radio>
                <a-radio :value="3">按钮</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="名称" name="menuName"><a-input v-model:value="form.menuName" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="权限标识"><a-input v-model:value="form.perms" placeholder="如：course:list" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="路由路径"><a-input v-model:value="form.path" placeholder="如：/course/list" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="组件标识"><a-input v-model:value="form.component" placeholder="如：course/list" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8"><a-form-item label="图标"><a-input v-model:value="form.icon" placeholder="如：TeamOutlined" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="排序"><a-input-number v-model:value="form.sort" :min="0" style="width:100%" /></a-form-item></a-col>
          <a-col :span="8">
            <a-form-item label="移动端可见"><a-switch v-model:checked="mobileOn" /></a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8"><a-form-item :label="form.menuType === 3 ? '隐藏按钮' : '隐藏菜单'"><a-switch v-model:checked="hiddenOn" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="状态"><a-switch v-model:checked="statusOn" checked-children="启用" un-checked-children="停用" /></a-form-item></a-col>
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
import { useAuthStore } from '../../store/auth'
import type { MenuNode } from '../../types'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const open = ref(false)
const tree = ref<MenuNode[]>([])
const formRef = ref<FormInstance>()
const form = reactive<Record<string, any>>({ id: undefined, parentId: undefined, menuName: '', menuType: 2, path: '', component: '', icon: '', perms: '', sort: 0, hidden: 0, mobileVisible: 1, status: 1 })
const hiddenOn = computed({ get: () => form.hidden === 1, set: (v) => (form.hidden = v ? 1 : 0) })
const mobileOn = computed({ get: () => form.mobileVisible === 1, set: (v) => (form.mobileVisible = v ? 1 : 0) })
const statusOn = computed({ get: () => form.status === 1, set: (v) => (form.status = v ? 1 : 0) })
const rules = {
  menuName: [{ required: true, message: '请填写菜单名称' }],
  menuType: [{ required: true, message: '请选择类型' }]
}
const columns = [
  { title: '菜单名称', key: 'menuName', width: 280 },
  { title: '路由', dataIndex: 'path', key: 'path', width: 180 },
  { title: '权限标识', key: 'perms', width: 200 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70, align: 'center' as const },
  { title: '导航可见', key: 'visible', width: 90, align: 'center' as const },
  { title: '移动端', key: 'mobile', width: 90, align: 'center' as const },
  { title: '状态', key: 'status', width: 90, align: 'center' as const },
  { title: '操作', key: 'action', width: 190, align: 'right' as const }
]
const parentTree = computed<MenuNode[]>(() => {
  const prune = (nodes: MenuNode[]): MenuNode[] => nodes
    .filter((n) => n.id !== form.id)
    .map((n) => ({ ...n, children: prune(n.children || []) }))
  return prune(tree.value)
})

async function load() {
  loading.value = true
  try { tree.value = await systemApi.menuTree() } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
function openCreate(parent?: MenuNode) {
  Object.assign(form, { id: undefined, parentId: parent?.id, menuName: '', menuType: parent ? (parent.menuType === 1 ? 2 : 3) : 1, path: '', component: '', icon: '', perms: '', sort: 0, hidden: 0, mobileVisible: 1, status: 1 })
  open.value = true
}
function openEdit(row: MenuNode) {
  // 显式赋值，避免弹窗残留上一次编辑的状态
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId || undefined,
    menuName: row.menuName,
    menuType: row.menuType,
    path: row.path ?? '',
    component: row.component ?? '',
    icon: row.icon ?? '',
    perms: row.perms ?? '',
    sort: row.sort ?? 0,
    hidden: row.hidden ?? 0,
    mobileVisible: row.mobileVisible ?? 0,
    status: row.status ?? 1
  })
  open.value = true
}
async function save() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    await systemApi.saveMenu({ ...form })
    message.success('保存成功')
    open.value = false
    await load()
  } catch (e) { message.error((e as Error).message) } finally { saving.value = false }
}
async function toggleStatus(row: MenuNode, v: boolean) {
  try { await systemApi.menuStatus(row.id, v ? 1 : 0); row.status = v ? 1 : 0; message.success('已更新') } catch (e) { message.error((e as Error).message) }
}
async function remove(row: MenuNode) {
  try { await systemApi.removeMenu(row.id); message.success('已删除'); await load() } catch (e) { message.error((e as Error).message) }
}

onMounted(load)
</script>
