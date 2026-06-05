/**
 * 订单卡片（ORDER 私信消息）的编解码。
 *
 * 后端 im_message.content_type=ORDER 时，content 存一段 JSON 快照；
 * 后端不解析、原样透传，渲染/解析全在前端，避免 im 模块耦合 task/trade。
 */

export interface OrderCard {
  kind: 'TASK' | 'TRADE'
  id: number
  title: string
  cover?: string | null
  pricePoint?: number | null   // 任务悬赏 / 商品价格，单位均为积分
}

export function encodeOrderCard(card: OrderCard): string {
  return JSON.stringify(card)
}

/** 解析 ORDER 消息的 content；非法/旧数据返回 null，由调用方降级为纯文本。 */
export function parseOrderCard(content: string): OrderCard | null {
  try {
    const o = JSON.parse(content) as OrderCard
    if (o && (o.kind === 'TASK' || o.kind === 'TRADE') && typeof o.id === 'number' && typeof o.title === 'string') {
      return o
    }
  } catch {
    /* 非 JSON，忽略 */
  }
  return null
}

export function orderDetailPath(card: OrderCard): string {
  return card.kind === 'TASK' ? `/app/tasks/${card.id}` : `/app/trade/${card.id}`
}
