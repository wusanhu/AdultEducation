<template>
  <div class="fullscreen-page study-page">
    <!-- Top Bar -->
    <div class="study-topbar">
      <van-icon name="arrow-left" size="20" color="#fff" @click="onBack" />
      <span class="study-title text-ellipsis">{{ courseInfo?.courseName || '课程学习' }}</span>
      <span class="study-timer">{{ formatTime(currentTime) }} / {{ formatTime(duration) }}</span>
    </div>

    <!-- Video Player -->
    <div class="video-wrap" ref="videoContainer">
      <video
        ref="videoRef"
        :src="courseInfo?.videoUrl"
        controls
        controlslist="nodownload noremoteplayback"
        disablepictureinpicture
        playsinline
        webkit-playsinline
        preload="auto"
        class="video-player"
        @play="onPlay"
        @pause="onPause"
        @seeking="onSeeking"
        @seeked="onSeeked"
        @ended="onEnded"
        @timeupdate="onTimeUpdate"
        @loadedmetadata="onLoadedMetadata"
      />
    </div>

    <!-- Progress Info -->
    <div class="study-info">
      <div class="progress-bar-wrap">
        <van-progress
          :percentage="progressPercent"
          :show-pivot="false"
          color="#1989fa"
          stroke-width="4"
          track-color="#333"
        />
      </div>
      <div class="flex-between mt-8">
        <span class="info-text">有效学习：{{ formatTime(effectiveSeconds) }}</span>
        <span class="info-text">切换次数：{{ screenSwitchCount }}</span>
      </div>
    </div>

    <!-- Completion Button -->
    <div v-if="isCompleted" class="study-bottom">
      <van-button type="success" block round @click="onComplete">
        {{ courseInfo?.needExam ? '完成学习，进入考试' : '完成学习' }}
      </van-button>
    </div>

    <!-- Popup Question Dialog -->
    <van-dialog
      v-model:show="showPopup"
      title="学习验证"
      :show-confirm-button="false"
      :close-on-click-overlay="false"
    >
      <div class="popup-content">
        <div class="popup-timer">剩余 {{ popupTimeout }}s</div>
        <p class="popup-question">{{ popupQuestion?.content }}</p>
        <div v-if="popupQuestion?.questionType === 1 || popupQuestion?.questionType === 3" class="popup-options">
          <van-radio-group v-model="popupAnswer">
            <div
              v-for="opt in parsedOptions"
              :key="opt.key"
              class="popup-option"
            >
              <van-radio :name="opt.key">{{ opt.label }}</van-radio>
            </div>
          </van-radio-group>
        </div>
        <div v-if="popupQuestion?.questionType === 2" class="popup-options">
          <van-checkbox-group v-model="popupAnswerMulti">
            <div
              v-for="opt in parsedOptions"
              :key="opt.key"
              class="popup-option"
            >
              <van-checkbox :name="opt.key" shape="square">{{ opt.label }}</van-checkbox>
            </div>
          </van-checkbox-group>
        </div>
        <van-button type="primary" block round size="small" @click="submitPopup" :disabled="popupTimeout <= 0">
          确认
        </van-button>
      </div>
    </van-dialog>

    <!-- Identity Verify Dialog -->
    <van-dialog
      v-model:show="showVerify"
      title="身份核验"
      :show-confirm-button="false"
      :close-on-click-overlay="false"
    >
      <div class="popup-content">
        <p>需要进行身份核验以继续学习</p>
        <van-button type="primary" block round size="small" class="mt-12" @click="doFaceVerify">
          人脸核验（模拟）
        </van-button>
        <van-button plain type="default" block round size="small" class="mt-8" @click="doSmsVerify">
          短信验证码核验
        </van-button>
      </div>
    </van-dialog>

    <!-- SMS Code Dialog -->
    <van-dialog
      v-model:show="showSmsDialog"
      title="输入验证码"
      show-cancel-button
      @confirm="submitSmsCode"
    >
      <div class="popup-content">
        <p class="mb-8">验证码已发送至 {{ userStore.user?.phone }}</p>
        <van-field v-model="smsCode" placeholder="请输入6位验证码" maxlength="6" type="digit" />
      </div>
    </van-dialog>

    <!-- Warning Overlay -->
    <div v-if="showWarning" class="warning-overlay">
      <div class="warning-box">
        <van-icon name="warning-o" size="40" color="#ee0a24" />
        <p class="warning-text">{{ warningText }}</p>
        <van-button type="primary" size="small" round @click="dismissWarning">我知道了</van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Icon as VanIcon, Button as VanButton, Progress as VanProgress,
  Dialog as VanDialog, Field as VanField, RadioGroup as VanRadioGroup,
  Radio as VanRadio, CheckboxGroup as VanCheckboxGroup, Checkbox as VanCheckbox,
  showToast,
} from 'vant'
import { study, pauseStudy, courses, exam as examApi } from '@/api'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const courseId = computed(() => Number(route.params.courseId))
const courseInfo = ref<any>(null)
const videoRef = ref<HTMLVideoElement | null>(null)

// Session
const sessionId = ref('')
const lastHeartbeatTime = ref(0)
const lastPosition = ref(0)
const effectiveSeconds = ref(0)
const currentTime = ref(0)
const duration = ref(0)
const isPlaying = ref(false)
const isCompleted = ref(false)
const progressPercent = ref(0)
const screenSwitchCount = ref(0)
const screenSwitchAccum = ref(0)
let switchTimer: ReturnType<typeof setInterval> | null = null
let hiddenSince: number | null = null

// Popup
const showPopup = ref(false)
const popupQuestion = ref<any>(null)
const popupAnswer = ref('')
const popupAnswerMulti = ref<string[]>([])
const popupTimeout = ref(60)
let popupTimer: ReturnType<typeof setInterval> | null = null
let popupTriggeredAt = -1
const popupTimeoutCount = ref(0)

// Verify
const showVerify = ref(false)
const showSmsDialog = ref(false)
const smsCode = ref('')
const verifyLogId = ref(0)

// Warning
const showWarning = ref(false)
const warningText = ref('')

// Seek protection
const seekFrom = ref(0)
const resumePosition = ref(0)

const parsedOptions = computed(() => {
  if (!popupQuestion.value?.optionsJson) return []
  try {
    const opts = JSON.parse(popupQuestion.value.optionsJson)
    return Object.entries(opts).map(([key, label]) => ({ key, label: label as string }))
  } catch {
    return []
  }
})

function formatTime(sec: number) {
  const s = Math.floor(sec)
  const m = Math.floor(s / 60)
  const ss = s % 60
  return `${m.toString().padStart(2, '0')}:${ss.toString().padStart(2, '0')}`
}

function onBack() {
  if (sessionId.value) {
    pauseStudy(courseId.value).catch(() => {})
  }
  router.back()
}

function onLoadedMetadata() {
  if (videoRef.value) {
    duration.value = videoRef.value.duration || courseInfo.value?.videoDuration || 0
    // Resume from saved position
    const savedPos = resumePosition.value || courseInfo.value?.lastPosition || 0
    if (savedPos > 0 && savedPos < duration.value) {
      videoRef.value.currentTime = savedPos
      currentTime.value = savedPos
      lastPosition.value = savedPos
    }
  }
}

function onPlay() {
  isPlaying.value = true
  startHeartbeat()
}

function onPause() {
  isPlaying.value = false
  sendHeartbeat()
}

function onSeeking() {
  if (videoRef.value) {
    seekFrom.value = videoRef.value.currentTime
  }
}

function onSeeked() {
  if (!videoRef.value) return
  const newPos = videoRef.value.currentTime
  const diff = newPos - seekFrom.value
  // Anti-seek: reject jumps > 5s forward
  if (diff > 5) {
    videoRef.value.currentTime = seekFrom.value
    showToast('不允许快进超过5秒')
  }
}

function onTimeUpdate() {
  if (!videoRef.value) return
  currentTime.value = videoRef.value.currentTime
  const pct = duration.value > 0 ? (currentTime.value / duration.value) * 100 : 0
  progressPercent.value = Math.min(Math.round(pct), 100)

  // Check popup trigger (300-600s interval)
  checkPopupTrigger()

  // Check screen switch threshold (30s)
  if (screenSwitchAccum.value >= 30) {
    showWarningMsg('切换屏幕累计超过30秒，请注意')
    screenSwitchAccum.value = 0
  }
}

function onEnded() {
  isPlaying.value = false
  isCompleted.value = true
  sendHeartbeat()
  showToast('恭喜完成视频学习！')
}

// Heartbeat every 30s
function startHeartbeat() {
  stopHeartbeat()
  lastHeartbeatTime.value = Date.now()
  switchTimer = setInterval(() => {
    sendHeartbeat()
  }, 30000)
}

function stopHeartbeat() {
  if (switchTimer) {
    clearInterval(switchTimer)
    switchTimer = null
  }
}

async function sendHeartbeat() {
  if (!sessionId.value) return
  const now = Date.now()
  const delta = Math.floor((now - lastHeartbeatTime.value) / 1000)
  lastHeartbeatTime.value = now
  if (isPlaying.value) {
    effectiveSeconds.value += delta
  }

  try {
    await study.heartbeat({
      courseId: courseId.value,
      sessionId: sessionId.value,
      deltaSeconds: delta,
      progress: duration.value > 0 ? (currentTime.value / duration.value) * 100 : 0,
      position: Math.floor(currentTime.value),
      inForeground: document.hidden ? 0 : 1,
      screenSwitch: screenSwitchCount.value,
      screenSwitchDuration: screenSwitchAccum.value,
    })
  } catch {
    // ignore heartbeat errors
  }
}

// Popup question trigger
function checkPopupTrigger() {
  if (showPopup.value || popupTriggeredAt >= 0) return
  const elapsed = effectiveSeconds.value
  // Random trigger between 300-600s
  if (elapsed >= 300 && elapsed <= 600) {
    triggerPopup()
  }
}

async function triggerPopup() {
  popupTriggeredAt = effectiveSeconds.value
  // Pause video
  if (videoRef.value) videoRef.value.pause()

  try {
    const state = await study.state(courseId.value)
    if (state?.popupQuestion) {
      popupQuestion.value = state.popupQuestion
    } else {
      // Generate a mock question
      popupQuestion.value = {
        id: 0,
        questionType: 3,
        content: '请确认您正在认真学习本课程',
        optionsJson: JSON.stringify({ A: '是，我正在学习', B: '否' }),
        answer: 'A',
      }
    }
  } catch {
    popupQuestion.value = {
      id: 0,
      questionType: 3,
      content: '请确认您正在认真学习本课程',
      optionsJson: JSON.stringify({ A: '是，我正在学习', B: '否' }),
      answer: 'A',
    }
  }

  popupAnswer.value = ''
  popupAnswerMulti.value = []
  popupTimeout.value = 60
  showPopup.value = true

  popupTimer = setInterval(() => {
    popupTimeout.value--
    if (popupTimeout.value <= 0) {
      clearInterval(popupTimer!)
      popupTimer = null
      onPopupTimeout()
    }
  }, 1000)
}

async function submitPopup() {
  if (popupTimer) {
    clearInterval(popupTimer)
    popupTimer = null
  }
  showPopup.value = false

  try {
    await study.popupConfirm(courseId.value, sessionId.value)
    // Resume video
    if (videoRef.value) videoRef.value.play()
  } catch {
    showToast('验证失败，请继续学习')
  }
  popupTriggeredAt = -1
}

function onPopupTimeout() {
  showPopup.value = false
  popupTimeoutCount.value++
  popupTriggeredAt = -1

  if (popupTimeoutCount.value >= 3) {
    showWarningMsg('超时次数过多，学习记录已标记异常')
    // Pause and require identity verification
    if (videoRef.value) videoRef.value.pause()
    showVerify.value = true
  } else {
    showWarningMsg(`弹窗验证超时（${popupTimeoutCount.value}/3），请注意学习`)
    if (videoRef.value) videoRef.value.play()
  }
}

// Identity verification
async function doFaceVerify() {
  try {
    const res = await study.verifyStart(courseId.value, 'face')
    verifyLogId.value = res?.logId || res?.id || 0
    await study.verifyFace(verifyLogId.value, 'mock_base64')
    showVerify.value = false
    showToast('人脸核验通过')
    if (videoRef.value) videoRef.value.play()
  } catch {
    showToast('核验失败，请重试')
  }
}

async function doSmsVerify() {
  try {
    const res = await study.verifyStart(courseId.value, 'sms')
    verifyLogId.value = res?.logId || res?.id || 0
    await study.verifySmsSend(verifyLogId.value)
    showVerify.value = false
    showSmsDialog.value = true
  } catch {
    showToast('发送验证码失败')
  }
}

async function submitSmsCode() {
  try {
    await study.verifySmsSubmit(verifyLogId.value, smsCode.value)
    showSmsDialog.value = false
    smsCode.value = ''
    showToast('短信核验通过')
    if (videoRef.value) videoRef.value.play()
  } catch {
    showToast('验证码错误')
  }
}

function showWarningMsg(msg: string) {
  warningText.value = msg
  showWarning.value = true
}

function dismissWarning() {
  showWarning.value = false
}

function onComplete() {
  if (courseInfo.value?.needExam) {
    router.push(`/exam/${courseId.value}`)
  } else {
    showToast('学习完成！')
    router.back()
  }
}

// Visibility change handler
function onVisibilityChange() {
  if (document.hidden) {
    hiddenSince = Date.now()
    if (isPlaying.value) {
      videoRef.value?.pause()
    }
    screenSwitchCount.value++
    sendHeartbeat()
  } else {
    if (hiddenSince) {
      const elapsed = Math.floor((Date.now() - hiddenSince) / 1000)
      screenSwitchAccum.value += elapsed
      hiddenSince = null
    }
  }
}

onMounted(async () => {
  try {
    // Load course info
    courseInfo.value = await courses.detail(courseId.value)

    // Start study session
    const res = await study.start(courseId.value)
    sessionId.value = res?.sessionId || res?.session_id || ''

    // Restore progress from server
    if (res?.validDuration) {
      effectiveSeconds.value = res.validDuration
    }
    if (res?.screenSwitchCount) {
      screenSwitchCount.value = res.screenSwitchCount
    }
    if (res?.progress) {
      progressPercent.value = Math.min(100, Math.round(Number(res.progress)))
    }

    // Set up video resume
    resumePosition.value = res?.lastPosition || res?.position || 0
    if (videoRef.value && resumePosition.value > 0 && videoRef.value.readyState >= 1) {
      videoRef.value.currentTime = resumePosition.value
      currentTime.value = resumePosition.value
      lastPosition.value = resumePosition.value
    }

    // Listen for visibility changes
    document.addEventListener('visibilitychange', onVisibilityChange)
  } catch (e: any) {
    if (!e?.response) {
      showToast('网络异常，请稍后重试')
    }
  }
})

onBeforeUnmount(() => {
  stopHeartbeat()
  if (popupTimer) clearInterval(popupTimer)
  if (sessionId.value) {
    pauseStudy(courseId.value).catch(() => {})
  }
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.study-page {
  display: flex;
  flex-direction: column;
}
.study-topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #000;
  color: #fff;
  z-index: 10;
}
.study-title {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
}
.study-timer {
  font-size: 13px;
  color: #c8c9cc;
  font-variant-numeric: tabular-nums;
}
.video-wrap {
  width: 100%;
  background: #000;
  display: flex;
  justify-content: center;
  align-items: center;
}
.video-player {
  width: 100%;
  max-height: 56vw;
  outline: none;
}
.study-info {
  padding: 16px;
  background: #1a1a1a;
  color: #fff;
}
.progress-bar-wrap {
  padding: 4px 0;
}
.info-text {
  font-size: 12px;
  color: #969799;
}
.study-bottom {
  padding: 16px;
  background: #1a1a1a;
}
.popup-content {
  padding: 16px 20px 20px;
}
.popup-timer {
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  color: #ee0a24;
  margin-bottom: 12px;
}
.popup-question {
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 16px;
}
.popup-options {
  margin-bottom: 16px;
}
.popup-option {
  padding: 8px 0;
}
.warning-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}
.warning-box {
  background: #fff;
  border-radius: 12px;
  padding: 32px 24px;
  text-align: center;
  max-width: 280px;
}
.warning-text {
  font-size: 15px;
  margin: 16px 0;
  color: #323233;
  line-height: 1.5;
}
</style>
