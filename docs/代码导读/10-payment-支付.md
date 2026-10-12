# 10 · payment — 支付

> 阅读级别：**略读**（约 900 字）。本模块是订单状态机的「驱动器」之一——支付成功把订单从「待支付」推进到「待接单」。核心三件事：**金额取订单侧、接口幂等、支付单与订单同事务**。

---

## 1. 是什么

`payment` 模块管理支付单（`payment` 表）。一期是**模拟支付**：调用 `pay()` 直接置为已支付，没有真实第三方。

> 真实支付怎么接？把 `pay()` 里「标记已支付 + 通知订单模块」这段逻辑原样搬进**异步回调接口**即可，其余校验（归属、订单状态、金额一致性）完全复用。

## 2. 在全局的位置

```
order ──< payment
  │         │
  │         └─ 支付成功 → orderApi.markPaid(orderId) → 状态 10→20
  │
  └─ 订单超时取消 → PaymentCloseTask 作废对应支付单
```

- **上游**：`order`（支付单的金额、归属都取自订单）
- **下游**：回调 `order` 的 `markPaid()` 推进状态

## 3. 精读 2 个文件

| 文件 | 作用 |
|------|------|
| [PaymentServiceImpl.java](file:///c:/Users/huhaohua/Desktop/myproject/外卖系统/backend/src/main/java/com/takeout/payment/service/impl/PaymentServiceImpl.java) | 支付主流程：归属校验 → 金额一致性 → 幂等 → 置已支付 → 推进订单 |
| [PaymentCloseTask.java](file:///c:/Users/huhaohua/Desktop/myproject/外卖系统/backend/src/main/java/com/takeout/payment/task/PaymentCloseTask.java) | 定时作废「订单已取消但支付单仍待支付」的单子 |

## 4. 方法一览

| 方法 | 说明 |
|------|------|
| `pay(orderNo, payType)` | 发起支付。校验归属 + 订单状态 + 金额，置已支付，推进订单 |
| `getByOrderNo(orderNo)` | 查询订单的支付信息（可能不存在，返回 null） |

## 5. 关键代码拆解

### 5.1 金额一致性：前端传的金额不认

```java
entity.setAmount(order.getPayAmount());   // ← 金额取自订单，不是前端
```

`pay()` 接口只接收 `orderNo + payType`，**不接收金额**。金额从订单的 `payAmount` 取，否则用户可以「1 分钱买 100 元的菜」。

### 5.2 订单状态是支付的唯一闸口

```java
if (!OrderStatusEnum.PENDING_PAYMENT.getCode().equals(order.getStatus())) {
    throw new BusinessException(ORDER_STATUS_ERROR, "订单当前状态为「...」，无法支付");
}
```

只有「待支付」（10）的订单能支付。已支付、已取消、已完成都进不来。这跟 [04-order](04-order-订单.md) 里的状态机是同一套规则。

### 5.3 幂等：重复支付直接返回

```java
Payment payment = createIfAbsent(order, payType);   // 不存在则创建

if (PayStatusEnum.PAID.getCode().equals(payment.getStatus())) {
    return toVO(payment);   // ← 已支付过，直接返回，不重复推进订单
}
```

`createIfAbsent` 按 `orderId` 查支付单，没有就建一张。如果已经是「已支付」，直接返回已有结果。配合 Controller 上的 `@RepeatSubmit(interval = 3)` 注解，双重防重复。

### 5.4 支付单与订单同事务

```java
// ① 支付单置已支付
paymentMapper.updateById(update);

// ② 通知订单模块推进状态：10 → 20
orderApi.markPaid(order.getOrderId());
```

这两步在同一个 `@Transactional` 里。**不会出现「钱付了但订单没动」**——如果 `markPaid` 抛异常，支付单的更新也会回滚。

> ⚠️ 跨模块调用在同一事务里生效的前提：`orderApi.markPaid()` 内部也是 `@Transactional`，且传播行为默认是 `REQUIRED`（加入当前事务）。本项目所有 Service 方法都是这个默认行为，所以没问题。

### 5.5 payment.order_id 唯一索引（已修复）

`schema.sql` 中 `payment.order_id` 是 **UNIQUE KEY**（不是普通索引）。这是并发缺陷修复的一部分：

- 旧代码 `createIfAbsent` 是「先查后插」，并发下两个请求可能都查不到，然后都插入 → 两张支付单。
- 唯一索引兜底：第二次插入直接抛 `DuplicateKeyException`，由全局异常处理器转成友好提示。
- 详见 [04-order 坑 6](04-order-订单.md)。

## 6. 特有坑

### 坑 1：支付单和订单的状态可能脱节

场景：用户在支付页停留 15 分钟，订单被系统超时取消了，但支付单还是「待支付」。用户回去看会困惑。

解法：`PaymentCloseTask` 定时任务，每 2 分钟扫一次「创建超过 30 分钟且状态为待支付」的支付单，查对应订单——如果订单已取消，把支付单置为「已关闭」。

### 坑 2：真实支付回调的签名校验

一期模拟支付没有回调。二期接微信/支付宝时，**回调接口必须验签**，且金额要跟订单比对，不能信任回调里传的金额。

## 7. 自检问题

1. 用户连续点两次「支付」按钮，第二次会发生什么？哪段代码保证的？
2. 支付单的 `amount` 字段来自哪里？如果前端在请求体里传一个 `amount` 字段会怎样？
3. 订单超时被系统取消后，对应的支付单是什么状态？哪个定时任务处理的？
