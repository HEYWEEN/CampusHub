import { Link } from 'react-router-dom'
import type { TeamRecruitVO } from '../../types/team'
import PublicUserCard from './PublicUserCard'
import { TEAM_STATUS_LABEL } from '../../utils/labels'
import './Domain.css'

export default function TeamCard({ recruit }: { recruit: TeamRecruitVO }) {
  const st = TEAM_STATUS_LABEL[recruit.status]
  return (
    <Link to={`/app/team/${recruit.recruitId}`} className="task-card">
      <div className="task-card-head">
        <span className="task-type" style={{ color: 'var(--accent)' }}>组队</span>
        <span className={`status-badge status-${st.tone}`}>{st.text}</span>
      </div>

      <h3 className="task-card-title">{recruit.title}</h3>
      {recruit.description && <p className="task-card-desc">{recruit.description}</p>}

      {recruit.skillTags.length > 0 && (
        <div className="team-tags">
          {recruit.skillTags.slice(0, 4).map((t) => (
            <span key={t} className="team-tag">{t}</span>
          ))}
        </div>
      )}

      <div className="task-card-foot">
        <PublicUserCard user={recruit.creator} size="sm" />
        <div className="task-card-foot-right">
          <span className="team-size">{recruit.currentSize}/{recruit.totalSize} 人</span>
          <span className="task-card-action">查看详情</span>
        </div>
      </div>
    </Link>
  )
}
