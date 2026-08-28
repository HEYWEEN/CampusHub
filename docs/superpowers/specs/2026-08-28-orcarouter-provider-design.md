# OrcaRouter 可选 Provider 接入设计

## 目标

在不改变 CampusHub 前端和 AI 校园助手业务流程的前提下，将现有 DeepSeek 专用调用层抽象为 OpenAI-compatible 调用层，并把 OrcaRouter 加入为可选 Provider。DeepSeek 保持默认，已有部署不需要迁移即可继续运行。

## 配置与兼容性

- 新增 `campushub.ai.provider`，支持 `deepseek` 和 `orcarouter`，默认 `deepseek`。
- 保留现有 `campushub.deepseek.*` 配置和 `DEEPSEEK_API_KEY` 环境变量。
- 新增 `campushub.orcarouter.*` 配置和 `ORCAROUTER_API_KEY` 环境变量。
- OrcaRouter 默认 Base URL 为 `https://api.orcarouter.ai/v1`，模型默认使用官方自动路由模型 `orcarouter/auto`，同时允许部署者覆盖模型 ID。
- 当前 Provider 未配置 Key、返回空响应或调用失败时，继续触发现有规则降级，不影响其他业务模块。

## 代码结构

- 用通用 `OpenAiCompatibleClient` 取代 `DeepSeekClient`，保留现有 Chat Completions DTO 和 Function Calling 数据结构。
- `AiProviderProperties` 负责选择当前 Provider，并解析其名称、API Key、Base URL、模型与超时。
- `AgentConfig` 使用解析后的 Provider 配置创建 `RestClient`。
- `AgentService` 与 `ToolSpecs` 只依赖通用客户端，不感知具体供应商。

## 请求流程

AI 校园助手仍通过 `/chat/completions` 发送 OpenAI 格式请求。选择 OrcaRouter 时使用 Bearer `ORCAROUTER_API_KEY`，请求中的模型默认为 `orcarouter/auto`；工具调用结果继续按 OpenAI `tool_calls` 格式回填。前端 API、会话历史和工具执行逻辑均不改变。

## 测试与文档

- 单元测试覆盖默认 DeepSeek、选择 OrcaRouter、无效 Provider、缺少 Key和 Provider 错误信息。
- 客户端测试覆盖鉴权头、请求路径、模型字段、工具定义和响应解析。
- 运行后端完整 Maven 测试，以及前端构建，防止跨模块回归。
- README、本地配置模板和生产环境变量模板说明两种 Provider 的启用方式。

## 不在范围内

- 不在前端增加运行时 Provider 选择器。
- 不提交真实 API Key，也不依赖真实付费 API 完成自动化测试。
- 不修改 CampusHub 的收入归因或 OrcaRouter OSS 计划后台；该部分由项目作者在 OrcaRouter 官方渠道注册完成。
