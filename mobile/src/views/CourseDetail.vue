<template>
  <div class="page-container">
    <PageHeader title="课程详情" />

    <van-loading v-if="loading" class="loading-center" />

    <template v-if="detail">
      <div v-if="detail.cover" class="cover-wrap">
        <img :src="detail.cover" class="cover-img" />
      </div>
      <div class="card card--gilded">
        <h3 class="course-title">{{ detail.courseName }}</h3>
        <div class="meta-list mt-12">
          <span class="meta-chip">学分 <b class="num">{{ detail.credit }}</b></span>
          <span class="meta-chip">学时 <b class="num">{{ detail.studyHour }}</b></span>
          <span v-if="detail.videoDuration" class="meta-chip">时长 <b class="num">{{ formatDuration(detail.videoDuration) }}</b></span>
        </div>
        <div v-if="detail.intro" class="intro mt-12">{{ detail.intro }}</div>
      </div>

      <div v-if="detail.videoUrl" class="card">
        <h4 class="block-title">课程视频</h4>
        <video
          :src="detail.videoUrl"
          controls
          controlslist="nodownload"
          class="video-preview"
          preload="metadata"
        />
      </div>

      <div v-if="detail.attachments && detail.attachments.length > 0" class="card">
        <h4 class="block-title">附件资料</h4>
        <div v-for="(att, i) in detail.attachments" :key="i" class="att-item">
          <van-icon name="description" color="#d97706" />
          <a :href="att.fileUrl" target="_blank">{{ att.fileName }}</a>
        </div>
      </div>

      <div class="bottom-action">
        <van-button
          v-if="studyStatus <= 1"
          type="primary"
          block
          round
          @click="$router.push(`/study/${detail.id}`)"
        >
          {{ studyStatus === 1 ? '继续学习' : '开始学习' }}
        </van-button>
        <van-button
          v-if="studyStatus === 2 && detail.needExam"
          type="warning"
          block
          round
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
import { courses } from '@/api'

const route = useRoute()
const detail = ref<any>(null)
const loading = ref(true)
const studyStatus = computed(() => Number(detail.value?.studyRecord?.status ?? 0))

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
.cover-wrap {
  width: 100%;
  max-height: 210px;
  overflow: hidden;
  border-bottom: 1px solid var(--line);
}
.cover-img {
  width: 100%;
  object-fit: cover;
}
.course-title {
  font-family: var(--font-body);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--text-strong);
}
.meta-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.meta-chip {
  font-size: 12px;
  color: var(--text-sub);
  background: var(--bg-soft);
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 4px 9px;
}
.meta-chip b {
  color: var(--text-strong);
  font-weight: 600;
  margin-left: 2px;
}
.intro {
  font-size: 13px;
  color: var(--text-sub);
  line-height: 1.7;
  border-top: 1px dashed var(--line-strong);
  padding-top: 12px;
}
.block-title {
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 700;
  color: var(--text-strong);
  letter-spacing: 0.08em;
  padding-left: 10px;
  position: relative;
}
.block-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%) rotate(45deg);
  width: 6px;
  height: 6px;
  background: var(--primary);
  border-radius: 1px;
}
.video-preview {
  width: 100%;
  border-radius: 8px;
  max-height: 220px;
  background: #0d1b2e;
}
.att-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 0;
  font-size: 14px;
  border-bottom: 1px dashed var(--line);
}
.att-item:last-child {
  border-bottom: none;
}
.att-item a {
  color: var(--text-main);
  text-decoration: none;
}
.bottom-action {
  padding: 12px 16px calc(16px + env(safe-area-inset-bottom));
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--bg-card);
  border-top: 1px solid var(--line);
  z-index: 5;
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}
</style>
