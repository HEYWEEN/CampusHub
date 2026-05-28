import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../stores/auth'
import { loginByCode, sendSmsCode } from '../../api/auth'
import { BizError } from '../../types/api'
import './LoginPage.css'

export default function LoginPage() {
  const navigate = useNavigate()
  const login = useAuthStore((s) => s.login)
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [sentCountdown, setSentCountdown] = useState(0)

  const handleGetCode = async () => {
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      setError('请输入有效的 11 位手机号')
      return
    }
    setError('')
    try {
      await sendSmsCode(phone)
      setSentCountdown(60)
      const id = window.setInterval(() => {
        setSentCountdown((n) => {
          if (n <= 1) {
            window.clearInterval(id)
            return 0
          }
          return n - 1
        })
      }, 1000)
    } catch (err) {
      setError(err instanceof BizError ? err.message : '验证码发送失败')
    }
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!phone || !code) {
      setError('请填写手机号和验证码')
      return
    }
    setLoading(true)
    setError('')
    try {
      const tokens = await loginByCode(phone, code)
      login(tokens)
      if (tokens.verifyStatus === 'guest') {
        navigate('/verify', { replace: true })
      } else {
        navigate('/app/tasks', { replace: true })
      }
    } catch (err) {
      setError(err instanceof BizError ? err.message : '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-page">
      <header className="login-header">
        <a href="/" className="logo">
          <img className="logo-mark" src="/logo.png" alt="CampusHub" />
          <span>CampusHub</span>
        </a>
        <a href="/" className="ghost-link">
          返回首页 <span aria-hidden>→</span>
        </a>
      </header>

      <section className="login-hero">
        <div className="login-eyebrow">
          <span className="pulse" />
          <span>登录 · 自动注册</span>
          <span className="ln" />
        </div>

        <h1 className="login-title">
          登录
        </h1>
        <p className="login-sub">
          用学生手机号一键登录，若未注册则会一键注册。
        </p>

        <form className="login-form" onSubmit={handleSubmit} noValidate>
          <div className="login-field">
            <input
              type="tel"
              className="login-input"
              placeholder="请输入手机号"
              value={phone}
              onChange={(e) => {
                setPhone(e.target.value)
                if (error) setError('')
              }}
              maxLength={11}
              autoComplete="tel"
              aria-label="手机号"
            />
          </div>

          <div className="login-field-row">
            <input
              type="text"
              className="login-input"
              placeholder="请输入验证码"
              value={code}
              onChange={(e) => {
                setCode(e.target.value)
                if (error) setError('')
              }}
              maxLength={6}
              inputMode="numeric"
              autoComplete="one-time-code"
              aria-label="验证码"
            />
            <button
              type="button"
              className="login-code-btn"
              onClick={handleGetCode}
              disabled={sentCountdown > 0}
            >
              {sentCountdown > 0 ? `${sentCountdown} s` : '获取验证码'}
            </button>
          </div>

          <div className="login-error-slot" aria-live="polite">
            {error && <span className="login-error">{error}</span>}
          </div>

          <button type="submit" className="login-submit" disabled={loading}>
            <span>{loading ? '登录中…' : '立即登录'}</span>
            <span aria-hidden className="login-arrow">↗</span>
          </button>
        </form>

        <p className="login-tip">
          首次使用？验证通过后会自动跳转
          <a href="/verify" className="login-flip">
            <span>学生证认证</span>
            <span aria-hidden>→</span>
          </a>
        </p>

        {import.meta.env.DEV && (
          <button
            type="button"
            onClick={() => {
              login({
                userId: 'u1',
                accessToken: 'dev-fake-token',
                refreshToken: 'dev-fake-refresh',
                verifyStatus: 'approved',
              })
              navigate('/app/tasks', { replace: true })
            }}
            className="ghost-link"
            style={{ alignSelf: 'flex-start', marginTop: 24, color: 'var(--muted)' }}
          >
            [DEV] 跳过登录，直接进入 App <span aria-hidden>→</span>
          </button>
        )}
      </section>
    </main>
  )
}
