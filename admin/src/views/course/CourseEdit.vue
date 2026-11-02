<template>
  <div>
    <div class="toolbar" style="margin-bottom:16px">
      <a-button shape="circle" @click="$router.push('/course/list')"><ArrowLeftOutlined /></a-button>
      <div>
        <h2 class="page-title" style="margin:0">{{ isNew ? '新建课程' : '编辑课程' }}</h2>
        <div style="font-size:12px;color:#94a3b8">课程上架后将向培训班学员推送课程上新通知</div>
      </div>
      <div class="spacer" />
      <a-button @click="$router.push('/course/list')">取消</a-button>
      <a-button type="primary" :loading="saving" @click="save">保存课程</a-button>
    </div>

    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :lg="16">
          <div class="page-card" style="margin-bottom:16px">
            <div style="font-size:15px;font-weight:600;margin-bottom:16px">基本信息</div>
            <a-row :gutter="16">
              <a-col :span="16">
                <a-form-item label="课程名称" name="courseName"><a-input v-model:value="form.courseName" placeholder="请输入课程名称" /></a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="归属培训班" name="projectId">
                  <a-select v-model:value="form.projectId" show-search option-filter-prop="label" placeholder="选择培训班" :options="projectOptions" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="课程简介"><a-textarea v-model:value="form.intro" :rows="3" placeholder="课程内容、学习目标与适用对象" /></a-form-item>
            <a-row :gutter="16">
              <a-col :span="6"><a-form-item label="课程学分" name="credit"><a-input-number v-model:value="form.credit" :min="0.5" :step="0.5" style="width:100%" addon-after="分" /></a-form-item></a-col>
              <a-col :span="6"><a-form-item label="课程学时" name="studyHour"><a-input-number v-model:value="form.studyHour" :min="0.5" :step="0.5" style="width:100%" addon-after="学时" /></a-form-item></a-col>
              <a-col :span="6"><a-form-item label="课程类型"><a-select v-model:value="form.isRequired"><a-select-option :value="1">必修课</a-select-option><a-select-option :value="0">选修课</a-select-option></a-select></a-form-item></a-col>
              <a-col :span="6"><a-form-item label="排序"><a-input-number v-model:value="form.sort" :min="0" style="width:100%" /></a-form-item></a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="8"><a-form-item label="允许拖拽进度"><a-switch v-model:checked="form.allowSeek" checked-children="允许" un-checked-children="禁止" /></a-form-item></a-col>
              <a-col :span="8"><a-form-item label="需要课后测验"><a-switch v-model:checked="form.needExam" checked-children="需要" un-checked-children="不需要" /></a-form-item></a-col>
              <a-col :span="8"><a-form-item label="课程状态"><a-select v-model:value="form.status"><a-select-option :value="0">草稿</a-select-option><a-select-option :value="1">立即上架</a-select-option><a-select-option :value="2">下架</a-select-option></a-select></a-form-item></a-col>
            </a-row>
            <a-row v-if="form.needExam" :gutter="16">
              <a-col :span="8"><a-form-item label="合格分数线"><a-input-number v-model:value="form.passScore" :min="1" :max="100" style="width:100%" addon-after="分" /></a-form-item></a-col>
              <a-col :span="8"><a-form-item label="重考次数"><a-input-number v-model:value="form.examLimit" :min="0" style="width:100%" addon-after="次（0 不限）" /></a-form-item></a-col>
            </a-row>
          </div>

          <div class="page-card" style="margin-bottom:16px">
            <div style="font-size:15px;font-weight:600;margin-bottom:16px">视频与课件</div>
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="课程封面">
                  <div style="border:1px dashed #cbd5e1;border-radius:10px;padding:8px;text-align:center;background:#f8fafc">
                    <img v-if="form.cover" :src="form.cover" alt="封面" style="width:100%;height:126px;object-fit:cover;border-radius:7px" />
                    <div v-else style="height:126px;display:flex;align-items:center;justify-content:center;color:#94a3b8;font-size:12px">建议 16:9 封面图</div>
                    <a-upload :show-upload-list="false" :before-upload="uploadCover" accept="image/*">
                      <a-button size="small" style="margin-top:8px"><UploadOutlined /> 上传封面</a-button>
                    </a-upload>
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="16">
                <a-form-item label="课程视频" name="videoUrl">
                  <div style="border:1px dashed #cbd5e1;border-radius:10px;padding:18px;background:#f8fafc">
                    <div v-if="form.videoUrl" style="display:flex;align-items:center;gap:12px">
                      <VideoCameraOutlined style="font-size:26px;color:#2563eb" />
                      <div style="flex:1;min-width:0">
                        <div style="font-size:13px;font-weight:500;word-break:break-all">{{ form.videoUrl }}</div>
                        <div style="font-size:12px;color:#94a3b8">视频时长：{{ form.videoDuration }} 秒</div>
                      </div>
                      <a-button size="small" @click="form.videoUrl = ''; form.videoDuration = 0">重新上传</a-button>
                    </div>
                    <div v-else style="display:flex;align-items:center;justify-content:space-between">
                      <div style="color:#94a3b8;font-size:13px">支持 mp4/webm/mov，最大 500MB</div>
                      <a-upload :show-upload-list="false" :before-upload="uploadVideo" accept="video/*">
                        <a-button :loading="videoUploading"><UploadOutlined /> 上传视频</a-button>
                      </a-upload>
                    </div>
                    <a-progress v-if="videoUploading" :percent="100" status="active" size="small" style="margin-top:10px" />
                  </div>
                </a-form-item>
                <a-form-item label="配套课件">
                  <div v-for="(file, index) in form.attachments" :key="index" style="display:flex;align-items:center;gap:8px;padding:7px 0;border-bottom:1px dashed #f1f5f9">
                    <FilePdfOutlined style="color:#dc2626" />
                    <a :href="file.fileUrl" target="_blank" style="flex:1;font-size:13px">{{ file.fileName }}</a>
                    <a style="color:#dc2626" @click="form.attachments.splice(index, 1)">移除</a>
                  </div>
                  <a-upload :show-upload-list="false" :before-upload="uploadAttachment" accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt">
                    <a-button size="small"><PlusOutlined /> 上传课件</a-button>
                  </a-upload>
                </a-form-item>
              </a-col>
            </a-row>
          </div>

          <div v-if="form.needExam" class="page-card" style="margin-bottom:16px">
            <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:14px">
              <div>
                <div style="font-size:15px;font-weight:600">课后题库</div>
                <div style="font-size:12px;color:#94a3b8;margin-top:2px">支持单选、多选、判断题；总分 {{ totalScore }} 分</div>
              </div>
              <a-button type="primary" ghost size="small" @click="addQuestion"><PlusOutlined /> 添加题目</a-button>
            </div>
            <a-empty v-if="form.questions.length === 0" description="尚未配置题目，上架前至少添加一道题" />
            <div v-for="(q, qIndex) in form.questions" :key="qIndex" style="border:1px solid #e8eef7;border-radius:10px;padding:16px;margin-bottom:12px">
              <div style="display:flex;gap:10px;align-items:center;margin-bottom:10px">
                <a-tag color="blue">第 {{ qIndex + 1 }} 题</a-tag>
                <a-select v-model:value="q.questionType" size="small" style="width:110px" @change="onTypeChange(q)">
                  <a-select-option :value="1">单选题</a-select-option>
                  <a-select-option :value="2">多选题</a-select-option>
                  <a-select-option :value="3">判断题</a-select-option>
                </a-select>
                <div style="flex:1" />
                <span style="font-size:12px;color:#94a3b8">分值</span>
                <a-input-number v-model:value="q.score" :min="1" size="small" style="width:78px" />
                <a style="color:#dc2626;font-size:13px" @click="form.questions.splice(qIndex, 1)">删除</a>
              </div>
              <a-textarea v-model:value="q.content" :rows="2" placeholder="请输入题目内容" style="margin-bottom:10px" />
              <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:10px">
                <div v-for="opt in q.options" :key="opt.key" style="display:flex;align-items:center;gap:8px">
                  <span style="width:22px;height:22px;border-radius:50%;background:#eef2ff;color:#4338ca;display:flex;align-items:center;justify-content:center;font-size:11px">{{ opt.key === 'true' ? '对' : opt.key === 'false' ? '错' : opt.key }}</span>
                  <a-input v-model:value="opt.text" size="small" :placeholder="`选项 ${opt.key}`" />
                </div>
              </div>
              <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
                <span style="font-size:12px;color:#64748b">正确答案</span>
                <a-select v-if="q.questionType !== 2" v-model:value="q.answerSingle" size="small" style="width:130px">
                  <a-select-option v-for="opt in q.options" :key="opt.key" :value="opt.key">{{ opt.key === 'true' ? '正确' : opt.key === 'false' ? '错误' : opt.key }}</a-select-option>
                </a-select>
                <a-checkbox-group v-else v-model:value="q.answerMulti" :options="q.options.map((o) => ({ label: o.key, value: o.key }))" />
              </div>
            </div>
          </div>
        </a-col>

        <a-col :xs="24" :lg="8">
          <div class="page-card" style="margin-bottom:16px;position:sticky;top:82px">
            <div style="font-size:15px;font-weight:600;margin-bottom:14px">身份核验策略</div>
            <a-form-item label="学习身份验证档位">
              <a-select v-model:value="form.verifyGearCode" @change="onGearChange">
                <a-select-option v-for="gear in gears" :key="gear.gearCode" :value="gear.gearCode">
                  {{ gear.gearName }}（{{ gear.gearCode === 0 ? '不核验' : `失败 ${gear.maxFailCount} 次处理` }}）
                </a-select-option>
              </a-select>
            </a-form-item>
            <div v-if="currentGear" style="background:#f0f9ff;border:1px solid #bae6fd;border-radius:9px;padding:14px;font-size:12px;color:#075985;line-height:1.8">
              <div style="font-weight:600;margin-bottom:4px">{{ currentGear.gearName }}</div>
              <div>{{ currentGear.description }}</div>
              <div style="margin-top:8px;display:flex;flex-wrap:wrap;gap:6px">
                <a-tag color="cyan">触发点 {{ displayPoints(currentGear.triggerPoints) }}</a-tag>
                <a-tag v-if="currentGear.intervalMinute">每 {{ currentGear.intervalMinute }} 分钟</a-tag>
                <a-tag>超时 {{ currentGear.timeoutSecond }} 秒</a-tag>
              </div>
            </div>
            <a-form-item v-if="form.verifyGearCode === 9" label="自定义触发进度点（JSON 数组）" style="margin-top:14px">
              <a-input v-model:value="form.verifyGearJson" placeholder="例如 [40,80]" />
            </a-form-item>
            <a-divider />
            <div class="markdown-note">
              <p style="margin:0 0 6px">· 档位为课程属性，上架后修改仅对新学习生效</p>
              <p style="margin:0 0 6px">· 核验超时或失败会暂停计时，不计入有效学时</p>
              <p style="margin:0">· 连续失败达到上限将标记学习异常并进入人工复核</p>
            </div>
          </div>
        </a-col>
      </a-row>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, type FormInstance } from 'ant-design-vue'
import { ArrowLeftOutlined, FilePdfOutlined, PlusOutlined, UploadOutlined, VideoCameraOutlined } from '@ant-design/icons-vue'
import { courseApi, projectApi } from '../../api'
import { upload } from '../../api/http'
import type { Attachment, CourseDetail, ProjectRow, Question, VerifyGear } from '../../types'

interface OptionItem { key: string; text: string }
interface QuestionForm {
  questionType: number
  content: string
  options: OptionItem[]
  answerSingle: string
  answerMulti: string[]
  score: number
}
interface CourseForm {
  projectId?: number
  courseName: string
  cover: string
  credit: number
  studyHour: number
  isRequired: number
  needExam: boolean
  passScore: number
  examLimit: number
  videoUrl: string
  videoDuration: number
  intro: string
  status: number
  verifyGearCode: number
  verifyGearJson: string
  allowSeek: boolean
  sort: number
  attachments: Attachment[]
  questions: QuestionForm[]
}

const route = useRoute()
const router = useRouter()
const isNew = route.params.id === 'new'
const courseId = isNew ? 0 : Number(route.params.id)
const formRef = ref<FormInstance>()
const saving = ref(false)
const videoUploading = ref(false)
const projects = ref<ProjectRow[]>([])
const gears = ref<VerifyGear[]>([])
const form = reactive<CourseForm>(emptyForm())
const rules = {
  courseName: [{ required: true, message: '请填写课程名称' }],
  projectId: [{ required: true, message: '请选择培训班' }],
  credit: [{ required: true, message: '请填写课程学分' }],
  studyHour: [{ required: true, message: '请填写课程学时' }],
  videoUrl: [{ required: true, message: '请上传课程视频' }]
}
const projectOptions = computed(() => projects.value.map((p) => ({ value: p.id, label: p.projectName })))
const currentGear = computed(() => gears.value.find((g) => g.gearCode === form.verifyGearCode))
const totalScore = computed(() => form.questions.reduce((sum, q) => sum + Number(q.score || 0), 0))

function emptyForm(): CourseForm {
  return {
    projectId: undefined, courseName: '', cover: '', credit: 1, studyHour: 1, isRequired: 1, needExam: false,
    passScore: 60, examLimit: 0, videoUrl: '', videoDuration: 0, intro: '', status: 0,
    verifyGearCode: 0, verifyGearJson: '', allowSeek: false, sort: 0, attachments: [], questions: []
  }
}

function onGearChange() {
  if (form.verifyGearCode !== 9) form.verifyGearJson = ''
}
function displayPoints(points?: string) {
  if (!points || points === '[]') return '无'
  try {
    const list = JSON.parse(points) as number[]
    return list.map((n) => `${n}%`).join(' / ')
  } catch { return points }
}
function newQuestion(): QuestionForm {
  return { questionType: 1, content: '', options: ['A', 'B', 'C', 'D'].map((key) => ({ key, text: '' })), answerSingle: 'A', answerMulti: [], score: 20 }
}
function addQuestion() { form.questions.push(newQuestion()) }
function onTypeChange(q: QuestionForm) {
  if (q.questionType === 3) {
    q.options = [{ key: 'true', text: '正确' }, { key: 'false', text: '错误' }]
    q.answerSingle = 'true'
  } else {
    q.options = ['A', 'B', 'C', 'D'].map((key) => ({ key, text: '' }))
    q.answerSingle = q.questionType === 2 ? '' : 'A'
    q.answerMulti = []
  }
}
function parseQuestion(q: Question): QuestionForm {
  let options: OptionItem[] = []
  try { options = JSON.parse(q.optionsJson || '[]') as OptionItem[] } catch { options = [] }
  if (!options.length) options = ['A', 'B', 'C', 'D'].map((key) => ({ key, text: '' }))
  const answer = (q.answer || '').split(',').map((s) => s.trim()).filter(Boolean)
  return {
    questionType: q.questionType,
    content: q.content,
    options,
    answerSingle: answer[0] || '',
    answerMulti: q.questionType === 2 ? answer : [],
    score: Number(q.score || 0)
  }
}

async function uploadCover(file: File) {
  try {
    const result = await upload<{ url: string }>('/files/upload', file, { type: 'image' })
    form.cover = result.url
    message.success('封面上传成功')
  } catch (e) { message.error((e as Error).message) }
  return false
}
async function uploadVideo(file: File) {
  videoUploading.value = true
  try {
    const result = await upload<{ url: string }>('/files/upload', file, { type: 'video' })
    form.videoUrl = result.url
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.src = result.url
    video.onloadedmetadata = () => { form.videoDuration = Math.floor(video.duration || 0) }
    message.success('视频上传成功')
  } catch (e) { message.error((e as Error).message) } finally { videoUploading.value = false }
  return false
}
async function uploadAttachment(file: File) {
  try {
    const result = await upload<{ url: string; fileName: string; fileType: string; fileSize: number }>('/files/upload', file, { type: 'document' })
    form.attachments.push({ fileName: result.fileName, fileUrl: result.url, fileType: result.fileType, fileSize: result.fileSize })
    message.success('课件上传成功')
  } catch (e) { message.error((e as Error).message) }
  return false
}

async function load() {
  const [projectPage, gearList] = await Promise.all([projectApi.page({ pageNum: 1, pageSize: 200 }), courseApi.gears()])
  projects.value = projectPage.records
  gears.value = gearList
  if (!isNew) {
    const detail = await courseApi.detail(courseId)
    Object.assign(form, {
      projectId: detail.projectId, courseName: detail.courseName, cover: detail.cover || '', credit: Number(detail.credit),
      studyHour: Number(detail.studyHour), isRequired: detail.isRequired, needExam: detail.needExam === 1,
      passScore: detail.passScore, examLimit: detail.examLimit, videoUrl: detail.videoUrl || '',
      videoDuration: detail.videoDuration || 0, intro: detail.intro || '', status: detail.status,
      verifyGearCode: detail.verifyGearCode, verifyGearJson: detail.verifyGearJson || '',
      allowSeek: detail.allowSeek === 1, sort: detail.sort,
      attachments: (detail.attachments || []).map((a: Attachment) => ({ ...a })),
      questions: (detail.questions || []).map(parseQuestion)
    })
  }
}

async function save() {
  try { await formRef.value?.validate() } catch { return }
  if (form.needExam && form.questions.length === 0) { message.warning('需要课后测验的课程请至少配置一道题目'); return }
  for (const q of form.questions) {
    if (!q.content.trim()) { message.warning('题目内容不能为空'); return }
    const answer = q.questionType === 2 ? q.answerMulti.join(',') : q.answerSingle
    if (!answer) { message.warning('请设置每道题的正确答案'); return }
  }
  saving.value = true
  const payload = {
    projectId: form.projectId,
    courseName: form.courseName,
    cover: form.cover || null,
    credit: form.credit,
    studyHour: form.studyHour,
    isRequired: form.isRequired,
    needExam: form.needExam ? 1 : 0,
    passScore: form.passScore,
    examLimit: form.examLimit,
    videoUrl: form.videoUrl,
    videoDuration: form.videoDuration,
    intro: form.intro,
    status: form.status,
    verifyGearCode: form.verifyGearCode,
    verifyGearJson: form.verifyGearCode === 9 ? form.verifyGearJson || null : null,
    allowSeek: form.allowSeek ? 1 : 0,
    sort: form.sort,
    attachments: form.attachments.map((a) => ({ fileName: a.fileName, fileUrl: a.fileUrl, fileType: a.fileType, fileSize: a.fileSize })),
    questions: form.questions.map((q, index) => ({
      questionType: q.questionType,
      content: q.content,
      optionsJson: JSON.stringify(q.options),
      answer: q.questionType === 2 ? q.answerMulti.join(',') : q.answerSingle,
      score: q.score,
      sort: index + 1
    }))
  }
  try {
    if (isNew) await courseApi.save(payload)
    else await courseApi.update(courseId, payload)
    message.success('课程保存成功')
    router.push('/course/list')
  } catch (e) { message.error((e as Error).message) } finally { saving.value = false }
}

onMounted(async () => {
  try { await load() } catch (e) { message.error((e as Error).message) }
})
</script>
