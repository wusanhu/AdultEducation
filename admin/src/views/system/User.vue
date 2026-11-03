<template>
  <div class="page-card">
    <div class="toolbar">
      <a-input v-model:value="query.keyword" placeholder="账号 / 姓名 / 手机号" allow-clear style="width:200px" @press-enter="reload" />
      <a-tree-select v-model:value="query.orgId" :tree-data="orgTree" allow-clear placeholder="所属组织"
                     :field-names="{ label: 'orgName', value: 'id', children: 'children' }" style="width:190px" />
      <a-select v-model:value="query.roleId" placeholder="角色" allow-clear style="width:140px" :options="roleOptions" />
      <a-select v-model:value="query.userType" placeholder="用户类型" allow-clear style="width:130px">
        <a-select-option :value="1">管理员</a-select-option>
        <a-select-option :value="2">教员</a-select-option>
        <a-select-option :value="3">学员</a-select-option>
      </a-select>
      <a-select v-model:value="query.status" placeholder="状态" allow-clear style="width:110px">
        <a-select-option :value="1">正常</a-select-option>
        <a-select-option :value="0">停用</a-select-option>
      </a-select>
      <a-button type="primary" @click="reload">查询</a-button>
      <a-button @click="reset">重置</a-button>
      <div class="spacer" />
      <a-button v-if="auth.hasPerm('system:user:import')" @click="downloadTemplate"><DownloadOutlined /> 导入模板</a-button>
      <a-upload v-if="auth.hasPerm('system:user:import')" :show-upload-list="false" :before-upload="importUsers" accept=".xlsx,.xls">
        <a-button :loading="importing"><UploadOutlined /> 批量导入</a-button>
      </a-upload>
      <a-button v-if="auth.hasPerm('system:user:export')" @click="exportUsers">导出</a-button>
      <a-button v-if="auth.hasPerm('system:user:add')" type="primary" @click="openCreate"><PlusOutlined /> 新增账号</a-button>
    </div>

    <a-table :data-source="rows" :columns="columns" :loading="loading" :pagination="pagination" row-key="id" size="middle" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'user'">
          <div style="font-weight:500">{{ record.realName }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.username }}</div>
        </template>
        <template v-else-if="column.key === 'org'">
          <div>{{ record.orgName || '—' }}</div>
          <div v-if="record.deptName" style="font-size:12px;color:#94a3b8">{{ record.deptName }}</div>
        </template>
        <template v-else-if="column.key === 'role'">
          <a-tag color="blue">{{ record.roleName || '—' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-badge :status="record.status === 1 ? 'success' : 'default'" :text="record.status === 1 ? '正常' : '停用'" />
          <a-tag v-if="record.mustChangePwd" color="orange" style="margin-left:6px">待改密</a-tag>
        </template>
        <template v-else-if="column.key === 'login'">
          <div style="font-size:12px">{{ record.lastLoginTime || '从未登录' }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ record.lastLoginIp || '' }}</div>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a v-if="auth.hasPerm('system:user:edit')" @click="openEdit(record)">编辑</a>
            <a-popconfirm title="重置为随机初始密码并通知用户？" @confirm="resetPwd(record)">
              <a v-if="auth.hasPerm('system:user:reset')">重置密码</a>
            </a-popconfirm>
            <a-popconfirm :title="record.status === 1 ? '确认停用该账号？' : '确认启用该账号？'" @confirm="toggleStatus(record)">
              <a v-if="auth.hasPerm('system:user:edit')">{{ record.status === 1 ? '停用' : '启用' }}</a>
            </a-popconfirm>
            <a-popconfirm title="确认删除该账号？" @confirm="remove(record)">
              <a v-if="auth.hasPerm('system:user:delete')" style="color:#dc2626">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-drawer v-model:open="open" :title="form.id ? '编辑账号' : '新增账号'" width="480" :footer="null">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="账号" name="username"><a-input v-model:value="form.username" :disabled="!!form.id" placeholder="登录账号" /></a-form-item>
        <a-form-item label="姓名" name="realName"><a-input v-model:value="form.realName" /></a-form-item>
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="手机号" name="phone"><a-input v-model:value="form.phone" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="身份证号" name="idCard"><a-input v-model:value="form.idCard" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="角色" name="roleId">
          <a-select v-model:value="form.roleId" show-search option-filter-prop="label" :options="roleOptions" @change="onRoleChange" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="所属组织" name="orgId">
              <a-tree-select v-model:value="form.orgId" :tree-data="orgTree" allow-clear
                             :field-names="{ label: 'orgName', value: 'id', children: 'children' }" tree-default-expand-all />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="部门" name="deptId">
              <a-tree-select v-model:value="form.deptId" :tree-data="orgTree" allow-clear
                             :field-names="{ label: 'orgName', value: 'id', children: 'children' }" tree-default-expand-all />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="用户类型">
              <a-select v-model:value="form.userType">
                <a-select-option :value="1">管理员</a-select-option>
                <a-select-option :value="2">教员</a-select-option>
                <a-select-option :value="3">学员</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态"><a-switch v-model:checked="statusOn" checked-children="正常" un-checked-children="停用" /></a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注"><a-textarea v-model:value="form.remark" :rows="3" /></a-form-item>
        <a-alert v-if="!form.id" type="info" show-icon message="新账号将生成随机初始密码，并通过站内信通知用户，首次登录须修改密码。" />
        <div style="text-align:right;margin-top:20px">
          <a-button style="margin-right:10px" @click="open = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="save">保存</a-button>
        </div>
      </a-form>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal, type FormInstance, type TablePaginationConfig } from 'ant-design-vue'
import { DownloadOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { download, upload } from '../../api/http'
import { systemApi } from '../../api'
import { useAuthStore } from '../../store/auth'
import type { OrgNode, RoleRow, UserRow } from '../../types'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const importing = ref(false)
const open = ref(false)
const rows = ref<UserRow[]>([])
const orgTree = ref<OrgNode[]>([])
const roles = ref<RoleRow[]>([])
const formRef = ref<FormInstance>()
const query = reactive<Record<string, any>>({ pageNum: 1, pageSize: 10, keyword: '', orgId: undefined, roleId: undefined, userType: undefined, status: undefined })
const form = reactive<Record<string, any>>({ id: undefined, username: '', realName: '', phone: '', idCard: '', orgId: undefined, deptId: undefined, roleId: undefined, userType: 3, status: 1, remark: '' })
const statusOn = computed({ get: () => form.status === 1, set: (v) => (form.status = v ? 1 : 0) })
const pagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (n: number) => `共 ${n} 个账号` })
const roleOptions = computed(() => roles.value.map((r) => ({ value: r.id, label: `${r.roleName}（${r.roleCode}）`, code: r.roleCode })))
const rules = {
  username: [{ required: true, message: '请填写账号' }],
  realName: [{ required: true, message: '请填写姓名' }],
  roleId: [{ required: true, message: '请选择角色' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确' }]
}
const columns = [
  { title: '用户', key: 'user', width: 150 },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '组织 / 部门', key: 'org', width: 190 },
  { title: '角色', key: 'role', width: 140 },
  { title: '状态', key: 'status', width: 140, align: 'center' as const },
  { title: '最近登录', key: 'login', width: 170 },
  { title: '操作', key: 'action', width: 230, fixed: 'right', align: 'right' as const }
]

async function load() {
  loading.value = true
  try {
    const page = await systemApi.users(query)
    rows.value = page.records
    pagination.current = page.pageNum
    pagination.total = page.total
  } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}
async function loadMeta() {
  const [tree, roleList] = await Promise.all([systemApi.orgTree(), systemApi.roles()])
  orgTree.value = tree
  roles.value = roleList
}
function reload() { query.pageNum = 1; void load() }
function reset() { Object.assign(query, { pageNum: 1, keyword: '', orgId: undefined, roleId: undefined, userType: undefined, status: undefined }); void load() }
function onTableChange(pag: TablePaginationConfig) { query.pageNum = Number(pag.current || 1); query.pageSize = Number(pag.pageSize || 10); void load() }
function onRoleChange(val: number) {
  const role = roles.value.find((r) => r.id === val)
  if (role?.roleCode === 'student') form.userType = 3
  else if (role?.roleCode === 'teacher') form.userType = 2
  else if (role?.roleCode === 'leader') form.userType = 1
  else if (role?.roleCode === 'super_admin') form.userType = 1
}
function openCreate() {
  Object.assign(form, { id: undefined, username: '', realName: '', phone: '', idCard: '', orgId: undefined, deptId: undefined, roleId: undefined, userType: 3, status: 1, remark: '' })
  open.value = true
}
async function openEdit(row: UserRow) {
  try {
    const detail = await systemApi.userDetail(row.id)
    Object.assign(form, detail || row)
    open.value = true
  } catch (e) { message.error((e as Error).message) }
}
async function save() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    const result = await systemApi.saveUser({ ...form })
    if (!form.id) {
      const pwd = (result as unknown as { initialPassword?: string }).initialPassword
      Modal.success({ title: '账号创建成功', content: `账号：${form.username}，初始密码：${pwd || '已通过站内信发送'}。请提醒用户首次登录修改密码。` })
    } else {
      message.success('保存成功')
    }
    open.value = false
    await load()
  } catch (e) { message.error((e as Error).message) } finally { saving.value = false }
}
async function resetPwd(row: UserRow) {
  try {
    const res = await systemApi.resetPassword(row.id)
    Modal.success({ title: '密码已重置', content: `账号 ${row.username} 的新密码：${res.password}` })
  } catch (e) { message.error((e as Error).message) }
}
async function toggleStatus(row: UserRow) {
  try { await systemApi.userStatus(row.id, row.status === 1 ? 0 : 1); message.success('已更新'); await load() } catch (e) { message.error((e as Error).message) }
}
async function remove(row: UserRow) {
  try { await systemApi.removeUser(row.id); message.success('已删除'); await load() } catch (e) { message.error((e as Error).message) }
}
async function importUsers(file: File) {
  importing.value = true
  try {
    const res = await upload<{ success?: number; fail?: number; errors?: string[] }>('/system/users/import', file)
    Modal.info({ title: '导入完成', content: `成功 ${res.success ?? 0} 条，失败 ${res.fail ?? 0} 条。${res.errors?.length ? '失败原因：' + res.errors.slice(0, 5).join('；') : ''}` })
    await load()
  } catch (e) { message.error((e as Error).message) } finally { importing.value = false }
  return false
}
function downloadTemplate() { void download('/system/users/template', undefined, '账号导入模板.xlsx') }
function exportUsers() { void download('/system/users/export', { keyword: query.keyword, orgId: query.orgId, roleId: query.roleId }, '账号列表.xlsx').catch((e: Error) => message.error(e.message)) }

onMounted(async () => {
  try { await loadMeta() } catch (e) { message.error((e as Error).message) }
  await load()
})
</script>
