# CampusHub · 校园互助服务平台

[![Star History](https://api.star-history.com/svg?repos=HEYWEEN/CampusHub\&type=Date)](https://star-history.com/#HEYWEEN/CampusHub\&type=Date)

> 南京大学软件工程专业 2025–2026 学年《软件工程与计算 II》课程项目。

面向在校大学生的 **O2O 互助平台**，支持跑腿代取、二手交易、学习辅导、组队匹配等高频校园场景，围绕"**后台强实名、前台弱展示**"的隐私原则构建信用与申诉体系。

---

## 一、技术栈

| 层次  | 选型                                             |
| --- | ---------------------------------------------- |
| 前端  | React 19 · TypeScript 5.9 · Vite 8 · ESLint 9  |
| 后端  | Java 17 · Spring Boot 3.5.x · Maven            |
| 数据库 | MySQL 8（开发环境可选 H2 / Docker）                    |
| 协作  | GitHub Actions · Markdown 文档 · AI 辅助开发（全流程可追溯） |

---

## 二、快速上手

### 2.1 前置依赖

* Node.js ≥ 18（建议 20 LTS）
* Java 17+
* Maven 3.9+（或使用仓库内置的 `./mvnw`）
* MySQL 8

### 2.2 一键启动 `start.sh`（推荐）

根目录提供 `start.sh`，一键起前后端，并自动检查/生成配置文件。

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

### 2.3 配置文件清单

> **核心约定**：所有 `.example` 模板都可入库；对应的真实文件**绝不进 git**（已在 `.gitignore`）。

| 文件 | 用于 | 来源 / 模板 | 进 git？ | 谁负责填 |
| --- | --- | --- | --- | --- |
| `backend/src/main/resources/application-local.properties` | 本地开发 | `application-local.properties.example` | ❌ | **每位成员**：填自己的 MySQL 密码 |
| `backend/src/main/resources/application-prod.properties` | 服务器 | 已存在，全部用 `${ENV}` 占位 | ✅ | 不用改 |
| `.env.prod`（根目录） | 服务器 | `.env.prod.example` | ❌ | **部署人**：填 `DB_USERNAME` / `DB_PASSWORD` 等 |
| `frontend/.env.production` | 服务器 | `frontend/.env.production.example` | ❌ | **部署人**：填 `VITE_API_BASE_URL` |

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

### 2.4 手动启动（不想用脚本时）

**前端：**

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
npm run build        # 生产构建
npm run lint         # ESLint 检查
```

**后端：**

```bash
cd backend
./mvnw spring-boot:run     # macOS / Linux
mvnw.cmd spring-boot:run   # Windows
./mvnw test                # 运行单测
```

---

## 三、项目结构

> **图例：** ✅ 已落地 · 🚧 P4 阶段建设中 · 📋 P3 设计已规划 · 📁 仅占位（按需扩展）

### 3.1 顶层结构

```
CampusHub/
├── frontend/                    # ✅ React 19 + TypeScript 5.9 + Vite 8
├── backend/                     # ✅ Spring Boot 4.0.5 + Maven + JPA + MySQL
├── docs/                        # ✅ 全部阶段交付物
│   ├── P0/  P1/  P2/  P3/      # ✅ 已完成的阶段文档
│   └── P4/                     # 🚧 编码开发阶段（Sprint 看板 + 分工 + AI Prompt）
├── .github/workflows/           # ✅ GitHub Actions CI/CD（main.yml）
├── .run-logs/                   # ⚙️  start.sh 启动日志输出位置
├── .run-pids/                   # ⚙️  start.sh prod 模式 PID 记录
├── .env.prod.example            # ✅ 生产环境变量模板
├── start.sh                     # ✅ 一键启动脚本（macOS / Linux / Git Bash）
└── README.md
```

### 3.2 后端结构（按 P3 包骨架规划，P4 阶段落地）

**包根：** `com.campushub`（注：当前骨架仍是 `com.example.demo`，P4 INF-01 任务执行后切换）

```
backend/
├── pom.xml                                        # ⚠️ 待修复：当前写的 spring-boot 4.0.5 不存在，需改为 3.5.3 + starter-web（见 P4 INF-00 任务）
├── mvnw / mvnw.cmd                                # ✅ 内置 Maven Wrapper（无需本地装 Maven）
└── src/
    ├── main/
    │   ├── java/com/campushub/
    │   │   ├── CampushubApplication.java          # 🚧 启动类（INF-01）
    │   │   ├── common/                            # 🚧 跨模块共享（A 负责）
    │   │   │   ├── response/                      # ApiResponse / PageResponse / ResponseCode
    │   │   │   ├── exception/                     # BizException / GlobalExceptionHandler
    │   │   │   ├── util/                          # JwtUtil / AesUtil / ExifCleaner / PhoneMaskUtil
    │   │   │   ├── enums/                         # VerifyStatus / BaseEnum
    │   │   │   ├── interceptor/                   # JwtAuthInterceptor / TraceIdInterceptor
    │   │   │   └── PublicUserVO.java              # ⚠️ 全局唯一公开用户对象（强制使用）
    │   │   ├── config/                            # 🚧 Spring 配置（WebMvcConfig / SecurityConfig / JwtConfig）
    │   │   │
    │   │   ├── auth/                              # 🚧 P0 鉴权（A）— 短信 / 登录 / 学生证认证
    │   │   ├── user/                              # 🚧 P0 用户（A）— 资料 / 隐私开关 / PublicUserVO
    │   │   ├── task/                              # 🚧 P0 任务（B）— 含 State 模式状态机
    │   │   │   ├── state/                         # PendingAccept → InProgress → WaitConfirm → Completed/Canceled/Expired
    │   │   │   └── scheduler/                     # TaskTimeoutScanner（5 分钟精度）
    │   │   ├── trade/                             # 🚧 P0 二手交易（C）
    │   │   ├── edu/                               # 🚧 P0 辅导 + P1 资料/课评（C）
    │   │   ├── credit/                            # 🚧 P0 信用结算 + P1 评分/信用分（D）
    │   │   │   ├── strategy/                      # 信用分计算 Strategy 模式
    │   │   │   └── listener/                      # 订阅 task/trade 事件（BEFORE_COMMIT）
    │   │   ├── notify/                            # 🚧 P1 站内信（D）
    │   │   │   └── listener/                      # 订阅各模块事件 → 模板化推送
    │   │   ├── team/    im/    report/    admin/  # 📋 P2/P3 优先级，本期延后
    │   │   ├── search/                            # 📋 列表/筛选/分页（依赖各业务表）
    │   │   └── wall/                              # 📋 树洞（待定）
    │   │
    │   └── resources/
    │       ├── application.properties             # ✅ 通用配置（profile 切换）
    │       ├── application-local.properties       # ❌ 本地开发，不入 git（成员各自填密码）
    │       ├── application-local.properties.example  # ✅ 本地模板
    │       ├── application-prod.properties        # ✅ 生产配置（全 ${ENV} 占位）
    │       ├── static/  templates/                # ✅ 静态资源
    │       └── db/                                # 🚧 数据库脚本（C 负责）
    │           ├── schema.sql                     # 全量 DDL（30+ 张表）
    │           └── migration/V*__*.sql            # Flyway 增量
    │
    └── test/
        └── java/com/campushub/
            ├── archunit/                          # 🚧 架构守护测试（包依赖白名单）
            ├── auth/  user/  task/  ...           # 🚧 与 main 镜像（各模块 owner 自测）
            └── integration/                       # 🚧 集成测试（D 负责，QA-02/QA-03）
```

**每个业务模块的标准子包**（共 12 个模块完全相同结构）：

```
<module>/
├── api/                # 跨模块对外 interface（其他模块只能 @Autowired 这个）
├── controller/         # @RestController HTTP 入口
├── service/            # 业务逻辑（interface + Impl）
├── repository/         # JPA Repository
├── entity/             # 数据实体（对应数据库表）
├── dto/                # 请求体（xxxDTO）
├── vo/                 # 响应体（xxxVO，禁含 realName/studentNo/phone 明文）
├── event/              # 应用事件（跨模块订阅）
└── exception/          # 模块业务异常
```

### 3.3 前端结构（按 P3 §6 规划）

```
frontend/
├── package.json                                   # ✅ React 19 / TS 5.9 / Vite 8
├── vite.config.ts  tsconfig.json  eslint.config.js
├── index.html
└── src/
    ├── main.tsx  App.tsx                          # ✅ 入口
    ├── api/                                       # 🚧 与后端模块一一对应
    │   ├── client.ts                              # Axios 实例 + JWT 拦截器 + ApiResponse 统一解包
    │   ├── auth.ts  task.ts  trade.ts  edu.ts
    │   └── credit.ts  notify.ts  user.ts
    ├── pages/                                     # 🚧 按业务模块组织
    │   ├── auth/                                  # 登录 / 短信 / 学生证认证
    │   ├── task/                                  # 大厅 / 详情 / 发布 / 接单 / 凭证 / 确认
    │   ├── trade/  edu/                           # 二手 / 辅导
    │   └── user/  credit/  notify/                # 主页 / 信用 / 站内信
    ├── components/                                # 🚧 通用组件
    │   └── PublicUserCard.tsx                     # ⚠️ 渲染 PublicUserVO 的唯一入口（禁自拼字段）
    ├── store/                                     # 状态管理（Zustand / Redux 二选一）
    ├── router/  hooks/
    └── types/                                     # 🚧 TS 类型（与后端 VO/DTO 同名同形）
        ├── PublicUserVO.ts  TaskDetailVO.ts  ...
```

### 3.4 文档结构

```
docs/
├── P0/                          # ✅ 项目启动（第 1 周）
│   ├── AI 工具选型评估表.pdf
│   ├── AI 协作契约.pdf
│   ├── 团队章程文档.pdf
│   └── Git 仓库链接.txt
│
├── P1/                          # ✅ 需求分析（第 2-3 周）
│   ├── 软件需求规格说明书.md      # SRS
│   ├── 需求分析.md  用户故事.md
│   ├── 用例图.pdf  3.3问题.md
│   ├── 调研方案.md  调研原始数据.md
│   ├── AI-Prompt/  Prompt日志.md
│   └── AI协作反思日志_P1.md
│
├── P2/                          # ✅ 架构设计（第 4-6 周）
│   ├── 架构设计文档.md            # 最终架构
│   ├── 架构候选方案对比表.md       # 多方案对比
│   ├── 架构选择辩论会记录.md       # 决策过程
│   ├── 架构辩论赛-成员记录/
│   ├── ADR.md                   # Architecture Decision Records
│   ├── AI前置Prompt.md           # ⚠️ 启动 P2 协作前必读
│   ├── AI-prompt/  image/
│   └── AI协作反思日志_P2.md
│
├── P3/                          # ✅ 详细设计（第 7-9 周）
│   ├── 01_统一规约.md             # 命名 / API / 数据 三大公约
│   ├── 02_功能映射主表.md         # 65 个功能 × 类/表/API 三视角对齐
│   ├── 03_包结构骨架.md           # 后端 12 模块标准结构
│   ├── 04_核心类图.md
│   ├── 05_SOLID检查清单与设计模式.md
│   ├── 06_ER图.md
│   ├── 07_详细设计文档（整合版）.md
│   ├── API/                     # OpenAPI 3.1 全量定义
│   │   ├── openapi.yaml
│   │   └── AI辅助审查_API.md
│   ├── 数据库/                  # ER 图 + DDL + 索引说明
│   ├── 决策记录.md
│   └── AI协作反思日志_P3.md
│
└── P4/                          # 🚧 编码开发（第 10-14 周，本期）
    ├── 01_Sprint看板.md         # 任务分解 / 工时 / 风险 / 验收
    ├── 02_后端代码分工.md        # A/B/C/D 模块所有权 + 跨模块接口契约
    ├── 03_AI使用前统一Prompt.md  # ⚠️ 所有人写代码前必喂
    └── bug/                     # 全员实时记录，C 统一整合
```

### 3.5 关键约束（落地强制项）

| 约束 | 说明 | 强制方式 |
|------|------|--------|
| 包根 `com.campushub` | 所有 Java 代码包前缀统一 | ArchUnit 测试 |
| 跨模块只能调 `*Api` interface | 禁止 `@Autowired ServiceImpl` | ArchUnit + Code Review |
| 隐私字段黑名单 | VO 中禁出 `realName / studentNo / phone` 明文 | Code Review + 单测断言 |
| 表名模块前缀 | `auth_* / task_* / trade_* / ...` | DBA（C）守门 |
| 状态转换白名单 | task 模块 State 模式枚举写死 | ArchUnit + 单测 |
| 统一响应体 | 所有接口返回 `ApiResponse<T>` | GlobalExceptionHandler + Code Review |
| 配置文件不入 git | `application-local.properties` / `.env.prod` | `.gitignore` |

---

## 四、阶段交付索引

| 阶段 | 周次 | 状态 | 核心交付 | 目录 |
|----|----|:--:|----|----|
| P0 | 第 1 周 | ✅ | AI 协作契约、团队章程、Git 仓库链接 | [`docs/P0/`](docs/P0) |
| P1 | 第 2–3 周 | ✅ | 调研方案、SRS、用例图、用户故事、AI 协作反思日志 #1 | [`docs/P1/`](docs/P1) |
| P2 | 第 4–6 周 | ✅ | 架构候选对比、架构辩论会、最终架构文档、ADR、反思日志 #2 | [`docs/P2/`](docs/P2) |
| P3 | 第 7–9 周 | ✅ | 统一规约、功能映射主表、包结构骨架、类图、ER 图、OpenAPI、详细设计 | [`docs/P3/`](docs/P3) |
| P4 | 第 10–14 周 | 🚧 | Sprint 看板、后端分工、AI 统一 Prompt、可运行系统、CI/CD、两个 AI 实验、Bug 日志、反思日志 #4 | [`docs/P4/`](docs/P4) |

**启动 P4 编码前必读：**
- [`docs/P3/01_统一规约.md`](docs/P3/01_统一规约.md) — 命名 / API / 数据 三大公约
- [`docs/P3/03_包结构骨架.md`](docs/P3/03_包结构骨架.md) — 后端目录怎么建
- [`docs/P4/02_后端代码分工.md`](docs/P4/02_后端代码分工.md) — 你负责哪个模块
- [`docs/P4/03_AI使用前统一Prompt.md`](docs/P4/03_AI使用前统一Prompt.md) — 每次让 AI 写代码前先喂这段

---

## 五、AI 协作准则

1. **可追溯**：每位成员使用 AI 生成的输入 Prompt 必须归档到 `docs/AI-Prompt/`，并在阶段反思日志中列出。
2. **标注来源**：SRS 中每条需求与用户故事标注 `[AI生成]` / `[AI生成+人工修改]` / `[人工补充]` / `[human-written]`。
3. **禁止的行为**：
   * 不将真实学号、手机号、证件照片等敏感数据提交到仓库；
   * 不直接粘贴 AI 输出而不做人工复审；
   * 不绕过 pre-commit 钩子或 CI。

---

## 六、团队成员

| 角色    | 姓名  |
| ----- | --- |
| 队长    | 何翌闻 |
| 组员    | 陈泽昊 |
| 组员    | 陈旭枫 |
| 组员    | 李承垚 |

---

## 七、许可证

本项目仅用于课程学习交付，暂未开源发布；如需引用请先联系项目负责人。
