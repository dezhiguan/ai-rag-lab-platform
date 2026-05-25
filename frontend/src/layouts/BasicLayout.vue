<template>
  <el-container class="rag-layout">
    <el-aside :width="sidebarWidth" class="rag-sidebar">
      <div class="sidebar-brand">
        <span class="brand-mark">RAG</span>
        <span class="brand-text">Lab Platform</span>
      </div>
      <el-scrollbar class="sidebar-scroll">
        <el-menu
          :default-active="activeMenu"
          router
          class="sidebar-menu"
        >
          <template v-for="group in visibleMenuGroups" :key="group.key">
            <div class="menu-group-title">{{ group.title }}</div>
            <el-menu-item
              v-for="item in group.items"
              :key="item.path"
              :index="item.path"
            >
              {{ item.label }}
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="rag-main-wrap">
      <el-header class="rag-header" height="var(--rag-header-height)">
        <div class="header-title">AI RAG Lab Platform</div>
        <div class="header-actions">
          <el-tag size="small" :type="authStore.isGuest ? 'warning' : 'success'">
            {{ authStore.modeLabel }}
          </el-tag>
          <span class="header-user">{{ authStore.username }}</span>
          <el-button type="primary" link @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="rag-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const sidebarWidth = 'var(--rag-sidebar-width)'

interface MenuItem {
  path: string
  label: string
  guestVisible: boolean
}

interface MenuGroup {
  key: string
  title: string
  items: MenuItem[]
}

const menuGroups: MenuGroup[] = [
  {
    key: 'overview',
    title: '总览',
    items: [
      { path: '/project-overview', label: '项目总览', guestVisible: true },
      { path: '/dashboard', label: 'Dashboard', guestVisible: false },
    ],
  },
  {
    key: 'experiment',
    title: '实验',
    items: [
      { path: '/chat', label: '问答', guestVisible: false },
      { path: '/debug', label: 'Debug', guestVisible: true },
      { path: '/evaluation', label: '评测中心', guestVisible: true },
      { path: '/rag-experiment', label: '参数实验台', guestVisible: true },
    ],
  },
  {
    key: 'observe',
    title: '可观测',
    items: [
      { path: '/system-status', label: '系统状态', guestVisible: true },
      { path: '/rag-metrics', label: 'RAG 指标', guestVisible: true },
      { path: '/rag-query-logs', label: '查询日志中心', guestVisible: true },
      { path: '/slow-query-analysis', label: '慢查询分析', guestVisible: true },
    ],
  },
  {
    key: 'data',
    title: '数据管理',
    items: [{ path: '/kb', label: '知识库', guestVisible: false }],
  },
  {
    key: 'about',
    title: '说明',
    items: [{ path: '/about', label: 'About', guestVisible: false }],
  },
]

const visibleMenuGroups = computed(() =>
  menuGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => authStore.isAdmin || item.guestVisible),
    }))
    .filter((group) => group.items.length > 0),
)

const activeMenu = computed(() => {
  const p = route.path
  if (p.startsWith('/debug')) return '/debug'
  if (p.startsWith('/evaluation')) return '/evaluation'
  if (p.startsWith('/kb') || p.startsWith('/documents')) return '/kb'
  if (p.startsWith('/chat')) return '/chat'
  if (p.startsWith('/rag-experiment')) return '/rag-experiment'
  if (p.startsWith('/rag-query-logs')) return '/rag-query-logs'
  if (p.startsWith('/slow-query-analysis')) return '/slow-query-analysis'
  if (p.startsWith('/rag-metrics')) return '/rag-metrics'
  if (p.startsWith('/system-status')) return '/system-status'
  return p
})

async function handleLogout() {
  await authStore.logout()
  await router.replace('/login')
}
</script>

<style scoped>
.rag-layout {
  height: 100vh;
  overflow: hidden;
}

.rag-sidebar {
  background: #fff;
  border-right: 1px solid var(--rag-border);
  display: flex;
  flex-direction: column;
}

.sidebar-brand {
  height: var(--rag-header-height);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  border-bottom: 1px solid var(--rag-border);
  flex-shrink: 0;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--rag-primary);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

.brand-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--rag-text-main);
  line-height: 1.3;
}

.sidebar-scroll {
  flex: 1;
}

.sidebar-menu {
  border-right: none;
  padding: 8px 0 16px;
}

.menu-group-title {
  padding: 12px 20px 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--rag-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 40px;
  line-height: 40px;
  margin: 2px 8px;
  border-radius: 8px;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--rag-primary-soft);
  color: var(--rag-primary);
}

.rag-main-wrap {
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.rag-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: var(--rag-card-bg);
  border-bottom: 1px solid var(--rag-border);
  flex-shrink: 0;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--rag-text-main);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.header-user {
  font-size: 14px;
  color: var(--rag-text-secondary);
}

.rag-main {
  background: var(--rag-bg);
  padding: 20px 24px;
  overflow-x: hidden;
  overflow-y: auto;
}
</style>
