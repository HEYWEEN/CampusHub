# P4 Bug 日志整合摘要（C 维护）

> 整合周期：2026-05-19 ~ 2026-05-23  
> 维护人：李承垚（C）  
> 来源：`docs/P4/bug/*.md`（本周以 [`李承垚_2026-05-23.md`](./李承垚_2026-05-23.md) 为主）

---

## 汇总表（≥5 条）

| # | 模块 | 标题 | 严重等级 | 根因（一句话） | 验证 |
|---|------|------|:--------:|----------------|------|
| 1 | edu | 违禁词计数被回滚 | 🟠 | 副作用与失败异常同事务 | `TutorTaskServiceTest` 4 用例全绿 |
| 2 | trade | JPQL TIMESTAMP vs Instant | 🔴 | 类型不匹配导致上下文启动失败 | `mvn test` 105/105 |
| 3 | trade | BizException 枚举误用 | 🔴 | 构造器签名理解错误 | `mvn compile` 通过 |
| 4 | trade | long/Long equals 编译错 | 🔴 | 原始类型调用对象方法 | `mvn compile` 通过 |
| 5 | trade | 并发双买未测 409 | 🟡 | 测试断言不完整 | `TradeOrderServiceTest#createOrder_concurrentOnlyOneSucceeds` |

---

## 按模块分布

- **trade**：3 条（TRADE-02~04 实现期）
- **edu**：1 条（EDU-05 冷静期）
- **测试/QA**：1 条（并发场景）

---

## 高频根因模式

1. **AI 直出代码未对齐项目已有工具类签名**（`BizException` / `Instant` 字段）
2. **业务副作用与失败路径的事务边界未分开**（违禁词计数）
3. **并发场景需要「先写测试再写实现」**（乐观锁防双买）

---

## 后续建议

- PR 模板增加 checkbox：`失败路径副作用是否独立事务？`
- trade/edu 模块新增接口时，优先补 409/422/423 异常码单测
- Controller 层（multipart 校验）补 MockMvc 测试，弥补 JaCoCo 中 `TradeController` 低覆盖

---

**最后更新：** 2026-05-23  
**维护人：** 李承垚（C）
