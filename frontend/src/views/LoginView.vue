<template>
  <div class="login-page">
    <el-card shadow="never" class="login-card">
      <div class="login-header">
        <h1>AI RAG Lab Platform</h1>
        <p>登录后访问 RAG 实验平台</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleSubmit">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="admin / guest" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            autocomplete="current-password"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="handleSubmit">
          登录
        </el-button>
      </el-form>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="login-tip"
        title="体验账号 guest 可浏览与查询；admin 为管理员账号。"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.username.trim(), form.password)
    ElMessage.success('登录成功')
    await router.replace('/project-overview')
  } catch (error) {
    const message = error instanceof Error ? error.message : '登录失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #eef2ff 0%, #f5f7fa 45%, #e8f4ff 100%);
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  border-radius: 12px;
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-header h1 {
  margin: 0 0 8px;
  font-size: 24px;
  color: #303133;
}

.login-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.login-btn {
  width: 100%;
  margin-top: 8px;
}

.login-tip {
  margin-top: 20px;
}
</style>
