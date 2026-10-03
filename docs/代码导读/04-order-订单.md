# 04 · order 订单

> 系列第 4 篇。前置：`00 总览`（尤其「一次请求的完整旅程」）、`01 注解与语法专题`（`@Transactional`、`BigDecimal`、`switch` 表达式、`LambdaQueryWrapper`）、`02 common`、`03 auth`。
>
> **这是全项目最重要的模块，也是唯一会「动钱」和「动库存」的模块。** 建议留出整块时间读。

---

## ① order 是什么

`order` = **订单**。

**它解决什么问题**：把「顾客想买什么」变成「一笔有金额、有状态、可追溯的交易」。

它是全系统唯一同时承担这三件事的模块：

| 职责         | 具体内容                                            | 出错代价                               |
| ------------ | --------------------------------------------------- | -------------------------------------- |
| **算钱**     | 菜品总额、配送费、实付金额，全部由后端重算          | 直接资损                               |
| **扣还库存** | 下单扣、取消还                                      | 库存虚高 → 超卖；库存虚低 → 有货卖不出 |
| **管状态**   | 待支付 → 待接单 → 备餐中 → 配送中 → 已完成 / 已取消 | 状态错乱 → 钱货不一致                  |

一句话：**别的模块出错是体验问题，order 出错是钱的问题。**

---

## ② 在全局的位置

```
          order
            ├──> AddressApi   （取地址 → 存快照）
            ├──> DishApi      （取菜品价格 → 存快照 → 原子扣库存）
            ├──> MerchantApi  （校验审核/营业状态 → 取起送价、配送费）
            └──> CartApi      （下单后清购物车）
                    ↓
                 common（Result / 异常 / 上下文 / 单号生成）

   payment ──> OrderApi（查订单、标记已支付）
   stats   ──> OrderApi（取聚合统计）
```

**依赖 4 个模块的 api，被 2 个模块依赖。** 这个位置决定了它是「聚合根」——它把多个模块的数据组合成一笔交易，但**每份数据都由拥有者提供，它自己一份都不存**（除了快照）。

> 注意一个反直觉的地方：**order 依赖 dish，但 payment 依赖 order**（不是反过来）。
> 为什么？因为下单时**不需要创建支付单**——支付单是首次进支付流程时才按订单金额生成的。这样依赖方向是单向的 `payment → order`，没有环。这是有意设计的（见第 ⑥ 段坑 5 的关联说明）。

---

## ③ 只需精读这 2 个文件

### 精读 1：`order/service/impl/OrderServiceImpl.java` 的 `createOrder()` 方法

文件一共 **517 行**（17 个公开方法），**不要全读**。先只读 `createOrder()`（第 85~200 行，约 115 行），它是整个系统的枢纽。

**为什么是它**：读完这一段，你就理解了「金额怎么算、库存怎么扣、快照怎么存、事务边界在哪」这四件事。而另外 16 个方法，本质上都是「改状态 + 可能要还库存」的变体。

读的时候对照 `00 总览` 第五节那张 17 步的表。

### 精读 2：`order/service/OrderStatusMachine.java`（107 行）

**为什么是它**：它是「订单状态不允许乱改」这条规则的**唯一守门人**。全文很短，但 `transfer()` 一个方法就完成了三件事：校验流转是否合法 → 改状态并打时间戳 → 写状态日志。

**它解决的问题**：如果把「能不能改」散落在接单/拒单/取消/确认收货十几个方法里，早晚有一个入口漏判。集中到一处后，新增一个状态入口时**不可能绕过校验**。

### 顺带必看：`dish/mapper/DishMapper.java`（35 行）

虽然它属于 `dish` 模块，但**并发防超卖的核心那行 SQL 在这里**（见第 ⑤ 段拆解 1）。全文只有 3 条 `UPDATE`，5 分钟就能看完。

---

## ④ 子包与方法一览

`order` 模块 32 个文件，是最大的模块。按子包分组：

| 子包                 | 文件数      | 关键文件                                                          | 作用                          |
| -------------------- | ----------- | ----------------------------------------------------------------- | ----------------------------- |
| `service/impl`       | 1           | **`OrderServiceImpl`（517 行）**                                  | 全部业务逻辑（17 个公开方法） |
| `service`            | 2           | **`OrderStatusMachine`（107 行）**、`OrderService`（接口，94 行） | 状态机 / 服务契约             |
| `mapper`             | 3           | `OrderMapper`（94 行，含统计 SQL）                                | 数据库访问                    |
| `entity`             | 3           | `Order`、`OrderItem`、`OrderStatusLog`                            | 对应 3 张表                   |
| `api/impl`           | 1           | `OrderApiImpl`（113 行）                                          | 对外契约实现                  |
| `api`                | 1 + 6 个 VO | `OrderApi`（8 个方法）                                            | 给 payment / stats 用         |
| `controller`         | 3           | 顾客端 / 商家端 / 管理端                                          | HTTP 入口                     |
| `dto` `vo` `convert` | 11          | —                                                                 | 入参 / 出参 / 转换            |
| `task`               | 1           | `OrderTimeoutTask`（53 行）                                       | 超时未支付自动取消            |

### `OrderServiceImpl` 的 17 个方法（按端分组）

| 端   | 方法                                     | 一句话作用                        | 有没有副作用     |
| ---- | ---------------------------------------- | --------------------------------- | ---------------- |
| 顾客 | `createOrder`                            | 下单（算钱 + 扣库存 + 存快照）    | 扣库存、清购物车 |
| 顾客 | `myOrders` / `myOrderDetail`             | 我的订单 / 详情                   | 无               |
| 顾客 | `cancelByCustomer`                       | 取消（仅待支付、待接单）          | **还库存**       |
| 顾客 | `confirmByCustomer`                      | 确认收货 → 已完成                 | **加销量**       |
| 商家 | `merchantOrders` / `merchantOrderDetail` | 订单列表 / 详情                   | 无               |
| 商家 | `acceptByMerchant`                       | 接单 → 备餐中                     | 无               |
| 商家 | `rejectByMerchant`                       | 拒单 → 已取消                     | **还库存**       |
| 商家 | `cancelByMerchant`                       | 取消（待接单/备餐中）             | **还库存**       |
| 商家 | `deliverByMerchant`                      | 出餐 → 配送中                     | 无               |
| 商家 | `finishByMerchant`                       | 送达 → 已完成                     | **加销量**       |
| 管理 | `adminOrders` / `adminOrderDetail`       | 全平台订单                        | 无               |
| 系统 | `markPaid`                               | 支付成功 → 待接单（payment 调用） | 无               |
| 系统 | `closeTimeoutOrder`                      | 关闭单笔超时订单                  | **还库存**       |
| 系统 | `listTimeoutOrders`                      | 查超时候选（不开事务）            | 无               |

> 👀 **注意「副作用」那一列**：只有 6 个方法会动库存或销量。看懂这 6 个，就掌握了 order 模块的全部风险点。
> 「还库存」= `restoreStock()`，「加销量」= `accumulateSales()`，见第 ⑤ 段拆解 3。

### `OrderApi` —— 给别的模块用的 8 个方法

| 方法                                                 | 谁在用  | 用途                                 |
| ---------------------------------------------------- | ------- | ------------------------------------ |
| `getByOrderNo`                                       | payment | 支付前查订单、校验归属与状态         |
| `getById`                                            | payment | 作废未支付支付单时判断订单是否已取消 |
| `markPaid`                                           | payment | 支付成功后把订单推进到待接单         |
| `statMerchant` / `statMerchantTrend` / `statHotDish` | stats   | 商家经营看板                         |
| `statPlatform` / `statMerchantRank`                  | stats   | 平台看板                             |

> **为什么统计接口放在 order 模块里**：只有 order 能访问 `orders` / `order_item` 表。`stats` 模块要出报表就必须通过这些契约拿聚合结果——**它自己不拥有任何表**。

---

## ⑤ 关键代码拆解

### 拆解 1：一行 SQL 防超卖 ⭐ 本篇最值得带走

`dish/mapper/DishMapper.java`：

```java
@Update("UPDATE dish SET stock = stock - #{quantity} " +
        "WHERE id = #{dishId} AND stock >= #{quantity} AND del_flag = 0")
int deductStock(@Param("dishId") Long dishId, @Param("quantity") int quantity);
```

**关键点：把「库存够不够」这个判断，从 Java 代码里挪到了 SQL 的 `WHERE` 条件里。**

对比两种写法（假设库存只剩 1，两个请求同时来买 1 份）：

**❌ 先查后改（有并发漏洞）**

```java
Dish dish = dishMapper.selectById(id);
if (dish.getStock() >= quantity) {          // 两个请求都读到 stock = 1，都判断通过
    dishMapper.updateStock(id, dish.getStock() - quantity);   // 都写成 0
}
// 结果：卖出 2 份，库存变 0 —— 超卖
```

**✅ 原子扣减（本项目采用）**

```java
int affected = deductStock(dishId, quantity);   // 一条 SQL 搞定判断 + 扣减
if (affected == 0) { throw new BusinessException(STOCK_NOT_ENOUGH); }
```

```sql
-- 数据库层面的执行过程：
-- 请求A：UPDATE ... WHERE id=3 AND stock >= 1   → 命中，stock: 1 → 0，返回影响行数 1
-- 请求B：UPDATE ... WHERE id=3 AND stock >= 1   → 此时 stock 已是 0，WHERE 不命中，返回影响行数 0
--        ↓
--        affected == 0 → 抛「库存不足」→ 整个下单事务回滚
```

**为什么不需要在 Java 里加锁**：`UPDATE` 语句在 InnoDB 里会对命中的行**加排他行锁**，请求 B 必须等 A 提交后才能执行。而 A 提交后 `stock` 已经是 0，B 的 `WHERE stock >= 1` 自然不成立。**判断和扣减在同一个原子操作里，中间没有缝隙可钻。**

> 💡 顺带理解「为什么扣库存要在下单时扣，而不是支付时扣」：
> 如果支付时才扣，那么「下单成功但一直不支付」的订单会把库存**悬挂占用**，别人买不到；而如果下单不占库存、支付时才判断，就会出现「付完钱发现没货」。**下单即扣 + 取消归还**是外卖/电商的通行解法。

### 拆解 2：状态机的「三合一」

`OrderStatusMachine.transfer()` 一个方法干了三件事，**顺序不能变**：

```java
public void transfer(Order order, OrderStatusEnum target, OrderOperatorEnum operator,
                     Long operatorId, String remark) {
    // 第一件：校验这次流转合不合法（唯一真相来自 OrderStatusEnum 的流转表）
    Integer from = order.getStatus();
    if (!OrderStatusEnum.canTransfer(from, target.getCode())) {
        throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, String.format(
                "订单当前状态为「%s」，只能流转到：%s",
                current == null ? "未知" : current.getDesc(),
                OrderStatusEnum.allowedTargets(from)));      // ← 报错时告诉用户「现在能做什么」
    }

    // 第二件：改状态 + 按目标状态打时间戳（只在进入对应状态时写一次）
    LocalDateTime now = LocalDateTime.now();
    Order update = new Order();
    update.setId(order.getId());
    update.setStatus(target.getCode());
    switch (target) {
        case PENDING_ACCEPT -> update.setPayTime(now);
        case PREPARING -> update.setAcceptTime(now);
        case COMPLETED -> update.setFinishTime(now);
        case CANCELED -> { update.setCancelTime(now); update.setCancelReason(remark); }
        default -> { }
    }
    orderMapper.updateById(update);
    order.setStatus(target.getCode());      // ← 同步内存对象，调用方后续判断用的就是新状态

    // 第三件：写状态日志（和改状态在同一次调用里，不可能只改不留痕）
    writeLog(order.getId(), from, target.getCode(), operator, operatorId, remark);
}
```

**三个细节值得学**：

1. **错误提示带上「现在能做什么」**（`allowedTargets`）。用户看到的是「订单当前状态为「已完成」，只能流转到：无（终态）」，而不是干巴巴的「状态非法」。排查问题时这个提示能省很多时间。
2. **`switch` 按目标状态打时间戳**。这样 `pay_time` / `accept_time` / `finish_time` 各自只被写一次，不会被后续流转覆盖。
3. **`order.setStatus(target.getCode())` 这一行容易看漏**。它不是写库（写库在上一行），而是**同步内存对象**——这样调用方紧接着做 `if (order.getStatus() == ...)` 判断时拿到的是新值。不加这一行会出现很难查的 bug。

### 拆解 3：为什么「还库存」和「加销量」不是一对对称操作

```java
/** 归还订单全部菜品库存（取消/拒单/超时时调用） */
private void restoreStock(Long orderId) {
    List<OrderItem> items = listItems(orderId);
    for (OrderItem item : items) {
        dishApi.restoreStock(item.getDishId(), item.getQuantity());
    }
}

/** 订单完成时累加销量：菜品销量 + 店铺销量（店铺销量记 1 笔订单） */
private void accumulateSales(Order order) {
    List<OrderItem> items = listItems(order.getId());
    for (OrderItem item : items) {
        dishApi.incrSales(item.getDishId(), item.getQuantity());
    }
    merchantApi.incrSales(order.getMerchantId(), 1);
}
```

**关键设计：销量只在「已完成」时累加，不在「已支付」时累加。**

这样做的收益是**取消链路完全不需要回退销量**——因为压根没加过。

想象一下如果销量在支付时累加：

- 支付 → 销量 +2
- 顾客 10 分钟后取消 → 必须**回退销量 -2**
- 万一某个取消路径（比如超时、商家拒单、平台介入）漏了回退 → 销量**永久虚高**，而且没有任何报表能发现（因为销量只是个展示数字）

**一条规则贯穿，优于两处口径互相校正。** 这是设计取舍里很典型的一类：把状态变更和统计口径绑定在同一个「终态」上。

---

## ⑥ order 特有的坑

### 坑 1：⚠️ 状态变更曾缺少乐观锁（已修复，附并发验证）

**先说结论**：这是一个**真实的并发缺陷**，在时序操作下不会出现（冒烟测试 55 项全通过也验证了这一点），但会出现在**真正并发**的场景里，比如用户双击「取消订单」。

**已在 commit `f629224` 修复**。这个坑仍然值得完整读一遍——它演示的 **TOCTOU** 规律适用于所有涉及状态、库存、余额的代码。

**修复前** `transfer()` 里的写入长这样（下面整段分析都基于这段旧代码）：

```java
Order update = new Order();
update.setId(order.getId());
update.setStatus(target.getCode());
orderMapper.updateById(update);
```

MyBatis-Plus 的 `updateById` 生成的 SQL 是：

```sql
UPDATE orders SET status = ?, update_time = ? WHERE id = ? AND del_flag = 0
```

**问题了：`WHERE` 里没有把「原状态」作为前置条件。**

> 已核实：项目**没有启用** MyBatis-Plus 的乐观锁插件（`MybatisPlusConfig` 里只注册了分页插件和全表更新拦截），实体上也没有 `@Version` 字段。所以这里确实没有任何并发保护。

并发推演（两个请求同时取消同一笔待支付订单）：

```
请求A                                请求B
────────────────────────────────────────────────────────────
selectById → status = 10             selectById → status = 10
canTransfer(10 → 60) = true          canTransfer(10 → 60) = true
restoreStock()  库存 +2               restoreStock()  库存 +2   ← 库存被还了两遍！
updateById(status = 60)              updateById(status = 60)
```

**结果**：订单只取消了一次，但**库存被归还了两遍** → 库存虚高 → 后续可能超卖。

**注意这个缺陷的特殊性**：`transfer()` 里的状态校验是**读时校验**（校验的是内存里那个 `order` 对象的状态），而**写的时候没有再次确认**。「检查」和「执行」之间有时间窗，这就是经典的 **TOCTOU（Time-of-Check to Time-of-Use）** 问题。

**修复方式**（乐观锁：把原状态作为 UPDATE 的前置条件。这是代码里现在的实际写法）：

```java
LocalDateTime now = LocalDateTime.now();
Order update = new Order();
update.setStatus(target.getCode());
// ...（按目标状态打时间戳，同前）

// 把「原状态」也作为 WHERE 条件（乐观锁）
int affected = orderMapper.update(update, Wrappers.<Order>lambdaUpdate()
        .eq(Order::getId, order.getId())
        .eq(Order::getStatus, from));          // ← 关键：状态必须还是读取时的那个
if (affected == 0) {
    log.warn("订单状态并发冲突，已放弃本次流转 orderId={} from={} to={}",
            order.getId(), from, target.getCode());
    throw new BusinessException(ResultCode.ORDER_STATUS_ERROR,
            "订单状态已被其他操作变更，请刷新后重试");
}
order.setStatus(target.getCode());
```

**为什么用 `update(实体, wrapper)` 而不是 `update(null, wrapper)`**：MyBatis-Plus 的自动填充（`AuditMetaObjectHandler.updateFill`）需要一个**实体对象**才能写 `update_time` / `update_by`。传 `null` 会导致审计字段不再更新。所以：**实体负责「要设置什么值」，wrapper 负责「满足什么条件才更新」**。

> 另一个可选方案是 `SELECT ... FOR UPDATE` 悲观锁，但订单表并发不算高，乐观锁更轻、也不用担心锁等待。

**这个修复被真实验证过**（并发验证，非时序测试）：

```
对同一笔待支付订单，用 HttpClient 并发发 6 个取消请求
→ 后端日志出现 5 条「订单状态并发冲突，已放弃本次流转 orderId=8 from=10 to=60」
   ↑ 注意 5 条全是 from=10，证明这 6 个请求确实同时读到了 status=10（竞态窗口被真实命中）
→ 最终只有 1 个请求成功，库存只归还一次（90 → 91，而不是 90 → 96）
→ order_status_log 里只有一条流转记录（10 → 60）
```

**修复前**这 6 个请求会全部通过 `canTransfer(10→60)` 校验、各自执行一次还库存 —— 库存被归还 6 次。

> 💡 这个验证方法本身也值得学：**要证明并发保护有效，不能只跑时序测试**，必须真的并发发请求，并去日志里确认「竞态窗口被命中过」。否则测试通过只能说明「没坏」，不能说明「修好了」。

> **为什么这个坑值得单独写一段**：它演示了一个通用规律——**任何「先读判断、再写更新」的代码，都天然存在并发窗口**。凡是涉及状态流转、库存、余额的地方，写之前都要问一句：**我这次写入，有没有校验「我读到的前提还成立」？**
>
> 「前端按钮置灰」不能替代它：前端只是体验优化，**服务端才是安全边界**。

### 坑 2：`restoreStock` 有 4 个调用点，漏一个就永久丢库存

目前调用它的地方：

| 调用点              | 触发场景                    |
| ------------------- | --------------------------- |
| `cancelByCustomer`  | 顾客取消（待支付 / 待接单） |
| `rejectByMerchant`  | 商家拒单                    |
| `cancelByMerchant`  | 商家取消（待接单 / 备餐中） |
| `closeTimeoutOrder` | 超时未支付自动取消          |

**为什么这个名单要背下来**：以后新增任何「让订单进入已取消」的路径（例如「商家 5 分钟未接单自动取消」），**必须**在这里补上调用。漏掉的后果是：库存被扣了但永远回不来 —— 而且**没有任何测试或报表能发现**（订单是已取消的，看起来一切正常，只是那个菜的库存少了几十份）。

**这类 bug 的排查成本极高**，因为症状（某个菜莫名卖不出去了）和原因（两周前某个取消路径漏了还库存）之间没有任何关联线索。

### 坑 3：金额口径必须全项目唯一

本项目定义的口径是：**`orders.pay_amount` 是唯一可信金额；报表口径 = 已完成（status=50）订单的 `pay_amount` 之和**。

这个口径写在 `OrderMapper` 的注释里，因为它必须和「销量在完成时累加」保持一致。**如果哪天有人把报表口径改成「已支付订单金额之和」，报表数字就会和菜品销量对不上账**——老板会问「为什么显示卖了 100 单，销量只有 80」。

> 凡是涉及统计的功能，**先确认口径，再写 SQL**。口径不一致比算错更可怕，因为算错能发现，口径不一致永远说不清。

### 坑 4：快照字段不能靠 join 现查

`orders` 表里这三列是**下单那一刻复制过来的**：

- `receiver` / `receiver_phone` / `receiver_address`（地址快照）
- `order_item.dish_name` / `dish_image` / `price`（菜品快照）

**为什么不用 join 现查**：

- 地址可以被删除 → join 出来是 null，历史订单变成「无地址的黑单」
- 菜品可以改价 → join 出来是**今天的价格**，三个月前的订单显示今天的价格
- 菜品可以删除、改名、换图 → 历史订单全部跟着变

**快照的本质是「用冗余换不可变性」**：历史事实一旦记录就不能被后续操作污染。这是订单系统的铁律。

### 坑 5：`OrderTimeoutTask` 和 `OrderServiceImpl` 为什么必须拆成两个类

```java
// OrderTimeoutTask（定时任务）—— 没有 @Transactional
for (Order order : orders) {
    try {
        orderService.closeTimeoutOrder(order);   // ← 通过接口调用另一个 Bean 的方法
    } catch (Exception e) { log.error(...); }
}

// OrderServiceImpl.closeTimeoutOrder() —— 有 @Transactional
```

**因为 Spring 的 `@Transactional` 是靠代理实现的**：如果一个方法在**同一个类内部**被 `this.xxx()` 调用，它**不走代理**，注解直接失效。

如果把这 7 行循环写在 `OrderServiceImpl` 里、循环里调 `this.closeTimeoutOrder()`，那么：

- `@Transactional` 不生效 → 单笔订单失败时，**它已经改过的库存不会被回滚**
- 更糟的是：失败的那笔订单状态可能改了一半，脏数据留下来了

**拆成两个类就能通过代理调用**，事务正常生效。这是 Spring 项目里极高频的坑，看到「定时任务 + 事务方法」的组合就要警觉。

### 坑 6（关联）：支付单可能被重复创建（已修复）

`payment` 表原先只有 `uk_pay_no`（支付单号唯一）和 `idx_order_id`（**普通索引，不是唯一**）。

`PaymentServiceImpl.createIfAbsent()` 的逻辑是「先按 orderId 查，没有就插入」。同样是「先查后插」，同样有并发窗口——两个并发支付请求可能各自插入一条支付单，**一个订单出现两条支付记录**。

**已在 commit `f629224` 修复**：`schema.sql` 里把 `idx_order_id` 改成 `UNIQUE KEY uk_order_id (order_id)`，第二条插入会直接失败、事务回滚，**由数据库层面兜住**（对现有库已执行对应的 `ALTER TABLE`，改前确认过无重复数据）。

> 这和坑 1 是**同一类问题**（先读后写），但修复层次不同：坑 1 靠应用层乐观锁，这个靠数据库唯一约束。
> **能用数据库约束兜住的，优先用数据库约束**——它比应用代码更可靠，因为不依赖每个调用方都记得写对判断。

---

## ⑦ 自检问题（不给答案）

**问题 1**：一笔订单在「备餐中（30）」时，顾客在自己手机上点「取消订单」会发生什么？如果顾客绕过前端界面、直接用 Postman 调取消接口呢？这两者有什么本质区别？服务端这道校验写在哪一行？

**问题 2**：`restoreStock()` 现在有 4 个调用点。假设产品提出新需求——「商家超过 5 分钟未接单，系统自动取消该订单并退款」。你需要动哪些文件？其中哪一步**绝对不能漏**？为什么漏了也不会有测试报错？

**问题 3**（检验你是否真的能上手改代码）：假设要新增一个字段 `orders.platform_fee`（平台抽成金额）。请按顺序说出你要改动**哪些文件**——从数据库脚本开始，到实体、到算钱、到返回给前端。（提示：想想八件套里哪些需要动，`OrderVO` 要不要加、`convert` 要不要改）

---

**上一篇** → `03-auth-认证.md` ｜ **下一篇** → `05-merchant-店铺.md`

---

## 附：order 模块的「一句话总结」

如果只能记住一句话：

> **order 模块的所有复杂度，都来自它在同时维护三件互相牵制的东西——金额、库存、状态。任何一处设计变更，都要同时想清楚另外两处会不会失衡。**

比如「销量只在完成时累加」这个决定，就是同时考虑了状态（终态）和库存（不回退）才定下来的。
