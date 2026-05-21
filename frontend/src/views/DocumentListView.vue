<template>
  <el-card v-loading="kbLoading" shadow="never" class="mb-card">
    <template #header>
      <div class="toolbar">
        <div>
          <el-button link @click="router.push('/kb')">← 返回知识库</el-button>
          <span class="title">{{ kb?.name ?? '知识库' }}</span>
        </div>
        <div class="actions">
          <el-button :loading="initLoading" @click="handleInitSample">初始化样例数据</el-button>
          <UploadDocument v-if="kbId" :kb-id="kbId" @success="loadDocuments" />
        </div>
      </div>
    </template>
    <p v-if="kb?.description" class="desc">{{ kb.description }}</p>
  </el-card>

  <el-card shadow="never">
    <template #header>文档列表</template>
    <el-table v-loading="docLoading" :data="documents" stripe>
      <el-table-column prop="fileName" label="文件名" min-width="200" />
      <el-table-column prop="fileType" label="类型" width="80" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <DocumentStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="fileSize" label="大小" width="100">
        <template #default="{ row }">
          {{ row.fileSize ? formatSize(row.fileSize) : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="上传时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :disabled="row.status !== 'COMPLETED'"
            @click="goChunks(row.id)"
          >
            查看 Chunk
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getKnowledgeBase, type KnowledgeBase } from '@/api/knowledgeBase'
import { listDocuments, type DocumentItem } from '@/api/document'
import { initSampleData } from '@/api/sample'
import DocumentStatusTag from '@/components/DocumentStatusTag.vue'
import UploadDocument from '@/components/UploadDocument.vue'

const route = useRoute()
const router = useRouter()
const kbId = computed(() => Number(route.params.kbId))

const kbLoading = ref(false)
const docLoading = ref(false)
const initLoading = ref(false)
const kb = ref<KnowledgeBase | null>(null)
const documents = ref<DocumentItem[]>([])

function formatSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

async function loadKb() {
  if (!kbId.value) return
  kbLoading.value = true
  try {
    const res = await getKnowledgeBase(kbId.value)
    if (res.code === 200) kb.value = res.data
  } finally {
    kbLoading.value = false
  }
}

async function loadDocuments() {
  if (!kbId.value) return
  docLoading.value = true
  try {
    const res = await listDocuments(kbId.value)
    if (res.code === 200) documents.value = res.data ?? []
  } finally {
    docLoading.value = false
  }
}

async function handleInitSample() {
  initLoading.value = true
  try {
    const res = await initSampleData()
    if (res.code === 200) {
      ElMessage.success(res.data?.message ?? '初始化成功')
      if (res.data?.knowledgeBaseId) {
        router.replace(`/kb/${res.data.knowledgeBaseId}/documents`)
      }
      await loadKb()
      await loadDocuments()
    } else {
      ElMessage.error(res.message)
    }
  } catch {
    ElMessage.error('初始化失败')
  } finally {
    initLoading.value = false
  }
}

function goChunks(documentId: number) {
  router.push(`/documents/${documentId}/chunks`)
}

onMounted(async () => {
  await loadKb()
  await loadDocuments()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.title {
  margin-left: 8px;
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.desc {
  margin: 0;
  color: #606266;
}

.mb-card {
  margin-bottom: 16px;
}
</style>
