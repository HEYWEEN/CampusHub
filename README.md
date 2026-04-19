# CampusHub · 校园互助服务平台

[![Star History](https://api.star-history.com/svg?repos=HEYWEEN/CampusHub\&type=Date)](https://star-history.com/#HEYWEEN/CampusHub\&type=Date)

> 南京大学软件工程专业 2025–2026 学年《软件工程与计算 II》课程项目。

面向在校大学生的 **O2O 互助平台**，支持跑腿代取、二手交易、学习辅导、组队匹配等高频校园场景，围绕"**后台强实名、前台弱展示**"的隐私原则构建信用与申诉体系。

---

## 一、技术栈

| 层次  | 选型                                             |
| --- | ---------------------------------------------- |
| 前端  | React 19 · TypeScript 5.9 · Vite 8 · ESLint 9  |
| 后端  | Java 17 · Spring Boot · Maven                  |
| 数据库 | MySQL 8（开发环境可选 H2 / Docker）                    |
| 协作  | GitHub Actions · Markdown 文档 · AI 辅助开发（全流程可追溯） |

---

## 二、快速上手

### 2.1 前置依赖

* Node.js ≥ 18（建议 20 LTS）
* Java 17+
* Maven 3.9+（或使用仓库内置的 `./mvnw`）
* MySQL 8（可选，视配置而定）

### 2.2 启动前端

```bash
cd frontend
npm install
npm run dev        # 默认 http://localhost:5173
npm run build      # 生产构建
npm run lint       # ESLint 检查
```

### 2.3 启动后端

```bash
cd backend
./mvnw spring-boot:run                 # macOS / Linux
mvnw.cmd spring-boot:run               # Windows
./mvnw test                            # 运行单测
```

> 本地敏感配置请写入 `backend/src/main/resources/application-local.yml`，该文件已在 `.gitignore` 中忽略。

---

## 三、项目结构

```
CampusHub/
├── frontend/                    # React + TypeScript + Vite
├── backend/                     # Spring Boot + Maven
├── docs/                        # 阶段交付物
│   ├── P0/                      # 项目启动：AI 工具选型、团队章程、协作契约
│   ├── P1/                      # 需求分析：SRS、用例、用户故事、反思日志
│   ├── P2/                      # 系统设计阶段（含 AI 前置 Prompt 统一约束）
│   └── AI-Prompt/               # 所有与 AI 的交互 Prompt（按命名规范归档）
├── .github/workflows/           # CI/CD 流水线
├── .gitignore
└── README.md
```

---

## 四、阶段交付索引

| 阶段 | 周次          | 核心交付                                                                               | 目录                                 |
| -- | ----------- | ---------------------------------------------------------------------------------- | ---------------------------------- |
| P0 | 第 1 周✅    | AI 协作契约、团队章程、Git 仓库链接                                                              | [`docs/P0/`](docs/P0)              |
| P1 | 第 2–3 周 ✅ | 调研方案与原始数据、软件需求规格说明书、用例图、用户故事、AI 协作反思日志 #1、Prompt 日志                                | [`docs/P1/`](docs/P1)              |
| P2 | 第 4 周起      | 系统设计阶段。**启动前务必阅读** [`docs/P2/AI前置Prompt.md`](docs/P2/AI前置Prompt.md)，它定义了本阶段所有 AI 协作的统一约束 | [`docs/P2/`](docs/P2)              |
| —  | 跨阶段         | 所有阶段的 AI Prompt 归档与复盘                                                              | [`docs/AI-Prompt/`](docs/AI-Prompt) |

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
