# EXP-02 — AI 调试对决（组员 C / trade + edu）

> 实验人：李承垚（C）  
> 日期：2026-05-23  
> 模块：trade（TRADE-03）+ edu（EDU-05）  
> 详细 Bug 记录：[`docs/P4/bug/李承垚_2026-05-23.md`](./bug/李承垚_2026-05-23.md)

---

## Bug A：Spring 上下文启动失败（trade 乐观锁 UPDATE）

| 步骤 | 人工调试 | AI 辅助调试 |
|------|----------|-------------|
| 1. 复现 | `mvn test` 全红，日志 `Failed to load ApplicationContext` | 粘贴堆栈给 AI，约 10s 指出 `Timestamp` vs `Instant` |
| 2. 定位 | 顺依赖链找到 `TradeItemRepository#updateStatusIfMatch` | AI 直接圈出 JPQL 中 `CURRENT_TIMESTAMP` |
| 3. 假设 | 实体 `updatedAt` 是 `Instant`，JPQL 函数返回 `Timestamp` | 与人工一致 |
| 4. 修复 | 删除 UPDATE 中的 `updatedAt` 赋值 | AI 建议同样方案 |
| 5. 验证 | `mvn test` 上下文恢复 | 同左 |
| 6. 耗时 | ~15 min（含读 Hibernate 文档） | ~3 min |

**Prompt 摘要：** 「Hibernate SemanticException Cannot assign Timestamp to Instant，JPQL UPDATE 怎么写？」

**结论：** AI 对「类型不匹配」类编译/启动错误定位快；但 AI 初版生成的 UPDATE 语句本身就有 bug。

---

## Bug B：违禁词冷静期计数不生效（edu）

| 步骤 | 人工调试 | AI 辅助调试 |
|------|----------|-------------|
| 1. 复现 | `TutorTaskServiceTest#createTutorTask_thirdHitStartsCooldown` 报 `NoSuchElementException` | 提供测试代码 + 服务代码 |
| 2. 定位 | 断点发现 `hitRepo` 查无记录 | AI 指出 `@Transactional` 回滚吞掉 write |
| 3. 假设 | 抛 `BizException` 导致整事务回滚 | 同左 |
| 4. 修复 | 新建 `ForbiddenWordHitService`，`REQUIRES_NEW` | AI 给出 propagation 方案 + 代码骨架 |
| 5. 验证 | 单测 4/4 绿 | 同左 |
| 6. 耗时 | ~20 min | ~8 min |

**Prompt 摘要：** 「Spring 抛 BizException 后，同事务里的 insert 会回滚吗？如何只提交副作用？」

---

## 四问分析

1. **AI 更容易修哪类 bug？** 类型/签名/框架栈追踪类（编译错误、上下文启动失败）。
2. **AI 更容易漏哪类 bug？** 事务边界与业务语义（「拦截失败也要记数」）。
3. **人工不可替代环节？** 写失败路径的单测断言（冷静期 DB 值、并发 409）。
4. **Prompt 改进？** 在 `03_AI使用前统一Prompt.md` §B 增加：「失败时仍需持久化的副作用请用 `REQUIRES_NEW`」。

---

## 对比结论

| 维度 | 人工 | AI |
|------|------|-----|
| 定位速度 | 中 | 快（有堆栈时） |
| 修复正确性 | 高（理解事务语义） | 中（需显式提示事务需求） |
| 回归测试 | 必须人工写 | 可生成骨架，断言需人工补 |

**信任度（1–5）：** 定位 4 / 直出可合并 3 / 事务类场景 2
