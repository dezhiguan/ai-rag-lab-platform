<template>
  <el-card shadow="never" class="chunk-card">
    <template #header>
      <div class="chunk-header">
        <span>Chunk #{{ chunk.chunkIndex }}</span>
        <div class="meta">
          <el-tag size="small" type="info">token: {{ chunk.tokenCount }}</el-tag>
          <el-text size="small" truncated class="hash">{{ chunk.contentHash }}</el-text>
        </div>
        <el-button link type="primary" @click="expanded = !expanded">
          {{ expanded ? '收起' : '展开' }}
        </el-button>
      </div>
    </template>
    <pre v-show="expanded" class="content">{{ chunk.content }}</pre>
    <el-text v-show="!expanded" type="info" size="small" truncated>
      {{ preview }}
    </el-text>
  </el-card>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { DocumentChunk } from '@/api/document'

const props = defineProps<{
  chunk: DocumentChunk
}>()

const expanded = ref(false)
const preview = computed(() => {
  const text = props.chunk.content
  return text.length > 120 ? text.slice(0, 120) + '...' : text
})
</script>

<style scoped>
.chunk-card {
  margin-bottom: 12px;
}

.chunk-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.hash {
  max-width: 200px;
}

.content {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
}
</style>
