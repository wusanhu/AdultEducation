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
        :controls="allowSeek"
        controlslist="nodownload noremoteplayback"
        disablepictureinpicture
        playsinline
        webkit-playsinline
        preload="auto"
        class="video-player"
        @click="onVideoClick"
        @play="onPlay"
        @pause="onPause"
        @seeking="onSeeking"
        @seeked="onSeeked"
        @ended="onEnded"
        @timeupdate="onTimeUpdate"
        @loadedmetadata="onLoadedMetadata"
      />
      <!-- Custom controls shown when progress seeking is locked -->
      <div v-if="courseInfo && !allowSeek" class="lock-badge">
        <van-icon name="lock" size="12" />
        <span>进度已锁定，不可拖动</span>
      </div>
      <div v-if="courseInfo && !allowSeek" class="custom-controls" @click.stop>
        <button class="cc-btn" type="button" @click="togglePlay">
          <van-icon :name="isPlaying ? 'pause' : 'play'" size="20" />
        </button>
        <span class="cc-time">{{ formatTime(currentTime) }} / {{ formatTime(duration) }}</span>
        <div class="cc-track">
          <div class="cc-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <button class="cc-btn" type="button" @click="toggleFullscreen">
          <van-icon name="expand-o" size="18" />
        </button>
      </div>
    </div>

    <!-- Progress Info -->
    <div class="study-info">
      <div class="progress-bar-wrap">
        <van-progress
          :percentage="progressPercent"
          :show-pivot="false"
          color="var(--brand)"
          stroke-width="4"
          track-color="var(--line)"
        />
      </div>
      <div class="flex-between mt-8">
        <span class="info-text">有效学习：{{ formatTime(effectiveSeconds) }}</span>
        <span class="info-text">切换次数：{{ screenSwitchCount }}</span>
      </div>
      <div class="seek-tip" :class="{ locked: !allowSeek }">
        <van-icon :name="allowSeek ? 'exchange' : 'lock'" size="12" />
        <span>{{ allowSeek ? '本课程可自由拖动进度' : '本课程已关闭进度拖动，需顺序观看' }}</span>
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
        <van-button type="primary" block round size="small" @click="submitPopup" :disabled="popupTimeout <= 0 || !popupAnswered">
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
        <p v-if="verifyRemaining > 0" class="verify-countdown">剩余 {{ verifyRemaining }} 秒</p>
        <input
          ref="faceInput"
          type="file"
          accept="image/*"
          capture="user"
          class="face-input"
          @change="onFaceFileChange"
        />
        <van-button type="primary" block round size="small" class="mt-12" :loading="verifying" @click="doFaceVerify">
          人脸核验
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
        <p class="mb-8">验证码已发送至 {{ smsPhone || userStore.user?.phone }}</p>
        <p v-if="smsMockCode" class="mock-code">演示验证码：{{ smsMockCode }}</p>
        <van-field v-model="smsCode" placeholder="请输入6位验证码" maxlength="6" type="digit" />
      </div>
    </van-dialog>

    <!-- Warning Overlay -->
    <div v-if="showWarning" class="warning-overlay">
      <div class="warning-box">
        <van-icon name="warning-o" size="40" color="var(--bad)" />
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
const videoContainer = ref<HTMLElement | null>(null)
// 1 = free seeking, 0 = progress locked
const allowSeek = computed(() => Number(courseInfo.value?.allowSeek) === 1)

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
const faceInput = ref<HTMLInputElement | null>(null)
const verifying = ref(false)
const verifyRemaining = ref(0)
const smsPhone = ref('')
const smsMockCode = ref('')
let verifyTimer: ReturnType<typeof setInterval> | null = null

// Heartbeat
const heartbeatInFlight = ref(false)
const pendingScreenSwitch = ref(0)
const pendingScreenSwitchDuration = ref(0)

// Warning
const showWarning = ref(false)
const warningText = ref('')

// Seek protection
const resumePosition = ref(0)
let suppressSeekGuard = false
let seekingInProgress = false
// Last known continuous playback position, used to detect manual jumps
const anchor = { pos: 0, at: 0, playing: false }

const parsedOptions = computed(() => {
  if (!popupQuestion.value?.optionsJson) return []
  try {
    const opts = JSON.parse(popupQuestion.value.optionsJson)
    return Object.entries(opts).map(([key, label]) => ({ key, label: label as string }))
  } catch {
    return []
  }
})

const popupAnswered = computed(() => {
  const q = popupQuestion.value
  if (!q) return false
  const selected = q.questionType === 2
    ? [...popupAnswerMulti.value].sort().join(',')
    : popupAnswer.value
  if (!selected) return false
  if (!q.answer) return true
  const expected = String(q.answer).toUpperCase().split(/[,，\s]+/).filter(Boolean).sort().join(',')
  return selected.toUpperCase() === expected
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
      applyPosition(savedPos)
    } else {
      refreshAnchor()
    }
  }
}

// Programmatic position updates must not be treated as user seeking
function applyPosition(pos: number) {
  if (!videoRef.value) return
  suppressSeekGuard = true
  videoRef.value.currentTime = pos
  currentTime.value = pos
  lastPosition.value = pos
  refreshAnchor()
  window.setTimeout(() => {
    suppressSeekGuard = false
    refreshAnchor()
  }, 500)
}

function refreshAnchor() {
  const v = videoRef.value
  if (!v) return
  anchor.pos = v.currentTime
  anchor.at = performance.now()
  anchor.playing = !v.paused
}

// Expected position if the video had played continuously from the last anchor
function expectedPosition() {
  if (!anchor.playing) return anchor.pos
  return anchor.pos + (performance.now() - anchor.at) / 1000
}

function togglePlay() {
  const v = videoRef.value
  if (!v) return
  if (v.paused) {
    v.play().catch(() => showToast('播放失败，请稍后重试'))
  } else {
    v.pause()
  }
}

function onVideoClick() {
  // With native controls enabled, the browser handles clicks itself
  if (!allowSeek.value) togglePlay()
}

function toggleFullscreen() {
  const el = videoContainer.value
  const v = videoRef.value as any
  if (!el) return
  const doc = document as any
  if (doc.fullscreenElement || doc.webkitFullscreenElement) {
    (doc.exitFullscreen || doc.webkitExitFullscreen)?.call(doc)
  } else if (el.requestFullscreen || (v && v.webkitEnterFullscreen)) {
    if (el.requestFullscreen) {
      el.requestFullscreen().catch(() => {})
    } else {
      v.webkitEnterFullscreen()
    }
  }
}

function onPlay() {
  isPlaying.value = true
  refreshAnchor()
  startHeartbeat()
}

function onPause() {
  isPlaying.value = false
  refreshAnchor()
  sendHeartbeat()
}

function onSeeking() {
  seekingInProgress = true
}

function onSeeked() {
  seekingInProgress = false
  const v = videoRef.value
  if (!v) return
  if (allowSeek.value || suppressSeekGuard) {
    refreshAnchor()
    return
  }
  const target = Math.min(expectedPosition(), Math.max(0, duration.value - 0.2))
  // Locked courses always snap back to the continuous playback position
  if (Math.abs(v.currentTime - target) > 2) {
    suppressSeekGuard = true
    v.currentTime = target
    currentTime.value = target
    showToast('本课程不允许拖动进度')
    window.setTimeout(() => {
      suppressSeekGuard = false
      refreshAnchor()
    }, 400)
  } else {
    refreshAnchor()
  }
}

function onTimeUpdate() {
  if (!videoRef.value) return
  if (!seekingInProgress) refreshAnchor()
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
  if (!sessionId.value || heartbeatInFlight.value) return
  heartbeatInFlight.value = true
  const now = Date.now()
  const delta = Math.floor((now - lastHeartbeatTime.value) / 1000)
  lastHeartbeatTime.value = now
  if (isPlaying.value) {
    effectiveSeconds.value += delta
  }

  const switchCount = pendingScreenSwitch.value
  const switchDuration = pendingScreenSwitchDuration.value
  pendingScreenSwitch.value = 0
  pendingScreenSwitchDuration.value = 0

  try {
    const res = await study.heartbeat({
      courseId: courseId.value,
      sessionId: sessionId.value,
      deltaSeconds: delta,
      progress: duration.value > 0 ? (currentTime.value / duration.value) * 100 : 0,
      position: Math.floor(currentTime.value),
      inForeground: document.hidden ? 0 : 1,
      screenSwitch: switchCount > 0 ? 1 : 0,
      screenSwitchDuration: switchDuration,
    })
    applyHeartbeatState(res)
  } catch {
    // ignore heartbeat errors
  } finally {
    heartbeatInFlight.value = false
  }
}

function applyHeartbeatState(res: any) {
  if (!res) return
  if (res.sessionId) sessionId.value = res.sessionId
  if (res.validDuration != null) effectiveSeconds.value = Number(res.validDuration)
  if (res.progress != null) progressPercent.value = Math.min(100, Math.round(Number(res.progress)))
  if (res.screenSwitchCount != null) screenSwitchCount.value = Number(res.screenSwitchCount)

  if (res.locked) {
    videoRef.value?.pause()
    stopHeartbeat()
    showWarningMsg(res.abnormalReason || res.pauseReason || '学习记录已标记异常，请联系教员复核')
    return
  }
  if (res.completed) {
    isCompleted.value = true
    isPlaying.value = false
    videoRef.value?.pause()
    stopHeartbeat()
    showToast('学习完成')
    return
  }
  if (res.popup && !showPopup.value) {
    presentPopup(Number(res.popupRemaining || res.popupTimeout || 60))
  }
  if (res.needVerify && res.verify?.logId) {
    openVerify(Number(res.verify.logId), Number(res.verify.remainingSeconds || 60))
  }
  if (res.paused && res.pauseReason) {
    videoRef.value?.pause()
    isPlaying.value = false
    if (!showVerify.value && !showPopup.value && String(res.pauseReason).indexOf('完成') < 0) {
      showWarningMsg(res.pauseReason)
    }
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
      popupQuestion.value = mockPopupQuestion()
    }
  } catch {
    popupQuestion.value = mockPopupQuestion()
  }
  presentPopup(60)
}

function mockPopupQuestion() {
  return {
    id: 0,
    questionType: 3,
    content: '请确认您正在认真学习本课程',
    optionsJson: JSON.stringify({ A: '是，我正在学习', B: '否' }),
    answer: 'A',
  }
}

function presentPopup(seconds: number) {
  if (!popupQuestion.value) {
    popupQuestion.value = mockPopupQuestion()
  }
  if (popupTimer) {
    clearInterval(popupTimer)
    popupTimer = null
  }
  popupAnswer.value = ''
  popupAnswerMulti.value = []
  popupTimeout.value = Math.max(1, Math.floor(seconds))
  showPopup.value = true
  if (videoRef.value) videoRef.value.pause()

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
  if (!popupAnswered.value) {
    showToast('请选择正确答案后再确认')
    return
  }
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
function openVerify(logId: number, remainingSeconds = 60) {
  if (!logId) return
  verifyLogId.value = logId
  verifyRemaining.value = Math.max(1, Math.floor(remainingSeconds))
  showVerify.value = true
  if (verifyTimer) clearInterval(verifyTimer)
  verifyTimer = setInterval(() => {
    verifyRemaining.value--
    if (verifyRemaining.value <= 0) {
      clearInterval(verifyTimer!)
      verifyTimer = null
      showVerify.value = false
      showWarningMsg('身份核验超时，学习计时已暂停')
      videoRef.value?.pause()
      sendHeartbeat()
    }
  }, 1000)
}

function closeVerify() {
  showVerify.value = false
  if (verifyTimer) {
    clearInterval(verifyTimer)
    verifyTimer = null
  }
  verifyRemaining.value = 0
}

async function doFaceVerify() {
  if (!verifyLogId.value) {
    try {
      const res = await study.verifyStart(courseId.value, 'manual')
      verifyLogId.value = Number(res?.logId || 0)
      if (res?.remainingSeconds) verifyRemaining.value = Number(res.remainingSeconds)
      showVerify.value = true
    } catch {
      showToast('无法开始身份核验')
      return
    }
  }
  faceInput.value?.click()
}

async function onFaceFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !verifyLogId.value) return
  verifying.value = true
  try {
    const base64 = await compressImage(file)
    const res = await study.verifyFace(verifyLogId.value, base64)
    handleVerifyResult(res)
  } catch {
    showToast('人脸核验失败，请重试')
  } finally {
    verifying.value = false
  }
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })
}

async function compressImage(file: File): Promise<string> {
  const raw = await readFileAsDataUrl(file)
  const img = new Image()
  await new Promise<void>((resolve, reject) => {
    img.onload = () => resolve()
    img.onerror = () => reject(new Error('图片读取失败'))
    img.src = raw
  })
  const maxSide = 640
  const scale = Math.min(1, maxSide / Math.max(img.width, img.height))
  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(img.width * scale))
  canvas.height = Math.max(1, Math.round(img.height * scale))
  const ctx = canvas.getContext('2d')
  if (!ctx) return raw
  ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
  return canvas.toDataURL('image/jpeg', 0.82)
}

function handleVerifyResult(res: any) {
  if (res?.passed) {
    closeVerify()
    showToast('身份核验通过')
    if (videoRef.value) videoRef.value.play()
    return
  }
  showToast(res?.message || '核验未通过，请重试')
  if (res?.locked) {
    closeVerify()
    showWarningMsg('核验失败次数过多，学习记录已标记异常')
    videoRef.value?.pause()
    stopHeartbeat()
    return
  }
  if (res?.timeout) {
    closeVerify()
    showWarningMsg('身份核验已超时')
  }
}

async function doSmsVerify() {
  try {
    if (!verifyLogId.value) {
      const startRes = await study.verifyStart(courseId.value, 'manual')
      verifyLogId.value = Number(startRes?.logId || 0)
    }
    if (!verifyLogId.value) {
      showToast('无法开始身份核验')
      return
    }
    const res = await study.verifySmsSend(verifyLogId.value)
    smsPhone.value = res?.phone || userStore.user?.phone || ''
    smsMockCode.value = res?.mockCode || ''
    if (res?.mockCode) {
      smsCode.value = res.mockCode
    }
    showVerify.value = false
    showSmsDialog.value = true
    showToast(res?.mockCode ? `演示验证码：${res.mockCode}` : '验证码已发送')
  } catch {
    showToast('发送验证码失败')
  }
}

async function submitSmsCode() {
  try {
    const res = await study.verifySmsSubmit(verifyLogId.value, smsCode.value)
    handleVerifyResult(res)
    if (res?.passed) {
      showSmsDialog.value = false
      smsCode.value = ''
      smsMockCode.value = ''
    }
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
    pendingScreenSwitch.value++
    sendHeartbeat()
  } else {
    if (hiddenSince) {
      const elapsed = Math.floor((Date.now() - hiddenSince) / 1000)
      screenSwitchAccum.value += elapsed
      pendingScreenSwitchDuration.value += elapsed
      hiddenSince = null
      // 后台停留时间不计入有效学时，回到前台立即回传切屏时长
      lastHeartbeatTime.value = Date.now()
      sendHeartbeat()
    }
  }
}

async function initStudy() {
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
      applyPosition(resumePosition.value)
    }
    if (res?.status === 2) {
      isCompleted.value = true
    }
    if (res?.resumeVerifyId) {
      // 异常解锁后重新进入，必须先完成一次核验
      openVerify(Number(res.resumeVerifyId), 60)
    } else if (res?.locked) {
      showWarningMsg(res.abnormalReason || '学习记录已标记异常，请联系教员复核')
    }
  } catch (e: any) {
    if (!e?.response) {
      showToast('网络异常，请稍后重试')
    }
  }
}

onMounted(() => {
  initStudy()
  // Listen for visibility changes
  document.addEventListener('visibilitychange', onVisibilityChange)
})

// Reload when navigating directly from one course to another
watch(courseId, async (id, oldId) => {
  if (!id || id === oldId) return
  stopHeartbeat()
  if (oldId && sessionId.value) {
    pauseStudy(Number(oldId)).catch(() => {})
  }
  videoRef.value?.pause()
  sessionId.value = ''
  courseInfo.value = null
  currentTime.value = 0
  duration.value = 0
  effectiveSeconds.value = 0
  progressPercent.value = 0
  screenSwitchCount.value = 0
  screenSwitchAccum.value = 0
  resumePosition.value = 0
  isCompleted.value = false
  await initStudy()
})

onBeforeUnmount(() => {
  stopHeartbeat()
  if (popupTimer) clearInterval(popupTimer)
  if (verifyTimer) clearInterval(verifyTimer)
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
  background: var(--bg);
}
.study-topbar {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  padding: var(--sp-3) var(--page-x);
  background: #000;
  color: #fff;
}
.study-title {
  flex: 1;
  min-width: 0;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.2px;
}
.study-timer {
  flex: none;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.72);
  font-variant-numeric: tabular-nums;
}
.video-wrap {
  width: 100%;
  background: #000;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}
.video-player {
  width: 100%;
  max-height: 56vw;
  outline: none;
}
.lock-badge {
  position: absolute;
  top: var(--sp-3);
  right: var(--sp-3);
  display: flex;
  align-items: center;
  gap: var(--sp-1);
  padding: var(--sp-1) var(--sp-2);
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: var(--r-chip);
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 11px;
  pointer-events: none;
}
.custom-controls {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  padding: var(--sp-3);
  background: rgba(0, 0, 0, 0.6);
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}
.cc-btn {
  flex: none;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}
.cc-btn:active {
  background: rgba(255, 255, 255, 0.3);
}
.cc-time {
  flex: none;
  font-size: 11px;
  color: #fff;
  font-variant-numeric: tabular-nums;
}
.cc-track {
  flex: 1;
  height: 4px;
  border-radius: var(--r-tag);
  background: rgba(255, 255, 255, 0.28);
  overflow: hidden;
}
.cc-fill {
  height: 100%;
  border-radius: var(--r-tag);
  background: var(--brand-tint);
  transition: width 0.2s linear;
}
.study-info {
  padding: var(--page-x);
  background: var(--surface);
  color: var(--ink);
  border-bottom: 1px solid var(--line);
}
.seek-tip {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  margin-top: var(--sp-3);
  font-size: 12px;
  line-height: 1.5;
  color: var(--brand);
}
.seek-tip.locked {
  color: var(--warn);
}
.progress-bar-wrap {
  padding: var(--sp-1) 0;
}
.info-text {
  font-size: 13px;
  color: var(--ink-2);
  font-variant-numeric: tabular-nums;
}
.study-bottom {
  padding: var(--page-x);
  padding-bottom: calc(var(--page-x) + env(safe-area-inset-bottom));
  background: var(--surface);
}
.popup-content {
  padding: var(--sp-4) var(--sp-5) var(--sp-5);
}
.popup-timer {
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  color: var(--bad);
  font-variant-numeric: tabular-nums;
  margin-bottom: var(--sp-3);
}
.popup-question {
  font-size: 15px;
  line-height: 1.6;
  color: var(--ink);
  margin-bottom: var(--sp-4);
}
.popup-options {
  margin-bottom: var(--sp-4);
}
.popup-option {
  padding: var(--sp-2) 0;
}
.verify-countdown {
  text-align: center;
  color: var(--bad);
  font-weight: 600;
  margin-bottom: var(--sp-2);
}
.face-input {
  display: none;
}
.mock-code {
  color: var(--brand);
  font-size: 13px;
  margin-bottom: var(--sp-2);
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
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-card);
  padding: var(--sp-8) var(--sp-6);
  text-align: center;
  max-width: 280px;
}
.warning-text {
  font-size: 15px;
  margin: var(--sp-4) 0;
  color: var(--ink);
  line-height: 1.5;
}
</style>
