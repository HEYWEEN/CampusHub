import './LoginPage.css'

export default function RegisterPage() {
  return (
    <main className="login-page">
      <header className="login-header">
        <a href="/" className="logo">
          <img className="logo-mark" src="/logo.png" alt="CampusHub" />
          <span>CampusHub</span>
        </a>
        <a href="/login" className="ghost-link">
          已有账号 · 登录 <span aria-hidden>→</span>
        </a>
      </header>

      <section className="login-hero">
        <div className="login-eyebrow">
          <span className="pulse" />
          <span>注册 · FE-B 阶段实施</span>
          <span className="ln" />
        </div>

        <h1 className="login-title">
          New <span className="it">here?</span>
        </h1>
        <p className="login-sub">
          目前推荐直接走"登录页"——首次输入手机号会自动建账，无需单独注册流程。
        </p>

        <a href="/login" className="login-submit" style={{ marginTop: 56, maxWidth: 460, textDecoration: 'none' }}>
          <span>去登录页</span>
          <span aria-hidden className="login-arrow">↗</span>
        </a>
      </section>
    </main>
  )
}
