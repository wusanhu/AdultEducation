<template>
  <div class="uploader-wrapper">
    <van-uploader
      v-model:model-value="fileList"
      :max-count="maxCount"
      :after-read="onAfterRead"
      :before-delete="onBeforeDelete"
      :accept="accept"
      :preview-size="previewSize"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Uploader as VanUploader, showToast } from 'vant'
import type { UploaderFileListItem } from 'vant'
import { files } from '@/api'

const props = withDefaults(defineProps<{
  modelValue?: string | string[]
  maxCount?: number
  accept?: string
  previewSize?: number | string
  type?: string
}>(), {
  maxCount: 1,
  accept: 'image/*',
  previewSize: 80,
  type: 'common',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | string[]): void
}>()

const fileList = ref<UploaderFileListItem[]>([])

watch(() => props.modelValue, (val) => {
  if (!val) {
    fileList.value = []
    return
  }
  const urls = Array.isArray(val) ? val : [val]
  fileList.value = urls.filter(Boolean).map((url) => ({
    url,
    isImage: true,
  }))
}, { immediate: true })

async function onAfterRead(item: any) {
  const items = Array.isArray(item) ? item : [item]
  for (const f of items) {
    try {
      f.status = 'uploading'
      f.message = '上传中...'
      const res = await files.upload(f.file!, props.type)
      f.url = res.url || res.fileUrl
      f.status = 'done'
      f.message = ''
      emitValue()
    } catch {
      f.status = 'failed'
      f.message = '上传失败'
      showToast('上传失败')
    }
  }
}

function onBeforeDelete() {
  emitValue()
  return true
}

function emitValue() {
  const urls = fileList.value.filter((f) => f.url).map((f) => f.url!)
  if (props.maxCount === 1) {
    emit('update:modelValue', urls[0] || '')
  } else {
    emit('update:modelValue', urls)
  }
}
</script>
