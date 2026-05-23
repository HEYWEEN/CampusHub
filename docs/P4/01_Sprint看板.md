# Phase 4 — Sprint 合并任务看板

## 0. 排期总览（甘特视图）

```
周次  | W10            | W11            | W12            | W13            | W14
------|----------------|----------------|----------------|----------------|----------------
内容  | 骨架+接口契约   | P0 核心开发    | P0 收尾+P1 启动 | P1+联调+CI/CD  | 实验+演示+收尾
里程碑| M1 骨架就绪    | M2 P0 单模块跑通| M3 P0 全链路通 | M4 P1 基本可用 | M5 演示交付
实验  | —              | 信任度实验启动 | 信任度实验收尾  | 调试对决进行中 | 反思日志#4
```

**关键里程碑硬约束：**
- **W10 周末**：骨架 + `common/` + `auth` 接口可登录 → 解锁所有人开工
- **W12 周末**：完整业务流（注册→发任务→接单→完成→评价）能跑通（数据可以脏）
- **W14 周三**：所有 P0 + P1 P0 化、CI/CD 绿、两个实验报告完稿

---

## 1. 任务总表（按模块 + 优先级 + 负责人）

> 完成标准统一约束：①符合 P3 详细设计 ②有单元测试且通过 ③在 OpenAPI 上能调通 ④Bug 日志已记录（若有）。

### 1.1 基础设施（M1，全员阻塞依赖）

| ID | 优先级 | 模块 | 任务 | 工时 | 负责人 | 状态 | 完成标准 | 依赖 |
|----|------|------|------|----:|------|:----:|--------|------|
| INF-01 | P0 | 骨架 | 按 `03_包结构骨架.md` 执行 `scaffold-p3.sh`，提交 12 模块空壳 + `.gitkeep` | 1h | A（兼组长） | ⬜ | 12 个模块目录在 git 中可见；`mvn compile` 通过 | — |
| INF-02 | P0 | common | `ApiResponse` / `PageResponse` / `ResponseCode` / `BizException` / `GlobalExceptionHandler` | 2h | A | ⬜ | 接口异常返回统一 JSON 结构；含 traceId | INF-01 |
| INF-03 | P0 | config | `WebMvcConfig`（CORS）/ `JwtConfig` / `SecurityConfig`（放行白名单） | 1.5h | A | ⬜ | 前端可跨域调通 `/api/health` | INF-02 |
| INF-04 | P0 | common | `JwtUtil` / `JwtAuthInterceptor` / `TraceIdInterceptor` | 2h | A | ⬜ | 带合法 JWT 能拿到 userId；无 JWT 返回 401 | INF-03 |
| INF-05 | P0 | 数据库 | 执行 `schema.sql` 建表 + `V2__seed_data.sql` 种子（5 个测试账号 + 信用账户初始化） | 1h | C | ⬜ | 本地 MySQL 30 张表全部建好；测试账号可登录 | INF-01 |
| INF-06 | P0 | 跨模块 API | 定义 `CreditApi` / `UserApi` / `TaskApi` / `NotifyApi` interface（不实现） | 1.5h | D | ✅ | 接口签名与 P3 类图一致；他人可 `@Autowired` 占位（D 只交 CreditApi，TaskApi/NotifyApi 归各模块 owner） | INF-01 |

**INF 小计：9h** | M1 截止：W10 周日 23:59

---

### 1.2 P0 — 用户管理（auth + user）

| ID | 优先级 | 模块 | 任务 | 工时 | 负责人 | 状态 | 完成标准 | 依赖 |
|----|------|------|------|----:|------|:----:|--------|------|
| AUTH-01 | P0 | auth | 短信验证码发送 `POST /api/auth/sms-codes`（含 60s/24h 限流） | 2h | A | ⬜ | 重复请求 429；测试覆盖正常+限流 | INF-04 |
| AUTH-02 | P0 | auth | 手机号+验证码登录/注册 `POST /api/auth/token` | 2h | A | ⬜ | 首次返回 verifyStatus=GUEST；JWT 有效 | AUTH-01 |
| AUTH-03 | P0 | auth | 学生认证提交 + 状态查询 `POST/GET /api/auth/verifications` | 2h | A | ⬜ | 图片走对象存储 mock；姓名/学号写入时加密 | AUTH-02 |
| AUTH-04 | P0 | auth | JWT 续签 + 登出 | 1h | A | ⬜ | refresh token 可换新 JWT | AUTH-02 |
| USER-01 | P0 | user | 修改昵称/头像 `PATCH /api/users/me/profile` | 1.5h | A | ⬜ | 敏感词命中 400；昵称长度校验 | AUTH-02 |
| USER-02 | P0 | user | 三项隐私开关 `PATCH /api/users/me/privacy`（默认开 + 写审计日志） | 1.5h | A | ⬜ | 变更后 `user_audit_log` 有记录 | AUTH-02 |
| USER-03 | P0 | user | 公开主页 `GET /api/users/{userId}/public`（仅 PublicUserVO） | 1h | A | ⬜ | 响应中无 realName/studentNo 字段 | AUTH-02 |
| USER-04 | P0 | user | 个人主页 `GET /api/users/me`（全字段） | 0.5h | A | ⬜ | 本人才能拿到完整字段 | AUTH-02 |

**A 小计：14.5h**（含 INF；另接 NTF-01/02 共 3.5h，见 §1.5）→ **A 总计 18h**

---

### 1.3 P0 — 需求/订单核心（task）

| ID | 优先级 | 模块 | 任务 | 工时 | 负责人 | 状态 | 完成标准 | 依赖 |
|----|------|------|------|----:|------|:----:|--------|------|
| TASK-01 | P0 | task | `TaskStateContext` + 7 个 State 类骨架（State 模式） | 2h | B | ⬜ | ArchUnit 测试通过：状态只能按白名单转换 | INF-06 |
| TASK-02 | P0 | task | 发布跑腿任务 `POST /api/tasks`（信用分校验 + 冻结悬赏） | 2h | B | ⬜ | 信用<60 返回 403；调用 CreditApi.freeze | TASK-01, CRD-01 |
| TASK-03 | P0 | task | 任务编辑 + 取消 `PATCH /api/tasks/{id}` / `POST /api/tasks/{id}/cancel` | 1.5h | B | ⬜ | 仅 PendingAccept 可编辑；取消触发 unfreeze | TASK-01 |
| TASK-04 | P0 | task | 抢单 `POST /api/tasks/{id}/accept`（乐观锁 + 自接禁止 + 上限） | 2h | B | ⬜ | 并发 50 次仅 1 次成功；自接 400 | TASK-01, CRD-01 |
| TASK-05 | P0 | task | 上传凭证 + 发布者确认 `POST /api/tasks/{id}/proof` / `/confirm` | 1.5h | B | ⬜ | 触发 TaskCompletedEvent；CreditApi.settle 被调用 | TASK-04, NTF-01 |
| TASK-06 | P0 | task | 任务大厅 + 详情 `GET /api/search/tasks` / `GET /api/tasks/{id}` | 1.5h | B | ⬜ | 分页参数生效；详情含 PublicUserVO | TASK-02 |
| TASK-07 | P0 | task | 超时扫描 `TaskTimeoutScanner`（5 分钟精度） | 1h | B | ⬜ | 过期任务状态自动 → EXPIRED | TASK-01 |
| TASK-08 | P0 | task | 延长截止 + 接单上限调整 `POST /api/tasks/{id}/extend` / `PATCH /accept-limit` | 1h | B | ⬜ | 最多 2 次 + 单次≤2h 校验 | TASK-03 |

**B 小计：12.5h**

---

### 1.4 P0 — 二手交易 + 辅导发布（trade + edu）

| ID | 优先级 | 模块 | 任务 | 工时 | 负责人 | 状态 | 完成标准 | 依赖 |
|----|------|------|------|----:|------|:----:|--------|------|
| TRADE-01 | P0 | trade | 发布二手商品 `POST /api/trade/items`（图片 EXIF 清洗） | 2h | C | ⬜ | 上传图片元数据被清除；9 图上限 | INF-05 |
| TRADE-02 | P0 | trade | 商品上下架 `PATCH /api/trade/items/{id}/status` | 0.5h | C | ⬜ | 仅本人可操作；越权 403 | TRADE-01 |
| TRADE-03 | P0 | trade | 议价后下单 `POST /api/trade/orders` + 详情 `GET /api/trade/orders/{id}` | 2h | C | ⬜ | 冻结买家积分；状态 IN_TRADE | TRADE-01, CRD-01 |
| TRADE-04 | P0 | trade | 双方确认完成 `POST /api/trade/orders/{id}/confirm` | 1.5h | C | ⬜ | 双方都确认后调用 settle | TRADE-03 |
| EDU-05 | P0 | edu | 辅导需求发布 `POST /api/edu/tutor-tasks`（违禁词拦截） | 2h | C | ⬜ | 命中违禁词 400；3 次冷静 24h | TASK-02 |
| SCH-01 | P0 | 数据库 | `schema.sql` 维护（所有模块表 DDL）+ 索引 + Flyway 迁移脚本 | 2h | C | ⬜ | 全量 DDL 可重放；含必要索引 | INF-05 |

**C 小计：10h**

---

### 1.5 P1 — 信用（credit，D 主责）+ 通知（notify，A 主责）

> 🔄 **2026-05-19 调整**：notify 模块整体从 D 转给 A（理由：站内信本质是 user-facing 基础设施，与 user 模块强相关）。D 聚焦 credit + 集测正常流。

| ID | 优先级 | 模块 | 任务 | 工时 | 负责人 | 状态 | 完成标准 | 依赖 |
|----|------|------|------|----:|------|:----:|--------|------|
| CRD-01 | P0 | credit | `CreditApi` 实现：freeze / unfreeze / settle / getScoreOf / **deduct** | 3h | D | ✅ | 14 个 service 单测全过；bizKey 幂等；余额/分数不出负；@Version 乐观锁 | INF-06 |
| CRD-02 | P1 | credit | 双向评分 `POST /api/credit/reviews`（任务/交易完成后触发） | 2h | D | ✅ | 评分 1-5 校验；不可重复评 409；双方评完各 +1 信用分（幂等 bizKey） | CRD-01, TASK-05 |
| CRD-03 | P1 | credit | 信用分计算 Strategy（按 P1 SRS：**1 加分 + 4 扣分**，看板早期"5+4"不准） | 2h | D | ✅ | 5 条 ScoreRule 全覆盖；变更写 `credit_score_log`（schema.sql 已补 DDL）；分数夹紧 [0,120] | CRD-02 |
| CRD-04 | P1 | credit | `credit/listener/TaskEventListener`（订阅 TaskCompleted/Canceled） | 1h | D | 🟡 | 🔴 BLOCKED on B 的 `TaskCompletedEvent/TaskCanceledEvent` payload 字段定稿；约定走 `unfreeze + settle` 两段调用以适配单 userId 的 settle 契约 | CRD-01, TASK-05 |
| **F-CREDIT-01** | P0 | credit | **`GET /api/credits/me` 我的信用总览** | 0.5h | D | ✅ | 字段与前端 `types/credit.ts` 严格对齐；canPublish/canAccept/dailyAcceptLimit 按 SRS 派生 | CRD-01 |
| **F-CREDIT-08** | P1 | credit | **`GET /api/credits/me/records` 积分流水分页** | 0.5h | D | ✅ | page/size 1-based + 上限 100；按 createdAt DESC；返回 PageResponse 标准结构 | CRD-01 |
| NTF-01 | P1 | notify | 站内信发送 + 列表 + 已读 `GET/POST/PATCH /api/notify/messages` | 2h | **A** | ⬜ | 触发→站内信记录可查；幂等 | INF-06 |
| NTF-02 | P1 | notify | `notify/listener/TaskEventListener`（任务事件 → 站内信模板） | 1.5h | **A** | ⬜ | 5 类任务事件均能触达；24h 同类去重 | NTF-01, TASK-05 |

**D 小计：8h**（模块）+ 1.5h(单测) + 2h(QA-02) + 1.5h(QA-03) + 0.5h(DOC-03 投稿) = **13.5h**
**A 多承担：NTF-01 + NTF-02 = 3.5h**（详见 §1.2）；**A 兼集测交付 owner**（review + 整合 ~1h，含在 DOC-03 主笔工时里）

---

### 1.6 前端（我）

| ID | 优先级 | 任务 | 工时 | 状态 | 完成标准 |
|----|------|------|----:|:----:|--------|
| FE-01 | P0 | 全局基础：Axios `client.ts`（拦截 JWT + 统一解包） / 路由 / 状态管理 | 2h | ⬜ | 与后端 ApiResponse 结构对齐 |
| FE-02 | P0 | 认证页：登录 / 短信 / 学生证认证提交与状态 | 2h | ⬜ | 完整登录流可走通 |
| FE-03 | P0 | 任务模块：大厅 + 详情 + 发布 + 接单 + 凭证 + 确认 | 3h | ⬜ | 完整任务生命周期可演示 |
| FE-04 | P0 | 用户模块：个人主页 / 公开主页 / 隐私开关 | 1.5h | ⬜ | 三项隐私开关默认开 |
| FE-05 | P0 | 二手 + 辅导发布最小页面 | 1.5h | ⬜ | 至少能发起一单 |
| FE-06 | P1 | 评价弹窗 + 信用分展示 + 站内信中心 | 2h | ⬜ | P1 联调用 |

**前端小计：12h**

---

### 1.7 测试与质量（全员）

| ID | 优先级 | 任务 | 工时 | 负责人 | 状态 | 完成标准 |
|----|------|------|----:|------|:----:|--------|
| QA-01 | P0 | 各自模块单元测试（每个 Service 至少 3 个用例：正常+边界+异常） | 各自 1.5h | 各模块 owner | ⬜ | 模块 line coverage ≥ 60% |
| QA-02 | P0 | 集成测试 #1：完整正常流（注册→发任务→接单→完成→评价） | 2h | D | 🟡 | 🔴 BLOCKED on B 的 task 接口 + C 的 trade 接口；BaseIT 基类已交（commit `13f1efc`） |
| QA-03 | P0 | 集成测试 #2：异常流 ×4（未登录访问 / 重复评 / 自评 / 评分越界） | 1.5h | **D** | ✅ | `CreditExceptionFlowTest` 4 个用例全过；ApiResponse 错误码结构正确；commit `8c8cc28` |
| QA-04 | P0 | GitLab CI/CD 配置（依赖 / 静态检查 / 单测 / 集测 / 构建） | 2h | B | ⬜ | 至少 1 次绿色运行 |

**QA 小计：~11.5h**（含各自的 1.5h）

---

### 1.8 实验与文档（按 P4 文档交付物分工）

| ID | 任务 | 工时 | 负责人 | 状态 | 完成标准 |
|----|------|----:|------|:----:|--------|
| EXP-01 | "AI 代码信任度实验"（选 1 个功能点，建议 `登录校验` 或 `订单状态更新`） | 4h | B | ⬜ | 含 Prompt + 直出代码 + 人工修复 + 对比表 |
| EXP-02 | "AI 调试对决"（≥ 2 个 Bug） | 4h | C | ⬜ | 含 6 步对比表 + 4 问分析 |
| DOC-01 | Bug 日志整合（所有人随时记到 `docs/P4/bug/<姓名>_<日期>.md`） | 2h | C 整合 | ⬜ | 至少 5 条 bug + 根因 + 验证 |
| DOC-02 | 演示说明 + README 更新 | 2h | B | ⬜ | 助教按文档可启动 |
| DOC-03 | AI 协作反思日志 #4（**A 主笔汇总** + B/C/D 各投稿自己模块的 0.5h） | 0.5+0.5+0.5+1=2.5h | **A 主笔** | 🟡 | A 主笔 §1-5 已完稿；**D 投稿 §6.3 完成**（2026-05-23）；B/C 投稿位待补 |

**实验文档小计：14h**

---

## 2. 工时核对

> 🔄 **2026-05-19 重平衡**（v2）：
> - notify 模块（NTF-01/02）从 D 转给 A
> - 集成测试（QA-02 + QA-03）**全部归 D**（同一人写风格统一；A 作为交付 owner 兜底 review）
> - 反思日志（DOC-03）改为 A 主笔 + 全员投稿

| 角色 | 模块工时 | QA 工时 | 实验/文档 | 合计 |
|------|--------:|-------:|---------:|------:|
| 前端（我） | 12 | 1.5 | 1（看板维护） | **14.5h** |
| A（auth/user/common **+notify**；集测交付 owner） | 14.5 + 3.5 = 18 | 1.5 | DOC-03 主笔 + 集测 review 共 1h | **20.5h** |
| B（task） | 12.5 | 1.5 | QA-04 CI/CD 2 + EXP-01 + DOC-02 = 6；DOC-03 投稿 0.5 | **20.5h** |
| C（trade/edu/sql） | 10 | 1.5 | EXP-02 + DOC-01 = 6；DOC-03 投稿 0.5 | **18h** |
| D（credit + 集测全套） | 8（去 notify） | 1.5 + QA-02 2 + **QA-03 1.5** = 5 | DOC-03 投稿 0.5 | **13.5h** ✅ |

---

## 3. 风险登记表

| 风险 | 概率 | 影响 | 缓解 | Owner |
|------|:----:|:----:|------|:----:|
| `CreditApi` 是 task/trade 完成结算的硬依赖，D 进度延迟 → 阻塞 B+C | 高 | 高 | CRD-01 必须 W11 中期完成；过期则 D 临时给 B/C 提供 stub | D + 组长 |
| 前端 owner 一人对 4 个后端 → 接口字段不一致 | 高 | 中 | **W10 周末前 OpenAPI 冻结**；之后改字段需 PR + 我同意 | 我 |
| 状态机（task）实现复杂，单测不全 → 联调炸 | 中 | 高 | TASK-01 单独 2h；ArchUnit 强约束状态白名单 | B |
| GitLab Runner 学校网络不稳 → CI/CD 跑不起来 | 中 | 中 | 预案：本地 docker-compose 跑 GitLab Runner；备用 GitHub Actions | B |
| 信用分算法的"公平性"无法量化 → 评审打分低 | 中 | 中 | Strategy 模式 + 算法文档 + 5 个测试场景固化 | D |
| Bug 日志每次必记容易漏 → C 整合时一片空白 | 高 | 中 | 在 `.git/hooks` 或 PR 模板加 checkbox：本次是否触发 bug 记录 | 全员 |

---

## 4. 每日站会节奏

- **时间：** 每晚 22:00（线上，10 分钟封顶）
- **三问：** 昨天做了什么 / 今天做什么 / 卡在哪
- **看板更新：** 任务状态由 owner 自己改本文档对应行的"状态"列
- **阻塞升级：** 标 🔴 的任务次日早上前由组长（A）拉单点同步

---

## 5. 验收自检（对齐 P4 验收标准）

- [ ] P0 优先级 38 个功能全部可用（本看板 ID 前缀 INF/AUTH/USER/TASK/TRADE/EDU/SCH/CRD-01/NTF-01/FE-01~05）
- [ ] P1 功能基本可用（CRD-02~04 / NTF-02 / FE-06）
- [ ] 单测覆盖率 ≥ 60%（mvn jacoco 报告）
- [ ] 集测 ≥ 1 正常流 + 2 异常流（QA-02 / QA-03）
- [ ] GitLab CI/CD 至少 1 次成功记录（QA-04）
- [ ] "AI 代码信任度实验"完稿（EXP-01）
- [ ] "AI 调试对决"≥ 2 Bug（EXP-02）
- [ ] Bug 日志含根因（DOC-01）
- [ ] 反思日志 #4 有实质内容（DOC-03）
- [ ] 演示路径文档完成（DOC-02）

