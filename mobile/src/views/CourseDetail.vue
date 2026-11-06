<template>
  <div class="page-container course-page">
    <PageHeader title="课程详情" />

    <van-loading v-if="loading" class="loading-center" />

    <template v-if="detail">
      <!-- 封面 -->
      <div class="cover-hero">
        <img v-if="detail.cover" :src="detail.cover" class="cover-img" alt="" />
        <div v-else class="cover-fallback">
          <span class="cover-glyph" aria-hidden="true">课</span>
        </div>
      </div>

      <!-- 概要 -->
      <div class="card info-card">
        <span class="kicker">COURSE</span>
        <div class="title-row">
          <h1 class="course-title">{{ detail.courseName }}</h1>
          <StatusTag v-if="detail.studyRecord" :status="studyStatus" type="study" />
        </div>
        <div class="meta-list">
          <span class="pill pill--accent">学分 {{ detail.credit }}</span>
          <span class="pill">学时 {{ detail.studyHour }}</span>
          <span v-if="detail.videoDuration" class="pill">时长 {{ formatDuration(detail.videoDuration) }}</span>
        </div>
        <div v-if="detail.intro" class="intro-block">
          <span class="kicker">课程简介</span>
          <p class="intro">{{ detail.intro }}</p>
        </div>
      </div>

      <div v-if="detail.videoUrl">
        <div class="section-title">课程视频</div>
        <div class="card video-card">
          <video
            :src="detail.videoUrl"
            controls
            controlslist="nodownload"
            class="video-preview"
            preload="metadata"
          />
        </div>
      </div>

      <div v-if="detail.attachments && detail.attachments.length > 0">
        <div class="section-title">附件资料</div>
        <div class="card att-card">
          <div v-for="(att, i) in detail.attachments" :key="i" class="att-item">
            <span class="att-icon"><van-icon name="notes-o" size="16" /></span>
            <a :href="att.fileUrl" target="_blank" class="att-name">{{ att.fileName }}</a>
            <van-icon name="arrow" size="12" color="var(--ink-4)" />
          </div>
        </div>
      </div>

      <div v-if="studyStatus <= 1 || (studyStatus === 2 && hasExam)" class="bottom-action">
        <van-button
          v-if="studyStatus <= 1"
          type="primary"
          block
          @click="$router.push(`/study/${detail.id}`)"
        >
          {{ studyStatus === 1 ? '继续学习' : '开始学习' }}
        </van-button>
        <van-button
          v-if="studyStatus === 2 && hasExam"
          type="warning"
          plain
          block
          @click="$router.push(`/exam/${detail.id}`)"
        >
          进入考试
        </van-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Button as VanButton, Icon as VanIcon, Loading as VanLoading } from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { courses } from '@/api'

const route = useRoute()
const detail = ref<any>(null)
const loading = ref(true)
const studyStatus = computed(() => Number(detail.value?.studyRecord?.status ?? 0))
// 仅开启测验开关不够，题目为空时后端会拒绝作答，入口一并以“已配置题目”为准
const hasExam = computed(() => Number(detail.value?.needExam) === 1
  && (detail.value?.questions?.length ?? 0) > 0)

function formatDuration(sec: number) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    detail.value = await courses.detail(id)
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.course-page {
  padding-bottom: 140px;
  padding-bottom: calc(140px + env(safe-area-inset-bottom));
}

/* ---------- 封面 ---------- */
.cover-hero {
  position: relative;
  height: 184px;
  margin: var(--sp-2) var(--page-x) var(--sp-4);
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: var(--r-card);
  background: var(--brand-tint);
  box-shadow: var(--shadow-card);
}

.cover-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, var(--brand-tint) 0%, #dbe6fb 58%, #eef3fc 100%);
}

.cover-glyph {
  font-family: var(--font-serif);
  font-size: 88px;
  line-height: 1;
  color: rgba(29, 78, 216, 0.16);
}

/* ---------- 概要 ---------- */
.info-card {
  padding: var(--sp-5);
}

.title-row {
  display: flex;
  align-items: flex-start;
  gap: var(--sp-3);
  margin: 7px 0 var(--sp-4);
}

.course-title {
  flex: 1;
  min-width: 0;
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
  line-height: 1.45;
  letter-spacing: 0.01em;
  color: var(--ink);
}

.title-row :deep(.status-tag) {
  margin-top: 4px;
}

.meta-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--sp-2);
}

.intro-block {
  margin-top: var(--sp-5);
  padding-top: var(--sp-4);
  border-top: 1px solid var(--line);
}

.intro {
  margin-top: var(--sp-2);
  font-size: 14px;
  line-height: 1.8;
  color: var(--ink-2);
}

/* ---------- 视频 ---------- */
.video-card {
  padding: 0;
  overflow: hidden;
  background: #101820;
  border-color: transparent;
}

.video-preview {
  display: block;
  width: 100%;
  max-height: 232px;
  background: #101820;
}

/* ---------- 附件 ---------- */
.att-card {
  padding: var(--sp-1) var(--sp-4);
}

.att-item {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  min-height: 50px;
  padding: var(--sp-2) 0;
}

.att-item + .att-item {
  border-top: 1px solid var(--line);
}

.att-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 32px;
  height: 32px;
  border-radius: var(--r-tag);
  background: var(--accent-tint);
  border: 1px solid var(--accent-line);
  color: var(--accent-strong);
}

.att-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  line-height: 1.5;
  color: var(--ink);
  text-decoration: none;
}

.att-name:active {
  color: var(--brand);
}

/* ---------- 底部操作 ---------- */
.bottom-action {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
  padding: var(--sp-3) var(--page-x) calc(var(--sp-3) + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.94);
  border-top: 1px solid var(--line);
  backdrop-filter: saturate(180%) blur(12px);
  -webkit-backdrop-filter: saturate(180%) blur(12px);
}

.bottom-action :deep(.van-button) {
  height: 46px;
  font-size: 15px;
  font-weight: 600;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 48px;
}
</style>
