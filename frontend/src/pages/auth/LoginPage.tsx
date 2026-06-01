import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../stores/auth'
import { loginByCode, loginByPassword, register, sendSmsCode } from '../../api/auth'
import { BizError, type TokenPair } from '../../types/api'
import './LoginPage.css'

/** 后端 AuthErrorCode.PASSWORD_NOT_SET —— 验证码-only 用户尝试密码登录。 */
const PASSWORD_NOT_SET = 2005

type Mode = 'password' | 'code' | 'setpw'

const PHONE_RE = /^1[3-9]\d{9}$/

export default function LoginPage() {
  const navigate = useNavigate()
  const login = useAuthStore((s) => s.login)

  const [mode, setMode] = useState<Mode>('password')
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [sentCountdown, setSentCountdown] = useState(0)

  const switchMode = (next: Mode) => {
    setMode(next)
    setError('')
    setCode('')
    setPassword('')
  }

  const finishLogin = (tokens: TokenPair) => {
    login(tokens)
    navigate(tokens.verifyStatus === 'guest' ? '/verify' : '/app/tasks', { replace: true })
  }

  const handleGetCode = async () => {
    if (!PHONE_RE.test(phone)) {
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
    if (!PHONE_RE.test(phone)) {
      setError('请输入有效的 11 位手机号')
      return
    }
    if (mode === 'code' && !code) {
      setError('请填写验证码')
      return
    }
    if (mode === 'password' && !password) {
      setError('请填写密码')
      return
    }
    if (mode === 'setpw') {
      if (!code) {
        setError('请填写验证码')
        return
      }
      if (!/^(?=.*[A-Za-z])(?=.*\d)[\x21-\x7E]{8,32}$/.test(password)) {
        setError('密码需 8–32 位，且同时包含字母和数字')
        return
      }
    }

    setLoading(true)
    setError('')
    try {
      if (mode === 'password') {
        finishLogin(await loginByPassword(phone, password))
      } else if (mode === 'code') {
        finishLogin(await loginByCode(phone, code))
      } else {
        finishLogin(await register(phone, code, password))
      }
    } catch (err) {
      if (mode === 'password' && err instanceof BizError && err.code === PASSWORD_NOT_SET) {
        // 验证码-only 用户：引导去设置密码
        setError('该账号还没有设置密码，点下方「设置密码」补设后即可使用')
      } else {
        setError(err instanceof BizError ? err.message : '操作失败，请重试')
      }
    } finally {
      setLoading(false)
    }
  }

  const eyebrow =
    mode === 'password' ? '密码登录' : mode === 'code' ? '验证码登录 · 自动注册' : '设置登录密码'
  const title = mode === 'setpw' ? '设置密码' : '登录'
  const sub =
    mode === 'password'
      ? '用学生手机号 + 密码登录。'
      : mode === 'code'
        ? '用学生手机号一键登录，若未注册则会一键注册。'
        : '验证手机号后设置登录密码，之后即可用密码登录。'

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
          <span>{eyebrow}</span>
          <span className="ln" />
        </div>

        <h1 className="login-title">{title}</h1>
        <p className="login-sub">{sub}</p>

        {/* 登录方式切换（设置密码模式下隐藏） */}
        {mode !== 'setpw' && (
          <div className="login-tabs" role="tablist">
            <button
              type="button"
              role="tab"
              aria-selected={mode === 'password'}
              className={`login-tab${mode === 'password' ? ' active' : ''}`}
              onClick={() => switchMode('password')}
            >
              密码登录
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={mode === 'code'}
              className={`login-tab${mode === 'code' ? ' active' : ''}`}
              onClick={() => switchMode('code')}
            >
              验证码登录
            </button>
          </div>
        )}

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

          {/* 验证码行：验证码登录 / 设置密码 模式需要 */}
          {(mode === 'code' || mode === 'setpw') && (
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
          )}

          {/* 密码行：密码登录 / 设置密码 模式需要 */}
          {(mode === 'password' || mode === 'setpw') && (
            <div className="login-field">
              <input
                type="password"
                className="login-input"
                placeholder={mode === 'setpw' ? '设置密码（8–32 位，字母+数字）' : '请输入密码'}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value)
                  if (error) setError('')
                }}
                maxLength={32}
                autoComplete={mode === 'setpw' ? 'new-password' : 'current-password'}
                aria-label="密码"
              />
            </div>
          )}

          <div className="login-error-slot" aria-live="polite">
            {error && <span className="login-error">{error}</span>}
          </div>

          <button type="submit" className="login-submit" disabled={loading}>
            <span>
              {loading
                ? '处理中…'
                : mode === 'setpw'
                  ? '设置并登录'
                  : '立即登录'}
            </span>
            <span aria-hidden className="login-arrow">↗</span>
          </button>
        </form>

        {/* 底部辅助链接 */}
        {mode === 'setpw' ? (
          <p className="login-tip">
            已有密码？
            <button type="button" className="login-flip login-linkbtn" onClick={() => switchMode('password')}>
              <span>返回登录</span>
              <span aria-hidden>→</span>
            </button>
          </p>
        ) : (
          <p className="login-tip">
            还没有密码？
            <button type="button" className="login-flip login-linkbtn" onClick={() => switchMode('setpw')}>
              <span>设置密码</span>
              <span aria-hidden>→</span>
            </button>
          </p>
        )}

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
