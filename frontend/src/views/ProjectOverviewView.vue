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

    <!-- 当前访问身份 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <el-space>
          <span>当前访问身份</span>
          <el-tag type="success" size="small">V10 访问保护</el-tag>
        </el-space>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="当前用户">{{ authStore.username }}</el-descriptions-item>
        <el-descriptions-item label="当前角色">{{ authStore.roleLabel }}</el-descriptions-item>
        <el-descriptions-item label="当前模式">
          <el-tag :type="authStore.isGuest ? 'warning' : 'success'" size="small">
            {{ authStore.modeLabel }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-alert
        v-if="authStore.isGuest"
        type="warning"
        :closable="false"
        show-icon
        class="guest-mode-alert"
        title="只读体验模式：可浏览 Debug、评测、参数实验台与可观测看板；不可创建知识库、上传文档或重建索引。"
      />
    </el-card>

    <!-- 快速开始 -->
    <el-card shadow="never" class="section-card">
      <template #header><span>快速开始</span></template>
      <el-row :gutter="16">
        <el-col
          v-for="item in quickStartItems"
          :key="item.title"
          :xs="24"
          :sm="12"
          :lg="8"
        >
          <el-card shadow="hover" class="quick-card">
            <div class="quick-title">{{ item.title }}</div>
            <p class="quick-desc">{{ item.desc }}</p>
            <pre v-if="item.command" class="quick-command">{{ item.command }}</pre>
            <el-space wrap>
              <template v-if="item.action === 'initSample'">
                <el-button
                  v-if="canWrite"
                  type="primary"
                  size="small"
                  :loading="initSampleLoading"
                  @click="handleInitSample"
                >
                  一键初始化
                </el-button>
                <el-tooltip v-else content="当前为体验账号，不支持该操作" placement="top">
                  <el-button type="primary" size="small" disabled>一键初始化</el-button>
                </el-tooltip>
              </template>
              <el-button
                v-if="item.path"
                type="primary"
                size="small"
                plain
                @click="goTo(item.path)"
              >
                {{ item.linkLabel || '前往' }}
              </el-button>
            </el-space>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 项目完成状态 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <el-space>
          <span>项目完成状态</span>
          <el-tag type="success" size="small">V0～V8 已完成</el-tag>
        </el-space>
      </template>
      <el-table :data="versionCompletion" stripe style="width: 100%">
        <el-table-column prop="version" label="版本" width="88" />
        <el-table-column prop="title" label="名称" width="140" />
        <el-table-column prop="summary" label="核心能力说明" min-width="280" />
        <el-table-column label="状态" width="88">
          <template #default>
            <el-tag type="success" size="small">已完成</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 适用场景 -->
    <el-card shadow="never" class="section-card">
      <template #header><span>适用场景</span></template>
      <el-row :gutter="12">
        <el-col
          v-for="scene in useCases"
          :key="scene.title"
          :xs="24"
          :sm="12"
          :md="8"
        >
          <el-card shadow="hover" class="scene-card">
            <div class="scene-title">{{ scene.title }}</div>
            <p class="scene-desc">{{ scene.desc }}</p>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 线上部署准备 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <el-space>
          <span>线上部署准备</span>
          <el-tag type="warning" size="small">V9 双服务器方案</el-tag>
        </el-space>
      </template>

      <el-descriptions title="当前部署架构" :column="1" border class="dual-server-desc">
        <el-descriptions-item label="应用入口层">
          轻量服务器 2C4G — Nginx、前端 dist、RAG Java 后端；预留 AI 求职 Agent
        </el-descriptions-item>
        <el-descriptions-item label="数据检索层">
          ECS 4C8G — PostgreSQL + PgVector、Elasticsearch、Redis（仅内网）
        </el-descriptions-item>
        <el-descriptions-item label="内网通信">
          <el-tag type="success" size="small">已验证</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="对外访问">
          Nginx 统一暴露前端与 /api；PG / ES / Redis 不公网暴露
        </el-descriptions-item>
        <el-descriptions-item label="当前备案策略">
          暂不备案，先使用公网 IP 提供在线体验
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions title="线上部署状态" :column="1" border class="dual-server-desc online-status-desc">
        <el-descriptions-item
          v-for="row in onlineDeploymentStatus"
          :key="row.label"
          :label="row.label"
        >
          <el-tag :type="row.type" size="small">{{ row.value }}</el-tag>
          <span v-if="row.note" class="online-status-note">{{ row.note }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-table :data="deploymentReadiness" stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="item" label="准备项" width="200" />
        <el-table-column prop="desc" label="说明" min-width="280" />
        <el-table-column prop="path" label="文件 / 文档" min-width="240">
          <template #default="{ row }">
            <code class="deploy-path">{{ row.path }}</code>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="160">
          <template #default="{ row }">
            <el-tag :type="row.statusType || 'success'" size="small">
              {{ row.status || '已准备' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="usage-tip"
        title="V9-06 实机联调已完成。公网可访问前端与 /api；样例数据与索引已初始化。真实 LLM Key 请在服务器 /opt/rag-lab/app/.env.app 配置。"
      />
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { initSampleData } from '@/api/sample'
import { useAuthStore } from '@/stores/auth'
import { usePermission } from '@/composables/usePermission'

const router = useRouter()
const authStore = useAuthStore()
const { canWrite, requireWrite } = usePermission()
const initSampleLoading = ref(false)

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

interface VersionCompletionItem {
  version: string
  title: string
  summary: string
}

const versionCompletion: VersionCompletionItem[] = [
  { version: 'V0', title: '项目骨架', summary: '前后端骨架、健康检查与 Dashboard 统计' },
  { version: 'V1', title: '文档导入与分块', summary: '知识库、文档上传解析、固定分块与样例数据' },
  { version: 'V2', title: 'Naive RAG', summary: 'PgVector 向量检索与 Chat 问答闭环' },
  { version: 'V2.5', title: '真实模型接入', summary: 'Qwen Embedding、DeepSeek Chat Provider' },
  { version: 'V3', title: 'Debug 可观察', summary: '召回、Context、Prompt、Answer 与耗时全链路' },
  { version: 'V4', title: 'BM25', summary: 'Elasticsearch 关键词检索与索引重建' },
  { version: 'V5', title: 'Hybrid Search', summary: 'Vector 与 BM25 应用层融合检索' },
  { version: 'V6', title: 'Reranker', summary: '轻量本地重排与排名可观察' },
  { version: 'V7', title: 'Evaluation', summary: '内置用例 Top1 命中与三模式评测对比' },
  { version: 'V8', title: '工程化增强', summary: '系统状态、指标、日志、慢查询、参数实验与项目总览' },
]

interface QuickStartItem {
  title: string
  desc: string
  command?: string
  path?: string
  linkLabel?: string
  action?: 'initSample'
}

const quickStartItems: QuickStartItem[] = [
  {
    title: '启动后端',
    desc: 'JDK 17 + Maven，dev profile',
    command: 'cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev',
  },
  {
    title: '启动前端',
    desc: 'Node 18+，Vite 开发服务器',
    command: 'cd frontend && npm install && npm run dev',
  },
  {
    title: '初始化样例数据',
    desc: '导入 3 个内置 Markdown 文档到样例知识库',
    action: 'initSample',
    path: '/kb',
    linkLabel: '知识库',
  },
  {
    title: '重建向量 / ES 索引',
    desc: '问答页重建向量；Debug 页重建 ES（BM25/Hybrid）',
    path: '/chat',
    linkLabel: '问答 / Debug',
  },
  {
    title: 'Debug 控制台',
    desc: 'Vector / BM25 / Hybrid 与 Reranker 全链路',
    path: '/debug',
  },
  {
    title: 'Evaluation 评测中心',
    desc: '单模式与三模式 Top1 命中率对比',
    path: '/evaluation',
  },
  {
    title: '参数实验台',
    desc: '调整 Context 参数并多组对比',
    path: '/rag-experiment',
  },
]

interface UseCaseItem {
  title: string
  desc: string
}

const useCases: UseCaseItem[] = [
  { title: 'RAG 学习实验', desc: '按版本理解文档分块、检索、生成与可观察链路' },
  { title: '企业知识库问答原型', desc: '快速搭建知识库问答与引用来源展示' },
  { title: '检索策略对比', desc: 'Vector、BM25、Hybrid 与 Reranker 效果横向比较' },
  { title: 'RAG 效果评测', desc: '内置用例批量验收 Top1 文档命中率' },
  { title: '参数调优与问题排查', desc: '实验台调参、日志中心与慢查询分析定位瓶颈' },
]

interface OnlineDeploymentStatusItem {
  label: string
  value: string
  type?: 'success' | 'warning' | 'info'
  note?: string
}

const onlineDeploymentStatus: OnlineDeploymentStatusItem[] = [
  { label: '线上部署状态', value: '已联调', type: 'success', note: 'Runbook + 验收脚本 V9-06' },
  { label: '数据与检索层', value: '已部署', type: 'success', note: 'ECS Compose：PG / ES / Redis' },
  { label: '应用入口层', value: '已部署', type: 'success', note: 'Nginx + dist + Java 后端' },
  { label: '内网访问', value: '已验证', type: 'success', note: '轻量 → ECS 内网 PG / ES / Redis' },
  { label: '当前访问方式', value: '公网 IP', type: 'info', note: 'http://<LIGHT_SERVER_PUBLIC_IP>/（备案前）' },
]

interface DeploymentReadinessItem {
  item: string
  desc: string
  path: string
  status?: string
  statusType?: 'success' | 'warning' | 'info'
}

const deploymentReadiness: DeploymentReadinessItem[] = [
  {
    item: '双服务器部署拓扑',
    desc: '应用入口层（轻量 2C4G）+ 数据检索层（ECS 4C8G），内网互联',
    path: 'docs/09-production-deployment.md',
    status: '已配置',
    statusType: 'success',
  },
  {
    item: '后端生产配置',
    desc: 'jar 运行在轻量服务器；通过内网连接 ECS 上的 PG / ES',
    path: 'backend/src/main/resources/application-prod.yml',
  },
  {
    item: '前端构建配置',
    desc: '同域访问 /api，VITE_API_BASE_URL 留空',
    path: 'frontend/.env.production.example',
  },
  {
    item: 'Nginx 配置模板',
    desc: '轻量服务器：静态资源 + /api → 本机 8080；不暴露数据层端口',
    path: 'deploy/nginx.conf.example',
  },
  {
    item: '生产环境变量模板',
    desc: 'POSTGRES / ES / REDIS 使用 <ECS_PRIVATE_IP> 占位符',
    path: '.env.prod.example',
  },
  {
    item: '双服务器环境准备',
    desc: '数据层 ECS 与应用层轻量服务器初始化说明',
    path: 'docs/10-aliyun-ecs-setup.md',
    status: '已规划',
    statusType: 'info',
  },
  {
    item: '数据与检索层 Compose',
    desc: 'PG + PgVector、Elasticsearch、Redis；卷 postgres_data / es_data / redis_data',
    path: 'deploy/data-layer/docker-compose.data.yml',
    status: '已准备',
    statusType: 'success',
  },
  {
    item: '数据层健康检查脚本',
    desc: '检查 PG / ES / Redis 端口与服务；支持传入 <ECS_PRIVATE_IP>',
    path: 'scripts/check-data-layer.sh',
    status: '已准备',
    statusType: 'success',
  },
  {
    item: '应用入口层部署脚本',
    desc: 'deploy-backend-app.sh、deploy-frontend-app.sh、.env.app',
    path: 'deploy/app-layer/README.md',
    status: '已准备',
    statusType: 'success',
  },
  {
    item: 'Nginx 应用层配置',
    desc: '静态资源 + /api → 本机 8080；Vue Router history',
    path: 'deploy/app-layer/nginx-rag.conf.example',
    status: '已准备',
    statusType: 'success',
  },
  {
    item: '应用层健康检查脚本',
    desc: 'Java、Nginx、8080、静态目录、/api/system/health、数据层端口',
    path: 'scripts/check-app-layer.sh',
    status: '已准备',
    statusType: 'success',
  },
  {
    item: '实机联调 Runbook',
    desc: '双服务器部署步骤、检查清单、常见问题',
    path: 'docs/11-dual-server-online-runbook.md',
    status: '已准备',
    statusType: 'success',
  },
  {
    item: '线上联调验收脚本',
    desc: 'verify-online-deployment.sh、init-online-data.sh',
    path: 'scripts/verify-online-deployment.sh',
    status: '已准备',
    statusType: 'success',
  },
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

async function handleInitSample() {
  if (!requireWrite()) return
  initSampleLoading.value = true
  try {
    const res = await initSampleData()
    if (res.code === 200) {
      ElMessage.success(res.data?.message || '样例数据初始化成功')
    } else {
      ElMessage.error(res.message || '初始化失败')
    }
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : '请求失败'
    ElMessage.error(message)
  } finally {
    initSampleLoading.value = false
  }
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

.guest-mode-alert {
  margin-top: 12px;
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

.quick-card {
  margin-bottom: 12px;
  min-height: 160px;
}

.quick-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.quick-desc {
  margin: 0 0 8px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.quick-command {
  margin: 0 0 10px;
  padding: 8px 10px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-all;
  color: #303133;
}

.scene-card {
  margin-bottom: 12px;
  min-height: 100px;
}

.scene-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.scene-desc {
  margin: 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.deploy-path {
  font-size: 12px;
  color: #409eff;
  word-break: break-all;
}

.dual-server-desc {
  margin-bottom: 0;
}

.online-status-desc {
  margin-top: 16px;
}

.online-status-note {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
