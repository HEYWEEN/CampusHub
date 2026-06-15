# Phase 4 — AI 使用前统一 Prompt（上下文注入器）

> **目的：** 让 4 名后端组员在让 AI 写代码前，**先喂这段 prompt** 把项目上下文、命名规约、安全红线一次性注入，避免 AI 凭空生成与项目不符的"通用 Spring Boot 代码"。
> **使用方式：**
> 1. 新开 AI 会话 → 把 §A 的 prompt 整段贴进去 → AI 回复"已理解 CampusHub 项目背景"后再开始具体编码任务
> 2. 每次提具体编码需求时，按 §B 的模板补充任务上下文
> 3. AI 输出后，按 §C 的清单做人工审查
>
> ⚠️ **常见误解**：以为"AI 越聪明 prompt 越短" — 实际上**项目特有约束（命名、隐私字段黑名单、状态机白名单）AI 无法从训练数据中获得**，必须显式告知。

---

## §A 通用项目上下文注入 Prompt（每次新会话开头必喂）

> 直接复制下面 ```text 代码块内的全部内容到 AI 对话框，作为第一条消息发送。

```text
你将协助我开发 CampusHub（高校综合互助平台）后端代码，技术栈与约束如下。请先确认理解，再开始任务。

==================== 一、技术栈 ====================
- 语言：Java 17
- 框架：Spring Boot 3.5.3（不是 4.x！4.x 截至 2026-05 尚未发布）
- 关键依赖（artifactId 严格按 Maven Central 实际命名）：
    - spring-boot-starter-web      （注意：不是 starter-webmvc）
    - spring-boot-starter-data-jpa
    - spring-boot-starter-security
    - spring-boot-starter-test    （含 JUnit 5 + Mockito + AssertJ + Spring Test）
    - mysql-connector-j (runtime)
    - h2 (test scope)
- 数据库：MariaDB / MySQL 8.x（约 23 张表，模块前缀强约束；枚举列 INT，boolean 列 TINYINT）
- 鉴权：JWT（HS256），有效期 2h，refresh token 7d
- 构建：Maven（用 ./mvnw，无需本地装 Maven）
- CI/CD：GitHub Actions（已有 main.yml）；P4 阶段可选迁移 GitLab CI/CD

⚠️ 历史教训：早先 AI 起 pom.xml 时编造了 4.0.5 + starter-webmvc 等不存在的依赖。
    生成新 pom 或 maven 依赖时，**只允许使用上面列出的 artifactId**，不要自创变体。

==================== 二、包结构（强约束）====================
所有代码位于 com.campushub 下，按业务模块分包：
- common/          # 跨模块共享（ApiResponse / BizException / JwtUtil 等），无业务逻辑
- config/          # Spring 配置类
- auth/            # 鉴权（短信、登录、学生认证）
- user/            # 用户资料、隐私开关
- task/            # 任务（含 7 态状态机：PendingAccept/InProgress/WaitConfirm/Completed/Canceled/Expired）
- trade/           # 二手交易
- edu/             # 学习社区（资料/课评/辅导）
- team/            # 比赛组队（P1）
- im/              # 即时通信（P2）
- notify/          # 站内信
- credit/          # 积分 + 信用分
- report/          # 举报仲裁
- admin/           # 管理端
- search/          # 搜索（无主表）

每个模块标准子包：
api/ controller/ service/ repository/ entity/ dto/ vo/ event/ exception/

⚠️ 跨模块调用必须走 *Api interface（例如 CreditApi），严禁直接 @Autowired 其他模块的 ServiceImpl。

==================== 三、命名与返回规约 ====================
- Controller 路径：/api/{模块}/...（例：/api/tasks、/api/credit/reviews）
- 入参类：xxxDTO（例：TaskCreateDTO）
- 出参类：xxxVO（例：TaskDetailVO）— 必须以 VO 结尾或位于 common 包
- 数据库实体：xxx（例：Task）→ 对应表 task_order（注意：实体类名与表名不强制一致）
- 表名前缀严格按模块：auth_* / user_* / task_* / trade_* / edu_* / team_* / im_* / notify_* / credit_* / report_* / admin_*
- 统一响应体（全部接口必须使用）：
    ApiResponse<T> { int code; String message; T data; String traceId; }
- 分页响应体：
    PageResponse<T> { List<T> items; long total; int page; int size; }
- 异常：业务异常抛 BizException(code, message)，由 GlobalExceptionHandler 统一处理

==================== 四、安全与隐私红线（不可越过）====================
1. 隐私字段黑名单：realName、studentNo、phone（明文）— 绝对不允许出现在任何返回给前端的 VO 中
2. 公开用户对象统一使用 PublicUserVO（只含 userId / nickname / avatarUrl / verifiedTag）
3. 手机号、姓名、学号写入数据库前必须走 AesUtil 加密；查询用 HMAC 索引列
4. 所有写操作必须有权限校验（@CurrentUserId 或拦截器注入），缺失直接抛 403
5. 三项隐私开关（hide_publish_hist / hide_accept_hist / hide_course_reviews）默认值都是 true（默认隐藏）
6. 任何越权访问必须返回 403，不能返回 404 给攻击者侦察机会

==================== 五、业务核心规则 ====================
1. task 状态机（State 模式）状态转换白名单（违法转换抛 TaskStateException）：
   PendingAccept → InProgress / Canceled / Expired
   InProgress → WaitConfirm / Canceled / Expired
   WaitConfirm → Completed / Canceled
   Completed / Canceled / Expired → （终态，不可转出）

2. 信用分（credit_account.credit_score）规则：
   - 初始 80 分；范围 [0, 100]
   - < 60 分禁止发布任务（getScoreOf 校验）
   - 变更必须写 credit_score_log

3. 积分（credit_account.point_balance）规则：
   - 永远 ≥ 0（DB CHECK 约束 + 应用层双校验）
   - 4 个操作：FREEZE / UNFREEZE / SETTLE / DEDUCT，全部写 credit_record 流水

4. 抢单 / 议价下单必须使用乐观锁（version 字段 + UPDATE ... WHERE version=?），禁止用悲观锁

5. 通知去重：24h 内同 (userId, type, bizId) 只发 1 条

==================== 六、编码风格 ====================
- 优先用接口编程（Service / Repository 均有 interface）
- 单文件不超过 300 行；方法不超过 50 行
- 注释只在"为什么"非显然时写，不写"做什么"
- 异常处理用 BizException，禁止裸 throw new RuntimeException
- 日志用 SLF4J，关键节点 INFO，分支异常 WARN
- 测试与代码 1:1 分包（test/java/com/campushub/task/... 对应 main/java/com/campushub/task/...）

==================== 七、你的输出要求 ====================
1. 每次给代码前先用 1-2 句话说明"我打算如何实现 + 哪些假设"
2. 代码必须可直接编译，不要伪代码、不要 // TODO
3. 涉及数据库的代码必须给出对应的表字段假设（即使没有 schema.sql）
4. 涉及跨模块调用时显式列出依赖的 *Api 接口
5. 如果我的需求违反上面任意红线，直接指出并拒绝实现
6. 单元测试与实现同时给出，至少包含：正常路径 + 1 个边界 + 1 个异常

==================== 八、确认 ====================
请回复一句"已理解 CampusHub 项目背景，可以开始具体任务"，然后等待我给出具体的编码需求（例如：实现 POST /api/tasks 接口）。
不要在此时直接生成任何业务代码。
```

---

## §B 单次任务追加 Prompt 模板（每次具体需求时使用）

> 在 AI 回复"已理解"之后，按下面模板提具体需求。**不要省略任何字段**。

```text
任务：实现 [功能名]
功能 ID（对照 docs/P3/02_功能映射主表.md）：[例 F-TASK-03]
所属模块：[例 task]
负责人：[你的名字]

== 1. 业务规则 ==
[抄一遍 P3 功能映射主表里"关键规则 / 验收要点"那一列，不要省略]

== 2. API 契约 ==
- Method + Path：[例 POST /api/tasks/{taskId}/accept]
- 请求体：[字段名 + 类型 + 必填/可选 + 约束]
- 响应体：[同上，必须用 ApiResponse<XxxVO> 包装]
- 鉴权要求：[需要登录 / 需要已认证 / 需要信用分 ≥ 60 / ...]
- 异常情况：[列出所有可能返回的 4xx 错误码 + 触发条件]

== 3. 数据库 ==
- 涉及表：[例 task_order / credit_account / credit_record]
- 关键字段：[列出会读 / 写的字段]
- 并发控制：[乐观锁 version / 悲观锁 / 无]

== 4. 跨模块依赖 ==
- 调用：[例 CreditApi.freeze、UserApi.getVerifyStatus]
- 发布事件：[例 TaskAcceptedEvent]

== 5. 我希望你给我 ==
- [ ] Controller 方法
- [ ] Service 实现 + Service interface 更新
- [ ] DTO / VO 类
- [ ] 单元测试（≥3 个用例）
- [ ] 如有数据库字段变更，给出 Flyway 增量 SQL

== 6. 我已知的边界情况，请确保覆盖 ==
- [列 2-3 个你担心的场景]

== 7. 不要做 ==
- 不要修改 common/ 和 config/ 包（A 负责，私改会冲突）
- 不要直接 @Autowired 其他模块的 ServiceImpl
- 不要在 VO 中暴露 realName / studentNo / phone 明文
```

---

## §C AI 输出后人工审查清单（强制 4 检）

> **常见误解**：以为代码能编译就 OK。实际上 AI 最常翻车的是"看起来对，但违反项目隐私/状态机约束"。

### ✅ 检查 1：编译与基本正确性
- [ ] `mvn compile` 通过
- [ ] 单测 `mvn test` 通过
- [ ] 代码风格符合项目（包路径、命名、缩进）

### ✅ 检查 2：项目特有约束
- [ ] 返回体用了 `ApiResponse<T>` 包装，没有裸返回 entity
- [ ] VO 中没有 realName / studentNo / phone 明文
- [ ] 跨模块调用走了 `*Api` interface，没有直接 `@Autowired ServiceImpl`
- [ ] 异常抛 `BizException`，没有裸 `RuntimeException`
- [ ] 状态机转换在白名单内（task 模块）

### ✅ 检查 3：边界情况
- [ ] 并发场景考虑了乐观锁（抢单 / 议价下单）
- [ ] 信用分校验在写操作之前（不要先扣再校验）
- [ ] 隐私字段加密走 AesUtil（不要明文写库）
- [ ] 文件上传有大小 / 类型 / 数量校验

### ✅ 检查 4：测试质量
- [ ] 单测有真实断言，不是只 `assertNotNull(result)` 这种空洞断言
- [ ] 至少包含：正常路径 + 边界 + 异常 各 1 个
- [ ] 异常路径测了**具体的错误码**而不只是 `assertThrows(Exception.class)`

⚠️ **任何一项不通过 → 在 PR 描述里如实记录"AI 直出问题" → 这是 P4 信任度实验的原始数据来源**

---

## §D 必须记录到 P4 信任度实验的字段

每次让 AI 写代码，**在 `docs/P4/bug/<你的名字>_<日期>.md` 同步记录**（不只是 bug，AI 直出问题也算）：

```markdown
## [日期 时间] AI 协作记录

- **功能**：F-TASK-03 抢单
- **AI 工具**：Claude / GPT-4 / DeepSeek-Coder
- **Prompt 摘要**：[1 句话]
- **AI 直出代码**：能否编译 ✓ / 能否运行 ✓ / 测试是否通过 ✗（缺乐观锁）
- **人工发现问题**：
  1. 没有用 version 字段做乐观锁，并发时会重复接单
  2. 自接禁止只校验了 publisherId，没校验"已是 accepter"的边界
  3. 返回了 task entity 而不是 TaskDetailVO，暴露了内部字段
- **修复时长**：30 分钟
- **修复后**：测试通过 ✓
- **结论**：AI 对乐观锁的处理需要显式提醒；隐私字段问题反复出现，应在通用 prompt 中强化
```

> 这些记录是 P4 交付物 #6（信任度实验报告）和 #8（Bug 修复日志）的原始素材。**不要等到最后补**。

---

## §E 反例（不要这样用 AI）

❌ **反例 1：** "帮我写一个 Spring Boot 的接单接口"
→ AI 会写出一个通用版本，但乐观锁、自接禁止、信用分校验、CreditApi 调用全部缺失。

❌ **反例 2：** "按 P3 设计实现 F-TASK-03"
→ AI 不知道 P3 是什么，会编造一份"它以为的"详细设计。

❌ **反例 3：** 把 AI 输出的代码直接 commit
→ 隐私字段泄露 / 状态机违法转换 / 缺少单元测试，PR 评审会被打回，浪费时间。

✅ **正确用法：** §A 喂上下文 → §B 提具体需求 → §C 人工 4 检 → §D 记录 → commit 时标 `[AI-assisted]`

---

**Prompt 版本：** v1.0
**最后修订：** 2026-05-19
**维护：** 前端 owner（我）
**变更规则：** 项目约束（命名 / 隐私 / 状态机）发生变化时同步更新 §A。
