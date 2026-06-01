# P4 Bug 日志整合摘要（C 维护）

> 整合周期：2026-05-19 ~ 2026-06-01  
> 维护人：李承垚（C）  
> 来源：`docs/P4/bug/*.md`（含 [`李承垚_2026-05-23.md`](./李承垚_2026-05-23.md)、[`何翌闻_2026-05-28.md`](./何翌闻_2026-05-28.md)、[`陈泽昊_2026-05-30.md`](./陈泽昊_2026-05-30.md)、[`schema_audit_2026-05-28.md`](./schema_audit_2026-05-28.md)）

---

## 汇总表（≥5 条）

| # | 模块 | 标题 | 严重等级 | 根因（一句话） | 验证 |
|---|------|------|:--------:|----------------|------|
| 1 | edu | 违禁词计数被回滚 | 🟠 | 副作用与失败异常同事务 | `TutorTaskServiceTest` 4 用例全绿 |
| 2 | trade | JPQL TIMESTAMP vs Instant | 🔴 | 类型不匹配导致上下文启动失败 | `mvn test` 105/105 |
| 3 | trade | BizException 枚举误用 | 🔴 | 构造器签名理解错误 | `mvn compile` 通过 |
| 4 | trade | long/Long equals 编译错 | 🔴 | 原始类型调用对象方法 | `mvn compile` 通过 |
| 5 | trade | 并发双买未测 409 | 🟡 | 测试断言不完整 | `TradeOrderServiceTest#createOrder_concurrentOnlyOneSucceeds` |
| 6 | config | MySQL 9.x JDBC 握手失败 | 🔴 | `allowPublicKeyRetrieval` 未配置 | 本地 `./start.sh local` 后端可启动 |
| 7 | db | Flyway V3 PostgreSQL 语法 | 🔴 | `ADD COLUMN IF NOT EXISTS` MySQL 不支持 | Flyway migrate 通过 |
| 8 | credit/notify | 跨模块 `@Component` bean 名冲突 | 🟠 | 默认 bean 名全局扁平 | `@Component("creditTaskEventListener")` + 全量 test 绿 |
| 9 | credit | listener 对称订阅致双 unfreeze 风险 | 🟠 | 未 grep publisher 已内联的副作用 | 仅订阅 `TaskCompletedEvent` |
| 10 | credit/task | 完成路径 accepter 押金不退 | 🟠 | `TaskCompletedEvent` 缺 `depositPoint` | 集测断言固定已知行为 |
| 11 | trade | 商品列表/详情接口缺失 | 🔴 | schema_audit 前后端未对齐 | `GET /api/search/items` + `GET /api/trade/items/{id}` 已补 |
| 12 | trade | 发商品 multipart vs JSON 不匹配 | 🔴 | 前后端 Content-Type 不一致 | 改 JSON + `/api/uploads` 预上传 |
| 13 | common | EXIF 清洗未接入上传链路 | 🟡 | schema_audit 后职责迁移遗漏 | `ImageStorageTest#put_cleansExifFromJpegBeforeWrite` |

---

## 按模块分布

- **trade**：5 条（TRADE 实现期 + schema_audit 修复 + EXIF 迁移）
- **edu**：1 条（EDU-05 冷静期）
- **credit**：3 条（CRD-04 listener 段，D 记录）
- **config/db**：2 条（MySQL JDBC + Flyway 语法，A 记录）
- **测试/QA**：1 条（并发场景）
- **跨模块**：1 条（bean 命名冲突）

---

## 高频根因模式

1. **AI 直出代码未对齐项目已有工具类签名**（`BizException` / `Instant` 字段）
2. **业务副作用与失败路径的事务边界未分开**（违禁词计数）
3. **并发场景需要「先写测试再写实现」**（乐观锁防双买）
4. **前后端/API 契约未冻结即开发**（schema_audit 33 处不一致）
5. **跨模块同名 Spring bean 默认命名冲突**（listener 类）
6. **架构变更后职责迁移遗漏**（EXIF 从 trade Service → ImageStorage）

---

## 后续建议

- PR 模板增加 checkbox：`失败路径副作用是否独立事务？`
- trade/edu 模块新增接口时，优先补 409/422/423 异常码单测
- 所有 `XxxEventListener` 必须用模块前缀显式 `@Component` 命名（见 D Bug #1）
- 写 listener 前先 grep 事件发布者是否已内联同类副作用（见 D Bug #2）

---

**最后更新：** 2026-06-01  
**维护人：** 李承垚（C）
