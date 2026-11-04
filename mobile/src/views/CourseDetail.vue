<template>
  <div class="page-container">
    <PageHeader title="课程详情" />

    <van-loading v-if="loading" class="loading-center" />

    <template v-if="detail">
      <div v-if="detail.cover" class="cover-wrap">
        <img :src="detail.cover" class="cover-img" />
      </div>
      <div class="card">
        <h3 class="course-title">{{ detail.courseName }}</h3>
        <div class="meta-list mt-12">
          <span class="meta-chip">学分 {{ detail.credit }}</span>
          <span class="meta-chip">学时 {{ detail.studyHour }}</span>
          <span v-if="detail.videoDuration" class="meta-chip">时长 {{ formatDuration(detail.videoDuration) }}</span>
        </div>
        <div v-if="detail.intro" class="intro mt-12">{{ detail.intro }}</div>
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
            <span class="att-icon"><van-icon name="description" size="16" /></span>
            <a :href="att.fileUrl" target="_blank" class="att-name">{{ att.fileName }}</a>
          </div>
        </div>
      </div>

      <div class="bottom-action">
        <van-button
          v-if="studyStatus <= 1"
          type="primary"
          block
          @click="$router.push(`/study/${detail.id}`)"
        >
          {{ studyStatus === 1 ? '继续学习' : '开始学习' }}
        </van-button>
        <van-button
          v-if="studyStatus === 2 && detail.needExam"
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
.page-container {
  padding-bottom: 140px;
  padding-bottom: calc(140px + env(safe-area-inset-bottom));
}
.cover-wrap {
  width: 100%;
  max-height: 200px;
  overflow: hidden;
  background: var(--brand-tint);
  border-bottom: 1px solid var(--line);
}
.cover-img {
  width: 100%;
  object-fit: cover;
  display: block;
}
.course-title {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--ink);
}
.meta-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
}
.meta-chip {
  padding: 2px 8px;
  border: 1px solid var(--brand-line);
  border-radius: var(--r-tag);
  background: var(--brand-tint);
  font-size: 11px;
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}
.intro {
  font-size: 14px;
  color: var(--ink-2);
  line-height: 1.65;
}
.video-card {
  padding: var(--sp-2);
}
.video-preview {
  width: 100%;
  display: block;
  border-radius: var(--r-chip);
  max-height: 220px;
  background: var(--ink);
}
.att-card {
  padding: var(--sp-1) var(--sp-4);
}
.att-item {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  padding: var(--sp-3) 0;
  min-width: 0;
}
.att-item + .att-item {
  border-top: 1px solid var(--line);
}
.att-icon {
  flex: none;
  width: 30px;
  height: 30px;
  border-radius: var(--r-tag);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-tint);
  color: var(--brand);
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
.bottom-action {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
  padding: var(--sp-3) var(--page-x) calc(var(--sp-3) + env(safe-area-inset-bottom));
  background: var(--surface);
  border-top: 1px solid var(--line);
}
.bottom-action :deep(.van-button) {
  height: 44px;
  border-radius: var(--r-control);
  font-size: 15px;
  font-weight: 600;
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}
</style>
