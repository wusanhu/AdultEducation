<template>
  <div class="page-card">
    <div class="toolbar">
      <a-select v-model:value="type" placeholder="全部类别" allow-clear style="width:160px" :options="typeOptions" />
      <a-input v-model:value="keyword" placeholder="模板名称 / 编码" allow-clear style="width:220px" @press-enter="load" />
      <a-button @click="load">查询</a-button>
      <div class="spacer" />
      <a-button v-if="canEdit" type="primary" @click="openCreate"><PlusOutlined /> 新增模板</a-button>
    </div>

    <a-alert
      type="info"
      show-icon
      message="标题、内容与跳转链接均支持 {变量} 占位，发送时按业务数据替换；链接需填写站内路由，例如 /project/{projectId}。停用某条模板后，对应消息将不再发送。"
      style="margin-bottom:16px"
    />

    <a-table :data-source="rows" :columns="columns" :loading="loading" row-key="id" size="middle" :pagination="{ pageSize: 20 }" :scroll="{ x: 1280 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div style="font-weight:500">{{ record.templateName }}</div>
          <code style="font-size:12px;color:#94a3b8">{{ record.templateCode }}</code>
        </template>
        <template v-else-if="column.key === 'type'">
          <a-tag :color="notifyCategory(record.type).color">{{ notifyCategory(record.type).label }}</a-tag>
        </template>
        <template v-else-if="column.key === 'title'">
          <a-tooltip :title="record.title"><span class="clip">{{ record.title }}</span></a-tooltip>
        </template>
        <template v-else-if="column.key === 'content'">
          <a-tooltip :title="record.content"><span class="clip">{{ record.content }}</span></a-tooltip>
        </template>
        <template v-else-if="column.key === 'mobileLink'">
          <code v-if="record.mobileLink" class="link-code">{{ record.mobileLink }}</code>
          <span v-else class="muted">-</span>
        </template>
        <template v-else-if="column.key === 'adminLink'">
          <code v-if="record.adminLink" class="link-code">{{ record.adminLink }}</code>
          <span v-else class="muted">-</span>
        </template>
        <template v-else-if="column.key === 'enabled'">
          <a-switch :checked="record.enabled === 1" size="small" :disabled="!canEdit" @change="(v: boolean) => toggle(record, v)" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space v-if="canEdit" size="small">
            <a @click="openEdit(record)">编辑</a>
            <a-popconfirm title="确认删除该模板？删除后对应消息将不再发送。" @confirm="remove(record)">
              <a style="color:#dc2626">删除</a>
            </a-popconfirm>
          </a-space>
          <span v-else class="muted">-</span>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="open" :title="form.id ? '编辑消息模板' : '新增消息模板'" :width="620" :confirm-loading="saving" @ok="save">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="14">
            <a-form-item label="模板名称" name="templateName"><a-input v-model:value="form.templateName" placeholder="如：学员入班通知" /></a-form-item>
          </a-col>
          <a-col :span="10">
            <a-form-item label="消息类别" name="type"><a-select v-model:value="form.type" :options="typeOptions" /></a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="模板编码" name="templateCode" :extra="form.id ? '编码是业务发送的取用键，编辑时不可修改' : '小写字母开头，可用数字与下划线，需与后端发送编码一致'">
          <a-input v-model:value="form.templateCode" :disabled="!!form.id" placeholder="如：project_join" />
        </a-form-item>
        <a-form-item label="标题模板" name="title"><a-input v-model:value="form.title" placeholder="如：培训班加入通知" /></a-form-item>
        <a-form-item label="内容模板" name="content"><a-textarea v-model:value="form.content" :rows="3" placeholder="如：您已加入培训班《{projectName}》，请进入学习。" /></a-form-item>
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="移动端跳转"><a-input v-model:value="form.mobileLink" placeholder="如：/projects/{projectId}" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="管理端跳转"><a-input v-model:value="form.adminLink" placeholder="如：/project/{projectId}" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="状态"><a-switch v-model:checked="enabledOn" checked-children="启用" un-checked-children="停用" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, type FormInstance } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { noticeTemplateApi } from '../../api'
import { useAuthStore } from '../../store/auth'
import { NOTICE_CATEGORIES, notifyCategory } from '../../constants/notify'
import type { NoticeTemplateRow } from '../../types'

const loading = ref(false)
const saving = ref(false)
const open = ref(false)
const type = ref<string>()
const keyword = ref('')
const rows = ref<NoticeTemplateRow[]>([])
const counts = ref<Record<string, number>>({})
const auth = useAuthStore()
const canEdit = computed(() => auth.hasPerm('system:notice:edit'))
const formRef = ref<FormInstance>()
const form = reactive<Record<string, any>>({
  id: undefined, templateCode: '', templateName: '', type: 'COURSE', title: '', content: '', mobileLink: '', adminLink: '', enabled: 1
})
const enabledOn = computed({ get: () => form.enabled === 1, set: (v) => (form.enabled = v ? 1 : 0) })
const rules = {
  templateName: [{ required: true, message: '请填写模板名称' }],
  templateCode: [
    { required: true, message: '请填写模板编码' },
    { pattern: /^[a-z][a-z0-9_]{1,63}$/, message: '编码需以小写字母开头，仅含小写字母、数字与下划线' }
  ],
  type: [{ required: true, message: '请选择消息类别' }],
  title: [{ required: true, message: '请填写标题模板' }],
  content: [{ required: true, message: '请填写内容模板' }]
}

const typeOptions = computed(() => NOTICE_CATEGORIES.map((c) => ({
  value: c.value,
  label: counts.value[c.value] ? `${c.label}（${counts.value[c.value]}）` : c.label
})))

const columns = [
  { title: '模板', key: 'name', width: 200 },
  { title: '类别', key: 'type', width: 110, align: 'center' as const },
  { title: '标题模板', key: 'title', width: 180 },
  { title: '内容模板', key: 'content', width: 300 },
  { title: '移动端跳转', key: 'mobileLink', width: 190 },
  { title: '管理端跳转', key: 'adminLink', width: 170 },
  { title: '状态', key: 'enabled', width: 90, align: 'center' as const },
  { title: '操作', key: 'action', width: 120, align: 'right' as const }
]

async function load() {
  loading.value = true
  try {
    rows.value = await noticeTemplateApi.list({ type: type.value, keyword: keyword.value.trim() || undefined })
  } catch (e) {
    message.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

async function loadCounts() {
  try { counts.value = await noticeTemplateApi.categories() } catch { counts.value = {} }
}

function openCreate() {
  Object.assign(form, {
    id: undefined, templateCode: '', templateName: '', type: type.value || 'COURSE',
    title: '', content: '', mobileLink: '', adminLink: '', enabled: 1
  })
  open.value = true
}

function openEdit(row: NoticeTemplateRow) {
  Object.assign(form, {
    id: row.id, templateCode: row.templateCode, templateName: row.templateName, type: row.type,
    title: row.title, content: row.content, mobileLink: row.mobileLink || '', adminLink: row.adminLink || '',
    enabled: row.enabled ?? 1
  })
  open.value = true
}

async function save() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  const payload = {
    templateCode: form.templateCode.trim(),
    templateName: form.templateName,
    type: form.type,
    title: form.title,
    content: form.content,
    mobileLink: form.mobileLink || null,
    adminLink: form.adminLink || null,
    enabled: form.enabled
  }
  try {
    if (form.id) await noticeTemplateApi.update(form.id, payload)
    else await noticeTemplateApi.save(payload)
    message.success('保存成功')
    open.value = false
    await Promise.all([load(), loadCounts()])
  } catch (e) {
    message.error((e as Error).message)
  } finally {
    saving.value = false
  }
}

async function toggle(row: NoticeTemplateRow, v: boolean) {
  try {
    await noticeTemplateApi.update(row.id, { ...row, enabled: v ? 1 : 0 })
    row.enabled = v ? 1 : 0
    message.success('已更新')
  } catch (e) {
    message.error((e as Error).message)
  }
}

async function remove(row: NoticeTemplateRow) {
  try {
    await noticeTemplateApi.remove(row.id)
    message.success('已删除')
    await Promise.all([load(), loadCounts()])
  } catch (e) {
    message.error((e as Error).message)
  }
}

onMounted(() => { void load(); void loadCounts() })
</script>

<style scoped>
.clip {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.link-code {
  font-size: 12px;
  color: #475569;
}
</style>
