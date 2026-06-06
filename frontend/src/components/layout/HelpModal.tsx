import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import './Help.css'

interface Section {
  icon: string
  title: string
  steps: string[]
}

const GUIDE: Section[] = [
  {
    icon: '🎓',
    title: '第一步 · 校园认证',
    steps: [
      '在「我的 → 校园认证」上传学生证完成认证。',
      '认证后才能发布任务/商品、接单、私信，信用分初始 100。',
    ],
  },
  {
    icon: '🏃',
    title: '任务 · 跑腿 / 互助 / 辅导',
    steps: [
      '发布：任务大厅 →「发布任务」，设置类型、悬赏积分、截止时间。发布即冻结悬赏作押金。',
      '接单：在大厅或「为你推荐」里挑单，点「接单」。',
      '完成：接单方提交凭证 → 发布方确认 → 积分结算给接单方。',
      '我的任务：「我的 → 我的任务」查看我发布的 / 我接的。',
    ],
  },
  {
    icon: '🛍️',
    title: '二手 · 买卖与砍价',
    steps: [
      '买：商品详情页「立即购买」按标价下单，冻结押金。',
      '砍价：点「出价砍价」给个价 → 卖家可同意 / 还价 / 拒绝，多轮拉锯，谈拢即成单。',
      '处理别人的砍价：「我的 → 我的交易 → 我卖的」，或直接在商品页/消息里点同意 / 还价 / 拒绝。',
      '收尾：买卖双方在「我的交易」各点一次确认收货 / 发货 → 完成结算。',
    ],
  },
  {
    icon: '👥',
    title: '组队 · 找队友',
    steps: [
      '发帖：组队大厅发布招募，写明人数与技能标签。',
      '申请：在帖子里申请加入，队长在「我的」审核同意 / 拒绝。',
    ],
  },
  {
    icon: '💬',
    title: '私信',
    steps: [
      '从用户主页或商品页发起私信，可发文字、图片。',
      '任务/商品详情页「联系卖家」会把订单卡片一并发给对方，点卡片直达详情。',
    ],
  },
  {
    icon: '⭐',
    title: '信用分',
    steps: [
      '范围 0~120，初始 100。低于阈值会被限制发单 / 接单。',
      '差评、爽约、违规会扣分；对不实差评可在「信用」页发起申诉。',
    ],
  },
  {
    icon: '🐾',
    title: '校园助手（AI）',
    steps: [
      '右下角悬浮球，能帮你找任务、生成发单草稿、搜二手商品、找组队。',
      '直接说「帮我找个取快递的单」「有什么二手自行车」即可。',
    ],
  },
  {
    icon: '🔔',
    title: '消息通知',
    steps: [
      '接单、成交、砍价、审核结果等都会发站内信，顶栏铃铛有未读红点。',
      '点通知可直接跳到对应页面处理。',
    ],
  },
]

export default function HelpModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [open, onClose])

  if (!open) return null

  return createPortal(
    <div className="help-overlay" onClick={onClose}>
      <div className="help-panel" onClick={(e) => e.stopPropagation()} role="dialog" aria-label="使用说明">
        <div className="help-head">
          <div>
            <h2 className="help-title">使用<span className="it">说明</span></h2>
            <p className="help-sub">CampusHub · 校园互助平台 · 快速上手</p>
          </div>
          <button type="button" className="help-close" onClick={onClose} aria-label="关闭">×</button>
        </div>

        <div className="help-body">
          {GUIDE.map((s) => (
            <section key={s.title} className="help-card">
              <div className="help-card-head">
                <span className="help-card-icon" aria-hidden>{s.icon}</span>
                <h3 className="help-card-title">{s.title}</h3>
              </div>
              <ul className="help-steps">
                {s.steps.map((t, i) => (
                  <li key={i}>{t}</li>
                ))}
              </ul>
            </section>
          ))}
        </div>

        <div className="help-foot">
          <span>遇到问题？右下角喊一声<b> 校园助手 🐾</b></span>
        </div>
      </div>
    </div>,
    document.body,
  )
}
