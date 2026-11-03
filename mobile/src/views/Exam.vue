<template>
  <div class="page-container">
    <PageHeader title="课后测验" />

    <van-loading v-if="loading" class="loading-center" />

    <template v-if="!loading && paper">
      <!-- Countdown -->
      <div class="card countdown-bar" :class="{ urgent: remaining < 300 }">
        <van-icon name="clock-o" />
        <span>剩余时间</span>
        <b class="num">{{ formatCountdown(remaining) }}</b>
      </div>

      <!-- Questions -->
      <div
        v-for="(q, idx) in questions"
        :key="q.id"
        class="card question-card"
      >
        <div class="q-header">
          <span class="q-num num">{{ idx + 1 }}</span>
          <span class="q-type">{{ typeLabel(q.questionType) }}</span>
          <span class="q-score num">{{ q.score }} 分</span>
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
        <div class="result-stamp seal seal--round" :class="passedResult ? 'stamp-ok' : 'stamp-fail'">
          {{ passedResult ? '通过' : '重修' }}
        </div>
        <p class="result-score num">得分 {{ result?.score }} <span class="result-total">/ {{ result?.totalScore ?? 100 }}</span></p>
        <p v-if="passedResult" class="result-hint">恭喜您通过了考试！</p>
        <p v-else class="result-hint">很遗憾未通过，请复习后重新考试</p>
        <p class="result-hint">及格线 <b class="num">{{ result?.passScore }}</b> 分</p>
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
    // 具体原因已由 http 拦截器 toast（如"请先完成课程视频学习后再参加测验"），这里不再重复提示
    setTimeout(() => router.back(), 900)
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
  font-size: 13px;
  color: var(--text-strong);
  font-weight: 500;
  justify-content: center;
  padding: 12px 16px;
}
.countdown-bar .num {
  font-size: 17px;
  font-weight: 700;
  color: var(--primary);
  letter-spacing: 0.06em;
}
.countdown-bar.urgent {
  border-color: rgba(220, 38, 38, 0.35);
  background: rgba(220, 38, 38, 0.06);
}
.question-card {
  margin-bottom: 0;
  border-bottom: none;
  border-radius: 12px;
  margin-bottom: 10px;
}
.q-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.q-num {
  background: var(--primary);
  color: #fff;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}
.q-type {
  font-size: 12px;
  color: var(--text-sub);
}
.q-score {
  font-size: 12px;
  color: var(--warning);
  margin-left: auto;
}
.q-content {
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-strong);
  margin-bottom: 12px;
}
.option-item {
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
}
.option-item:last-child {
  border-bottom: none;
}
.submit-bar {
  padding: 16px;
  position: sticky;
  bottom: 0;
  background: var(--bg-soft);
}
.result-content {
  padding: 28px 20px;
  text-align: center;
}
.result-stamp {
  width: 72px;
  height: 72px;
  font-size: 20px;
  margin: 0 auto 4px;
  border-width: 2.5px;
}
.stamp-ok {
  color: var(--success);
}
.stamp-fail {
  color: var(--danger);
}
.result-score {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-strong);
  margin: 16px 0 8px;
}
.result-total {
  font-size: 14px;
  color: var(--text-faint);
  font-weight: 400;
}
.result-hint {
  font-size: 13px;
  color: var(--text-sub);
}
.result-hint b {
  color: var(--primary);
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}
</style>
