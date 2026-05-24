export const TOKEN_STORAGE_KEY = 'rag_auth_token'

export type UserRole = 'ADMIN' | 'GUEST'

export interface AuthUser {
  username: string
  role: UserRole
  roleLabel: string
  mode: string
}

export interface LoginResult extends AuthUser {
  token: string
}
