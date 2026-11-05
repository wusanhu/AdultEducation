<template>
  <div class="page-container">
    <PageHeader title="课后测验" />

    <van-loading v-if="loading" class="loading-center" />

    <template v-if="!loading && paper">
      <!-- Countdown -->
      <div class="card countdown-bar" :class="{ urgent: remaining <= 60 }">
        <span class="countdown-icon"><van-icon name="clock-o" size="16" /></span>
        <span class="countdown-label">剩余时间</span>
        <span class="countdown-time">{{ formatCountdown(remaining) }}</span>
        <span class="countdown-total">共 {{ questions.length }} 题</span>
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
          :color="passedResult ? 'var(--ok)' : 'var(--bad)'"
          size="56"
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
  } catch (e: any) {
    // 展示后端的具体校验原因（如"请先完成课程视频学习后再参加测验"）
    showToast(e?.message || '加载试卷失败')
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
  gap: var(--sp-2);
  padding: 13px var(--sp-4);
  border-color: var(--accent-line);
  background: var(--accent-tint);
  box-shadow: none;
}
.countdown-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 28px;
  height: 28px;
  border: 1px solid var(--accent-line);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.7);
  color: var(--accent-strong);
}
.countdown-label {
  font-size: 12px;
  color: var(--ink-2);
}
.countdown-time {
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
  color: var(--accent-strong);
  font-variant-numeric: tabular-nums;
}
.countdown-total {
  margin-left: auto;
  font-size: 11px;
  color: var(--ink-3);
}
.countdown-bar.urgent {
  border-color: #edcfcd;
  background: var(--bad-tint);
}
.countdown-bar.urgent .countdown-icon,
.countdown-bar.urgent .countdown-time {
  border-color: #edcfcd;
  color: var(--bad);
}

.question-card {
  margin-bottom: var(--sp-3);
  padding: var(--sp-4);
}
.q-header {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  margin-bottom: 10px;
}
.q-num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--brand);
  font-family: var(--font-serif);
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}
.q-type {
  padding: 2px 7px;
  border: 1px solid var(--line);
  border-radius: var(--r-tag);
  background: var(--surface-2);
  font-size: 10px;
  color: var(--ink-3);
}
.q-score {
  margin-left: auto;
  font-family: var(--font-serif);
  font-size: 12px;
  font-weight: 600;
  color: var(--accent-strong);
}
.q-content {
  margin-bottom: var(--sp-3);
  font-size: 15px;
  line-height: 1.7;
  color: var(--ink);
}
.option-item {
  margin-bottom: var(--sp-2);
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--r-chip);
  background: var(--surface-2);
  transition: border-color 0.16s, background-color 0.16s;
}
.option-item:last-child {
  margin-bottom: 0;
}
.option-item:has(.van-radio__icon--checked),
.option-item:has(.van-checkbox__icon--checked) {
  border-color: var(--brand-line);
  background: var(--brand-tint);
}
.option-item :deep(.van-radio),
.option-item :deep(.van-checkbox) {
  width: 100%;
}
.option-item :deep(.van-radio__label),
.option-item :deep(.van-checkbox__label) {
  color: var(--ink-2);
  font-size: 14px;
  line-height: 1.55;
}
.submit-bar {
  position: sticky;
  bottom: 0;
  z-index: 2;
  padding: var(--sp-3) var(--page-x) calc(var(--sp-3) + env(safe-area-inset-bottom));
  background: rgba(244, 242, 236, 0.94);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}
.submit-bar :deep(.van-button) {
  height: 46px;
  font-size: 15px;
  letter-spacing: 0.06em;
}
.result-content {
  padding: var(--sp-5) var(--sp-5) var(--sp-4);
  text-align: center;
}
.result-score {
  margin: 14px 0 8px;
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 700;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
.result-hint {
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-3);
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}
</style>
