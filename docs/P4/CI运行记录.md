# P4 CI/CD 运行记录

> **交付物对应：** P4-编码开发.md 交付物 #5「CI/CD 配置与运行记录」
> **看板任务：** `QA-04` GitLab CI/CD 配置（依赖 / 静态检查 / 单测 / 集测 / 构建）
> **维护人：** 陈泽昊（a）

---

## 一、CI 工作流

- **平台：** GitHub Actions（看板原写 GitLab CI/CD，实际落地用 GitHub Actions —— 项目已托管 GitHub，避免双平台维护）
- **配置文件：** [`.github/workflows/main.yml`](../../.github/workflows/main.yml)
- **触发条件：**
  - `push` 到 `main` 或 `dev` 分支
  - 向 `main` / `dev` 发起 `pull_request`

---

## 二、覆盖步骤（对齐 P4 主文档「CI/CD 至少应包括」5 条）

| P4 验收要求 | 落地步骤 | Job |
|---|---|---|
| 自动安装依赖 | `npm ci` / `actions/setup-java` + Maven cache | 两 job 各自 |
| 自动运行静态检查 | `npm run lint`（ESLint flat config + `eslint-plugin-react-hooks` 7.0） | `frontend-check` |
| 自动运行单元测试 | `./mvnw clean install`（含 JUnit + Mockito） | `backend-check` |
| 自动运行集成测试 | 同上，包含 `BaseIT` + `TaskHappyPathFlowTest` + `TradeHappyPathFlowTest` + `TaskExceptionFlowTest` + `CreditExceptionFlowTest` | `backend-check` |
| 自动构建项目 | `npm run build`（Vite）+ Maven `install` 阶段产出 jar | 两 job 各自 |

后端 job 启了一个 MySQL 8.0 service container 给集成测试用（`mysql_root_password=root` / `db=campushub`），health check 通过后才进入 `mvnw install`。

---

## 三、最近一次绿色运行

| 项 | 值 |
|---|---|
| Commit | 6a3631e6061421474f5fc53f8426235682befa73 |
| 提交标题 | CI: 提升actions/*到v5(Node 24兼容) |
| 触发分支 | main |
| 运行结果 | ✅ 绿（frontend-check + backend-check 均通过，且无 Node 20 deprecation warning） |
| Actions Run URL | https://github.com/HEYWEEN/CampusHub/actions/runs/26864530708 |
| 运行时间 | 2026-6-3 12:54 |

---

## 四、关键 commit 演进

| Commit | 改动 | 备注 |
|---|---|---|
| `b67e7e0` | `CI: 添加静态检查` | 在 `frontend-check` 加 `npm run lint` step，对齐 P4「静态检查」硬要求 |
| `e98b93d` | 修 `react-hooks/set-state-in-effect` | 拆 `ProfileEditPage` → 父组件做数据获取，子组件用 props 初始化 state；首次 CI 跑 lint 时被 plugin 7.0 新规则拦下 |
| _(待 push)_ | `ci: bump actions/* to v5` | `actions/checkout` / `setup-node` / `setup-java` 全部从 `@v4` 升 `@v5`，消除 GitHub 2025-09-19 起的 Node 20 deprecation warning（6-16 前迁完即可） |

---

## 五、已知偏差与决策

1. **看板 owner vs. 主文档归属**：看板 `QA-04` owner 写 B，P4 主文档交付物 #5 归 a。实际由 a 完成 CI 配置 + lint 接入 + 运行记录文档；B 不再单独承担本项。
2. **静态检查范围**：当前仅前端 ESLint。看板字面提及「Checkstyle / SpotBugs」属 Java 端，未接入 —— 团队决定不强行补，理由：① P4 主文档原文「**静态检查或代码格式检查**」非二者皆需；② 剩余工时优先级更高（Bug 整合 / 押金缺口决策）。
3. **覆盖率上报**：D 的 credit 模块单测覆盖率 91.1% 为 5-30 本地 jacoco 报告手动产出，未在 CI 中输出。`QA-01` 仍 🟡（A/B/C 模块覆盖率待自查）。

---

**最后更新：** 2026-06-03
