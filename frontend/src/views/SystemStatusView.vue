<template>
  <div class="system-status-page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="card-header">
          <span>系统运行状态（V8）</span>
          <el-button type="primary" :loading="loading" @click="loadStatus">
            刷新状态
          </el-button>
        </div>
      </template>

      <el-alert
        v-if="loadError"
        type="error"
        :closable="false"
        show-icon
        :title="loadError"
        class="status-alert"
      />

      <el-row v-loading="loading" :gutter="16">
        <el-col :xs="24" :sm="12" :lg="8">
          <el-card shadow="hover" class="status-card">
            <template #header>后端服务</template>
            <template v-if="status">
              <div class="status-row">
                <span class="label">状态</span>
                <el-tag :type="isUp(status.backend.status) ? 'success' : 'danger'" size="small">
                  {{ isUp(status.backend.status) ? '正常' : '异常' }}
                </el-tag>
              </div>
              <div class="status-row">
                <span class="label">应用</span>
                <span>{{ status.backend.appName }}</span>
              </div>
              <div class="status-row">
                <span class="label">版本</span>
                <span>{{ status.backend.version }}</span>
              </div>
              <div class="status-row">
                <span class="label">服务器时间</span>
                <span class="value-mono">{{ status.backend.serverTime }}</span>
              </div>
              <p v-if="status.backend.message" class="hint">{{ status.backend.message }}</p>
            </template>
            <p v-else class="placeholder">暂无数据</p>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="8">
          <el-card shadow="hover" class="status-card">
            <template #header>PostgreSQL</template>
            <template v-if="status">
              <div class="status-row">
                <span class="label">状态</span>
                <el-tag :type="isUp(status.postgresql.status) ? 'success' : 'danger'" size="small">
                  {{ isUp(status.postgresql.status) ? '正常' : '异常' }}
                </el-tag>
              </div>
              <div class="status-row">
                <span class="label">组件</span>
                <span>{{ status.postgresql.name }}</span>
              </div>
              <p v-if="status.postgresql.message" class="hint" :class="{ error: !isUp(status.postgresql.status) }">
                {{ status.postgresql.message }}
              </p>
            </template>
            <p v-else class="placeholder">暂无数据</p>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="8">
          <el-card shadow="hover" class="status-card">
            <template #header>Elasticsearch</template>
            <template v-if="status">
              <div class="status-row">
                <span class="label">状态</span>
                <el-tag :type="isUp(status.elasticsearch.status) ? 'success' : 'danger'" size="small">
                  {{ isUp(status.elasticsearch.status) ? '正常' : '异常' }}
                </el-tag>
              </div>
              <div class="status-row">
                <span class="label">索引名</span>
                <span class="value-mono">{{ status.elasticsearch.indexName }}</span>
              </div>
              <p
                v-if="status.elasticsearch.message"
                class="hint"
                :class="{ error: !isUp(status.elasticsearch.status) }"
              >
                {{ status.elasticsearch.message }}
              </p>
            </template>
            <p v-else class="placeholder">暂无数据</p>
          </el-card>
        </el-col>

        <el-col :span="24">
          <el-card shadow="hover" class="status-card">
            <template #header>模型 Provider</template>
            <template v-if="status">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="Embedding Provider">
                  {{ status.modelProvider.embeddingProvider }}
                </el-descriptions-item>
                <el-descriptions-item label="Embedding Model">
                  {{ status.modelProvider.embeddingModel }}
                </el-descriptions-item>
                <el-descriptions-item label="Chat Provider">
                  {{ status.modelProvider.chatProvider }}
                </el-descriptions-item>
                <el-descriptions-item label="Chat Model">
                  {{ status.modelProvider.chatModel }}
                </el-descriptions-item>
              </el-descriptions>
            </template>
            <p v-else class="placeholder">暂无数据</p>
          </el-card>
        </el-col>

        <el-col :span="24">
          <el-card shadow="hover" class="status-card">
            <template #header>核心能力</template>
            <template v-if="status">
              <div class="feature-grid">
                <div v-for="item in status.features" :key="item.key" class="feature-item">
                  <span class="feature-label">{{ item.label }}</span>
                  <el-tag type="success" size="small">已支持</el-tag>
                </div>
              </div>
            </template>
            <p v-else class="placeholder">暂无数据</p>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getSystemStatus } from '@/api/system'
import type { SystemStatus } from '@/types/system'

const loading = ref(false)
const loadError = ref('')
const status = ref<SystemStatus | null>(null)

function isUp(value: string | undefined): boolean {
  return value === 'UP'
}

async function loadStatus() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getSystemStatus()
    if (res.code === 200 && res.data) {
      status.value = res.data
    } else {
      status.value = null
      loadError.value = res.message || '获取系统状态失败'
    }
  } catch (err: unknown) {
    status.value = null
    const message = err instanceof Error ? err.message : '无法连接后端服务'
    loadError.value = `后端不可用或请求失败：${message}`
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStatus()
})
</script>

<style scoped>
.system-status-page {
  max-width: 1200px;
}

.panel {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.status-alert {
  margin-bottom: 16px;
}

.status-card {
  margin-bottom: 16px;
  min-height: 140px;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  font-size: 14px;
}

.label {
  color: #909399;
  flex-shrink: 0;
}

.value-mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  word-break: break-all;
  text-align: right;
}

.hint {
  margin: 8px 0 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.hint.error {
  color: #f56c6c;
}

.placeholder {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.feature-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.feature-label {
  font-size: 14px;
  color: #303133;
}

</style>
