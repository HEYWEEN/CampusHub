import { useEffect } from 'react'
import './HomePage.css'

export default function HomePage() {
  useEffect(() => {
    // ── 1. wrap .flip children in .flip-inner (idempotent) ─────────────
    document.querySelectorAll<HTMLElement>('.flip').forEach((el) => {
      if (el.querySelector(':scope > .flip-inner')) return
      const inner = document.createElement('span')
      inner.className = 'flip-inner'
      while (el.firstChild) inner.appendChild(el.firstChild)
      el.appendChild(inner)
    })

    // ── 2. sticky header ────────────────────────────────────────────────
    const hdr = document.getElementById('hdr')
    const onScroll = () => hdr?.classList.toggle('scrolled', window.scrollY > 16)
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll()

    // ── 3. split text (idempotent: skip if already split) ──────────────
    let chIdx = 0
    const splitNode = (node: Node) => {
      if (node.nodeType === Node.TEXT_NODE) {
        const frag = document.createDocumentFragment()
        const text = node.textContent ?? ''
        ;[...text].forEach((c) => {
          const s = document.createElement('span')
          s.className = 'ch'
          s.textContent = c === ' ' ? ' ' : c
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

    // ── 4. scroll-reveal observer ──────────────────────────────────────
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

    // ── 5. number counter ──────────────────────────────────────────────
    const numIo = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (!e.isIntersecting) return
          const el = e.target as HTMLElement
          const to = parseInt(el.dataset.to || '0', 10)
          const start = performance.now()
          const dur = 1400
          const tick = (now: number) => {
            const p = Math.min(1, (now - start) / dur)
            const eased = 1 - Math.pow(1 - p, 3)
            el.textContent = String(Math.round(to * eased))
            if (p < 1) requestAnimationFrame(tick)
          }
          requestAnimationFrame(tick)
          numIo.unobserve(el)
        })
      },
      { threshold: 0.4 },
    )
    document.querySelectorAll<HTMLElement>('.num').forEach((n) => numIo.observe(n))

    // ── 6. cursor follower ─────────────────────────────────────────────
    const cur = document.getElementById('cursor')
    let mx = 0
    let my = 0
    let cx = 0
    let cy = 0
    let raf = 0
    const onMove = (e: MouseEvent) => {
      mx = e.clientX
      my = e.clientY
    }
    window.addEventListener('mousemove', onMove)
    const frame = () => {
      cx += (mx - cx) * 0.18
      cy += (my - cy) * 0.18
      if (cur) cur.style.transform = `translate(${cx}px,${cy}px) translate(-50%,-50%)`
      raf = requestAnimationFrame(frame)
    }
    frame()
    const hovers = document.querySelectorAll<HTMLElement>('a,button,.feat .card,.ev')
    const onEnter = () => cur?.classList.add('lg')
    const onLeave = () => cur?.classList.remove('lg')
    hovers.forEach((el) => {
      el.addEventListener('mouseenter', onEnter)
      el.addEventListener('mouseleave', onLeave)
    })

    return () => {
      window.removeEventListener('scroll', onScroll)
      window.removeEventListener('mousemove', onMove)
      cancelAnimationFrame(raf)
      io.disconnect()
      numIo.disconnect()
      hovers.forEach((el) => {
        el.removeEventListener('mouseenter', onEnter)
        el.removeEventListener('mouseleave', onLeave)
      })
    }
  }, [])

  return (
    <>
      <div className="cursor" id="cursor" />

      {/* ─────────── HEADER ─────────── */}
      <header id="hdr">
        <a href="#" className="logo">
          <span className="dot" />
          <span>CampusHub</span>
        </a>
        <nav className="primary">
          <a href="#features" className="flip"><span className="real">功能</span><span className="ghost">功能</span></a>
          <a href="#events" className="flip"><span className="real">活动</span><span className="ghost">活动</span></a>
          <a href="#how" className="flip"><span className="real">如何使用</span><span className="ghost">如何使用</span></a>
          <a href="#stories" className="flip"><span className="real">故事</span><span className="ghost">故事</span></a>
          <a href="#about" className="flip"><span className="real">关于</span><span className="ghost">关于</span></a>
        </nav>
        <a href="#" className="pill">
          打开 CampusHub <span className="arr">→</span>
        </a>
      </header>

      {/* ─────────── HERO ─────────── */}
      <section className="hero">
        <div className="wrap" style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
          <div className="eyebrow rise" data-rise>
            <span className="pulse" />
            <span>2026 春 · 已覆盖南京 12 所高校</span>
            <span className="ln" />
            <span className="mono">v 1.0</span>
          </div>

          <h1 className="title">
            <span className="row split whole" data-split>Everything</span>
            <span className="row split whole" data-split><span className="it">campus,</span></span>
            <span className="row split whole indent" data-split>in one tab.</span>
          </h1>

          <div className="hero-meta">
            <p className="rise" data-rise data-delay="200">
              从早八的高数课，到深夜南苑食堂的最后一份炒饭；从社团招新到二手 iPad 的转手 —— CampusHub 把校园里所有零碎的事，缝进一个安静、漂亮的地方。
            </p>
            <div className="cta-stack rise" data-rise data-delay="300">
              <a href="#" className="cta-big">
                选择我的学校
                <span>↗</span>
                <svg className="hand" viewBox="0 0 36 36" fill="none">
                  <path d="M10 22 L18 14 L26 22" stroke="#E25A3C" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
                  <path d="M18 14 L18 30" stroke="#E25A3C" strokeWidth="2.4" strokeLinecap="round" />
                </svg>
              </a>
              <a href="#" className="ghost-btn">看 60 秒短片</a>
            </div>
          </div>

          <div className="hero-foot rise" data-rise data-delay="500">
            <div>
              <div className="label">已为这些学校的同学服务</div>
              <div className="schools">
                <span>南京大学</span>
                <span>东南大学</span>
                <span>南京师大</span>
                <span>南京理工</span>
                <span>河海大学</span>
                <span>中国药大</span>
                <span>+ 6 所</span>
              </div>
            </div>
            <div className="mono" style={{ fontSize: '11.5px', color: 'var(--muted)' }}>↓ 向下滚动</div>
          </div>
        </div>

        {/* floating scene cards */}
        <div className="scene">
          <div className="card c1">
            <div className="mono" style={{ fontSize: '10.5px', color: 'var(--muted)', letterSpacing: '.15em' }}>跑腿 · 待接</div>
            <h4>仙林食堂 → 计科楼</h4>
            <div className="row"><span>悬赏 8 积分</span><span style={{ color: 'var(--accent)' }}>2 分钟前</span></div>
            <div className="row"><span>预计 15min</span><span>距 0.8 km</span></div>
          </div>
          <div className="card c2">
            <div className="mono" style={{ fontSize: '10.5px', opacity: 0.6, letterSpacing: '.15em' }}>二手 · 在售</div>
            <h4 style={{ color: 'var(--cream)' }}>Kindle Paperwhite 4</h4>
            <div className="row" style={{ color: 'rgba(255,255,255,.6)', borderColor: 'rgba(255,255,255,.18)' }}>
              <span>¥ 380</span><span>9 成新</span>
            </div>
            <div className="dot-row"><i /><i /><i /><i /><i style={{ background: 'rgba(255,255,255,.18)' }} /></div>
          </div>
          <div className="card c3">
            <div className="mono" style={{ fontSize: '10.5px', color: 'var(--muted)', letterSpacing: '.15em' }}>辅导 · 急</div>
            <h4>高数 II · 期中冲刺</h4>
            <div className="row"><span>计科陈学长</span><span style={{ color: 'var(--accent)' }}>立即聊</span></div>
          </div>
        </div>
      </section>

      {/* ─────────── MARQUEE ─────────── */}
      <div className="marq" aria-hidden="true">
        <div className="marq__track">
          <span>跑腿</span><span className="it">二手</span><span>辅导</span><span className="it">组队</span><span>消息</span><span className="it">信用</span><span>食堂</span><span className="it">自习室</span><span>失物招领</span><span className="it">朋友</span>
          <span>跑腿</span><span className="it">二手</span><span>辅导</span><span className="it">组队</span><span>消息</span><span className="it">信用</span><span>食堂</span><span className="it">自习室</span><span>失物招领</span><span className="it">朋友</span>
        </div>
      </div>

      {/* ─────────── FEATURES ─────────── */}
      <section id="features">
        <div className="wrap">
          <div className="s-head">
            <div className="s-num rise" data-rise>01 / 平台功能</div>
            <h2 className="rise" data-rise data-delay="120">小工具，<span className="it">大日常。</span></h2>
          </div>

          <div className="feat">
            {/* 跑腿 large */}
            <div className="card lg rise" data-rise>
              <div className="tag">Errand · 跑腿</div>
              <h3>把<span className="it">校园里的小麻烦</span>，交给同学。</h3>
              <p className="desc">取快递、带饭、占座、跑食堂打包 —— 5 块钱的事不必跑断腿。同学接单，积分结算，超时自动仲裁。</p>
              <div className="sched">
                <div className="ln"><b>09:30</b><span>南苑早餐 → 仙Ⅱ 215</span><span className="tag-cls">急</span></div>
                <div className="ln"><b>11:00</b><span>菜鸟驿站取件 → 16 栋</span><span>8 min</span></div>
                <div className="ln"><b>14:15</b><span>占座 · 图书馆 4 楼 A 区</span><span className="tag-cls">价 5</span></div>
                <div className="ln"><b>17:30</b><span>食堂打包 → 计科 311</span><span className="tag-cls s">已完成</span></div>
                <div className="ln"><b>20:00</b><span>打印店取材料 → 宿舍楼</span><span className="tag-cls s">已接</span></div>
              </div>
              <div className="corner">↗</div>
            </div>

            {/* 信用 dark */}
            <div className="card dark rise" data-rise data-delay="120">
              <div className="tag" style={{ color: 'rgba(255,255,255,.6)' }}>Credit · 信用</div>
              <h3>双向评分，<span className="it">让信任</span>成为系统。</h3>
              <p className="desc">每一单都互评，信用分公开。低于阈值无法发单，差评可申诉。靠谱的人，被看见。</p>
              <div className="map">
                <span className="pin p1" />
                <span className="pin p2" />
                <span className="pin p3" />
              </div>
              <div className="corner">↗</div>
            </div>

            {/* 二手 small */}
            <div className="card rise" data-rise>
              <div className="tag">Market · 二手</div>
              <h3>宿舍里的<span className="it">好东西</span>，传给下一个人。</h3>
              <p className="desc">闲置 iPad、写完的考研书、毕业季的电饭锅 —— 同校面交，议价免运费。</p>
              <img className="illo-img" src="/illustrations/coffee.png" alt="" loading="lazy" />
              <div className="corner">↗</div>
            </div>

            {/* 辅导 accent */}
            <div className="card accent rise" data-rise data-delay="120">
              <div className="tag" style={{ color: 'rgba(255,255,255,.7)' }}>Tutor · 辅导</div>
              <h3>找懂你专业的<span className="it">那位学长</span>。</h3>
              <p className="desc">高数、操作系统、机器学习、Java 课设 —— 按课程精准匹配，可看接单率和好评。</p>
              <img className="illo-img" src="/illustrations/focused.png" alt="" loading="lazy" />
              <div className="corner">↗</div>
            </div>

            {/* 组队 small */}
            <div className="card rise" data-rise data-delay="200">
              <div className="tag">Team · 组队</div>
              <h3>比赛 / 课设的<span className="it">队友</span>，在一处招募。</h3>
              <p className="desc">挑战杯、数模、互联网+、毕设小组 —— 标签匹配，技能互补，进度可见。</p>
              <img className="illo-img" src="/illustrations/catching-up.png" alt="" loading="lazy" />
              <div className="corner">↗</div>
            </div>
          </div>
        </div>
      </section>

      {/* ─────────── EVENTS ─────────── */}
      <section id="events" style={{ paddingTop: '40px' }}>
        <div className="wrap">
          <div className="s-head">
            <div className="s-num rise" data-rise>02 / 本周校园</div>
            <h2 className="rise" data-rise data-delay="120">最近<span className="it">发生</span>的事。</h2>
          </div>

          <div className="events">
            <a href="#" className="ev rise" data-rise>
              <div className="when">周四 · 19:00</div>
              <div className="name">仙林大讲堂 · <span className="it">人工智能与未来</span></div>
              <div className="where">仙林校区 · 行政楼报告厅</div>
              <div className="who">142 人感兴趣 · 8 位朋友</div>
              <div className="arr">↗</div>
            </a>
            <a href="#" className="ev rise" data-rise data-delay="80">
              <div className="when">周五 · 17:30</div>
              <div className="name">街舞社招新 · <span className="it">免费教课</span></div>
              <div className="where">体育馆 · 北门广场</div>
              <div className="who">418 人感兴趣 · 21 位朋友</div>
              <div className="arr">↗</div>
            </a>
            <a href="#" className="ev rise" data-rise data-delay="160">
              <div className="when">周六 · 08:00</div>
              <div className="name">紫金山<span className="it">晨跑团</span></div>
              <div className="where">北门集合 · 7:45 出发</div>
              <div className="who">36 人感兴趣 · 3 位朋友</div>
              <div className="arr">↗</div>
            </a>
            <a href="#" className="ev rise" data-rise data-delay="240">
              <div className="when">周六 · 20:00</div>
              <div className="name">仙II 草坪 · <span className="it">露天电影</span></div>
              <div className="where">仙II 中心草坪</div>
              <div className="who">88 人感兴趣 · 12 位朋友</div>
              <div className="arr">↗</div>
            </a>
            <a href="#" className="ev rise" data-rise data-delay="320">
              <div className="when">周日 · 14:00</div>
              <div className="name"><span className="it">求职模拟面试</span>工作坊</div>
              <div className="where">就业指导中心 · 2 楼</div>
              <div className="who">61 人感兴趣 · 4 位朋友</div>
              <div className="arr">↗</div>
            </a>
          </div>
        </div>
      </section>

      {/* ─────────── STATS ─────────── */}
      <section style={{ padding: 0 }}>
        <div className="wrap">
          <div className="stats">
            <div className="stat rise" data-rise>
              <div className="n"><span className="num" data-to="5">0</span><span className="it">k+</span></div>
              <div className="lab">学生每周使用 CampusHub</div>
            </div>
            <div className="stat rise" data-rise data-delay="120">
              <div className="n"><span className="num" data-to="12">0</span></div>
              <div className="lab">已覆盖南京及周边高校</div>
            </div>
            <div className="stat rise" data-rise data-delay="240">
              <div className="n"><span className="num" data-to="8">0</span>&nbsp;<span className="it">min</span></div>
              <div className="lab">平均接单等待时间</div>
            </div>
            <div className="stat rise" data-rise data-delay="360">
              <div className="n"><span className="num" data-to="98">0</span>%</div>
              <div className="lab">用户愿意推荐给新生</div>
            </div>
          </div>
        </div>
      </section>

      {/* ─────────── STEPS ─────────── */}
      <section id="how">
        <div className="wrap">
          <div className="s-head">
            <div className="s-num rise" data-rise>03 / 上手三步</div>
            <h2 className="rise" data-rise data-delay="120">三步搞定，<span className="it">不必填表。</span></h2>
          </div>

          <div className="steps">
            <div className="step rise" data-rise>
              <div className="n">STEP 01</div>
              <h4>用<span className="it">学生手机号</span>登录。</h4>
              <p>验证码登录，自动识别学校。首次登录引导上传学生证，后台审核一般 24h 内完成。</p>
            </div>
            <div className="step rise" data-rise data-delay="120">
              <div className="n">STEP 02</div>
              <h4>选你<span className="it">在意</span>的事。</h4>
              <p>跑腿、二手、辅导、组队 —— 首页会按你关注的话题重新排序，看到的全是你想要的。</p>
            </div>
            <div className="step rise" data-rise data-delay="240">
              <div className="n">STEP 03</div>
              <h4>让<span className="it">安静的提醒</span>开始。</h4>
              <p>只有真正要发生的事才推送。手机从焦虑变成背景音。承诺。</p>
            </div>
          </div>
        </div>
      </section>

      {/* ─────────── QUOTE ─────────── */}
      <section className="quote" id="stories">
        <div className="wrap">
          <div className="photo rise" data-rise>
            <img src="/illustrations/reflecting.png" alt="" />
            <span>张同学 · 南大计科大二</span>
          </div>
          <div>
            <q className="rise" data-rise data-delay="120">原本我手机里全是各种"校园 FOMO"，现在 CampusHub 成了那个把我推出宿舍的东西。</q>
            <div className="by rise" data-rise data-delay="240">
              — <b>张同学</b> · 南京大学计算机系大二 · 用 CampusHub 已经 4 个月
            </div>
          </div>
        </div>
      </section>

      {/* ─────────── CTA strip ─────────── */}
      <section className="cta-strip">
        <div className="wrap">
          <div className="s-num rise" data-rise>04 / 该你了</div>
          <div className="big rise" data-rise data-delay="120" style={{ marginTop: '30px' }}>
            不再开 <span className="it">十一个</span> App。<br />试试 <span className="it">一个</span> 就够。
          </div>
          <a href="#" className="cta-big rise" data-rise data-delay="240">
            进入 CampusHub —— 免费
            <span>↗</span>
            <svg className="hand" viewBox="0 0 36 36" fill="none">
              <path d="M10 22 L18 14 L26 22" stroke="#E25A3C" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
              <path d="M18 14 L18 30" stroke="#E25A3C" strokeWidth="2.4" strokeLinecap="round" />
            </svg>
          </a>
          <div className="sig rise" data-rise data-delay="400">campus<span className="it">hub.</span></div>
        </div>
      </section>

      {/* ─────────── FOOTER ─────────── */}
      <footer id="about">
        <div className="wrap">
          <div className="grid">
            <div>
              <div className="logo" style={{ marginBottom: '18px' }}>
                <span className="dot" />
                <span>CampusHub</span>
              </div>
              <p style={{ maxWidth: '32ch', color: 'var(--muted)', fontSize: '13px' }}>
                由南大学生为南大学生而做。独立、无广告、不出售你的数据给任何机构。
              </p>
            </div>
            <div>
              <h5>产品</h5>
              <ul>
                <li><a className="flip" href="#"><span className="real">跑腿</span><span className="ghost">跑腿</span></a></li>
                <li><a className="flip" href="#"><span className="real">二手</span><span className="ghost">二手</span></a></li>
                <li><a className="flip" href="#"><span className="real">辅导</span><span className="ghost">辅导</span></a></li>
                <li><a className="flip" href="#"><span className="real">组队</span><span className="ghost">组队</span></a></li>
              </ul>
            </div>
            <div>
              <h5>校园</h5>
              <ul>
                <li><a className="flip" href="#"><span className="real">引入我的学校</span><span className="ghost">引入我的学校</span></a></li>
                <li><a className="flip" href="#"><span className="real">学生会合作</span><span className="ghost">学生会合作</span></a></li>
                <li><a className="flip" href="#"><span className="real">社团入驻</span><span className="ghost">社团入驻</span></a></li>
              </ul>
            </div>
            <div>
              <h5>关于</h5>
              <ul>
                <li><a className="flip" href="#"><span className="real">我们的故事</span><span className="ghost">我们的故事</span></a></li>
                <li><a className="flip" href="#"><span className="real">招募</span><span className="ghost">招募</span></a></li>
                <li><a className="flip" href="#"><span className="real">课程项目</span><span className="ghost">课程项目</span></a></li>
              </ul>
            </div>
            <div>
              <h5>法律</h5>
              <ul>
                <li><a className="flip" href="#"><span className="real">隐私政策</span><span className="ghost">隐私政策</span></a></li>
                <li><a className="flip" href="#"><span className="real">服务条款</span><span className="ghost">服务条款</span></a></li>
                <li><a className="flip" href="#"><span className="real">信任与安全</span><span className="ghost">信任与安全</span></a></li>
              </ul>
            </div>
          </div>
          <div className="colophon">
            <span>© 2026 CampusHub 软件工程二大作业 · 南京大学</span>
            <span>v 1.0.0 · 设计于鼓楼图书馆</span>
          </div>
        </div>
      </footer>
    </>
  )
}
