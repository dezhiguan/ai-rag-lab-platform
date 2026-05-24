/** 生产构建 API 根地址；同域 Nginx 反代 /api 时留空 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''
