# P3 SOLID 检查清单与设计模式应用

> **配套交付：** [04_核心类图](./04_核心类图.md)（v1 修正版类图）  
> **实验流程来源：** [P3-详细设计](../P3-详细设计.md) §1.2「AI 设计缺陷注入」

---

## 1. v0：AI 原始类图设计（典型缺陷说明）

在向 AI 同时投喂 SRS 与架构文档时，容易得到一版**看似完整、但违反模块化纪律与 SOLID** 的设计。本团队记录的 **v0 特征**如下（用于对照实验，非可运行代码）：

1. **上帝服务 `CampusPlatformService`**：单类承担短信登录、任务发布、积分冻结、私信发送，Controller 仅作薄壳转发。
2. **跨层穿透：** `TaskServiceImpl` 直接 `@Autowired CreditAccountRepository`、`UserProfileRepository`（跨包访问他模块持久层）。
3. **胖接口 `UserFacade`**：一个接口同时包含 `login()`、`updateAvatar()`、`getCreditScore()`、`deductPoints()`，调用方被迫依赖不需要的方法。
4. **事件滥用：** 在 `notify` 的 Listener 里直接修改 `Task` 实体状态，造成隐式写路径与事务边界模糊。
5. **可扩展性：** `deductPoints(int reason)` 使用整段 `switch(reason)`，新增扣分原因时修改核心方法（倾向违反 OCP）。
6. **里氏替换（说明项）：** 子类 `FastTaskService` 覆盖 `accept()` 时放宽「自接校验」或抛出额外受检异常，破坏父类契约。

**结论：** v0 在「功能罗列」上达标，但在 **模块边界、依赖方向、接口粒度** 上与 P2 架构不变量冲突。

---

## 2. v1：修正链路（自 v0 到定稿类图）

| 步骤 | 动作 | 解决的 SOLID / 架构点 |
| :--- | :--- | :--- |
| ① | 按域拆分为 `AuthService`、`TaskService`、`CreditService` 等，删除上帝类 | **S** 单一职责 |
| ② | 跨模块只注入 `CreditApi`、`UserApi`、`TaskApi`；本模块只访问本模块 `Repository` | **D** 依赖倒置 + P2 §2 不变量 |
| ③ | 拆分 `UserApi`（公开资料查询）与 `AuthService`（认证票据），信用读写仅经 `CreditApi` | **I** 接口隔离 |
| ④ | 任务状态转移抽取为 `TaskState` + `TaskStateContext`（State 模式） | **O** 开闭（新增状态少改主干） |
| ⑤ | 信用扣分按 `reasonCode` 映射 `DeductStrategy`（Strategy 模式） | **O** 开闭 |
| ⑥ | 只读侧写 `SearchService` → `TaskApi`，禁止 search 写他域表 | **D** + 模块分层 |
| ⑦ | 事件监听类仅调用对应 `*Api` 或追加通知，不回写业务聚合 | **S** + 事务可预期 |

定稿类图见 [04_核心类图](./04_核心类图.md)。

---

## 3. SOLID 逐条检查清单（对照 v0）

| SOLID 原则 | 检查问题 | AI 设计是否违反 | 违反说明 | 修正方案 |
| :--- | :--- | :--- | :--- | :--- |
| **S** — 单一职责 | 有没有类承担了过多职责？ | **是** | `CampusPlatformService` 聚合鉴权、任务、积分、IM；Notify Listener 修改任务状态 | 按模块拆服务；Listener 只触发通知或调用目标域 `*Api` |
| **O** — 开闭原则 | 新增需求类型是否需要修改现有代码？ | **是** | 任务状态转移、扣分原因均用超长 `switch`，新增分支改核心类 | 引入 **State**（任务）与 **Strategy**（扣分）封装变化点 |
| **L** — 里氏替换 | 子类是否可以替换父类使用？ | **是（设计层）** | 子类 `accept()` 放宽校验 / 异常契约不一致 | 禁止弱化前置条件；异常统一为 `BizException` + 错误码 |
| **I** — 接口隔离 | 有没有接口太「胖」，包含了不需要的方法？ | **是** | `UserFacade` 混合认证、资料、信用 | 拆为 `AuthService`、`UserApi`、`CreditApi` 等窄接口 |
| **D** — 依赖倒转 | 高层模块是否直接依赖了低层模块的具体实现？ | **是** | `TaskServiceImpl` 依赖 `CreditAccountRepository` 等他模块 Repository | 仅依赖接口：`CreditApi`、`UserApi`；持久化留在各模块内部 |

---

## 4. 违规统计与计数规则

| 项 | 数值 |
| :--- | ---: |
| **v0 违反 SOLID 的独立设计点** | **6** |
| 计数规则 | 每一行对应一类可独立修复的设计问题（上帝类、跨包 Repository、胖接口、Listener 越权写、switch 扩展、LSP 契约破坏） |

> 与 [P3-详细设计](../P3-详细设计.md) §三「反思日志 #3」对齐时，可将 **「SOLID 检查中发现的问题数量」** 填 **6**；**「最严重的设计问题」** 建议填：**跨模块直接依赖他模块 Repository，破坏信用域不变量与事务边界**。

---

## 5. 设计模式应用（至少 2 种）

### 5.1 State（状态）模式 — `task` 模块

| 维度 | 说明 |
| :--- | :--- |
| **场景** | P1 FR-TASK-03：任务从「待接单 → 进行中 → 待确认 → 已完成 / 已取消 / 异常超时」等多状态流转，且各状态允许的操作不同（接单、提交凭证、确认、取消、延长截止时间）。 |
| **角色类** | `TaskState`（接口）、`TaskStateContext`、`PendingAcceptState`、`InProgressState`、`WaitConfirmState`、`CompletedState`、`CanceledState`、`ExpiredState`；`TaskServiceImpl` 将动作委托给 Context。 |
| **为什么在这里使用** | 把「何种状态下允许何种转移」封装到具体状态类，避免在 `TaskServiceImpl` 中堆积 `if/else` 与重复校验；与 P2「乐观锁 + 同事务积分联动」配合时，状态方法可集中抛出业务异常以触发回滚。 |
| **如果不用会怎样** | 状态分支膨胀、易漏判（如进行中仍允许「待接单才允许」的编辑）；新增终端状态或子流程时反复修改同一巨型方法，**开闭性**差，评审难以覆盖所有组合。 |

### 5.2 Strategy（策略）模式 — `credit` 模块（`CreditApi.deduct` / F-CREDIT-04）

| 维度 | 说明 |
| :--- | :--- |
| **场景** | P1 FR-CRED-01：多种扣分/惩戒原因（严重违规 −30、无故取消 −5、恶意差评核实 −10 等），规则可能独立演进；同一入口 `deduct(userId, delta, reasonCode, bizKey)` 需保持幂等与流水一致。 |
| **角色类** | `DeductStrategy`（接口）、若干实现类（如 `SevereViolationDeductStrategy`、`TaskNoShowDeductStrategy`）、`DeductStrategyRegistry`（按 `reasonCode` 选择策略）；`CreditServiceImpl` 负责账户加载、事务、流水落库，策略负责「该原因下的额度校验与附加副作用（若有）」。 |
| **为什么在这里使用** | 变化点在于「原因码对应的规则」，而非积分账户存储本身；策略类隔离规则，新增原因码可通过 **新增实现类 + 注册** 完成，减少修改核心 `deduct` 流程。 |
| **如果不用会怎样** | `deduct` 内无限 `if-else`/`switch`，规则互耦；一次新增原因可能误改其他分支的幂等键或流水类型，测试组合爆炸；违反 **OCP**，也与团队对「原因码可配置扩展」的演进预期不符。 |

### 5.3 （可选对照）Observer — Spring `ApplicationEvent`

进程内 **`ApplicationEvent` + `@TransactionalEventListener`** 实现模块间 **发布 / 订阅**，在语义上接近 Observer。本任务主交付仍以 **State + Strategy** 两种 **Gang of Four** 模式为准；事件机制详见 P2 §4.3，不在此重复计为「第 3 种必交模式」。

---

## 6. 变更记录

| 日期 | 变更 |
| :--- | :--- |
| 2026-05-12 | 初版：v0/v1 对照、SOLID 表、违规计数 6、State + Strategy 说明 |
