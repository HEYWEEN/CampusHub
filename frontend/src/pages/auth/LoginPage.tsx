import { useRef, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../stores/auth'
import { loginByCode, loginByPassword, sendSmsCode } from '../../api/auth'
import { changePassword } from '../../api/user'
import { BizError, type TokenPair } from '../../types/api'
import './LoginPage.css'

/** 后端 AuthErrorCode.PASSWORD_NOT_SET —— 验证码-only 用户尝试密码登录。 */
const PASSWORD_NOT_SET = 2005

type Mode = 'code' | 'password'

const PHONE_RE = /^1[3-9]\d{9}$/
const PW_RE = /^(?=.*[A-Za-z])(?=.*\d)[\x21-\x7E]{8,32}$/

export default function LoginPage() {
  const navigate = useNavigate()
  const login = useAuthStore((s) => s.login)

  // 默认验证码登录（更直观、未注册可自动注册）
  const [mode, setMode] = useState<Mode>('code')
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [codeSent, setCodeSent] = useState(false)
  const [registered, setRegistered] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [sentCountdown, setSentCountdown] = useState(0)
  const timerRef = useRef<number | null>(null)

  const switchMode = (next: Mode) => {
    setMode(next)
    setError('')
    setCode('')
    setPassword('')
    setCodeSent(false)
    setRegistered(false)
  }

  const navigateAfter = (tokens: TokenPair) => {
    navigate(tokens.verifyStatus === 'guest' ? '/verify' : '/', { replace: true })
  }

  const handleGetCode = async () => {
    if (!PHONE_RE.test(phone)) {
      setError('请输入有效的 11 位手机号')
      return
    }
    setError('')
    try {
      const { registered: isReg } = await sendSmsCode(phone)
      setRegistered(isReg)
      // 已注册账号若残留了密码输入，清掉（不会再展示设密框）
      if (isReg) setPassword('')
      setCodeSent(true)
      setSentCountdown(60)
      if (timerRef.current) window.clearInterval(timerRef.current)
      timerRef.current = window.setInterval(() => {
        setSentCountdown((n) => {
          if (n <= 1) {
            if (timerRef.current) window.clearInterval(timerRef.current)
            return 0
          }
          return n - 1
        })
      }, 1000)
    } catch (err) {
      setError(err instanceof BizError ? err.message : '验证码发送失败')
    }
  }

  const submitCode = async () => {
    if (!code) {
      setError('请填写验证码')
      return
    }
    const wantPassword = password.trim().length > 0
    if (wantPassword && !PW_RE.test(password)) {
      setError('密码需 8–32 位，且同时包含字母和数字')
      return
    }
    setLoading(true)
    setError('')
    try {
      // 1) 验证码登录：新号自动注册（无密码），并消费一次验证码
      const tokens = await loginByCode(phone, code)
      login(tokens)
      // 2) 若填了密码 → 设为登录密码（无密码账号免原密码；已有密码者会失败，已登录故忽略）
      if (wantPassword) {
        try {
          await changePassword(password)
        } catch { /* 已有密码用户：忽略，登录本身已成功 */ }
      }
      navigateAfter(tokens)
    } catch (err) {
      setError(err instanceof BizError ? err.message : '操作失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  const submitPassword = async () => {
    if (!password) {
      setError('请填写密码')
      return
    }
    setLoading(true)
    setError('')
    try {
      const tokens = await loginByPassword(phone, password)
      login(tokens)
      navigateAfter(tokens)
    } catch (err) {
      if (err instanceof BizError && err.code === PASSWORD_NOT_SET) {
        setError('该账号还没有设置密码，请改用验证码登录')
      } else {
        setError(err instanceof BizError ? err.message : '操作失败，请重试')
      }
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    // 内置超级管理员用 phone="admin" 走密码登录；其余一律 11 位手机号
    const phoneOk = PHONE_RE.test(phone) || (mode === 'password' && phone === 'admin')
    if (!phoneOk) {
      setError('请输入有效的 11 位手机号')
      return
    }
    if (mode === 'code') submitCode()
    else submitPassword()
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
          <span>{mode === 'code' ? '验证码登录 · 未注册自动注册' : '密码登录'}</span>
          <span className="ln" />
        </div>

        <h1 className="login-title">登录</h1>
        <p className="login-sub">
          {mode === 'code'
            ? '用学生手机号 + 短信验证码登录；新用户将自动注册。'
            : '用学生手机号 + 密码登录。'}
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

          {mode === 'code' ? (
            <>
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

              {/* 已注册账号：提示直接用验证码登录，不再显示设密框 */}
              {codeSent && registered && (
                <div className="login-field-hint">该手机号已注册，输入验证码即可登录。</div>
              )}

              {/* 发送验证码后、且该手机号未注册时才出现：可选设置登录密码 */}
              {codeSent && !registered && (
                <div className="login-field">
                  <input
                    type="password"
                    className="login-input"
                    placeholder="设置登录密码（可选）"
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value)
                      if (error) setError('')
                    }}
                    maxLength={32}
                    autoComplete="new-password"
                    aria-label="设置登录密码（可选）"
                  />
                  <div className="login-field-hint">
                    8–32 位、字母+数字。留空则仅用验证码登录，可日后在「我的」里补设。
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="login-field">
              <input
                type="password"
                className="login-input"
                placeholder="请输入密码"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value)
                  if (error) setError('')
                }}
                maxLength={32}
                autoComplete="current-password"
                aria-label="密码"
              />
            </div>
          )}

          <div className="login-error-slot" aria-live="polite">
            {error && <span className="login-error">{error}</span>}
          </div>

          <button type="submit" className="login-submit" disabled={loading}>
            <span>{loading ? '处理中…' : '登录'}</span>
            <span aria-hidden className="login-arrow">↗</span>
          </button>
        </form>

        {/* 切换登录方式 */}
        <p className="login-tip">
          {mode === 'code' ? '已设置过密码？' : '没有密码或想自动注册？'}
          <button
            type="button"
            className="login-flip login-linkbtn"
            onClick={() => switchMode(mode === 'code' ? 'password' : 'code')}
          >
            <span>{mode === 'code' ? '改用密码登录' : '改用验证码登录'}</span>
            <span aria-hidden>→</span>
          </button>
        </p>
      </section>
    </main>
  )
}
