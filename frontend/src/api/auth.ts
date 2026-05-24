import { http, type ApiResponse } from './http'
import type { AuthUser, LoginResult } from '@/types/auth'

export interface LoginPayload {
  username: string
  password: string
}

export async function login(payload: LoginPayload) {
  const { data } = await http.post<ApiResponse<LoginResult>>('/api/auth/login', payload)
  return data
}

export async function fetchMe() {
  const { data } = await http.get<ApiResponse<AuthUser>>('/api/auth/me')
  return data
}

export async function logout() {
  const { data } = await http.post<ApiResponse<null>>('/api/auth/logout')
  return data
}
