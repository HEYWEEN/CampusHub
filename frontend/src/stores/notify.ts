import { create } from 'zustand'

interface NotifyState {
  unreadCount: number
  setUnread: (n: number) => void
  decrement: () => void
}

export const useNotifyStore = create<NotifyState>()((set) => ({
  unreadCount: 0,
  setUnread: (n) => set({ unreadCount: Math.max(0, n) }),
  decrement: () => set((s) => ({ unreadCount: Math.max(0, s.unreadCount - 1) })),
}))
