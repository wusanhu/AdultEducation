<template>
  <div class="page-container">
    <PageHeader title="课程详情" />

    <van-loading v-if="loading" class="loading-center" />

    <template v-if="detail">
      <div v-if="detail.cover" class="cover-wrap">
        <img :src="detail.cover" class="cover-img" />
      </div>
      <div class="card">
        <h3>{{ detail.courseName }}</h3>
        <div class="meta-list mt-12">
          <span>学分：{{ detail.credit }}</span>
          <span>学时：{{ detail.studyHour }}</span>
          <span v-if="detail.videoDuration">时长：{{ formatDuration(detail.videoDuration) }}</span>
        </div>
        <div v-if="detail.intro" class="intro mt-12">{{ detail.intro }}</div>
      </div>

      <div v-if="detail.videoUrl" class="card">
        <h4 class="mb-8">课程视频</h4>
        <video
          :src="detail.videoUrl"
          controls
          controlslist="nodownload"
          class="video-preview"
          preload="metadata"
        />
      </div>

      <div v-if="detail.attachments && detail.attachments.length > 0" class="card">
        <h4 class="mb-8">附件资料</h4>
        <div v-for="(att, i) in detail.attachments" :key="i" class="att-item">
          <van-icon name="description" />
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
  max-height: 200px;
  overflow: hidden;
}
.cover-img {
  width: 100%;
  object-fit: cover;
}
.meta-list {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #969799;
}
.intro {
  font-size: 14px;
  color: #646566;
  line-height: 1.6;
}
.video-preview {
  width: 100%;
  border-radius: 8px;
  max-height: 220px;
}
.att-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 14px;
  border-bottom: 1px solid #f5f5f5;
}
.att-item a {
  color: #1989fa;
  text-decoration: none;
}
.bottom-action {
  padding: 16px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #ebedf0;
}
.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}
</style>
