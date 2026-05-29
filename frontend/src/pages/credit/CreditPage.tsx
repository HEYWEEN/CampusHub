import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getMyCredit, listMyRecords } from '../../api/credit'
import type { CreditRecord, CreditRecordDirection } from '../../types/credit'
import { formatRelativeTime } from '../../utils/format'
import '../me/User.css'
import '../tasks/Tasks.css'
import './Credit.css'

const DIRECTION_LABEL: Record<CreditRecordDirection, { text: string; tone: string }> = {
  FREEZE:   { text: '冻结',  tone: 'wait' },
  UNFREEZE: { text: '解冻',  tone: 'pending' },
  SETTLE:   { text: '结算',  tone: 'done' },
  DEDUCT:   { text: '扣减',  tone: 'expired' },
}

const REASON_LABEL: Record<string, string> = {
  TASK_PUBLISH:   '发布任务 · 冻结悬赏',
  TASK_COMPLETE:  '任务完成 · 结算',
  TASK_CANCEL:    '任务取消 · 解冻',
  TUTOR_COMPLETE: '辅导完成 · 结算',
  BAD_REVIEW:     '差评扣分',
  TRADE_FREEZE:   '二手下单 · 冻结',
  TRADE_COMPLETE: '二手完成 · 结算',
}

export default function CreditPage() {
  const { data: credit, isLoading: l1 } = useQuery({
    queryKey: ['credit', 'me'],
    queryFn: () => getMyCredit(),
  })
  const { data: recordsPage, isLoading: l2 } = useQuery({
    queryKey: ['credit', 'records'],
    queryFn: () => listMyRecords(),
  })
  const records = recordsPage?.items ?? []
  const totalRecords = recordsPage?.total ?? 0

  const isLoading = l1 || l2

  return (
    <div className="wrap">
      <div className="page-head">
        <h1 className="page-title">
          信用 · <span className="it">积分</span>。
        </h1>
        <div className="page-sub">信用账户 · 积分流水 · 申诉</div>
      </div>

      {isLoading || !credit ? (
        <div className="task-loading">加载中…</div>
      ) : (
        <>
          {/* 信用 + 积分 概览 */}
          <section className="me-card" style={{ marginBottom: 32 }}>
            <div className="credit-grid">
              <div className="credit-stat">
                <div className="credit-stat-num is-accent">{credit.creditScore}</div>
                <div className="credit-stat-label">信用分</div>
              </div>
              <div className="credit-stat">
                <div className="credit-stat-num">{credit.pointBalance}</div>
                <div className="credit-stat-label">可用积分</div>
              </div>
              <div className="credit-stat">
                <div className="credit-stat-num is-frozen">{credit.pointFrozen}</div>
                <div className="credit-stat-label">冻结积分</div>
              </div>
              <div className="credit-stat">
                <div className="credit-stat-num">
                  {credit.dailyAcceptLimit}<span className="credit-stat-unit">/3</span>
                </div>
                <div className="credit-stat-label">日接单上限</div>
              </div>
            </div>

            <div className="credit-bar">
              <div className="credit-bar-track">
                <div className="credit-bar-fill" style={{ width: `${Math.min(100, credit.creditScore)}%` }} />
              </div>
              <div className="credit-bar-meta">
                <span>0 · 禁止</span>
                <span>60 · 可发可接</span>
                <span>100 · 优秀</span>
              </div>
            </div>

            <div style={{ marginTop: 20, display: 'flex', gap: 12, flexWrap: 'wrap' }}>
              <Link to="/app/credit/appeals" className="me-action-btn is-ghost" style={{ width: 'auto' }}>
                我的申诉记录 →
              </Link>
              {!credit.canPublish && (
                <span className="form-error" style={{ margin: 0 }}>
                  信用分低于 60 · 暂时无法发布任务
                </span>
              )}
            </div>
          </section>

          {/* 积分流水 */}
          <section className="me-card">
            <div className="me-card-head">
              <h3 className="me-card-title">
                积分<span className="it">流水</span>
              </h3>
              <span className="page-sub" style={{ margin: 0 }}>{totalRecords} 条记录</span>
            </div>

            {records.length === 0 ? (
              <div className="task-empty" style={{ padding: '48px 0' }}>
                还没有<span className="it">流水。</span>
              </div>
            ) : (
              <table className="credit-table">
                <thead>
                  <tr>
                    <th>方向</th>
                    <th>数量</th>
                    <th>原因</th>
                    <th>时间</th>
                  </tr>
                </thead>
                <tbody>
                  {records.map((r) => (
                    <RecordRow key={r.id} record={r} />
                  ))}
                </tbody>
              </table>
            )}
          </section>
        </>
      )}
    </div>
  )
}

function RecordRow({ record }: { record: CreditRecord }) {
  const dir = DIRECTION_LABEL[record.direction]
  const reason = REASON_LABEL[record.reasonCode] ?? record.reasonCode
  const sign = record.delta > 0 ? '+' : ''
  return (
    <tr>
      <td><span className={`status-badge status-${dir.tone}`}>{dir.text}</span></td>
      <td className={`credit-delta${record.delta > 0 ? ' is-positive' : record.delta < 0 ? ' is-negative' : ''}`}>
        {sign}{record.delta}
      </td>
      <td>{reason}</td>
      <td className="credit-time">{formatRelativeTime(record.createdAt)}</td>
    </tr>
  )
}
