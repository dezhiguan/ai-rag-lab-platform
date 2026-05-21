<template>
  <el-tag :type="tagType" size="small">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string
}>()

const statusMap: Record<string, { label: string; type: '' | 'success' | 'warning' | 'danger' | 'info' }> = {
  UPLOADED: { label: '已上传', type: 'info' },
  PARSING: { label: '解析中', type: 'warning' },
  PARSED: { label: '已解析', type: 'info' },
  CHUNKING: { label: '分块中', type: 'warning' },
  COMPLETED: { label: '完成', type: 'success' },
  FAILED: { label: '失败', type: 'danger' },
}

const meta = computed(() => statusMap[props.status] ?? { label: props.status, type: 'info' as const })
const label = computed(() => meta.value.label)
const tagType = computed(() => meta.value.type)
</script>
