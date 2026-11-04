<template>
  <div class="page-container">
    <PageHeader title="课后测验" />

    <van-loading v-if="loading" class="loading-center" />

    <template v-if="!loading && paper">
      <!-- Countdown -->
      <div class="card countdown-bar">
        <van-icon name="clock-o" />
        <span>剩余时间：{{ formatCountdown(remaining) }}</span>
      </div>

      <!-- Questions -->
      <div
        v-for="(q, idx) in questions"
        :key="q.id"
        class="card question-card"
      >
        <div class="q-header">
          <span class="q-num">{{ idx + 1 }}</span>
          <span class="q-type">{{ typeLabel(q.questionType) }}</span>
          <span class="q-score">{{ q.score }}分</span>
        </div>
        <p class="q-content">{{ q.content }}</p>

        <!-- Single / Judgment -->
        <van-radio-group
          v-if="q.questionType === 1 || q.questionType === 3"
          v-model="answers[q.id]"
        >
          <div v-for="opt in parseOptions(q.optionsJson)" :key="opt.key" class="option-item">
            <van-radio :name="opt.key">{{ opt.label }}</van-radio>
          </div>
        </van-radio-group>

        <!-- Multiple -->
        <van-checkbox-group
          v-if="q.questionType === 2"
          v-model="multiAnswers[q.id]"
          shape="square"
        >
          <div v-for="opt in parseOptions(q.optionsJson)" :key="opt.key" class="option-item">
            <van-checkbox :name="opt.key">{{ opt.label }}</van-checkbox>
          </div>
        </van-checkbox-group>
      </div>

      <!-- Submit -->
      <div class="submit-bar">
        <van-button type="primary" block round :loading="submitting" @click="onSubmit">
          交卷
        </van-button>
      </div>
    </template>

    <!-- Result Dialog -->
    <van-dialog
      v-model:show="showResult"
      :title="passedResult ? '恭喜通过' : '未通过'"
      :show-confirm-button="false"
    >
      <div class="result-content">
        <van-icon
          :name="passedResult ? 'passed' : 'close'"
          :color="passedResult ? '#07c160' : '#ee0a24'"
          size="60"
        />
        <p class="result-score">得分：{{ result?.score }} / {{ result?.totalScore ?? 100 }}</p>
        <p v-if="passedResult" class="result-hint">恭喜您通过了考试！</p>
        <p v-else class="result-hint">很遗憾未通过，请复习后重新考试</p>
        <p class="result-hint">及格线：{{ result?.passScore }} 分</p>
        <van-button type="primary" block round size="small" class="mt-16" @click="onResultClose">
          确定
        </van-button>
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Icon as VanIcon, Button as VanButton, Loading as VanLoading,
  RadioGroup as VanRadioGroup, Radio as VanRadio,
  CheckboxGroup as VanCheckboxGroup, Checkbox as VanCheckbox,
  Dialog as VanDialog, showToast,
} from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import { exam } from '@/api'

const route = useRoute()
const router = useRouter()
const courseId = computed(() => Number(route.params.courseId))

const loading = ref(true)
const submitting = ref(false)
const paper = ref<any>(null)
const questions = ref<any[]>([])
const answers = reactive<Record<number, string>>({})
const multiAnswers = reactive<Record<number, string[]>>({})
const remaining = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const showResult = ref(false)
const result = ref<any>(null)
const passedResult = computed(() => Boolean(result.value?.passed ?? result.value?.isPass))

function typeLabel(t: number) {
  switch (t) {
    case 1: return '单选题'
    case 2: return '多选题'
    case 3: return '判断题'
    default: return '题目'
  }
}

function parseOptions(json: string) {
  try {
    const opts = JSON.parse(json)
    return Object.entries(opts).map(([key, label]) => ({ key, label: label as string }))
  } catch {
    return []
  }
}

function formatCountdown(sec: number) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

function startCountdown() {
  countdownTimer = setInterval(() => {
    remaining.value--
    if (remaining.value <= 0) {
      clearInterval(countdownTimer!)
      countdownTimer = null
      onSubmit()
    }
  }, 1000)
}

async function onSubmit() {
  if (submitting.value) return
  submitting.value = true

  const answerList = questions.value.map((q) => {
    if (q.questionType === 2) {
      return { questionId: q.id, answer: (multiAnswers[q.id] || []).join(',') }
    }
    return { questionId: q.id, answer: answers[q.id] || '' }
  })

  try {
    const res = await exam.submit(courseId.value, answerList)
    result.value = res
    showResult.value = true
  } catch {
    showToast('提交失败')
  } finally {
    submitting.value = false
  }
}

function onResultClose() {
  showResult.value = false
  router.back()
}

onMounted(async () => {
  try {
    const data = await exam.paper(courseId.value)
    paper.value = data
    questions.value = data?.questions || []
    remaining.value = data?.limitSeconds || 1800

    // Initialize answers
    for (const q of questions.value) {
      if (q.questionType === 2) {
        multiAnswers[q.id] = []
      } else {
        answers[q.id] = ''
      }
    }

    startCountdown()
  } catch {
    showToast('加载试卷失败')
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.countdown-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #ee0a24;
  font-weight: 500;
}
.question-card {
  margin-bottom: 0;
  border-bottom: 1px solid #f5f5f5;
}
.q-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.q-num {
  background: #1989fa;
  color: #fff;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}
.q-type {
  font-size: 12px;
  color: #969799;
}
.q-score {
  font-size: 12px;
  color: #ff976a;
  margin-left: auto;
}
.q-content {
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 12px;
}
.option-item {
  padding: 8px 0;
}
.submit-bar {
  padding: 16px;
  position: sticky;
  bottom: 0;
  background: #f7f8fa;
}
.result-content {
  padding: 24px 20px;
  text-align: center;
}
.result-score {
  font-size: 18px;
  font-weight: 600;
  margin: 16px 0 8px;
}
.result-hint {
  font-size: 14px;
  color: #646566;
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}
</style>
