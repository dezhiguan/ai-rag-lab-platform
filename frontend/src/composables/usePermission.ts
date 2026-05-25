import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { GUEST_WRITE_DENIED_MSG } from '@/constants/permission'

export function usePermission() {
  const authStore = useAuthStore()

  const canWrite = computed(() => authStore.user?.role === 'ADMIN')
  const isGuest = computed(() => authStore.user?.role === 'GUEST')
  const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

  function requireWrite(): boolean {
    if (canWrite.value) {
      return true
    }
    ElMessage.warning(GUEST_WRITE_DENIED_MSG)
    return false
  }

  return {
    canWrite,
    isGuest,
    isAdmin,
    requireWrite,
    deniedMessage: GUEST_WRITE_DENIED_MSG,
  }
}
