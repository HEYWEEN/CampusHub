# B 评 C —— trade 模块（二手交易+砍价）+ edu 模块（辅导违禁词）

> **评审人**：陈旭枫（B）
> **被评审人**：李承垚（C）
> **触发规则**：`02_后端代码分工.md §四 互邻评审 → B 评 C`
> **评审范围**：`trade/` 全模块 36 文件 + `edu/` 全模块 15 文件，commits 基于当前 main 分支最新状态
> **评审视角**：B 是 task 模块 owner，与 C 的 trade 模块共享 `CreditApi`（冻结/解冻）和事件驱动结算模式。B 自己做了 task 的状态机（State 模式 + `TaskStatus.TRANSITIONS` + `canTransitionTo()`），关注 C 的并发安全与跨模块契约一致性

---

## 一、总评

**通过 ✅，建议 3 项小调整**。C 的代码在并发控制（乐观锁 CAS update）、砍价状态机（`AwaitingParty` 回合制）、违禁词冷静期（`REQUIRES_NEW` 独立事务）三个关键点上都很用心。bizKey 命名、CreditApi 调用、事件发布完全符合 §三.1 契约。27 个单测全绿。代码量虽少但密度高，无一眼可见的 Bug。

| 维度 | 评价 |
|------|------|
| CreditApi 契约（bizKey 幂等） | ✅ `trade:<id>:freeze` / `:cancel_unfreeze` 后缀分离，与 §三.1 约定一致 |
| 并发安全 | ✅ 下单用 `updateStatusIfMatch`（version CAS），`@Version` 双重保护 |
| 状态机设计 | ✅ 砍价 Ping-Pong（PENDING → ACCEPTED/REJECTED/CANCELED）+ 订单双向确认（BUYER_CONFIRMED ↔ SELLER_CONFIRMED） |
| 事务边界 | ✅ `ForbiddenWordHitService` 用 `REQUIRES_NEW` 隔离命中计数，发布失败不丢计数 |
| 模块边界（INV-01） | ✅ Offer 直接调 `TradeOrderService.createOrder`（同模块），跨模块走 `CreditApi` / `NotifyApi` / `UserApi`，不越界 |
| 单测覆盖 | ✅ trade 23 例 + edu 4 例，关键路径全覆 |
| 异常处理 | ✅ 全部走 `BizException`，无裸 `RuntimeException` |
| 状态转换校验 | 🟡 无集中 `TRANSITIONS` map（见 §三.1） |
| 幂等性 | 🟡 cancelOrder 幂等好，confirmOrder 重复确认返回已有状态（§三.2 小隐患） |

---

## 二、做得好的地方（值得 B 借鉴）

### 2.1 下单双重乐观锁 —— item status + @Version 双保险

```java
// TradeOrderServiceImpl.createOrder line 54-58
int updated = itemRepo.updateStatusIfMatch(
    item.getId(), TradeItemStatus.ON_SALE, TradeItemStatus.IN_TRADE, item.getVersion());
if (updated == 0) {
    throw new BizException(ResponseCode.CONFLICT, "商品已被其他买家下单");
}
```

与 B 在 task 模块 `accept()` 里做 version 比对同理，但 C 更进一步——直接用 `UPDATE ... WHERE status=? AND version=?` 的 CAS 原子操作，在数据库层避免了 SELECT + UPDATE 之间的 race window。B 的 task 模块当前是先 `findTask()` 再 `setStatus()` 再 `save()`，依赖 JPA `@Version` 在 save 时抛 `OptimisticLockException`。**C 的原子 CAS 方式更干净**——尤其当并发买家同时下单同一商品时，`updated == 0` 直接返回友好提示而非异常栈。

### 2.2 砍价 Ping-Pong 状态机干净利落

```java
// TradeOfferServiceImpl.requireActingParty line 170-183
AwaitingParty expected = userId == o.getBuyerId() ? AwaitingParty.BUYER : AwaitingParty.SELLER;
if (o.getAwaitingParty() != expected) {
    throw new BizException(TradeErrorCode.OFFER_NOT_YOUR_TURN, "当前不是你的回合", 409);
}
```

`AwaitingParty` 枚举（BUYER ↔ SELLER）+ `flip()` 方法实现回合轮流。买/卖双方在 PENDING 状态下只能当前回合方操作（counter/accept/reject），非回合方直接 409。状态流转清晰：PENDING → ACCEPTED（成单）/ REJECTED / CANCELED（仅买家可撤）。

**accept 复用 createOrder** 的设计尤其好：

```java
// TradeOfferServiceImpl.accept line 107-110
TradeOrderCreateDTO od = new TradeOrderCreateDTO();
od.setItemId(offer.getItemId());
od.setNegotiatedPricePoint(offer.getPricePoint());
TradeOrderVO order = orderService.createOrder(offer.getBuyerId(), od);
```

砍价同意即成单，买家恒为 `offer.buyerId`，复用 `orderService.createOrder`（同一事务）。**商品乐观锁/冻结失败 → 整体回滚，offer 不残留 ACCEPTED 状态**。这个事务边界的取舍（join 而非分离）与 B 在 task 的 `confirmComplete` 做积分解冻属于同一事务的思路一致。

### 2.3 违禁词计数 `REQUIRES_NEW` 独立事务

```java
// ForbiddenWordHitService.recordHit line 25
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void recordHit(long userId) {
    ForbiddenWordHit hit = hitRepo.findById(userId).orElseGet(...);
    hit.incrementHit();
    if (hit.getHitCount() >= 3) {
        hit.startCooldown(Instant.now().plus(Duration.ofHours(24)));
    }
    hitRepo.save(hit);
}
```

**关键设计**：`REQUIRES_NEW` 把命中计数从事务中剥离。用户发布辅导需求 → 违禁词命中 → 主事务回滚（不创建任务），但**命中计数已提交**。若不这样做，恶意用户每次发违禁词后再回滚，永远不会触发 3 次冷静期。C 在 javadoc 里写明了这个取舍，与 A 在 `notify/listener/TaskEventListener` 用 `AFTER_COMMIT` 做"通知不应回滚业务"属于同一类事务边界判断。

### 2.4 NotifyApi bizKey 带 version 防多轮还价去重

```java
// TradeOfferServiceImpl.counter line 97
notify(recipient, "TRADE_OFFER_COUNTERED", ...,
    "TRADE_OFFER_COUNTERED:" + offer.getId() + ":v" + offer.getVersion() + ":" + recipient);
```

同一次砍价的第 1 轮和第 2 轮还价如果 bizKey 相同，`NotifyApi` 的 `existsByBizKey` 会吞掉第 2 轮通知。C 在 bizKey 里加了 `v<version>` 区分轮次，精巧且不增加 notify 模块的负担。

### 2.5 订单双向确认 + 中间态幂等

```java
// TradeOrderServiceImpl.confirmOrder line 87-113
if (userId == order.getBuyerId()) {
    if (order.isBuyerConfirmed()) { return toVo(order); }  // 重复点击幂等
    order.setBuyerConfirmed(true);
} else if (userId == order.getSellerId()) {
    if (order.isSellerConfirmed()) { return toVo(order); }  // 重复点击幂等
    order.setSellerConfirmed(true);
}
if (order.isBuyerConfirmed() && order.isSellerConfirmed()) {
    order.setStatus(TradeOrderStatus.COMPLETED);
    eventPublisher.publishEvent(new TradeOrderCompletedEvent(...));
}
```

双方各自点击确认，互不阻塞，各自幂等（已确认方再次点击直接返回）。买卖双方可能相隔数小时操作。状态过渡 `IN_TRADE → BUYER_CONFIRMED → COMPLETED` 让 UI 可以提示"对方已确认，等你确认"。

---

## 三、建议调整（不阻塞合并）

### 3.1 🟡 `TradeOrderStatus` / `TradeItemStatus` 缺状态转换白名单

**现状**：`TradeOrderStatus` 和 `TradeItemStatus` 都是纯枚举 + code，没有 `canTransitionTo()` 方法或 `TRANSITIONS` map。所有状态合法性校验散落在 `confirmOrder()` / `cancelOrder()` / `updateStatus()` 方法里。

**对比 B 的做法**：`TaskStatus` 有 `TRANSITIONS` map + `setStatus()` 内嵌 `canTransitionTo()` 校验 → 加新状态只需改 1 处。

**建议**：不要求现在改，但若后续 trade 加"退款中"、"仲裁中"等中间态，建议提前把转换规则集中到枚举层。三状态（ON_SALE/OFF_SALE/IN_TRADE）或五状态（IN_TRADE/BUYER_CONFIRMED/SELLER_CONFIRMED/COMPLETED/CANCELED）的管理成本目前还低，暂不需要。

**优先级**：🟡 低，不阻塞。

### 3.2 🟡 `confirmOrder` 幂等隐患 —— 已 COMPLETED 后再次点击返回 422

**现状**：

```java
if (order.getStatus() == TradeOrderStatus.COMPLETED) {
    throw new BizException(TradeErrorCode.ORDER_ALREADY_CONFIRMED, "订单已完成", 422);
}
```

B 的 task 模块 `confirmComplete` 在 WAIT_CONFIRM → COMPLETED 后没有幂等处理——但这个是**不同场景**。Trade 的 confirm 是两步过程（双方各点一次），用户可能在对方已经确认、订单变成 COMPLETED 后再次点击 → 看到 422 错误，体验差。

**建议**：COMPLETED 时直接 `return toVo(order)`（幂等返回），与 CANCELED 行为一致。前端无需区分"已完成"和"我多点了一次"。

**优先级**：🟡 低，前端可加防抖绕过去。

### 3.3 🟡 `TradeItemServiceImpl.getItem` 冗余 deletedAt 检查

```java
// line 126-129
TradeItem item = itemRepo.findById(itemId)...;
if (item.getDeletedAt() != null) {
    throw new NotFoundException("商品已下架/删除");
}
```

可以改成 `itemRepo.findByIdAndDeletedAtIsNull(itemId)` 一条查询。当前先查出再判断的写法在数据量大时无性能差异（单条查询），但语义上应该对齐 B 的 `findTask()` 模式。

**优先级**：🟢 很低，refactor 时顺手改。

---

## 四、对 B 模块的联动影响核对

| B 侧依赖点 | C 侧实现 | 一致性 |
|----------|----------|--------|
| `CreditApi.freeze(buyer, price, "trade:<id>:freeze")` | `createOrder` line 63 | ✅ 后缀与 task 的 `task:<id>:freeze` 分开，不冲突 |
| `CreditApi.unfreeze(buyer, price, "trade:<id>:cancel_unfreeze")` | `cancelOrder` line 142 | ✅ 用 `cancel_unfreeze` 后缀与完成态区分 |
| `TradeOrderCompletedEvent` 字段 | record `(orderId, buyerId, sellerId, pointAmount)` | ✅ 四字段够 D 的 credit listener 做 settle |
| TaskApi（task 模块暴露） | C 不依赖 TaskApi | ✅ trade 与 task 无交叉 |
| `TradeItem.status` 枚举 | ON_SALE/OFF_SALE/IN_TRADE (0/1/2) | ✅ 不与 task 状态冲突 |
| NotifyApi 调用 | offer 侧 bizKey 带 `v<version>` | ✅ 不影响 B 的 task listener 通知去重 |

**结论**：B 与 C 仅在 `CreditApi` 层面有交集，双方 bizKey 命名完全独立（`task:` vs `trade:` 前缀），无冲突风险。C 的 `TradeOrderCompletedEvent` 与 B 的 `TaskCompletedEvent` 是平行事件，credit listener 各自订阅即可。

---

## 五、评审结论

| 项 | 结论 |
|----|------|
| 是否阻塞合入 | **否** |
| 是否建议 C 改代码 | 仅 §三.2（COMPLETED 幂等返回），其余可延后 |
| B 是否需要跟进 C 的变更 | 否，trade 与 task 在 CreditApi 层已解耦 |
| 值得 B 借鉴的点 | 1) CAS 原子更新代替 SELECT+save 的乐观锁；2) `REQUIRES_NEW` 用于计数类副作用；3) bizKey 带 version 做多轮去重 |
| 是否暴露契约缺陷 | 否，§三.1 的 bizKey 契约在 trade 侧执行到位 |

---

**评审完成时间**：2026-06-06
**下一步**：C 在合入前确认 §三.2 是否改（可选），B 将 §二.1 的 CAS 原子更新模式记入 DOC-03 供 task 模块后续优化参考。
