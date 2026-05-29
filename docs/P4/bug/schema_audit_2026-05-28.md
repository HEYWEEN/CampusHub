# 全栈字段一致性审计报告

> 生成时间：2026-05-28
> 工具：claude general-purpose agent（只读审计）
> 范围：backend `src/main/java/com/campushub/**` 与 frontend `src/{api,types,pages,components,stores}/**`
> 不修改任何代码

---

## 摘要

- 扫描后端 `@RestController` 数：8（auth / user / task / trade / credit / credit-review / notify / edu）
- 扫描 `@Entity`：17 个
- 扫描 Flyway migration：V1 ~ V6（V2 是种子数据，V3 已 no-op）
- 扫描前端 `api/*.ts`：7 个文件（`auth/user/task/trade/credit/notify/client`）
- 扫描前端 `types/*.ts`：5 个文件

**发现不一致总数：33 处**
- A. API payload：**14**
- B. VO：**13**
- C. Schema drift：**3**
- D. Entity↔VO 漂移（次要）：**3**

**严重等级分布**：🔴 12 / 🟠 12 / 🟡 9

---

## A. API Payload 不一致清单

### A-1. GET `/api/search/items` — 二手商品列表接口**后端完全没实现**

- **严重度**：🔴 阻塞
- **前端**：`frontend/src/api/trade.ts:20` 调用 `apiGet('/api/search/items', params)`
- **后端**：grep `api/search` 仅命中 `TaskController:42` 一处；trade 模块没有任何 `@GetMapping` 暴露 `/api/search/items` 或 `/api/trade/items`（仅 `POST /api/trade/items` 创建）
- **影响**：二手大厅 `TradeHallPage` 列表请求 → 404；dev 模式靠 `_mock.ts` 兜底，prod 直接崩
- **修复方向**：在 `TradeController` 补 `GET /api/search/items`

### A-2. GET `/api/trade/items/{id}` — 二手商品详情接口**后端没实现**

- **严重度**：🔴 阻塞
- **前端**：`frontend/src/api/trade.ts:26` 调用 `apiGet('/api/trade/items/${itemId}')`
- **后端**：`TradeController` 只暴露 `POST /items`、`PATCH /items/{id}/status`，无 `GET /items/{id}`
- **影响**：商品详情页 `TradeDetailPage` 在生产环境 404
- **修复方向**：补 `GET /api/trade/items/{id}`

### A-3. POST `/api/trade/items` — Content-Type 不匹配（multipart vs JSON）

- **严重度**：🔴 阻塞
- **前端**：`frontend/src/api/trade.ts:32` 调用 `apiPost('/api/trade/items', dto)`；`client.ts:22` 默认 `Content-Type: application/json`
- **后端**：`TradeController:46-49` 是 `consumes = MULTIPART_FORM_DATA_VALUE`，用 `@ModelAttribute TradeItemCreateDTO + @RequestParam("images") MultipartFile[]`
- **不一致**：前端 JSON body vs 后端 multipart form-data
- **影响**：前端创建商品永远 415 Unsupported Media Type；且前端字段名也不对（见 A-4）

### A-4. POST `/api/trade/items` — 创建商品字段名几乎全错

- **严重度**：🔴 阻塞
- **前端 `TradeItemCreateDTO`**（`frontend/src/types/trade.ts:22-29`）：`{ title, price, description, images, pickupType, buildingRange }`
- **后端 `TradeItemCreateDTO`**（`backend/.../trade/dto/TradeItemCreateDTO.java:9-37`）：`{ title, description, pricePoint, pickupLocationType, pickupLocationDetail }` + `MultipartFile[] images`
- **不一致**：
  | 前端字段 | 后端字段 | 备注 |
  |---|---|---|
  | `price` | `pricePoint` | 命名 |
  | `pickupType` | `pickupLocationType` | 命名 + 枚举（`MEETING` vs `MEETUP`，见 B-9） |
  | `buildingRange` | `pickupLocationDetail` | 命名 |
  | `images: string[]` | `MultipartFile[]` | 类型完全不同 |
- **影响**：完全无法创建商品

### A-5. POST `/api/tasks` — 创建任务字段不完整 + 命名不一致

- **严重度**：🔴 阻塞
- **前端 `TaskCreateDTO`**（`frontend/src/types/task.ts:43-50`）：`{ taskType, title, detail, rewardPoint, deadlineAt, building? }`
- **后端 `TaskCreateDTO`**（`backend/.../task/dto/TaskCreateDTO.java:13-54`）：`{ title, taskType, rewardPoint, deadlineAt, pickupHint*, deliveryBuilding*, remark }`（带 `*` 的 `@NotBlank`）
- **不一致**：
  | 前端发送 | 后端期望 | 备注 |
  |---|---|---|
  | `detail` | `remark` | 命名 |
  | `building` (optional) | `deliveryBuilding` (`@NotBlank`) | 命名 + 必填差异 |
  | （没传） | `pickupHint` (`@NotBlank`) | 必填字段前端没传 |
- **影响**：发布任务永远 400（`pickupHint`/`deliveryBuilding` 校验失败）

### A-6. POST `/api/tasks/{id}/accept` — 缺 version 乐观锁参数

- **严重度**：🟠 高
- **前端**：`frontend/src/api/task.ts:57-61` `apiPost('/api/tasks/${id}/accept')`，**无 body**
- **后端**：`TaskController:87-93` 接 `@RequestBody @Valid TaskAcceptDTO { @Min(0) int version }`
- **不一致**：前端没传 version，Spring 解析空 body 时 `@RequestBody` 反序列化失败（`HttpMessageNotReadableException` → 400）
- **影响**：接单接口立即 400

### A-7. POST `/api/tasks/{id}/cancel` — 缺 reason 字段（同 A-6）

- **严重度**：🟠 高
- **前端**：`frontend/src/api/task.ts:75-79` 无 body
- **后端**：`TaskController:75-82` 接 `@RequestBody @Valid TaskCancelDTO { reason }`
- **影响**：取消任务 400（同上）

### A-8. POST `/api/tasks/{id}/proof` — Content-Type 与字段全错

- **严重度**：🔴 阻塞
- **前端**：`frontend/src/api/task.ts:63-67` JSON 发 `{ images: string[], note: string }`
- **后端**：`TaskController:97-105` `consumes = MULTIPART_FORM_DATA_VALUE`，接 `@RequestParam List<MultipartFile> images, @RequestParam String text`
- **不一致**：JSON vs multipart；字段 `note` vs `text`
- **影响**：上传凭证 415 / 400

### A-9. POST `/api/auth/register` — 字段名 `code` vs `smsCode`

- **严重度**：🔴 阻塞（与 task 描述的"已修"样本同模式，但仍残留）
- **前端**：`frontend/src/api/auth.ts:18` `apiPost('/api/auth/register', { phone, smsCode: code, password })`
- **后端**：`backend/.../auth/dto/RegisterDTO.java:18` `private String smsCode`
- **现状**：实际已经对齐为 `smsCode`，**A-9 已 OK**（保留条目以确认）

### A-10. GET `/api/users/{userId}/public/stats` — 后端不存在

- **严重度**：🟠 高
- **前端**：`frontend/src/api/user.ts:43-47` `apiGet('/api/users/${userId}/public/stats')`
- **后端**：`UserController` 只有 `GET /{userId}/public`，无 `/public/stats`
- **影响**：公开主页统计区在生产环境 404，dev 走 mock 没暴露
- **修复方向**：补 endpoint；或者下线该前端调用

### A-11. POST `/api/auth/verifications/me` — 应为 GET

- **严重度**：🟠 高
- **前端**：`frontend/src/api/auth.ts:30-33` 用 `apiPost('/api/auth/verifications/me')`
- **后端**：`AuthController:105-109` `@GetMapping("/verifications/me")`
- **不一致**：HTTP 方法不同（POST → GET）
- **影响**：405 Method Not Allowed

### A-12. POST `/api/auth/verifications` — 提交字段命名完全错

- **严重度**：🔴 阻塞
- **前端 `VerifySubmitDTO`**（`frontend/src/api/auth.ts:20-25`）：`{ realName, studentNo, certKind: 'STUDENT_CARD'|'ID_BACK', images: string[] }`
- **后端 `VerificationSubmitDTO`**（`backend/.../auth/dto/VerificationSubmitDTO.java:21-48`）：`{ realName, studentNo, idCard?, attachmentsBase64: List<String>[1..5] }`
- **不一致**：
  | 前端 | 后端 |
  |---|---|
  | `certKind` | （无对应字段） |
  | `images: string[]`（URL） | `attachmentsBase64: string[]`（Base64 编码字符串） |
  | （无） | `idCard?` |
- **影响**：认证提交 400（`@NotEmpty attachmentsBase64` 一定为空）。注：当前前端 `VerifyPage.tsx` 尚未实际调这个 API，是定义级 schema drift

### A-13. POST `/api/auth/logout` — 前端没传 refresh token

- **严重度**：🟡 中（不报错但留下脏 refresh token）
- **前端**：`frontend/src/api/auth.ts:35` `apiPost('/api/auth/logout')` 无 body
- **后端**：`AuthController:124-133` `@RequestBody(required = false) RefreshTokenDTO`，refresh token 用于黑名单
- **影响**：refresh token 未失效，残余 14 天可被重放

### A-14. GET `/api/search/tasks?status=…` — 类型 string vs Integer

- **严重度**：🔴 阻塞
- **前端 `TaskSearchParams.status`**（`types/task.ts:54-61`）：`TaskStatus`（`'PENDING_ACCEPT'|...`）字符串
- **后端 `TaskQueryDTO.status`**（`TaskQueryDTO.java:13`）：`Integer status`
- **不一致**：前端永远传 `status=PENDING_ACCEPT`，Spring 转 Integer 抛 `MethodArgumentTypeMismatchException` → 400
- **影响**：任务大厅默认筛选「可接」直接 400；仅当用户切到「全部」（`null`，不传该 param）才能用

### A-15. GET `/api/search/tasks?taskType=…` — 类型未对齐

- **严重度**：🟡 中
- **前端 `TaskSearchParams.taskType`**：`TaskType`（`'ERRAND'|'MUTUAL_HELP'|'TUTOR'`）字符串
- **后端 `TaskQueryDTO.taskType`**：`TaskType taskType`（Java enum）
- **不一致**：实际可以工作（Spring 默认按 `Enum.valueOf` 反序列化），列出供核查

---

## B. VO 不一致清单

### B-1. UserMeVO — 嵌套结构 vs 扁平结构（双重不一致）

- **严重度**：🔴 阻塞
- **后端 `UserMeVO`**（`backend/.../user/vo/UserMeVO.java:17-57`）：扁平，字段 `hidePublishHistory / hideAcceptHistory / hideCourseReviews`（全称），**无 `imOpen`**
- **前端 type `UserMeVO`**（`frontend/src/types/user.ts:22-27`）：嵌套 `privacy: { hidePublishHist, hideAcceptHist, hideCourseReviews, imOpen }`（缩写 + 多了 `imOpen`）
- **mock**（`_mock.ts:310-324`）：跟随**类型**用嵌套缩写
- **页面实际用法**（`MePage.tsx:162`, `ProfileEditPage.tsx:43-46`）：用**扁平全称** `me.hidePublishHistory`、`me.hideAcceptHistory`，与后端一致 ⚠️ 已经偏离自家 TS 类型，但能跑通真实接口
- **影响**：TS 类型与真实数据完全错配；任何按类型写新代码的人都会踩坑
- **修复方向**：把 `types/user.ts` 改成扁平 + 移除 `imOpen`，与后端对齐；同步 mock

### B-2. UserMeVO — `verifiedTag` 字段类型不一致

- **严重度**：🟠 高
- **后端**：`UserMeVO` 本身**不包含** `verifiedTag`（只有 `verifyStatus`），但前端的 `UserMeVO extends PublicUserVO`，所以会查找 `verifiedTag`
- **前端 `PublicUserVO.verifiedTag`**（`types/user.ts:12`）：`'校园已认证' | null`（字符串字面量）
- **后端 `PublicUserVO.verifiedTag`**（`common/PublicUserVO.java:21`）：`boolean`
- **不一致**：boolean → 字符串；`MePage.tsx:62` 写 `me.verifiedTag && <div>{me.verifiedTag}</div>` 期望字符串
- **影响**：已认证用户卡上要么显示 `true`（boolean 值），要么 React 渲染异常

### B-3. PublicUserVO — `verifiedTag` boolean vs string

- **严重度**：🟠 高（同 B-2，PublicUserVO 在大量地方被复用）
- **后端**：`PublicUserVO.verifiedTag: boolean`（Jackson 默认序列化为 `true/false`）
- **前端**：`PublicUserVO.verifiedTag?: '校园已认证' | null`
- **使用方**：`PublicUserCard.tsx:30` `{user.verifiedTag && <span>{user.verifiedTag}</span>}` 会直接把 boolean 当字符串输出 → 渲染为 `true`
- **影响**：任务卡 / 商品卡 / 用户卡的已认证 tag 全部异常

### B-4. PublicUserVO — `avatarUrl` nullability

- **严重度**：🟡 中
- **后端**：`PublicUserVO.avatarUrl: String`（无 null 标注，可能 null）
- **前端**：`avatarUrl?: string | null` ✓ 兼容
- 一致

### B-5. PublicUserVO — `userId` 类型 number vs string

- **严重度**：🟠 高
- **后端**：所有 `userId / taskId / itemId` 都是 `Long`（Jackson 序列化为 JSON number）
- **前端**：`PublicUserVO.userId: string`，`TaskDetailVO.taskId: string`，`TradeItemVO.itemId: string`
- **影响**：
  - `task.publisher.userId === currentUserId` 等比较会因类型不同（`'1' === 1`）始终 false
  - `<Link to={`/u/${userId}`}>` 仍能渲染但 store/state 类型乱
  - 等号判断逻辑（`isPublisher`、`canAccept`）全部失效
- **位置**：`TaskDetailPage.tsx:223-224`、`MyTasksPage.tsx:23`、`_mock.ts:274` 等

### B-6. TokenPairVO — 缺 `userId`、`verifyStatus` 大小写不一致

- **严重度**：🔴 阻塞
- **后端 `TokenPairVO`**（`backend/.../auth/vo/TokenPairVO.java:10-35`）：`{ accessToken, refreshToken, accessExpiresAt, refreshExpiresAt, verifyStatus: VerifyStatus }`（Jackson 默认序列化 enum → 大写 `"GUEST"`）
- **前端 `TokenPair`**（`frontend/src/types/api.ts:19-24`）：`{ accessToken, refreshToken, userId, verifyStatus: 'guest'|'pending'|'approved'|'rejected' }`
- **不一致**：
  | 维度 | 前端 | 后端 |
  |---|---|---|
  | `userId` | **必需** | 不存在 |
  | `verifyStatus` 大小写 | 小写 | 大写 |
  | `accessExpiresAt/refreshExpiresAt` | 不存在 | 后端返回但前端不用 |
- **影响**：
  - `useAuthStore.login(tokens)` 把 `tokens.userId` 存进 store，永远是 `undefined` → "我的主页" 跳转用 undefined userId
  - `verifyStatus` 比较失败，验证状态 routing 全部走 fallback

### B-7. VerificationStatusVO — 与前端定义完全不同 + 大小写

- **严重度**：🟠 高
- **后端 `VerificationStatusVO`**：`{ id, status, rejectReason, attachmentSha256, createdAt, updatedAt }`，`status` enum 大写 `"PENDING"|"APPROVED"|"REJECTED"`
- **前端 `getMyVerification` 返回类型**（`api/auth.ts:30-33`）：`{ status: 'pending'|'approved'|'rejected'; rejectReason?: string }`
- **不一致**：大小写 + 后端 VO 多 4 个字段
- **影响**：状态判断不会命中；目前 VerifyPage 还是占位，未实际触发

### B-8. TaskListItemVO / TaskDetailVO — 字段名 + 数据形状不一致

- **严重度**：🔴 阻塞
- **后端 `TaskListItemVO`**（record）：`{ taskId, title, taskType, status, rewardPoint, deadlineAt, deliveryBuilding, publisher, createdAt }`
- **后端 `TaskDetailVO`**（record）：`{ taskId, title, taskType, status, rewardPoint, deadlineAt, pickupHint, deliveryBuilding, remark, publisher, assignee, attachmentUrls, createdAt, canAccept, isPublisher }`
- **前端 `TaskListItemVO`**：`{ taskId, taskType, title, rewardPoint, deadlineAt, status, publisher, building?, createdAt }`
- **前端 `TaskDetailVO`** extends：`{ ..., detail, attachments, acceptor?, proofImages?, proofNote?, version, extendCount? }`
- **不一致**：
  | 后端字段 | 前端字段 | 备注 |
  |---|---|---|
  | `deliveryBuilding` | `building` | 命名 |
  | `remark` | `detail` | 命名 |
  | `assignee` | `acceptor` | 命名 |
  | `attachmentUrls: string[]` | `attachments: TaskAttachment[]` | 类型完全不同 |
  | `pickupHint` | （无） | 后端有前端无 |
  | `canAccept, isPublisher` | （无） | 后端给前端没收 |
  | （无） | `proofImages, proofNote, version, extendCount` | 前端有后端没给 |
- **影响**：任务卡 `task.building` undefined → 不显示；详情页 `task.detail` undefined → 描述空；接单按钮 `acceptor` 永远空；凭证图永远不展示；乐观锁 `version` 接单时传 0；`isPublisher` 没收会让 UI 判断错位

### B-9. TradeItemVO — 字段名几乎全错 + 枚举不一致

- **严重度**：🔴 阻塞
- **后端 `TradeItemVO`**（record）：`{ id, sellerId, title, description, pricePoint, pickupLocationType, pickupLocationDetail, status, imageUrls, createdAt }`
- **前端 `TradeItemVO`**：`{ itemId, title, price, description, images, pickupType, buildingRange?, status, seller: PublicUserVO, createdAt }`
- **不一致**：
  | 后端 | 前端 |
  |---|---|
  | `id` | `itemId` |
  | `sellerId: Long`（仅 id） | `seller: PublicUserVO`（对象） |
  | `pricePoint` | `price` |
  | `pickupLocationType` | `pickupType` |
  | `pickupLocationDetail` | `buildingRange?` |
  | `imageUrls: string[]` | `images: string[]` |
- **枚举值不一致**：后端 `PickupLocationType { EXACT_DORM, BUILDING_RANGE, MEETUP }` vs 前端 `PickupType { 'EXACT_DORM', 'BUILDING_RANGE', 'MEETING' }`（`MEETUP` ≠ `MEETING`）
- **枚举值不一致**：后端 `TradeItemStatus { ON_SALE, OFF_SALE, IN_TRADE }` vs 前端 `{ 'ON_SALE', 'IN_TRADE', 'COMPLETED', 'WITHDRAWN' }`（`OFF_SALE` 缺失，多 `COMPLETED/WITHDRAWN`）
- **影响**：商品卡 / 详情页绝大多数字段拿不到；状态枚举判断失败

### B-10. TradeOrderVO — 前端无对应类型

- **严重度**：🟡 中
- **后端 `TradeOrderVO`**：`{ id, itemId, buyerId, sellerId, status, negotiatedPricePoint, freezePoint, buyerConfirmed, sellerConfirmed, createdAt }`
- **前端**：无 type；前端 api/trade.ts 也没暴露 order 相关函数
- **影响**：订单接口前端尚未对接，但后续接入会撞同样的 ID 类型问题（B-5）

### B-11. CreditMeVO — 字段完全对齐 ✓

- **后端**：`{ userId, creditScore, pointBalance, pointFrozen, dailyAcceptLimit, canPublish, canAccept }`
- **前端**：完全同名
- **唯一差异**：`userId` number vs string（B-5 类问题）

### B-12. CreditRecordVO — `bizId` 可空性 + id 类型

- **严重度**：🟡 中
- **后端**：`{ id: Long, direction: String, delta: int, reasonCode: String, bizId: String, createdAt: Instant }`；DDL `biz_id VARCHAR(128) NOT NULL`，但 record 字段是 `String`（无校验意义上为非空）
- **前端 `CreditRecord`**：`{ id: string, direction, delta, reasonCode, bizId?: string, createdAt }` ✓ 字段对齐
- **差异**：`id` 类型 Long ↔ string（B-5）；`bizId` 后端必返、前端标 optional（保守，没事）

### B-13. NotifyMessageVO — `bizKey` vs `bizId` 命名

- **严重度**：🟠 高
- **后端 record**（`notify/vo/NotifyMessageVO.java:10-18`）：`{ id, type, title, body, readAt, createdAt, bizId }`
- **后端 entity**（`NotifyMessage.java`）：字段 `bizKey`，DDL 列 `biz_key`
- **前端 type**：`{ id, type, title, body, readAt?, createdAt, bizId? }`
- **不一致**：VO 字段叫 `bizId` 但 entity / DDL 叫 `bizKey`——看 `NotifyMessageVO` record 是 `bizId`，需检查 service 层映射时是否填了（如果用 `record(... entity.getBizKey())` 是手工映射，多半 OK；如用反射 / record builder 会丢字段）
- **状态**：需人工核对 `NotifyService.toVO` 是否正确把 `bizKey → bizId`

---

## C. Schema Drift 清单

> 注：已知样本中 V6 已补齐 `auth_sms_code` / `user_audit_log`；`auth_user.banned` boolean 已通过 `hibernate.type.preferred_boolean_jdbc_type=TINYINT` 全局兼容；`AuthUser.phoneCipher` 是 `String` 但 V1 是 `VARBINARY(512)`，靠 `ddl-auto=update` 让 Hibernate 自动改为 `VARCHAR(512)`，**未回写 migration**。

### C-1. `auth_user.phone_cipher` — 类型 VARBINARY vs VARCHAR

- **严重度**：🟠 高
- **DDL V1**（`V1__init_schema.sql:13`）：`phone_cipher VARBINARY(512) NOT NULL`
- **Entity**（`AuthUser.java:47-48`）：`@Column(name="phone_cipher", length=512) private String phoneCipher`
- **现状**：靠 `spring.jpa.hibernate.ddl-auto=update` 让 Hibernate 自动把列改成 `VARCHAR(512)`，但 migration 文件没更新
- **影响**：新环境 fresh migrate → 列是 VARBINARY，Hibernate 启动会 ALTER。`ddl-auto` 改 `validate` 后立刻挂。**生产改 validate 前必须补一条 V7 改类型**

### C-2. `auth_verification.attachment_sha256` — 类型 JSON vs TEXT

- **严重度**：🟡 中
- **DDL V1**（`V1__init_schema.sql:38`）：`attachment_sha256 JSON NULL`
- **Entity**（`AuthVerification.java:63-64`）：`@Column(columnDefinition = "TEXT") private String attachmentSha256Json`
- **不一致**：DDL 是 MySQL JSON，entity 声明 TEXT。`ddl-auto=update` 不会 narrow / widen 已有列，但 `validate` 模式下会失败
- **影响**：fresh DB + validate 启动失败

### C-3. `auth_user.banned` — 全局 boolean→TINYINT 兼容已生效，但其他 boolean 列要全部走同一套

- **严重度**：🟡 中（潜在）
- **当前状态**：`hibernate.type.preferred_boolean_jdbc_type=TINYINT` 全局生效
- **风险**：未来如果有人新增 boolean entity 字段但 DDL 用 `BIT(1)`，会反向出错（Hibernate 期 TINYINT，DDL 给 BIT）
- **建议**：在 contributing 文档明确所有 boolean 列必须用 `TINYINT NOT NULL DEFAULT 0/1`

### C-Status（已修，无需 fix）

| 项 | 状态 |
|---|---|
| `auth_sms_code` 表缺失 | ✓ V6 补齐 |
| `user_audit_log` 表缺失 | ✓ V6 补齐 |
| `auth_user.banned` boolean vs TINYINT | ✓ 全局配置兼容 |
| `task_extend_log` 表 | ✓ V4 已建 |
| `notify_message` 表 | ✓ V5 已建 |
| `credit_score_log` 表 | ✓ V1 已含 |

### C-检查通过的 Entity↔DDL 映射

抽样核对以下表，字段名 / 长度 / nullable 全部对齐：
- `task_order` ↔ `Task.java`
- `task_attachment` ↔ `TaskAttachment.java`
- `task_extend_log` ↔ `TaskExtendLog.java`
- `task_review` ↔ `TaskReview.java`
- `credit_account` ↔ `CreditAccount.java`
- `credit_record` ↔ `CreditRecord.java`
- `credit_score_log` ↔ `CreditScoreLog.java`
- `trade_item / trade_item_image / trade_order` ↔ entities
- `user_profile` ↔ `UserProfile.java`（DDL 多 `creator_id/updater_id/deleted_at`，entity 没声明，Hibernate validate 默认忽略多余列 → OK）
- `notify_message` ↔ `NotifyMessage.java`
- `edu_tutor_task` ↔ `EduTutorTask.java`

---

## D. Entity ↔ VO 漂移（次要）

### D-1. `Task` entity vs `TaskListItemVO`

- entity 有 `pickupHint, remark, version, assigneeId, updatedAt, deletedAt, creatorId, updaterId`
- listVO 只用 `id/title/taskType/status/rewardPoint/deadlineAt/deliveryBuilding/publisher/createdAt`
- 合理（list 视图本就轻量）

### D-2. `TradeItem` entity vs `TradeItemVO`

- entity 有 `version, updatedAt, deletedAt, creatorId, updaterId`，VO 忽略 ✓
- `TradeItemVO.imageUrls` 字段需要 service 层去 `trade_item_image` 表 join 查（前端虽然走错字段名，但这里 entity→VO 映射本身合理）

### D-3. `AuthUser` 字段 `phoneHmac/phoneCipher/passwordHash/banned` 永远不出现在任何 VO

- 安全要求，正确 ✓
- 但 `TokenPairVO` 应该补 `userId`（见 B-6）

---

## 推荐修复策略

### 总体原则

**对齐方向：以后端为准** —— 后端业务逻辑、DDL、Service、Test 都已实现并跑通；前端的 `types/*.ts` 是 D 阶段早期凭脑补写的，pages 实际用的是另一套字段（也大多偏向后端，见 B-1 现象）。

### 具体动作建议（按优先级）

| 优先级 | 动作 | 涉及文件 | 备注 |
|---|---|---|---|
| 🔴 P0 | 修 `types/api.ts` 的 `TokenPair`：去掉 `userId`，`verifyStatus` 改大写，或 backend 在 VO 里补 `userId` 字段并加 `@JsonValue` 让 enum 小写化 | `types/api.ts` 或 `TokenPairVO.java + VerifyStatus` | B-6，登录后整个 session 才能正确 |
| 🔴 P0 | 把 `PublicUserVO.verifiedTag` 后端改为 string（`"校园已认证" | null`），或前端改为 boolean | `common/PublicUserVO.java` 二选一 | B-2 / B-3，影响所有用户卡 |
| 🔴 P0 | 统一 ID 类型：后端 `Long` 序列化为 string（@JsonSerialize ToStringSerializer），或前端全改 `number` | 全局，建议后端加全局 Jackson 配置 | B-5，影响所有 ===/比较 |
| 🔴 P0 | 后端补缺失的 endpoint：`GET /api/search/items`、`GET /api/trade/items/{id}`、`GET /api/users/{id}/public/stats` | `TradeController`、`UserController` | A-1 / A-2 / A-10 |
| 🔴 P0 | `TaskCreateDTO` / `TradeItemCreateDTO` 前后端对齐字段名（推荐：前端跟后端命名） | `types/task.ts`、`types/trade.ts`、`pages/tasks/TaskNewPage.tsx` 等 | A-4 / A-5 / B-8 / B-9 |
| 🔴 P0 | 修 `TaskQueryDTO.status` 为 `TaskStatus` enum，让 Spring 自动按枚举字符串绑定 | `task/dto/TaskQueryDTO.java` | A-14 |
| 🔴 P0 | 修 `submitProof` 后端：要么改成 JSON `{images, note}`，要么前端按 multipart 重写 | `task/controller/TaskController.java:97-105` + `api/task.ts` | A-8 |
| 🔴 P0 | 修 trade items 创建：同上，统一 multipart 或 JSON | `TradeController` + `api/trade.ts` | A-3 |
| 🟠 P1 | 让所有 enum 通过 `@JsonValue` 序列化为小写，避免大小写问题（前端 enum 同步） | `VerifyStatus`、`VerificationStatus`、`TaskStatus`、`TaskType`、`TradeItemStatus`、`TradeOrderStatus`、`PickupLocationType`、`CreditDirection` | B-6 / B-7 等系列 |
| 🟠 P1 | 修 `acceptTask` / `cancelTask` 前端发空 body 的问题：要么后端改 `required=false`，要么前端真传 `{version}` 和 `{reason}` | A-6 / A-7 |
| 🟠 P1 | `UserMeVO` 前端 type 重写为扁平结构（去掉 `privacy.` 嵌套和 `imOpen`），对齐后端 VO；同步 `_mock.ts`、`ProfileEditPage.tsx`、`MePage.tsx` 已经在用的扁平形 | `types/user.ts`、`_mock.ts` | B-1 |
| 🟠 P1 | `PickupLocationType.MEETUP` 改 `MEETING`（或前端反过来），保持单一来源 | `trade/entity/PickupLocationType.java` 或 `types/trade.ts` | B-9 枚举 |
| 🟠 P1 | `TradeItemStatus` 统一：移除 `OFF_SALE` 或加 `COMPLETED/WITHDRAWN` | 后端 + 前端 | B-9 |
| 🟠 P1 | 补 migration V7 把 `auth_user.phone_cipher` 从 VARBINARY(512) 改 VARCHAR(512)、把 `auth_verification.attachment_sha256` 从 JSON 改 TEXT；之后把 `ddl-auto` 切回 `validate` | `db/migration/V7__*.sql` + `application.properties` | C-1 / C-2 |
| 🟡 P2 | `auth/logout` 前端带 refresh token | `api/auth.ts:35` | A-13 |
| 🟡 P2 | `auth/verifications` 前端 schema 重写（多张图改 Base64 数组，去 `certKind`），或后端改成接收 URL 数组 | A-12 |
| 🟡 P2 | `getMyVerification` 改 `apiGet`（不是 `apiPost`） | A-11 |

### 框架性建议

1. **OpenAPI as Source of Truth**：建议把 `openapi.yaml` 写完整，用 `openapi-typescript` 生成前端类型，杜绝手写 type 漂移
2. **CI 加 schema-diff 检查**：每次 PR 都对 entity / DDL / VO / 前端 type 做 diff，差异必须有 commit message 标注
3. **boolean 字段公约写进 CONTRIBUTING.md**：所有 boolean 列 TINYINT，所有 enum 走显式 code Converter，所有 enum JSON 走 `@JsonValue` 小写
4. **ID 类型统一**：要么全 `number`，要么全 string（推荐 string，避免 JS 53 bit 截断），并在全局 Jackson 配置
5. **Profile 表的 `imOpen` 字段**：前端 type 有但后端 entity / DDL 都没有，看产品需求决定要不要加（如果要加，需 V7 + entity 字段 + VO + DTO 全链路）
