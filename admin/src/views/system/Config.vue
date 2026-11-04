<template>
  <div class="page-card">
    <div class="toolbar">
      <a-input v-model:value="keyword" placeholder="搜索参数名称 / 键名" allow-clear style="width:260px" />
      <a-button type="primary" @click="load">刷新</a-button>
      <div class="spacer" />
      <a-button v-if="canEdit" @click="openCreate"><PlusOutlined /> 新增参数</a-button>
      <a-button v-if="canEdit" type="primary" :loading="saving" @click="save"><SaveOutlined /> 保存修改</a-button>
    </div>

    <a-alert type="info" show-icon style="margin-bottom:16px"
             message="平台参数即时生效。防刷参数建议保持默认：随机弹窗 300–600 秒、心跳 30 秒、人脸阈值 0.75。" />

    <a-spin :spinning="loading">
      <div v-for="group in groups" :key="group.name" style="margin-bottom:22px">
        <div style="display:flex;align-items:center;gap:8px;margin-bottom:10px">
          <span style="width:4px;height:16px;border-radius:2px;background:#1f6feb" />
          <span style="font-weight:600;font-size:15px">{{ group.name }}</span>
          <span style="font-size:12px;color:#94a3b8">{{ group.items.length }} 项</span>
        </div>
        <a-row :gutter="[16, 0]">
          <a-col v-for="item in group.items" :key="item.configKey" :xs="24" :md="12" :xl="8">
            <div style="border:1px solid #e2e8f0;border-radius:10px;padding:14px 16px;margin-bottom:16px;background:#fbfdff">
              <div style="display:flex;justify-content:space-between;margin-bottom:6px">
                <span style="font-weight:500">{{ item.configName }}</span>
                <a-tag :color="typeColor(item.configType)">{{ item.configType }}</a-tag>
              </div>
              <div style="font-size:12px;color:#94a3b8;margin-bottom:8px">
                <code>{{ item.configKey }}</code>
                <span v-if="item.remark" style="margin-left:8px">{{ item.remark }}</span>
              </div>
              <a-switch v-if="item.configType === 'boolean'" v-model:checked="item.boolValue" checked-children="开启" un-checked-children="关闭" :disabled="!canEdit" />
              <a-input-number v-else-if="item.configType === 'number'" v-model:value="item.numValue" style="width:100%" :disabled="!canEdit" />
              <a-textarea v-else-if="item.configType === 'textarea' || item.configType === 'json'" v-model:value="item.configValue" :rows="3" :disabled="!canEdit" />
              <div v-else-if="item.configType === 'image'" style="display:flex;gap:10px;align-items:center">
                <a-input v-model:value="item.configValue" placeholder="图片地址 / 上传" :disabled="!canEdit" />
                <a-upload v-if="canEdit" :show-upload-list="false" accept="image/*" :before-upload="(file: File) => uploadImage(file, item)">
                  <a-button size="small">上传</a-button>
                </a-upload>
              </div>
              <a-input v-else v-model:value="item.configValue" :disabled="!canEdit" />
            </div>
          </a-col>
        </a-row>
      </div>
      <a-empty v-if="!groups.length" description="暂无参数" />
    </a-spin>

    <a-modal v-model:open="open" title="新增参数" :width="520" :confirm-loading="saving" @ok="createConfig">
      <a-form layout="vertical">
        <a-form-item label="参数名称" required><a-input v-model:value="newItem.configName" /></a-form-item>
        <a-form-item label="参数键" required><a-input v-model:value="newItem.configKey" placeholder="如：study.popupMin" /></a-form-item>
        <a-form-item label="参数值"><a-input v-model:value="newItem.configValue" /></a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="newItem.configType">
            <a-select-option value="text">text</a-select-option>
            <a-select-option value="number">number</a-select-option>
            <a-select-option value="boolean">boolean</a-select-option>
            <a-select-option value="textarea">textarea</a-select-option>
            <a-select-option value="json">json</a-select-option>
            <a-select-option value="image">image</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="分组 / 说明"><a-input v-model:value="newItem.remark" placeholder="如：学习防刷" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SaveOutlined } from '@ant-design/icons-vue'
import { systemApi } from '../../api'
import { upload } from '../../api/http'
import { useAuthStore } from '../../store/auth'
import type { ConfigRow } from '../../types'

interface Editable extends ConfigRow {
  boolValue?: boolean
  numValue?: number
}

const loading = ref(false)
const saving = ref(false)
const open = ref(false)
const keyword = ref('')
const rows = ref<Editable[]>([])
const auth = useAuthStore()
const canEdit = computed(() => auth.hasPerm('system:config:edit'))
const newItem = reactive<Record<string, any>>({ configName: '', configKey: '', configValue: '', configType: 'text', remark: '' })

const groups = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const map = new Map<string, Editable[]>()
  for (const row of rows.value) {
    if (kw && !row.configName.toLowerCase().includes(kw) && !row.configKey.toLowerCase().includes(kw)) continue
    const key = groupName(row.configKey)
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(row)
  }
  return Array.from(map.entries()).map(([name, items]) => ({ name, items }))
})

function groupName(key: string) {
  if (key.startsWith('platform.')) return '平台信息'
  if (key.startsWith('study.')) return '学习与防刷'
  if (key.startsWith('verify.') || key.startsWith('face.') || key.startsWith('sms.')) return '身份核验'
  if (key.startsWith('cert.')) return '证书设置'
  if (key.startsWith('approval.') || key.startsWith('credit.')) return '审批与学分'
  if (key.startsWith('oss.') || key.startsWith('wechat.')) return '存储与通知'
  return '其他参数'
}

function typeColor(type: string) {
  return type === 'boolean' ? 'green' : type === 'number' ? 'blue' : type === 'image' ? 'purple' : type === 'json' ? 'orange' : 'default'
}

async function load() {
  loading.value = true
  try {
    const list = await systemApi.configs()
    rows.value = list.map((c) => ({
      ...c,
      boolValue: c.configValue === 'true',
      numValue: Number(c.configValue || 0)
    }))
  } catch (e) { message.error((e as Error).message) } finally { loading.value = false }
}

async function save() {
  saving.value = true
  try {
    const payload = rows.value.map((r) => ({
      configKey: r.configKey,
      configName: r.configName,
      configValue: r.configType === 'boolean' ? String(!!r.boolValue) : r.configType === 'number' ? String(r.numValue ?? 0) : r.configValue,
      configType: r.configType,
      remark: r.remark
    }))
    await systemApi.saveConfigs(payload)
    message.success('参数已保存并生效')
    await load()
  } catch (e) { message.error((e as Error).message) } finally { saving.value = false }
}

async function uploadImage(file: File, item: Editable) {
  try {
    const res = await upload<{ url: string }>('/files/upload', file, { type: 'image' })
    item.configValue = res.url
    message.success('图片已上传，保存后生效')
  } catch (e) { message.error((e as Error).message) }
  return false
}

function openCreate() {
  Object.assign(newItem, { configName: '', configKey: '', configValue: '', configType: 'text', remark: '' })
  open.value = true
}
async function createConfig() {
  if (!newItem.configName || !newItem.configKey) { message.warning('请填写参数名称和键'); return }
  saving.value = true
  try {
    await systemApi.saveConfigs([{ ...newItem }])
    message.success('已新增')
    open.value = false
    await load()
  } catch (e) { message.error((e as Error).message) } finally { saving.value = false }
}

onMounted(load)
</script>
