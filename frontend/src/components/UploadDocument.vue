<template>
  <el-upload
    :auto-upload="false"
    :show-file-list="false"
    accept=".md,.markdown,.txt"
    :on-change="handleChange"
    :disabled="uploading || disabled"
  >
    <el-button type="primary" :loading="uploading" :disabled="disabled">上传 Markdown/TXT</el-button>
  </el-upload>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { UploadFile } from 'element-plus'
import { ElMessage } from 'element-plus'
import { uploadDocument } from '@/api/document'
import { usePermission } from '@/composables/usePermission'

const props = defineProps<{
  kbId: number
  disabled?: boolean
}>()

const emit = defineEmits<{
  success: []
}>()

const { requireWrite } = usePermission()
const uploading = ref(false)

async function handleChange(uploadFile: UploadFile) {
  if (props.disabled || !requireWrite()) return

  const raw = uploadFile.raw
  if (!raw) return

  const ext = raw.name.split('.').pop()?.toLowerCase()
  if (!ext || !['md', 'markdown', 'txt'].includes(ext)) {
    ElMessage.error('仅支持 Markdown(.md) 和 TXT(.txt) 文件')
    return
  }

  uploading.value = true
  try {
    const res = await uploadDocument(props.kbId, raw)
    if (res.code === 200) {
      ElMessage.success('上传并处理成功')
      emit('success')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}
</script>
