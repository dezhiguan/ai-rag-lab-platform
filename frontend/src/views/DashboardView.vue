<template>
  <el-row :gutter="16">
    <el-col :span="12">
      <el-card shadow="never">
        <template #header>
          <span>项目概览</span>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="项目名称">
            AI RAG Lab Platform
          </el-descriptions-item>
          <el-descriptions-item label="当前版本">
            V8 工程化增强版
          </el-descriptions-item>
          <el-descriptions-item label="后端连接状态">
            <el-tag v-if="healthLoading" type="info">检测中...</el-tag>
            <el-tag v-else-if="connected" type="success">后端连接成功</el-tag>
            <el-tag v-else type="danger">后端连接失败</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="timestamp" label="系统时间">
            {{ timestamp }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card v-loading="statsLoading" shadow="never">
        <template #header>
          <span>平台统计</span>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="知识库数量">
            {{ stats?.knowledgeBaseCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="文档数量">
            {{ stats?.documentCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="Chunk 数量">
            {{ stats?.chunkCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="已向量化 Chunk">
            {{ stats?.embeddedChunkCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="Chat 会话数">
            {{ stats?.chatSessionCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="Chat 消息数">
            {{ stats?.chatMessageCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="样例数据">
            <el-tag :type="stats?.sampleDataInitialized ? 'success' : 'info'">
              {{ stats?.sampleDataInitialized ? '已初始化' : '未初始化' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div class="actions">
          <el-button type="primary" @click="router.push('/kb')">进入知识库</el-button>
          <el-button @click="router.push('/chat')">进入问答</el-button>
          <el-button @click="router.push('/system-status')">系统状态</el-button>
          <el-button @click="router.push('/rag-metrics')">RAG 指标</el-button>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getHealth } from '@/api/system'
import { getDashboardStats, type DashboardStats } from '@/api/dashboard'

const router = useRouter()
const healthLoading = ref(true)
const statsLoading = ref(true)
const connected = ref(false)
const timestamp = ref('')
const stats = ref<DashboardStats | null>(null)

onMounted(async () => {
  try {
    const res = await getHealth()
    if (res.code === 200 && res.data?.status === 'UP') {
      connected.value = true
      timestamp.value = res.data.timestamp
    }
  } catch {
    connected.value = false
  } finally {
    healthLoading.value = false
  }

  try {
    const res = await getDashboardStats()
    if (res.code === 200) stats.value = res.data
  } finally {
    statsLoading.value = false
  }
})
</script>

<style scoped>
.actions {
  margin-top: 16px;
}
</style>
