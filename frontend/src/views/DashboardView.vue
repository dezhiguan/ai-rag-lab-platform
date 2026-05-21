<template>
  <el-card shadow="never">
    <template #header>
      <span>项目概览</span>
    </template>

    <el-descriptions :column="1" border>
      <el-descriptions-item label="项目名称">
        AI RAG Lab Platform
      </el-descriptions-item>
      <el-descriptions-item label="当前版本">
        V0 项目骨架版
      </el-descriptions-item>
      <el-descriptions-item label="后端连接状态">
        <el-tag v-if="loading" type="info">检测中...</el-tag>
        <el-tag v-else-if="connected" type="success">后端连接成功</el-tag>
        <el-tag v-else type="danger">后端连接失败</el-tag>
      </el-descriptions-item>
      <el-descriptions-item v-if="timestamp" label="系统时间">
        {{ timestamp }}
      </el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getHealth } from '@/api/system'

const loading = ref(true)
const connected = ref(false)
const timestamp = ref('')

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
    loading.value = false
  }
})
</script>
