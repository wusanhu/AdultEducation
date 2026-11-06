<template>
  <div>
    <div class="toolbar">
      <div>
        <div style="font-size:18px;font-weight:600">核验档位模板</div>
        <div style="font-size:12px;color:#94a3b8;margin-top:3px">档位为课程属性，预置档位不可删除；课程上架后调整仅对新学习生效。</div>
      </div>
      <div class="spacer" />
      <a-button type="primary" @click="openCreate"><PlusOutlined /> 新增档位</a-button>
    </div>

    <a-row :gutter="[16, 16]">
      <a-col v-for="gear in gears" :key="gear.id" :xs="24" :md="12" :xl="8">
        <div class="page-card" style="height:100%;position:relative">
          <div style="display:flex;align-items:flex-start;gap:12px">
            <div style="width:44px;height:44px;border-radius:12px;background:#eef2ff;display:flex;align-items:center;justify-content:center;color:#4338ca;font-size:17px;font-weight:700">
              {{ gear.gearCode }}
            </div>
            <div style="flex:1">
              <div style="display:flex;align-items:center;gap:8px">
                <span style="font-size:16px;font-weight:600">{{ gear.gearName }}</span>
                <a-tag v-if="gear.builtIn === 1" color="blue">预置</a-tag>
                <a-tag v-if="gear.liveCheck === 1" color="purple">真人活体</a-tag>
                <a-tag v-if="gear.verifyMode === 2" color="cyan">学习验证</a-tag>
                <a-tag :color="gear.status === 1 ? 'green' : 'default'">{{ gear.status === 1 ? '启用' : '停用' }}</a-tag>
              </div>
              <div style="font-size:12px;color:#94a3b8;margin-top:4px;line-height:1.7">{{ gear.description || '暂无说明' }}</div>
            </div>
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:16px;font-size:12px">
            <div style="background:#f8fafc;border-radius:7px;padding:8px 10px"><span class="muted">进度触发</span><div style="font-weight:600;margin-top:2px">{{ displayPoints(gear.triggerPoints) }}</div></div>
            <div style="background:#f8fafc;border-radius:7px;padding:8px 10px"><span class="muted">循环间隔</span><div style="font-weight:600;margin-top:2px">{{ gear.intervalMinute ? `${gear.intervalMinute} 分钟` : '无' }}</div></div>
            <div style="background:#f8fafc;border-radius:7px;padding:8px 10px"><span class="muted">核验超时</span><div style="font-weight:600;margin-top:2px">{{ gear.timeoutSecond }} 秒</div></div>
            <div style="background:#f8fafc;border-radius:7px;padding:8px 10px"><span class="muted">失败上限</span><div style="font-weight:600;margin-top:2px">{{ gear.maxFailCount }} 次</div></div>
          </div>
          <div style="display:flex;align-items:center;justify-content:space-between;margin-top:14px;padding-top:12px;border-top:1px solid #f1f5f9">
            <span style="font-size:12px;color:#64748b">{{ gear.failStrategy === 2 ? '失败后锁定，需人工复核' : '失败可重试，不终止课程' }}</span>
            <a-space>
              <a @click="openEdit(gear)">编辑</a>
              <a-popconfirm v-if="gear.builtIn !== 1" title="确定删除该档位？" @confirm="remove(gear.id)">
                <a style="color:#dc2626">删除</a>
              </a-popconfirm>
            </a-space>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-modal v-model:open="open" :title="form.id ? '编辑核验档位' : '新增核验档位'" :confirm-loading="saving" :width="600" @ok="save">
      <a-form layout="vertical" style="margin-top:10px">
        <a-row :gutter="16">
          <a-col :span="10">
            <a-form-item label="档位编码" required><a-input-number v-model:value="form.gearCode" :min="0" style="width:100%" :disabled="builtIn" /></a-form-item>
          </a-col>
          <a-col :span="14">
            <a-form-item label="档位名称" required><a-input v-model:value="form.gearName" placeholder="如：低频核验" /></a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="触发进度点（JSON 数组，百分比）">
          <a-input v-model:value="form.triggerPoints" placeholder="例如 [30,70]；不验证填 []" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="8"><a-form-item label="循环间隔（分钟）"><a-input-number v-model:value="form.intervalMinute" :min="0" style="width:100%" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="核验超时（秒）"><a-input-number v-model:value="form.timeoutSecond" :min="10" :max="300" style="width:100%" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="最大失败次数"><a-input-number v-model:value="form.maxFailCount" :min="1" :max="10" style="width:100%" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="失败处理策略">
          <a-radio-group v-model:value="form.failStrategy">
            <a-radio :value="1">暂停计时，可反复重试</a-radio>
            <a-radio :value="2">标记异常，需人工复核</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="核验方式">
          <a-radio-group v-model:value="form.verifyMode">
            <a-radio :value="1">人脸核验</a-radio>
            <a-radio :value="2">学习验证（弹窗确认）</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="form.verifyMode === 1">
          <template #label>
            <span>真人活体核验</span>
            <a-tooltip title="开启后，学员触发核验时须完成真人活体检测，并通过百度云与人脸基准照的 1:1 比对（需在学员管理中上传基准照；未上传基准照时仅完成活体检测）。">
              <QuestionCircleOutlined style="margin-left:6px;color:#94a3b8" />
            </a-tooltip>
          </template>
          <a-switch v-model:checked="form.liveCheck" checked-children="开启" un-checked-children="关闭" />
        </a-form-item>
        <a-form-item label="档位说明"><a-textarea v-model:value="form.description" :rows="3" placeholder="说明适用课程与核验要求" /></a-form-item>
        <a-form-item label="状态"><a-switch v-model:checked="form.enabled" checked-children="启用" un-checked-children="停用" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'
import { courseApi } from '../../api'
import type { VerifyGear } from '../../types'

const gears = ref<VerifyGear[]>([])
const open = ref(false)
const saving = ref(false)
const builtIn = ref(false)
const form = reactive({
  id: 0, gearCode: 9, gearName: '', triggerPoints: '[40,80]', intervalMinute: 0,
  timeoutSecond: 60, maxFailCount: 3, failStrategy: 1, liveCheck: false, verifyMode: 1, description: '', enabled: true
})

function displayPoints(points?: string) {
  if (!points || points === '[]') return '无'
  try { return (JSON.parse(points) as number[]).map((n) => `${n}%`).join(' / ') } catch { return points }
}
async function load() {
  try { gears.value = await courseApi.gears() } catch (e) { message.error((e as Error).message) }
}
function reset() {
  Object.assign(form, { id: 0, gearCode: 9, gearName: '', triggerPoints: '[40,80]', intervalMinute: 0, timeoutSecond: 60, maxFailCount: 3, failStrategy: 1, liveCheck: false, verifyMode: 1, description: '', enabled: true })
  builtIn.value = false
}
function openCreate() { reset(); open.value = true }
function openEdit(gear: VerifyGear) {
  Object.assign(form, {
    id: gear.id, gearCode: gear.gearCode, gearName: gear.gearName, triggerPoints: gear.triggerPoints || '[]',
    intervalMinute: gear.intervalMinute, timeoutSecond: gear.timeoutSecond, maxFailCount: gear.maxFailCount,
    failStrategy: gear.failStrategy, liveCheck: gear.liveCheck === 1, verifyMode: gear.verifyMode || 1, description: gear.description || '', enabled: gear.status === 1
  })
  builtIn.value = gear.builtIn === 1
  open.value = true
}
async function save() {
  if (!form.gearName.trim()) { message.warning('请填写档位名称'); return }
  try { JSON.parse(form.triggerPoints || '[]') } catch { message.warning('触发点须为合法 JSON 数组'); return }
  saving.value = true
  try {
    await courseApi.saveGear({
      id: form.id || null, gearCode: form.gearCode, gearName: form.gearName, triggerPoints: form.triggerPoints,
      intervalMinute: form.intervalMinute, timeoutSecond: form.timeoutSecond, maxFailCount: form.maxFailCount,
      failStrategy: form.failStrategy, liveCheck: form.liveCheck ? 1 : 0, verifyMode: form.verifyMode, status: form.enabled ? 1 : 0, description: form.description
    })
    message.success('保存成功')
    open.value = false
    await load()
  } catch (e) { message.error((e as Error).message) } finally { saving.value = false }
}
async function remove(id: number) {
  try { await courseApi.removeGear(id); message.success('已删除'); await load() } catch (e) { message.error((e as Error).message) }
}

onMounted(load)
</script>
