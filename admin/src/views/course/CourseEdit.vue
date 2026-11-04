<template>
  <div class="course-edit">
    <div class="edit-head">
      <a-button shape="circle" @click="$router.push('/course/list')"><ArrowLeftOutlined /></a-button>
      <div style="min-width:0">
        <div class="page-title" style="margin:0">{{ isNew ? '新建课程' : '编辑课程' }}</div>
        <div class="section-sub">课程上架后将向培训班学员推送课程上新通知</div>
      </div>
      <div style="flex:1" />
      <a-button @click="$router.push('/course/list')">取消</a-button>
      <a-button v-if="!readonly" type="primary" :loading="saving" @click="save">保存课程</a-button>
    </div>

    <a-alert v-if="readonly" type="warning" show-icon message="该课程所属培训班已归档，课程内容仅可查看，不能编辑或保存。" style="margin-bottom:16px" />

    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" :disabled="readonly">
      <a-row :gutter="16">
        <a-col :xs="24" :lg="17" :xl="18">
          <!-- 基本信息 -->
          <div class="page-card" style="margin-bottom:16px">
            <div class="section-head">
              <div>
                <div class="section-title">基本信息</div>
                <div class="section-sub">课程名称、归属培训班与学习要求</div>
              </div>
              <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </div>
            <a-row :gutter="16">
              <a-col :xs="24" :md="16">
                <a-form-item label="课程名称" name="courseName">
                  <a-input v-model:value="form.courseName" placeholder="请输入课程名称" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="8">
                <a-form-item label="归属培训班" name="projectId">
                  <a-select v-model:value="form.projectId" show-search option-filter-prop="label" placeholder="选择培训班" :options="projectOptions" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="课程简介">
              <a-textarea v-model:value="form.intro" :rows="3" placeholder="课程内容、学习目标与适用对象" />
            </a-form-item>
            <a-row :gutter="16">
              <a-col :xs="12" :md="6"><a-form-item label="课程学分" name="credit"><a-input-number v-model:value="form.credit" :min="0.5" :step="0.5" style="width:100%" addon-after="分" /></a-form-item></a-col>
              <a-col :xs="12" :md="6"><a-form-item label="课程学时" name="studyHour"><a-input-number v-model:value="form.studyHour" :min="0.5" :step="0.5" style="width:100%" addon-after="学时" /></a-form-item></a-col>
              <a-col :xs="12" :md="6">
                <a-form-item label="课程类型">
                  <a-select v-model:value="form.isRequired">
                    <a-select-option :value="1">必修课</a-select-option>
                    <a-select-option :value="0">选修课</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="12" :md="6"><a-form-item label="排序"><a-input-number v-model:value="form.sort" :min="0" style="width:100%" /></a-form-item></a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :xs="24" :md="8">
                <a-form-item label="视频进度控制" tooltip="开启后学员可自由拖动视频进度；关闭后移动端将锁定进度，只能顺序观看">
                  <a-switch v-model:checked="form.allowSeek" checked-children="可拖动" un-checked-children="不可拖动" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="8">
                <a-form-item label="需要课后测验"><a-switch v-model:checked="form.needExam" checked-children="需要" un-checked-children="不需要" /></a-form-item>
              </a-col>
              <a-col :xs="24" :md="8">
                <a-form-item label="课程状态">
                  <a-select v-model:value="form.status">
                    <a-select-option :value="0">草稿</a-select-option>
                    <a-select-option :value="1">立即上架</a-select-option>
                    <a-select-option :value="2">下架</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row v-if="form.needExam" :gutter="16">
              <a-col :xs="24" :md="8"><a-form-item label="合格分数线"><a-input-number v-model:value="form.passScore" :min="1" :max="100" style="width:100%" addon-after="分" /></a-form-item></a-col>
              <a-col :xs="24" :md="8"><a-form-item label="重考次数"><a-input-number v-model:value="form.examLimit" :min="0" style="width:100%" addon-after="次（0 不限）" /></a-form-item></a-col>
            </a-row>
          </div>

          <!-- 视频与课件 -->
          <div class="page-card" style="margin-bottom:16px">
            <div class="section-head">
              <div>
                <div class="section-title">视频与课件</div>
                <div class="section-sub">课程视频用于计时学习，课件供学员在线查看或下载</div>
              </div>
              <a-tag>课件 {{ form.attachments.length }} 个</a-tag>
            </div>

            <div class="media-grid">
              <a-form-item label="课程封面" style="margin-bottom:0">
                <div class="cover-box">
                  <img v-if="form.cover" :src="form.cover" alt="封面" class="cover-preview" />
                  <div v-else class="cover-empty">
                    <PictureOutlined style="font-size:26px" />
                    <span>建议 16:9 封面图</span>
                  </div>
                  <div class="media-actions">
                    <a-upload :show-upload-list="false" :before-upload="uploadCover" accept="image/*">
                      <a-button size="small"><UploadOutlined /> {{ form.cover ? '更换封面' : '上传封面' }}</a-button>
                    </a-upload>
                    <a-button v-if="form.cover" size="small" type="text" danger @click="form.cover = ''">移除</a-button>
                  </div>
                </div>
              </a-form-item>

              <a-form-item label="课程视频" name="videoUrl" style="margin-bottom:0">
                <div class="video-box">
                  <template v-if="form.videoUrl">
                    <video :src="form.videoUrl" class="video-preview" controls preload="metadata" />
                    <div class="video-meta">
                      <div class="video-info">
                        <VideoCameraOutlined style="color:#2563eb" />
                        <span class="video-name">{{ videoFileName }}</span>
                        <a-tag color="blue">{{ durationText }}</a-tag>
                      </div>
                      <a-button type="link" size="small" @click="clearVideo">移除视频</a-button>
                    </div>
                  </template>
                  <div v-else class="video-empty">
                    <VideoCameraOutlined style="font-size:30px;color:#93b4f7" />
                    <div class="video-empty-text">尚未上传课程视频</div>
                    <div class="video-empty-hint">支持 mp4 / webm / mov，最大 500MB</div>
                    <a-upload :show-upload-list="false" :before-upload="uploadVideo" accept="video/*">
                      <a-button type="primary" :loading="videoUploading"><UploadOutlined /> 上传视频</a-button>
                    </a-upload>
                  </div>
                  <a-progress v-if="videoUploading" :percent="100" status="active" size="small" style="margin-top:10px" />
                </div>
              </a-form-item>
            </div>

            <div class="attach-head">
              <div>
                <span class="section-title" style="font-size:14px">配套课件</span>
                <span class="section-sub" style="margin-left:8px">支持 PDF / Word / PPT / Excel / TXT，可批量上传</span>
              </div>
              <a-upload :show-upload-list="false" multiple :before-upload="uploadAttachment" accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt">
                <a-button size="small"><PlusOutlined /> 上传课件</a-button>
              </a-upload>
            </div>

            <div v-if="form.attachments.length" class="attach-grid">
              <div v-for="(file, index) in form.attachments" :key="`${file.fileUrl}-${index}`" class="attach-item">
                <span class="attach-badge" :class="attachmentKind(file.fileName)">{{ attachmentExt(file.fileName) }}</span>
                <div class="attach-main">
                  <a :href="file.fileUrl" target="_blank" rel="noopener" class="attach-name" :title="file.fileName">{{ file.fileName }}</a>
                  <div class="attach-meta">{{ attachmentLabel(file.fileName) }} · {{ formatSize(file.fileSize) }}</div>
                </div>
                <a-tooltip title="移除课件">
                  <a-button type="text" danger size="small" @click="form.attachments.splice(index, 1)"><CloseOutlined /></a-button>
                </a-tooltip>
              </div>
              <a-upload :show-upload-list="false" multiple :before-upload="uploadAttachment" accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt">
                <div class="attach-add"><PlusOutlined /> 继续添加</div>
              </a-upload>
            </div>
            <div v-else class="attach-empty">
              <FileAddOutlined style="font-size:22px;color:#cbd5e1" />
              <span>暂无课件，点击右上角上传</span>
            </div>
          </div>

          <!-- 课后题库 -->
          <div class="page-card qb-card">
            <div class="qb-head">
              <div>
                <div class="section-title">
                  课后题库
                  <a-tag v-if="!form.needExam" color="orange" style="margin-left:6px">未开启测验</a-tag>
                </div>
                <div class="section-sub">共 {{ form.questions.length }} 题 · 总分 {{ totalScore }} 分 · 合格线 {{ form.passScore }} 分</div>
              </div>
              <div class="qb-actions">
                <a-tag v-if="!canQuestion" color="default">无题库管理权限</a-tag>
                <a-tooltip v-if="canQuestion" :title="isNew ? '请先保存课程，再导入题库' : ''">
                  <a-button size="small" :disabled="isNew" @click="openImport"><ImportOutlined /> 导入题库</a-button>
                </a-tooltip>
                <a-button v-if="canQuestion" type="primary" size="small" @click="addQuestion"><PlusOutlined /> 添加题目</a-button>
              </div>
            </div>

            <div class="qb-body">
              <div v-if="!form.needExam" class="qb-off-tip">
                <InfoCircleOutlined />
                <span>当前课程未开启课后测验，题库会保存但不会在移动端展示。</span>
                <a v-if="canQuestion" @click="form.needExam = true">立即开启</a>
              </div>
              <a-empty v-if="form.questions.length === 0" description="尚未配置题目，可手动添加或导入 Excel 题库" />
              <div v-for="(q, qIndex) in form.questions" :key="qIndex" class="question-card">
                <div class="q-head">
                  <a-tag color="blue">第 {{ qIndex + 1 }} 题</a-tag>
                  <a-select v-model:value="q.questionType" size="small" style="width:104px" :disabled="!canQuestion" @change="onTypeChange(q)">
                    <a-select-option :value="1">单选题</a-select-option>
                    <a-select-option :value="2">多选题</a-select-option>
                    <a-select-option :value="3">判断题</a-select-option>
                  </a-select>
                  <span style="flex:1" />
                  <span class="q-label">分值</span>
                  <a-input-number v-model:value="q.score" :min="1" size="small" style="width:76px" :disabled="!canQuestion" />
                  <a-tooltip v-if="canQuestion" title="删除题目">
                    <a-button type="text" danger size="small" @click="form.questions.splice(qIndex, 1)"><DeleteOutlined /></a-button>
                  </a-tooltip>
                </div>

                <a-textarea v-model:value="q.content" :rows="2" placeholder="请输入题目内容" style="margin-bottom:10px" :disabled="!canQuestion" />

                <template v-if="q.questionType === 3">
                  <div class="q-judge">
                    <span class="q-label">判断选项</span>
                    <a-tag>正确</a-tag>
                    <a-tag>错误</a-tag>
                  </div>
                </template>
                <template v-else>
                  <div class="q-option-grid">
                    <div v-for="(opt, optIndex) in q.options" :key="opt.key" class="q-option">
                      <span class="opt-key">{{ opt.key }}</span>
                      <a-input v-model:value="opt.text" size="small" :placeholder="`选项 ${opt.key}`" :disabled="!canQuestion" />
                      <a-button v-if="canQuestion && q.options.length > 2" type="text" size="small" danger @click="removeOption(q, optIndex)">
                        <CloseOutlined />
                      </a-button>
                    </div>
                  </div>
                  <a-button v-if="canQuestion" type="link" size="small" style="padding-left:0" :disabled="q.options.length >= 6" @click="addOption(q)">
                    <PlusOutlined /> 添加选项
                  </a-button>
                </template>

                <div class="q-answer">
                  <span class="q-label">正确答案</span>
                  <a-select v-if="q.questionType !== 2" v-model:value="q.answerSingle" size="small" style="width:130px" :disabled="!canQuestion">
                    <a-select-option v-for="opt in q.options" :key="opt.key" :value="opt.key">
                      {{ opt.key === 'true' ? '正确' : opt.key === 'false' ? '错误' : opt.key }}
                    </a-select-option>
                  </a-select>
                  <a-checkbox-group v-else v-model:value="q.answerMulti" :options="q.options.map((o) => ({ label: o.key, value: o.key }))" :disabled="!canQuestion" />
                  <span v-if="q.questionType === 2" class="q-hint">多选题请至少选择两个答案</span>
                </div>
              </div>

              <a-button v-if="canQuestion && form.questions.length" block class="qb-add-bottom" @click="addQuestion"><PlusOutlined /> 添加题目</a-button>
            </div>
          </div>
        </a-col>

        <a-col :xs="24" :lg="7" :xl="6">
          <div class="page-card side-card">
            <div class="section-title">课程概览</div>
            <div class="summary-list">
              <div><span>归属培训班</span><b>{{ projectName }}</b></div>
              <div><span>学分 / 学时</span><b>{{ form.credit }} 分 / {{ form.studyHour }} 学时</b></div>
              <div><span>课程视频</span><b>{{ form.videoUrl ? durationText : '未上传' }}</b></div>
              <div><span>配套课件</span><b>{{ form.attachments.length }} 个</b></div>
              <div><span>课后题目</span><b>{{ form.needExam ? `${form.questions.length} 题 / ${totalScore} 分` : '不需要测验' }}</b></div>
            </div>

            <a-divider />
            <div class="section-title" style="font-size:14px;margin-bottom:10px">身份核验策略</div>
            <a-form-item label="学习身份验证档位">
              <a-select v-model:value="form.verifyGearCode" @change="onGearChange">
                <a-select-option v-for="gear in gears" :key="gear.gearCode" :value="gear.gearCode">
                  {{ gear.gearName }}（{{ gear.gearCode === 0 ? '不核验' : `失败 ${gear.maxFailCount} 次处理` }}）
                </a-select-option>
              </a-select>
            </a-form-item>
            <div v-if="currentGear" class="gear-note">
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
            <a-button v-if="!readonly" type="primary" block style="margin-top:18px" :loading="saving" @click="save">保存课程</a-button>
          </div>
        </a-col>
      </a-row>
    </a-form>

    <a-modal v-model:open="importOpen" title="导入课后题库" :footer="null" :width="580" destroy-on-close>
      <a-alert
        type="info"
        show-icon
        style="margin-bottom:14px"
        :message="`题目将直接写入当前课程题库，并追加到现有 ${form.questions.length} 道题之后`"
      />
      <a-upload-dragger :show-upload-list="false" :before-upload="importQuestions" accept=".xlsx,.xls" :disabled="importing">
        <p class="ant-upload-drag-icon">
          <LoadingOutlined v-if="importing" />
          <InboxOutlined v-else />
        </p>
        <p class="ant-upload-text">{{ importing ? '正在导入，请稍候…' : '点击或拖拽 Excel 文件到此处' }}</p>
        <p class="ant-upload-hint">仅支持 .xlsx / .xls</p>
      </a-upload-dragger>
      <div class="import-help">
        <div class="import-help-head">
          <span>模板列：题目类型、题目内容、选项A-F、正确答案、分值</span>
          <a @click="downloadTemplate">下载导入模板</a>
        </div>
        <ul>
          <li>题目类型：单选题 / 多选题 / 判断题</li>
          <li>正确答案：单选题填 A；多选题填 A,B；判断题填 正确 / 错误</li>
          <li>判断题可不填选项；分值不填默认 10 分</li>
          <li>导入成功后题目会立即出现在下方列表，可继续修改后保存</li>
        </ul>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, type FormInstance } from 'ant-design-vue'
import {
  ArrowLeftOutlined, CloseOutlined, DeleteOutlined, FileAddOutlined, ImportOutlined,
  InboxOutlined, InfoCircleOutlined, LoadingOutlined, PictureOutlined, PlusOutlined, UploadOutlined, VideoCameraOutlined
} from '@ant-design/icons-vue'
import { courseApi, projectApi } from '../../api'
import { upload } from '../../api/http'
import { useAuthStore } from '../../store/auth'
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

const OPTION_KEYS = ['A', 'B', 'C', 'D', 'E', 'F']
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const parsedId = Number(route.params.id)
// course/edit/new 路由不含 :id 参数，params.id 为 undefined，因此以有效数字 id 判定新建，否则会带着 NaN 去请求课程详情
const isNew = !Number.isInteger(parsedId) || parsedId <= 0
const courseId = isNew ? 0 : parsedId
const canQuestion = computed(() => auth.hasPerm('course:question'))
const formRef = ref<FormInstance>()
const saving = ref(false)
const readonly = ref(false)
const videoUploading = ref(false)
const importOpen = ref(false)
const importing = ref(false)
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
const projectOptions = computed(() => projects.value
  .filter((p) => p.status !== 3 || p.id === form.projectId)
  .map((p) => ({ value: p.id, label: p.projectName })))
const projectName = computed(() => projects.value.find((p) => p.id === form.projectId)?.projectName || '未选择')
const currentGear = computed(() => gears.value.find((g) => g.gearCode === form.verifyGearCode))
const totalScore = computed(() => form.questions.reduce((sum, q) => sum + Number(q.score || 0), 0))
const statusText = computed(() => (form.status === 1 ? '已上架' : form.status === 2 ? '已下架' : '草稿'))
const statusColor = computed(() => (form.status === 1 ? 'green' : form.status === 2 ? 'orange' : 'default'))
const videoFileName = computed(() => {
  if (!form.videoUrl) return ''
  const name = form.videoUrl.split('/').pop() || '课程视频'
  try { return decodeURIComponent(name) } catch { return name }
})
const durationText = computed(() => {
  const seconds = Number(form.videoDuration || 0)
  if (!seconds) return '时长未知'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m} 分 ${String(s).padStart(2, '0')} 秒`
})

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
  return { questionType: 1, content: '', options: OPTION_KEYS.slice(0, 4).map((key) => ({ key, text: '' })), answerSingle: 'A', answerMulti: [], score: 20 }
}
function addQuestion() {
  ensureExamEnabled()
  form.questions.push(newQuestion())
  requestAnimationFrame(() => {
    const cards = document.querySelectorAll('.question-card')
    cards[cards.length - 1]?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}
function ensureExamEnabled() {
  if (!form.needExam) {
    form.needExam = true
    message.info('已自动开启课后测验')
  }
}
function onTypeChange(q: QuestionForm) {
  if (q.questionType === 3) {
    q.options = [{ key: 'true', text: '正确' }, { key: 'false', text: '错误' }]
    q.answerSingle = 'true'
    q.answerMulti = []
    return
  }
  if (q.options.some((o) => o.key === 'true' || o.key === 'false')) {
    q.options = OPTION_KEYS.slice(0, 4).map((key) => ({ key, text: '' }))
    q.answerSingle = 'A'
  }
  if (q.questionType === 2) {
    q.answerMulti = q.answerSingle ? [q.answerSingle] : []
    q.answerSingle = ''
  } else {
    if (!q.answerSingle) q.answerSingle = q.answerMulti[0] || q.options[0]?.key || 'A'
    q.answerMulti = []
  }
}
function addOption(q: QuestionForm) {
  if (q.options.length >= OPTION_KEYS.length) return
  q.options.push({ key: OPTION_KEYS[q.options.length], text: '' })
}
function removeOption(q: QuestionForm, index: number) {
  if (q.options.length <= 2) return
  const [removed] = q.options.splice(index, 1)
  q.answerMulti = q.answerMulti.filter((key) => key !== removed.key)
  if (q.answerSingle === removed.key) q.answerSingle = q.options[0]?.key || ''
}
function parseQuestion(q: Question): QuestionForm {
  let options: OptionItem[] = []
  try { options = JSON.parse(q.optionsJson || '[]') as OptionItem[] } catch { options = [] }
  if (!options.length) {
    options = q.questionType === 3
      ? [{ key: 'true', text: '正确' }, { key: 'false', text: '错误' }]
      : OPTION_KEYS.slice(0, 4).map((key) => ({ key, text: '' }))
  }
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

function attachmentExt(name: string) {
  const dot = name.lastIndexOf('.')
  return dot >= 0 ? name.slice(dot + 1).toUpperCase().slice(0, 4) : 'FILE'
}
function attachmentKind(name: string) {
  const ext = attachmentExt(name).toLowerCase()
  if (ext === 'pdf') return 'pdf'
  if (ext === 'doc' || ext === 'docx') return 'word'
  if (ext === 'xls' || ext === 'xlsx' || ext === 'csv') return 'excel'
  if (ext === 'ppt' || ext === 'pptx') return 'ppt'
  if (ext === 'txt' || ext === 'md') return 'text'
  return 'other'
}
function attachmentLabel(name: string) {
  const kind = attachmentKind(name)
  return kind === 'pdf' ? 'PDF 文档' : kind === 'word' ? 'Word 文档' : kind === 'excel' ? 'Excel 表格'
    : kind === 'ppt' ? 'PPT 演示' : kind === 'text' ? '文本文件' : '附件'
}
function formatSize(size?: number) {
  if (!size) return '大小未知'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
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
    form.videoDuration = 0
    probeDuration(result.url)
    message.success('视频上传成功')
  } catch (e) { message.error((e as Error).message) } finally { videoUploading.value = false }
  return false
}
function probeDuration(url: string) {
  const video = document.createElement('video')
  video.preload = 'metadata'
  video.src = url
  video.onloadedmetadata = () => { form.videoDuration = Math.floor(video.duration || 0) }
}
function clearVideo() {
  form.videoUrl = ''
  form.videoDuration = 0
}
async function uploadAttachment(file: File) {
  try {
    const result = await upload<{ url: string; fileName: string; fileType: string; fileSize: number }>('/files/upload', file, { type: 'document' })
    form.attachments.push({ fileName: result.fileName, fileUrl: result.url, fileType: result.fileType, fileSize: result.fileSize })
    message.success(`课件《${result.fileName}》已上传`)
  } catch (e) { message.error((e as Error).message) }
  return false
}

function openImport() {
  if (isNew) {
    message.warning('请先保存课程，再导入题库')
    return
  }
  importOpen.value = true
}
async function importQuestions(file: File) {
  importing.value = true
  try {
    const result = await courseApi.importQuestions(courseId, file)
    form.questions = (result.questions || []).map(parseQuestion)
    ensureExamEnabled()
    importOpen.value = false
    message.success(`成功导入 ${result.imported} 道题目，已写入题库`)
  } catch (e) { message.error((e as Error).message) } finally { importing.value = false }
  return false
}
async function downloadTemplate() {
  try { await courseApi.downloadQuestionTemplate() } catch (e) { message.error((e as Error).message) }
}

async function load() {
  const [projectPage, gearList] = await Promise.all([projectApi.page({ pageNum: 1, pageSize: 200 }), courseApi.gears()])
  projects.value = projectPage.records
  gears.value = gearList
  if (!isNew) {
    const detail = await courseApi.detail(courseId)
    readonly.value = Boolean(detail.projectArchived)
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
    if (form.videoUrl && !form.videoDuration) probeDuration(form.videoUrl)
  }
}

async function save() {
  if (readonly.value) { message.warning('该培训班已归档，课程内容仅可查看'); return }
  try { await formRef.value?.validate() } catch { return }
  if (form.needExam && form.questions.length === 0) { message.warning('需要课后测验的课程请至少配置一道题目'); return }
  for (let i = 0; i < form.questions.length; i++) {
    const q = form.questions[i]
    if (!q.content.trim()) { message.warning(`第 ${i + 1} 题题目内容不能为空`); return }
    if (q.questionType !== 3 && q.options.some((o) => !o.text.trim())) {
      message.warning(`第 ${i + 1} 题选项内容不能为空`); return
    }
    if (q.questionType === 2 && q.answerMulti.length < 2) {
      message.warning(`第 ${i + 1} 题多选题请至少选择两个正确答案`); return
    }
    const answer = q.questionType === 2 ? q.answerMulti.join(',') : q.answerSingle
    if (!answer) { message.warning(`第 ${i + 1} 题请设置正确答案`); return }
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

<style scoped>
.edit-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.4;
}
.section-sub {
  margin-top: 3px;
  font-size: 12px;
  color: #94a3b8;
}

/* 视频与封面 */
.media-grid {
  display: grid;
  grid-template-columns: minmax(180px, 230px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
.cover-box {
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  padding: 8px;
  background: #f8fafc;
}
.cover-preview {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: cover;
  border-radius: 8px;
  background: #eef2f7;
}
.cover-empty {
  aspect-ratio: 16 / 9;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #94a3b8;
  font-size: 12px;
}
.media-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}
.video-box {
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  padding: 10px;
  background: #f8fafc;
}
.video-preview {
  display: block;
  width: 100%;
  max-height: 260px;
  aspect-ratio: 16 / 9;
  border-radius: 8px;
  background: #0f172a;
  object-fit: contain;
}
.video-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
.video-info {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.video-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #334155;
}
.video-empty {
  min-height: 178px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.video-empty-text { font-size: 13px; color: #64748b; font-weight: 500; }
.video-empty-hint { font-size: 12px; color: #94a3b8; margin-bottom: 6px; }

/* 课件 */
.attach-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 18px 0 10px;
  padding-top: 16px;
  border-top: 1px solid #eef2f7;
  flex-wrap: wrap;
}
.attach-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 10px;
}
.attach-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 10px 8px 10px 10px;
  border: 1px solid #e8eef7;
  border-radius: 9px;
  background: #fff;
  transition: border-color .15s ease;
}
.attach-item:hover { border-color: #bfd7ff; }
.attach-badge {
  flex: none;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .3px;
}
.attach-badge.pdf { background: #fef2f2; color: #dc2626; }
.attach-badge.word { background: #eff6ff; color: #2563eb; }
.attach-badge.excel { background: #ecfdf5; color: #059669; }
.attach-badge.ppt { background: #fff7ed; color: #ea580c; }
.attach-badge.text { background: #f8fafc; color: #64748b; }
.attach-badge.other { background: #f5f3ff; color: #7c3aed; }
.attach-main { flex: 1; min-width: 0; }
.attach-name {
  display: block;
  font-size: 13px;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.attach-name:hover { color: #2563eb; }
.attach-meta { margin-top: 2px; font-size: 11px; color: #94a3b8; }
.attach-add {
  min-height: 60px;
  height: 100%;
  border: 1px dashed #cbd5e1;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #64748b;
  font-size: 13px;
  background: #fafcff;
  cursor: pointer;
}
.attach-add:hover { border-color: #2563eb; color: #2563eb; }
.attach-empty {
  min-height: 74px;
  border: 1px dashed #e2e8f0;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #94a3b8;
  font-size: 12px;
  background: #fafcff;
}

/* 题库 */
.qb-card { padding: 0; }
.qb-head {
  position: sticky;
  top: -20px;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 16px 20px;
  background: #fff;
  border-bottom: 1px solid #eef2f7;
  border-radius: 12px 12px 0 0;
}
.qb-body { padding: 16px 20px 20px; }
.qb-off-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 9px 12px;
  border: 1px solid #fde68a;
  background: #fffbeb;
  color: #92400e;
  border-radius: 8px;
  font-size: 12px;
  flex-wrap: wrap;
}
.qb-off-tip a { color: #b45309; font-weight: 600; }
.qb-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.question-card {
  border: 1px solid #e8eef7;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 12px;
}
.q-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.q-label { font-size: 12px; color: #64748b; }
.q-option-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 8px;
  margin-bottom: 4px;
}
.q-option { display: flex; align-items: center; gap: 8px; min-width: 0; }
.opt-key {
  flex: none;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #eef2ff;
  color: #4338ca;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
}
.q-judge {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.q-answer {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #f1f5f9;
}
.q-hint { font-size: 11px; color: #94a3b8; }
.qb-add-bottom { border-style: dashed; }

/* 右侧 */
.side-card { position: sticky; top: -20px; }
.summary-list { margin-top: 12px; }
.summary-list > div {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 0;
  font-size: 12px;
  color: #94a3b8;
}
.summary-list b {
  max-width: 65%;
  font-weight: 500;
  color: #1e293b;
  text-align: right;
  word-break: break-all;
}
.gear-note {
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 9px;
  padding: 12px;
  font-size: 12px;
  color: #075985;
  line-height: 1.8;
}
.import-help {
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid #eef2f7;
  border-radius: 9px;
  background: #fafcff;
  font-size: 12px;
  color: #64748b;
}
.import-help-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
  color: #334155;
  flex-wrap: wrap;
}
.import-help ul { margin: 0; padding-left: 18px; line-height: 1.9; }

@media (max-width: 1200px) {
  .media-grid { grid-template-columns: 1fr; }
}
@media (max-height: 820px) {
  .side-card { position: static; }
}
</style>
