import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { TokenPair } from '../types/api'
import type { VerifyStatus } from '../types/user'

interface AuthState {
  userId: string | null
  accessToken: string | null
  refreshToken: string | null
  verifyStatus: VerifyStatus
  login: (tokens: TokenPair) => void
  logout: () => void
  setVerifyStatus: (status: VerifyStatus) => void
  isLoggedIn: () => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      userId: null,
      accessToken: null,
      refreshToken: null,
      verifyStatus: 'guest',
      login: (tokens) =>
        set({
          userId: tokens.userId,
          accessToken: tokens.accessToken,
          refreshToken: tokens.refreshToken,
          verifyStatus: tokens.verifyStatus,
        }),
      logout: () =>
        set({
          userId: null,
          accessToken: null,
          refreshToken: null,
          verifyStatus: 'guest',
        }),
      setVerifyStatus: (status) => set({ verifyStatus: status }),
      isLoggedIn: () => !!get().accessToken,
    }),
    { name: 'campushub-auth' },
  ),
)
