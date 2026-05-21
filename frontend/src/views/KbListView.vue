<template>
  <el-card shadow="never">
    <template #header>
      <div class="toolbar">
        <span>知识库列表</span>
        <el-button type="primary" @click="showCreate = true">创建知识库</el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="goDocuments(row.id)">文档</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="showCreate" title="创建知识库" width="480px">
    <el-form :model="form" label-width="80px">
      <el-form-item label="名称" required>
        <el-input v-model="form.name" maxlength="100" show-word-limit />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showCreate = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="handleCreate">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createKnowledgeBase,
  deleteKnowledgeBase,
  listKnowledgeBases,
  type KnowledgeBase,
} from '@/api/knowledgeBase'

const router = useRouter()
const loading = ref(false)
const creating = ref(false)
const showCreate = ref(false)
const list = ref<KnowledgeBase[]>([])
const form = reactive({ name: '', description: '' })

async function loadList() {
  loading.value = true
  try {
    const res = await listKnowledgeBases()
    if (res.code === 200) {
      list.value = res.data ?? []
    }
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  creating.value = true
  try {
    const res = await createKnowledgeBase({
      name: form.name.trim(),
      description: form.description.trim() || undefined,
    })
    if (res.code === 200) {
      ElMessage.success('创建成功')
      showCreate.value = false
      form.name = ''
      form.description = ''
      await loadList()
    } else {
      ElMessage.error(res.message)
    }
  } catch {
    ElMessage.error('创建失败')
  } finally {
    creating.value = false
  }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该知识库及其文档吗？', '提示', { type: 'warning' })
  try {
    const res = await deleteKnowledgeBase(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await loadList()
    } else {
      ElMessage.error(res.message)
    }
  } catch {
    /* cancelled */
  }
}

function goDocuments(kbId: number) {
  router.push(`/kb/${kbId}/documents`)
}

onMounted(loadList)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
