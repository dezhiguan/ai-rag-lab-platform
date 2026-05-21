<template>
  <el-card v-loading="docLoading" shadow="never" class="mb-card">
    <template #header>
      <div class="toolbar">
        <el-button link @click="goBack">← 返回文档列表</el-button>
        <span class="title">{{ document?.fileName ?? '文档' }}</span>
        <DocumentStatusTag v-if="document" :status="document.status" />
      </div>
    </template>
    <el-descriptions v-if="document" :column="2" border size="small">
      <el-descriptions-item label="文件类型">{{ document.fileType }}</el-descriptions-item>
      <el-descriptions-item label="文件大小">
        {{ document.fileSize ?? '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="内容哈希" :span="2">
        {{ document.contentHash ?? '-' }}
      </el-descriptions-item>
    </el-descriptions>
  </el-card>

  <el-card shadow="never">
    <template #header>Chunk 列表（共 {{ chunks.length }} 条）</template>
    <div v-loading="chunkLoading">
      <ChunkCard v-for="chunk in chunks" :key="chunk.id" :chunk="chunk" />
      <el-empty v-if="!chunkLoading && chunks.length === 0" description="暂无 Chunk" />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDocument, listChunks, type DocumentChunk, type DocumentItem } from '@/api/document'
import ChunkCard from '@/components/ChunkCard.vue'
import DocumentStatusTag from '@/components/DocumentStatusTag.vue'

const route = useRoute()
const router = useRouter()
const documentId = computed(() => Number(route.params.documentId))

const docLoading = ref(false)
const chunkLoading = ref(false)
const document = ref<DocumentItem | null>(null)
const chunks = ref<DocumentChunk[]>([])

async function loadDocument() {
  if (!documentId.value) return
  docLoading.value = true
  try {
    const res = await getDocument(documentId.value)
    if (res.code === 200) document.value = res.data
  } finally {
    docLoading.value = false
  }
}

async function loadChunks() {
  if (!documentId.value) return
  chunkLoading.value = true
  try {
    const res = await listChunks(documentId.value)
    if (res.code === 200) chunks.value = res.data ?? []
  } finally {
    chunkLoading.value = false
  }
}

function goBack() {
  if (document.value?.kbId) {
    router.push(`/kb/${document.value.kbId}/documents`)
  } else {
    router.push('/kb')
  }
}

onMounted(async () => {
  await loadDocument()
  await loadChunks()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title {
  font-weight: 600;
}

.mb-card {
  margin-bottom: 16px;
}
</style>
