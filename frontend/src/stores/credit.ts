import { create } from 'zustand'
import type { CreditMeVO } from '../types/credit'

interface CreditState {
  score: number
  pointBalance: number
  pointFrozen: number
  canPublish: boolean
  canAccept: boolean
  dailyAcceptLimit: number
  hydrate: (me: CreditMeVO) => void
  reset: () => void
}

export const useCreditStore = create<CreditState>()((set) => ({
  score: 80,                 // 默认 80（新用户初始）
  pointBalance: 0,
  pointFrozen: 0,
  canPublish: true,
  canAccept: true,
  dailyAcceptLimit: 1,
  hydrate: (me) =>
    set({
      score: me.creditScore,
      pointBalance: me.pointBalance,
      pointFrozen: me.pointFrozen,
      canPublish: me.canPublish,
      canAccept: me.canAccept,
      dailyAcceptLimit: me.dailyAcceptLimit,
    }),
  reset: () =>
    set({
      score: 80,
      pointBalance: 0,
      pointFrozen: 0,
      canPublish: true,
      canAccept: true,
      dailyAcceptLimit: 1,
    }),
}))
