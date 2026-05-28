import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../stores/auth'
import './LoginPage.css'

export default function VerifyPage() {
  const navigate = useNavigate()
  const verifyStatus = useAuthStore((s) => s.verifyStatus)

  return (
    <main className="login-page">
      <header className="login-header">
        <a href="/" className="logo">
          <img className="logo-mark" src="/logo.png" alt="CampusHub" />
          <span>CampusHub</span>
        </a>
        <button
          onClick={() => navigate('/app/tasks')}
          className="ghost-link"
          style={{ border: 0, background: 'transparent', cursor: 'pointer' }}
        >
          稍后再说 <span aria-hidden>→</span>
        </button>
      </header>

      <section className="login-hero">
        <div className="login-eyebrow">
          <span className="pulse" />
          <span>学生认证 · FE-B 阶段实施</span>
          <span className="ln" />
        </div>

        <h1 className="login-title">
          One <span className="it">last step.</span>
        </h1>
        <p className="login-sub">
          上传学生证 / 校园卡照片以激活完整功能。当前状态：
          <strong style={{ color: 'var(--accent)', marginLeft: 8 }}>{verifyStatus}</strong>
        </p>

        <div className="login-error-slot" style={{ marginTop: 56 }}>
          <span className="login-error" style={{ color: 'var(--muted)' }}>
            上传 / 拍照表单待 FE-B 阶段实现（F-AUTH-03）
          </span>
        </div>

        <button
          onClick={() => navigate('/app/tasks')}
          className="login-submit"
          style={{ marginTop: 8, maxWidth: 460 }}
        >
          <span>先体验未认证版本</span>
          <span aria-hidden className="login-arrow">↗</span>
        </button>
      </section>
    </main>
  )
}
