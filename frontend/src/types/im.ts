/**
 * 站内私信 VO — 与后端 im/vo 字段名严格对齐。
 */
import type { PublicUserVO } from './user'

export type ImMessageType = 'TEXT' | 'IMAGE' | 'SYSTEM'

export interface ImConversationVO {
  conversationId: number
  peer: PublicUserVO
  lastMessage: string | null
  lastContentType: ImMessageType | null
  lastMsgAt: string
  unreadCount: number
}

export interface ImMessageVO {
  messageId: number
  senderId: number
  mine: boolean
  contentType: ImMessageType
  content: string
  createdAt: string
}
