import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { sendMessage, startConversation } from '../../api/im'
import { BizError } from '../../types/api'
import { encodeOrderCard, type OrderCard } from '../../utils/orderCard'

/**
 * 「一键私信 + 发订单卡片」：开会话 → 发一条 ORDER 消息 → 跳进聊天页。
 * 任务详情 / 二手详情共用，各自 wire 到现有按钮即可。
 *
 * @param onError 失败回调 —— 调用方据此展示错误，避免点击「没反应」的静默失败。
 */
export function useSendOrderCard(onError?: (msg: string) => void) {
  const navigate = useNavigate()
  return useMutation({
    mutationFn: async ({ peerId, card }: { peerId: string | number; card: OrderCard }) => {
      const conv = await startConversation(peerId)
      await sendMessage(conv.conversationId, encodeOrderCard(card), 'ORDER')
      return conv.conversationId
    },
    onSuccess: (cid) => navigate(`/app/im/${cid}`),
    onError: (e) => onError?.(e instanceof BizError ? e.message : '发起私信失败，请稍后再试'),
  })
}
