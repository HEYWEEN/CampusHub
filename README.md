# CampusHub · 校园互助服务平台

[![CampusHub CI](https://github.com/HEYWEEN/CampusHub/actions/workflows/main.yml/badge.svg)](https://github.com/HEYWEEN/CampusHub/actions/workflows/main.yml)
[![Star History](https://api.star-history.com/svg?repos=HEYWEEN/CampusHub\&type=Date)](https://star-history.com/#HEYWEEN/CampusHub\&type=Date)

> 南京大学软件工程专业 2025–2026 学年《软件工程与计算 II》课程项目。

面向在校大学生的 **O2O 校园互助平台**：跑腿代取、二手交易（含多轮砍价）、学习辅导、组队匹配、私信沟通、信用体系与 AI 校园助手，围绕"**后台强实名、前台弱展示**"的隐私原则构建。后端为 `com.campushub` 包根的**模块化单体**，前端为 React 19 单页应用。

---

## 一、功能特性

| 模块 | 能力 |
| --- | --- |
| 🔐 **认证 / 用户** | 手机验证码登录、可选设密、学生证认证；资料与隐私开关；全站统一 `PublicUserVO` 脱敏（前台不出真名/学号/手机号明文） |
| 🏃 **任务**（跑腿 / 互助 / 辅导） | 发布即冻结悬赏作押金 → 接单 → 提交凭证 → 双方确认 → 积分结算；**State 状态机**管控流转；定时扫描超时自动过期 |
| 🛍️ **二手交易** | 商品发布、立即购买；**多轮砍价**（买家出价 ↔ 卖家还价，任一方同意即成单）；押金冻结、双方确认收货、未完成可取消退款 |
| 👥 **组队** | 招募发帖（人数 / 技能标签）、申请加入、队长审核同意 / 拒绝 |
| 📚 **辅导** | 家教 / 辅导信息发布与浏览 |
| 💬 **私信（IM）** | 轮询式实时聊天、图片消息、订单卡片消息（点击直达任务 / 商品详情） |
| 🔔 **站内信** | 事件驱动、模板化推送；通知可点击跳转到对应页 |
| ⭐ **信用体系** | 信用分 0~120（**Strategy 模式**按场景计算）；评价；差评申诉与仲裁 |
| 🎯 **智能匹配** | 规则加权打分推荐（类型 / 位置 / 发布者信用 / 悬赏 / 新鲜度 / 紧急度），新用户冷启动权重自动重分配 |
| 🐾 **校园助手** | DeepSeek 驱动的 AI 对话，工具调用实现"找任务 / 生成发单草稿 / 搜二手 / 找组队"，前端模拟流式输出 |
| 🛡️ **管理后台** | 用户封禁 / 解封、管理员角色分派、学生证认证审核、举报仲裁 |
| 📣 **举报** | 举报受理 + 仲裁处理（驳回 / 警告 / 扣信用分） |

> 站内还内置可拖拽的校园助手悬浮球、顶栏「使用说明」指南、首页智能推荐与最新消息公告位。

---

## 二、技术栈

| 层次 | 选型 |
| --- | --- |
| 前端 | React 19 · TypeScript 5.9 · Vite 8 · React Router 7 · TanStack Query 5 · Zustand 5 · Axios |
| 后端 | Java 17 · Spring Boot 3.5.3 · Spring Data JPA · Bean Validation · jjwt 0.12（自研 JWT 鉴权，未用 Spring Security） |
| 数据库 | MySQL 8 · Flyway 迁移（`ddl-auto=validate`，schema 由迁移管控）；单测用 H2 内存库 |
| AI | DeepSeek Chat（OpenAI 兼容）· 函数调用（tool use） |
| 工程 | GitHub Actions CI · ESLint 9 · JUnit 5 · Mockito · ArchUnit · Maven Wrapper |

---

## 三、系统架构

**模块化单体**：单一 Spring Boot 应用，内部按业务垂直切分为 13 个落地模块，强约束模块边界。

```
com.campushub
├── common / config            基础设施：统一响应、全局异常、JWT、工具、Spring 配置
├── auth / user                认证、用户资料、PublicUserVO 脱敏
├── task                       任务（State 状态机 + 超时扫描）
├── trade                      二手商品 / 订单 / 砍价(offer)
├── edu                        辅导
├── team                       组队招募 / 申请
├── im                         私信聊天
├── notify                     站内信（事件监听 + 模板）
├── credit                     信用分(Strategy) / 评价 / 申诉
├── recommend                  智能匹配打分
├── agent                      AI 校园助手（DeepSeek + 工具）
├── report / admin             举报 / 管理后台
└── (search / wall)            预留包
```

**关键设计与约束**

- **跨模块只能调 `*Api` interface**，禁止 `@Autowired ServiceImpl` —— 由 ArchUnit 测试守门。
- **统一响应体** `ApiResponse<T>` + `GlobalExceptionHandler` 兜底，分页统一 `PageResponse`。
- **事件驱动解耦**：任务 / 订单完成等发 `ApplicationEvent`，credit / notify / im 各自监听，避免模块互相依赖实现。
- **设计模式**：State（任务状态流转）、Strategy（信用分计算）、AttributeConverter（枚举 ↔ INT 列）。
- **隐私黑名单**：VO 中禁出 `realName / studentNo / phone` 明文，前台只渲染 `PublicUserVO`。
- **乐观锁 + 幂等**：交易 / 接单用 `@Version` + 条件更新防并发；积分 / 站内信用 `bizKey` 幂等。
- **数据库**：Flyway 增量迁移（`V1`…`V17`），枚举列统一用 `INT`；启动 `validate` 校验实体与表一致。

---

## 四、快速上手

### 4.1 前置依赖

* Node.js ≥ 18（建议 20 LTS）
* Java 17+
* Maven 3.9+（或使用仓库内置的 `./mvnw`）
* MySQL 8

### 4.2 一键启动 `start.sh`（推荐）

根目录提供 `start.sh`，一键起前后端，并自动检查 / 生成配置文件。

| 命令 | 作用 |
| --- | --- |
| `./start.sh` 或 `./start.sh local` | **本地开发**：`mvnw spring-boot:run` + `vite dev`（端口 5173 / 8080） |
| `./start.sh prod` | **生产模式**：构建 jar 与 dist，再用 `java -jar` + `vite preview` 启动 |
| `./start.sh stop` | 停止 prod 模式启动的后台进程（读 `.run-pids/` 中的 PID） |
| `./start.sh help` | 显示完整帮助 |

**平台兼容**

* **macOS / Linux**：直接 `./start.sh`
* **Windows**：用 **Git Bash** 或 **WSL**（原生 CMD/PowerShell 跑不了 `.sh`）。脚本内 `uname` 检测 OS，自动切换 `mvnw` ↔ `mvnw.cmd`，并按系统给出 MySQL 启动命令。

**首次运行流程**

```bash
./start.sh
# 首次会从模板自动生成 application-local.properties 并提示填 MySQL 密码
# 编辑 backend/src/main/resources/application-local.properties，写入密码
./start.sh        # 再跑一次即可
```

`Ctrl+C` 会同时停掉前后端。日志默认写在 `.run-logs/{backend,frontend}.log`。

### 4.3 配置文件清单

> **核心约定**：所有 `.example` 模板都可入库；对应的真实文件**绝不进 git**（已在 `.gitignore`）。

| 文件 | 用于 | 来源 / 模板 | 进 git？ | 谁负责填 |
| --- | --- | --- | --- | --- |
| `backend/src/main/resources/application-local.properties` | 本地开发 | `application-local.properties.example` | ❌ | **每位成员**：填自己的 MySQL 密码 |
| `backend/src/main/resources/application-prod.properties` | 服务器 | 已存在，全部用 `${ENV}` 占位 | ✅ | 不用改 |
| `.env.prod`（根目录） | 服务器 | `.env.prod.example` | ❌ | **部署人**：填 `DB_USERNAME` / `DB_PASSWORD` 等 |
| `frontend/.env.production` | 服务器 | `frontend/.env.production.example` | ❌ | **部署人**：填 `VITE_API_BASE_URL` |

> AI 校园助手需要 DeepSeek API Key（配置项 `campushub.deepseek.*`）；未配置时助手自动降级为规则兜底，不影响其余功能。

首次需先建库（local 模式 `start.sh` 会主动提示）：

```sql
CREATE DATABASE campushub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

启动 MySQL 服务（按系统选一条）：

```bash
brew services start mysql            # macOS (Homebrew)
sudo systemctl start mysql           # Linux
net start MySQL80                    # Windows（管理员）
```

### 4.4 手动启动（不想用脚本时）

**前端：**

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
npm run build        # 生产构建（tsc -b + vite build）
npm run lint         # ESLint 检查
```

**后端：**

```bash
cd backend
./mvnw spring-boot:run     # macOS / Linux（mvnw.cmd 为 Windows）
./mvnw test                # 运行全部单元 / 集成测试（H2）
./mvnw clean install       # 编译 + 测试 + 打包
```

### 4.5 演示账号 & 演示路径

| 账号 | 登录方式 | 用途 |
|------|---------|------|
| `admin` / `admin123` | `/login` 密码登录 | 内置超级管理员（审核 / 仲裁 / 用户管理），自动创建 |
| 任意手机号 + 验证码 `123456` | `/login` 验证码登录 | 普通用户（开发环境短信码固定 `123456`） |

> 数据库无预置业务数据，演示数据现场注册造。完整的**运行方式 / 演示账号 / 测试数据 / 核心演示路径 / 演示前 Bug 修复日志**见
> 👉 **[`docs/P4/08_演示说明.md`](docs/P4/08_演示说明.md)**

---

## 五、项目结构

```
CampusHub/
├── frontend/                    # React 19 + TS 5.9 + Vite 8 单页应用
│   └── src/
│       ├── api/                 # 各业务模块 HTTP 封装（client.ts 统一拦截 + ApiResponse 解包 + mock 兜底）
│       ├── pages/               # 按模块组织：home / auth / tasks / trade / team / edu / im / notify / credit / me / admin / u
│       ├── components/          # 通用与领域组件（PublicUserCard / TaskCard / TradeCard / OfferActions / AgentWidget / HelpModal …）
│       ├── stores/              # Zustand 状态（auth / notify）
│       ├── types/               # 与后端 VO/DTO 同名同形的 TS 类型
│       ├── utils/  router.tsx   # 工具与路由
│       └── styles/              # 设计 token 与全局样式
├── backend/                     # Spring Boot 3.5.3 + JPA + MySQL（com.campushub 模块化单体）
│   ├── pom.xml  mvnw  mvnw.cmd
│   └── src/
│       ├── main/java/com/campushub/<module>/   # 每模块标准子包（见下）
│       └── main/resources/db/
│           ├── schema.sql                       # 全量 DDL 参考
│           └── migration/V1..V17__*.sql         # Flyway 增量迁移
├── docs/                        # P0–P4 全阶段交付物
├── demo/                        # 静态页面与设计稿
├── .github/workflows/main.yml   # GitHub Actions CI
├── .env.prod.example
├── start.sh
└── README.md
```

**每个业务模块的标准子包**（边界一致，便于维护）：

```
<module>/
├── api/                # 跨模块对外 interface（其他模块只能 @Autowired 这个）
├── controller/         # @RestController HTTP 入口
├── service/            # 业务逻辑（interface + Impl）
├── repository/         # JPA Repository
├── entity/             # 数据实体（含枚举 AttributeConverter）
├── dto/  vo/           # 请求体 / 响应体（VO 禁含敏感明文）
├── event/              # 应用事件（跨模块订阅）
└── exception/          # 模块错误码 / 异常
```

---

## 六、测试与 CI

- **后端**：JUnit 5 + Mockito 单元测试 + `@SpringBootTest` 集成测试（H2 内存库），ArchUnit 守护模块边界；当前 **252 个测试全绿**。
- **前端**：`tsc` 类型检查 + ESLint。
- **CI**（`.github/workflows/main.yml`，push / PR 到 `main`/`dev` 触发）：
  - `frontend-check`：`npm ci` → `npm run lint` → `npm run build`
  - `backend-check`：起 MySQL 8 service → `./mvnw clean install`

---

## 七、阶段交付索引

| 阶段 | 周次 | 核心交付 | 目录 |
|----|----|----|----|
| P0 | 第 1 周 | AI 协作契约、团队章程、Git 仓库链接 | [`docs/P0/`](docs/P0) |
| P1 | 第 2–3 周 | 调研方案、SRS、用例图、用户故事、反思日志 #1 | [`docs/P1/`](docs/P1) |
| P2 | 第 4–6 周 | 架构候选对比、架构辩论会、最终架构文档、ADR、反思日志 #2 | [`docs/P2/`](docs/P2) |
| P3 | 第 7–9 周 | 统一规约、功能映射主表、包结构骨架、类图、ER 图、OpenAPI、详细设计 | [`docs/P3/`](docs/P3) |
| P4 | 第 10–14 周 | Sprint 看板、后端分工、AI 统一 Prompt、可运行系统、CI/CD、Bug 日志、反思日志 #4 | [`docs/P4/`](docs/P4) |

---

## 八、AI 协作准则

1. **可追溯**：使用 AI 生成的输入 Prompt 归档到 `docs/**/AI-Prompt/`，并在阶段反思日志中列出。
2. **标注来源**：需求与代码标注 `[AI生成]` / `[AI生成+人工修改]` / `[人工补充]`；提交信息以 `[AI-assisted]` / `[Human-written]` 前缀区分。
3. **红线**：不提交真实学号 / 手机号 / 证件照片等敏感数据；不直接粘贴 AI 输出而不复审；不绕过 CI。

---

## 九、团队成员

| 角色 | 姓名 |
| --- | --- |
| 队长 | 何翌闻 |
| 组员 | 陈泽昊 |
| 组员 | 陈旭枫 |
| 组员 | 李承垚 |

---

## 十、许可证

本项目仅用于课程学习交付，暂未开源发布；如需引用请先联系项目负责人。
