import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import AppHeader from '../../components/layout/AppHeader'
import './HomePage.css'

const TILES: { to: string; num: string; en: string; cn: string; desc: string; illo: string }[] = [
  {
    to: '/app/tasks',
    num: '01',
    en: 'task',
    cn: '跑腿',
    desc: '取快递、带饭、占座 —— 校园里的小麻烦交给同学。',
    illo: '/illustrations/free-time.png',
  },
  {
    to: '/app/trade',
    num: '02',
    en: 'trade',
    cn: '二手',
    desc: '同校面交、积分结算。',
    illo: '/illustrations/coffee.png',
  },
  {
    to: '/app/edu/tutor',
    num: '03',
    en: 'tutor',
    cn: '辅导',
    desc: '找懂你专业的学长学姐。课程对口、信用透明。',
    illo: '/illustrations/focused.png',
  },
  {
    to: '/app/team',
    num: '04',
    en: 'team',
    cn: '组队',
    desc: '比赛 / 课设 / 毕设 —— 标签匹配，技能互补。',
    illo: '/illustrations/catching-up.png',
  },
]

export default function HomePage() {
  useEffect(() => {
    // sticky header 已由 AppHeader 自治，不在此处重复

    // ── split text（仅 H1） ──────────────────────────────────────────
    let chIdx = 0
    const splitNode = (node: Node) => {
      if (node.nodeType === Node.TEXT_NODE) {
        const frag = document.createDocumentFragment()
        const text = node.textContent ?? ''
          ;[...text].forEach((c) => {
            const s = document.createElement('span')
            s.className = 'ch'
            s.textContent = c === ' ' ? ' ' : c
            s.style.transitionDelay = `${chIdx++ * 28}ms`
            frag.appendChild(s)
          })
        node.parentNode?.replaceChild(frag, node)
      } else if (node.nodeType === Node.ELEMENT_NODE) {
        ;[...node.childNodes].forEach(splitNode)
      }
    }
    document.querySelectorAll<HTMLElement>('[data-split]').forEach((el) => {
      if (el.dataset.splitDone === '1') return
      chIdx = 0
        ;[...el.childNodes].forEach(splitNode)
      el.dataset.splitDone = '1'
    })

    // ── scroll reveal ────────────────────────────────────────────────
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (!e.isIntersecting) return
          const el = e.target as HTMLElement
          const delay = parseInt(el.dataset.delay || '0', 10)
          if (el.matches('[data-split]')) {
            setTimeout(() => {
              el.querySelectorAll('.ch').forEach((ch) => ch.classList.add('in'))
            }, delay)
          } else {
            setTimeout(() => el.classList.add('in'), delay)
          }
          io.unobserve(el)
        })
      },
      { threshold: 0.12, rootMargin: '0px 0px -8% 0px' },
    )
    document.querySelectorAll<HTMLElement>('[data-rise],[data-split]').forEach((el) => io.observe(el))

    // ── cursor follower ─────────────────────────────────────────────
    const cur = document.getElementById('cursor')
    let mx = 0, my = 0, cx = 0, cy = 0, raf = 0
    const onMove = (e: MouseEvent) => { mx = e.clientX; my = e.clientY }
    window.addEventListener('mousemove', onMove)
    const frame = () => {
      cx += (mx - cx) * 0.18
      cy += (my - cy) * 0.18
      if (cur) cur.style.transform = `translate(${cx}px,${cy}px) translate(-50%,-50%)`
      raf = requestAnimationFrame(frame)
    }
    frame()
    const hovers = document.querySelectorAll<HTMLElement>('a, button, .tile')
    const onEnter = () => cur?.classList.add('lg')
    const onLeave = () => cur?.classList.remove('lg')
    hovers.forEach((el) => {
      el.addEventListener('mouseenter', onEnter)
      el.addEventListener('mouseleave', onLeave)
    })

    return () => {
      window.removeEventListener('mousemove', onMove)
      cancelAnimationFrame(raf)
      io.disconnect()
      hovers.forEach((el) => {
        el.removeEventListener('mouseenter', onEnter)
        el.removeEventListener('mouseleave', onLeave)
      })
    }
  }, [])

  return (
    <>
      <div className="cursor" id="cursor" />

      {/* 通用 AppHeader：根据登录态自动切换简洁版 / 完整 nav 版 */}
      <AppHeader />

      <main className="home-shell">
        {/* ─────────── HERO ─────────── */}
        <section className="home-hero">
          <div className="eyebrow rise" data-rise>
            <span className="pulse" />
            <span>南京大学 · 软件工程二大作业 · 2026</span>
            <span className="ln" />
          </div>

          <h1 className="home-title">
            <span className="row split whole" data-split>南京大学校园互助平台</span>
          </h1>

          <p className="home-sub rise" data-rise data-delay="220">
            南京大学<span className="serif it"> 校园互助平台 </span>—— 跑腿、二手、辅导、组队，全部在这里完成。
          </p>
        </section>

        {/* ─────────── 4 FUNCTION TILES ─────────── */}
        <section className="home-tiles">
          {TILES.map((t, i) => (
            <Link
              key={t.to}
              to={t.to}
              className="tile rise"
              data-rise
              data-delay={String(120 + i * 80)}
            >
              <div className="tile-head">
                <span className="tile-num">{t.num}</span>
                <span className="tile-en">{t.en}</span>
              </div>
              <h3 className="tile-title">
                <span className="it">{t.cn}</span>
              </h3>
              <p className="tile-desc">{t.desc}</p>
              <div className="tile-foot">
                <span className="tile-cta">立即去 <span aria-hidden>→</span></span>
                <img className="tile-illo" src={t.illo} alt="" loading="lazy" />
              </div>
            </Link>
          ))}
        </section>

        {/* ─────────── FOOTER ─────────── */}
        <footer className="home-foot">
          <div className="home-foot-left">
            <span className="dot-small" />
            <span>CampusHub · v 1.0</span>
          </div>
          <div className="home-foot-right mono">
            南京大学 · 软件工程与计算II大作业 · 2026 · 開發者的力量团队
          </div>
        </footer>
      </main>
    </>
  )
}
