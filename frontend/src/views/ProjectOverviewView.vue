<template>
  <div class="project-overview-page">
    <el-card shadow="never" class="hero-card">
      <div class="hero-content">
        <h1 class="hero-title">AI RAG Lab Platform</h1>
        <p class="hero-subtitle">企业级 RAG 知识库实验平台</p>
        <p class="hero-desc">
          可用于个人学习、RAG 能力实验、知识库问答实践、开源社区参考。从文档导入到多模式检索、重排、评测与工程化可观测，形成完整 RAG 实验闭环。
        </p>
      </div>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header><span>版本能力路线图</span></template>
      <el-timeline>
        <el-timeline-item
          v-for="item in versionRoadmap"
          :key="item.version"
          :type="item.type"
          :timestamp="item.version"
          placement="top"
        >
          <p class="roadmap-title">{{ item.title }}</p>
          <p class="roadmap-desc">{{ item.desc }}</p>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="section-card">
          <template #header><span>核心功能入口</span></template>
          <el-row :gutter="12">
            <el-col
              v-for="entry in featureEntries"
              :key="entry.path"
              :xs="24"
              :sm="12"
            >
              <el-card shadow="hover" class="entry-card" @click="goTo(entry.path)">
                <div class="entry-title">{{ entry.title }}</div>
                <p class="entry-desc">{{ entry.desc }}</p>
                <el-tag size="small" type="info">{{ entry.tag }}</el-tag>
              </el-card>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="section-card">
          <template #header><span>项目核心能力</span></template>
          <div class="capability-list">
            <div
              v-for="item in coreCapabilities"
              :key="item.title"
              class="capability-item"
            >
              <el-tag :type="item.type" size="small">{{ item.title }}</el-tag>
              <span class="capability-desc">{{ item.desc }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="section-card">
      <template #header><span>推荐使用流程</span></template>
      <el-steps :active="usageSteps.length" finish-status="success" align-center>
        <el-step
          v-for="step in usageSteps"
          :key="step.title"
          :title="step.title"
          :description="step.desc"
        />
      </el-steps>
      <div class="step-links">
        <el-button
          v-for="step in usageSteps"
          :key="`${step.title}-link`"
          type="primary"
          link
          @click="goTo(step.path)"
        >
          {{ step.title }} →
        </el-button>
      </div>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="usage-tip"
        title="建议按顺序完成数据准备与索引构建，再依次体验 Debug、Evaluation、参数实验台，并在指标与日志页面观察运行结果。"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

interface RoadmapItem {
  version: string
  title: string
  desc: string
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}

const versionRoadmap: RoadmapItem[] = [
  { version: 'V0', title: '项目骨架', desc: 'Spring Boot + Vue 3 骨架、健康检查、Dashboard' },
  { version: 'V1', title: '文档导入与分块', desc: '知识库、Markdown/TXT 上传、固定分块' },
  { version: 'V2', title: 'Naive RAG', desc: 'PgVector 向量检索 + Chat 问答闭环' },
  { version: 'V2.5', title: '真实模型接入', desc: 'Qwen Embedding、DeepSeek Chat Provider' },
  { version: 'V3', title: 'Debug 可观察', desc: '召回 / Context / Prompt / 耗时全链路可观测' },
  { version: 'V4', title: 'BM25', desc: 'Elasticsearch 关键词检索' },
  { version: 'V5', title: 'Hybrid Search', desc: 'Vector + BM25 应用层融合' },
  { version: 'V6', title: 'Reranker', desc: '轻量本地重排，可观察排名变化' },
  { version: 'V7', title: 'Evaluation', desc: '评测中心：Top1 命中、多模式对比' },
  { version: 'V8', title: '工程化增强', desc: '系统状态、指标、日志、慢查询、参数实验台', type: 'success' },
]

interface FeatureEntry {
  title: string
  desc: string
  path: string
  tag: string
}

const featureEntries: FeatureEntry[] = [
  { title: 'Dashboard', desc: '项目概览与平台统计', path: '/dashboard', tag: 'V0+' },
  { title: 'Debug 控制台', desc: 'Vector / BM25 / Hybrid + Reranker 全链路调试', path: '/debug', tag: 'V3+' },
  { title: 'Evaluation 评测中心', desc: '单模式评测与三模式对比', path: '/evaluation', tag: 'V7' },
  { title: '系统状态', desc: '后端、PostgreSQL、ES、Provider 状态', path: '/system-status', tag: 'V8-01' },
  { title: 'RAG 指标', desc: '查询次数、耗时、检索模式分布', path: '/rag-metrics', tag: 'V8-02' },
  { title: '查询日志中心', desc: '历史查询分页、筛选与详情', path: '/rag-query-logs', tag: 'V8-03' },
  { title: '慢查询分析', desc: '慢查询原因与优化建议', path: '/slow-query-analysis', tag: 'V8-04' },
  { title: '参数实验台', desc: '调整 Context 参数并多组对比', path: '/rag-experiment', tag: 'V8-05' },
]

interface CapabilityItem {
  title: string
  desc: string
  type?: 'primary' | 'success' | 'warning' | 'info'
}

const coreCapabilities: CapabilityItem[] = [
  { title: '文档导入与分块', desc: 'Markdown/TXT 上传与固定大小分块', type: 'primary' },
  { title: '向量检索', desc: 'PgVector 相似度检索', type: 'primary' },
  { title: 'BM25 关键词检索', desc: 'Elasticsearch 全文检索', type: 'primary' },
  { title: 'Hybrid 混合检索', desc: '向量与关键词双路融合', type: 'success' },
  { title: 'Reranker 重排', desc: '轻量重排提升 Top 结果质量', type: 'warning' },
  { title: 'Evaluation 评测', desc: '内置用例批量验收 Top1 命中率', type: 'success' },
  { title: 'Debug 可观察', desc: '召回、过滤、Prompt、Answer 逐步可见', type: 'info' },
  { title: '工程化指标与日志', desc: '指标看板、日志中心、慢查询分析', type: 'info' },
  { title: '参数实验与多组对比', desc: '请求级 Context 参数实验，最多 5 组对比', type: 'warning' },
]

interface UsageStep {
  title: string
  desc: string
  path: string
}

const usageSteps: UsageStep[] = [
  { title: '初始化样例数据', desc: '知识库页初始化样例文档', path: '/kb' },
  { title: '重建向量 / ES', desc: '问答页重建向量；Debug 页重建 ES 索引', path: '/chat' },
  { title: 'Debug HYBRID 查询', desc: 'Hybrid 模式执行查询，观察召回与 Context', path: '/debug' },
  { title: '开启 Reranker', desc: '观察重排前后排名与 Answer 变化', path: '/debug' },
  { title: 'Evaluation 多模式评测', desc: '三模式对比通过率与耗时', path: '/evaluation' },
  { title: '参数实验台调参', desc: '调整 topK / maxChunks 等并多组对比', path: '/rag-experiment' },
  { title: '观察运行指标', desc: 'RAG 指标、查询日志、慢查询分析', path: '/rag-metrics' },
]

function goTo(path: string) {
  router.push(path)
}
</script>

<style scoped>
.project-overview-page {
  max-width: 1200px;
}

.hero-card {
  margin-bottom: 16px;
  background: linear-gradient(135deg, #f0f7ff 0%, #ffffff 60%);
}

.hero-content {
  padding: 8px 4px;
}

.hero-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.hero-subtitle {
  margin: 0 0 12px;
  font-size: 16px;
  color: #409eff;
  font-weight: 600;
}

.hero-desc {
  margin: 0;
  line-height: 1.7;
  color: #606266;
  max-width: 760px;
}

.section-card {
  margin-bottom: 16px;
}

.roadmap-title {
  margin: 0 0 4px;
  font-weight: 600;
  color: #303133;
}

.roadmap-desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}

.entry-card {
  margin-bottom: 12px;
  cursor: pointer;
  transition: transform 0.15s ease;
}

.entry-card:hover {
  transform: translateY(-2px);
}

.entry-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.entry-desc {
  margin: 0 0 10px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  min-height: 40px;
}

.capability-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.capability-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.capability-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.step-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  justify-content: center;
  margin-top: 20px;
}

.usage-tip {
  margin-top: 24px;
}
</style>
