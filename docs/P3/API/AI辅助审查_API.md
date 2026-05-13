# AI 辅助审查：API 规范（对应《P3-详细设计》任务 2.2）

> 审查对象：`docs/P3/API/openapi.yaml`  
> 对照规约：`docs/P3/01_统一规约.md` §2  

## 1. 接口命名与资源一致性

| 问题 | 说明 | 处理 |
| :--- | :--- | :--- |
| 资源单复数 | AI 易混用 `/task` 与 `/tasks` | 已统一为复数 `/api/tasks`，动作挂子路径 `/accept`、`/reviews` |
| 搜索归属 | 列表接口放在 `tasks` 还是 `search` | 按功能映射 **F-SEARCH-01** 使用 `GET /api/search/tasks`，避免 `TaskController` 与 `SearchService` 职责打架 |
| 登录路径分裂 | 同时存在 `/token` 与 `/token/password` | 刻意拆分：验证码主路径（SRS）与密码日常登录（规约 §2.5.1）并存，文档中注明调用场景 |

## 2. 认证与安全

| 问题 | 说明 | 处理 |
| :--- | :--- | :--- |
| 未标注鉴权 | 发布、接单、评价未要求 Bearer | 在 OpenAPI 层对敏感路径声明 `security: bearerAuth`；短信发送为匿名但依赖 IP 限流（429） |
| Token 泄露面 | refresh 与 access 同结构返回 | 与 ADR 一致：HTTPS 传输 + 前端分存；后端 refresh 轮换策略在实现阶段补序列图 |
| 密码强度 | AI 常漏业务规则 | `RegisterRequest.password` 已写 8–32 且字母+数字；服务端须二次校验 |

## 3. 错误处理与错误码

| 问题 | 说明 | 处理 |
| :--- | :--- | :--- |
| HTTP 与业务码混用 | 仅依赖 200 + body code | 规约要求 4xx/5xx 与 `code` 并存；yaml 中为典型错误补充 `responses` 与示例 `ApiResponseError` |
| 错误码分段 | AI 易连续编号无模块语义 | 示例使用 `1xxx` 通用、`2xxx` auth、`4xxx` task、`10001` credit，与 §2.4 对齐 |
| 乐观锁失败 | 接单无 version | `accept` 请求体可选 `version`；冲突返回 `409 TASK_VERSION_CONFLICT` |

## 4. 参数校验

| 问题 | 说明 | 处理 |
| :--- | :--- | :--- |
| 手机号 pattern | 仅 `string` | 增加 `^1[3-9]\d{9}$` |
| 分页上下界 | 未限制 size | `maximum: 100`、`minimum: 1` 与规约一致 |
| 枚举用 string | 与 DB TINYINT 不一致 | 列表筛选 `status` 保留 int，任务类型用 string 枚举（Java Enum 名）以便可读；**实现期** DTO 与 DB 由 Converter 统一 |

## 5. 响应与隐私

| 问题 | 说明 | 处理 |
| :--- | :--- | :--- |
| 返回 Entity | AI 易把 `AuthUser` 直接当响应 | 显式定义 `PublicUserVO`、`TaskDetailVO`、`TradeOrderDetailVO`，禁止 password/phone_cipher |
| 包装不一致 | 部分接口裸返回 | 全部统一 `code/message/data/traceId` |

## 6. 人工修订摘要（相对「纯 AI 初稿」）

1. 拆分 `GET /api/search/tasks` 与 `GET /api/tasks/{id}`，落实 **INV-01** 搜索模块边界。  
2. 为锁账号、信用不足、乐观锁冲突补充标准错误响应与业务码示例。  
3. 注册与验证码登录拆为独立 operation，避免单接口 if-else 语义不清。  

以上修订已反映于 `openapi.yaml`。
