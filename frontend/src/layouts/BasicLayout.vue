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
        <el-menu-item index="/project-overview">项目总览</el-menu-item>
        <el-menu-item index="/dashboard">Dashboard</el-menu-item>
        <el-menu-item index="/system-status">系统状态</el-menu-item>
        <el-menu-item index="/rag-metrics">RAG 指标</el-menu-item>
        <el-menu-item index="/rag-query-logs">查询日志中心</el-menu-item>
        <el-menu-item index="/slow-query-analysis">慢查询分析</el-menu-item>
        <el-menu-item index="/rag-experiment">参数实验台</el-menu-item>
        <el-menu-item index="/kb">知识库</el-menu-item>
        <el-menu-item index="/chat">问答</el-menu-item>
        <el-menu-item index="/evaluation">评测中心</el-menu-item>
        <el-menu-item index="/debug">Debug</el-menu-item>
        <el-menu-item index="/about">About</el-menu-item>
      </el-menu>
      <div class="user-area">
        <el-tag size="small" type="info">{{ authStore.roleLabel }}</el-tag>
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user-trigger">
            {{ authStore.username }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ authStore.modeLabel }}</el-dropdown-item>
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
