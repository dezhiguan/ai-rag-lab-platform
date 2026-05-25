<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="logo">AI RAG Lab Platform</div>
      <el-menu
        mode="horizontal"
        :default-active="activeMenu"
        router
        class="menu"
      >
        <el-menu-item
          v-for="item in visibleMenuItems"
          :key="item.path"
          :index="item.path"
        >
          {{ item.label }}
        </el-menu-item>
      </el-menu>
      <div class="user-area">
        <el-tag size="small" :type="authStore.isGuest ? 'warning' : 'success'">
          {{ authStore.modeLabel }}
        </el-tag>
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user-trigger">
            {{ authStore.username }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ authStore.roleLabel }}</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

interface MenuItem {
  path: string
  label: string
  guestVisible: boolean
}

const allMenuItems: MenuItem[] = [
  { path: '/project-overview', label: '项目总览', guestVisible: true },
  { path: '/dashboard', label: 'Dashboard', guestVisible: false },
  { path: '/system-status', label: '系统状态', guestVisible: true },
  { path: '/rag-metrics', label: 'RAG 指标', guestVisible: true },
  { path: '/rag-query-logs', label: '查询日志中心', guestVisible: true },
  { path: '/slow-query-analysis', label: '慢查询分析', guestVisible: true },
  { path: '/rag-experiment', label: '参数实验台', guestVisible: true },
  { path: '/kb', label: '知识库', guestVisible: false },
  { path: '/chat', label: '问答', guestVisible: false },
  { path: '/evaluation', label: '评测中心', guestVisible: true },
  { path: '/debug', label: 'Debug', guestVisible: true },
  { path: '/about', label: 'About', guestVisible: false },
]

const visibleMenuItems = computed(() =>
  allMenuItems.filter((item) => authStore.isAdmin || item.guestVisible),
)

const activeMenu = computed(() => {
  if (route.path.startsWith('/debug')) return '/debug'
  if (route.path.startsWith('/evaluation')) return '/evaluation'
  return route.path
})

async function handleCommand(command: string) {
  if (command === 'logout') {
    await authStore.logout()
    await router.replace('/login')
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-light);
  background: #fff;
  padding: 0 24px;
  gap: 16px;
}

.logo {
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}

.menu {
  flex: 1;
  border-bottom: none;
  min-width: 0;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #303133;
  font-size: 14px;
}

.main {
  background: #f5f7fa;
  padding: 24px;
}
</style>
