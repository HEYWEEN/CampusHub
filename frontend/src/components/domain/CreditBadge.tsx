import './Domain.css'

interface Props {
  score: number
  size?: 'sm' | 'md'
}

function tierOf(score: number): 'gold' | 'silver' | 'bronze' | 'low' {
  if (score >= 90) return 'gold'
  if (score >= 75) return 'silver'
  if (score >= 60) return 'bronze'
  return 'low'
}

export default function CreditBadge({ score, size = 'sm' }: Props) {
  const tier = tierOf(score)
  return (
    <span className={`credit-badge credit-${tier} credit-${size}`}>
      <span className="credit-num">{score}</span>
      <span className="credit-label">信用</span>
    </span>
  )
}
