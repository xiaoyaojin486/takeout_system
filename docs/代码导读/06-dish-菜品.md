# 06 · dish 菜品

> 系列第 6 篇（简版）。前置：`00 总览`、`01 注解与语法专题`、`02 common`、`04 order`（尤其「拆解 1：一行 SQL 防超卖」）。

---

## ① dish 是什么

`dish` = **菜品**。

**它解决什么问题**：商家菜单的增删改上下架，并且——**它是全项目「库存」和「销量」这两个字段的唯一持有者**。

这一点很容易被忽略：扣库存发生在「下单」这个 order 模块的业务里，但**真正执行扣减的 SQL 写在 `DishMapper`**。同理，加销量发生在「订单完成」，SQL 也在 dish 模块。

> 记法：**order 决定「什么时候扣」，dish 决定「怎么扣」。** 这个分工就是「每张表只有一个主人」规则的体现。

---

## ② 在全局的位置

```
     category（分类）
        ↑
      dish（菜品）  ←── 库存 / 销量的唯一写入方
     ↑    ↑    ↑
  cart  order  stats
```

- **依赖**：`category-api`（校验分类归属）
- **被依赖**：`cart`（查价格库存）、`order`（扣库存、加快照）、`stats`（经 order 取热销）

---

## ③ 只需精读这 2 个地方

### 精读 1：`mapper/DishMapper.java`（35 行）⭐

**为什么是它**：全文只有 3 条 `@Update`，却是**整个项目并发安全的核心**。`04-order` 篇的「拆解 1」逐字拆解了它，这里只重复一遍结论：

```java
@Update("UPDATE dish SET stock = stock - #{quantity} " +
        "WHERE id = #{dishId} AND stock >= #{quantity} AND del_flag = 0")
int deductStock(@Param("dishId") Long dishId, @Param("quantity") int quantity);
```

**判断库存够不够**这件事被放进了 `WHERE`，而不是写在 Java 里。所以「检查」和「扣减」是一次原子操作，没有并发缝隙。

### 精读 2：`DishServiceImpl.customerMenu()` —— 菜单是怎么分组的

**为什么是它**：顾客端菜单是「分类 → 菜品」两层结构，这个方法里有两条容易忽略的规则：

```java
// 规则 1：分类被禁用 → 整组隐藏（先筛出启用状态的分类，并保持 sort 顺序）
List<CategorySimpleVO> categories = categoryApi.listByMerchantId(merchantId).stream()
        .filter(category -> CATEGORY_ENABLED.equals(category.getStatus()))
        .sorted(... sort 升序 ...)
        .collect(Collectors.toList());

// 规则 2：只取上架菜品
List<Dish> dishes = dishMapper.selectList(selectColumns()
        .eq(Dish::getMerchantId, merchantId)
        .eq(Dish::getStatus, STATUS_ON_SHELF));
```

---

## ④ 子包与方法一览

`dish` 模块 15 个文件（`DishServiceImpl` 243 行最长）。

### 商家端 5 个方法 + 顾客端 1 个

| 端 | 方法 | 作用 | 关键约束 |
| --- | --- | --- | --- |
| 商家 | `merchantPage` | 菜品分页 | 只查自己的；补分类名 |
| 商家 | `create` | 新增菜品 | **校验分类属于自己** |
| 商家 | `update` | 修改 | 校验菜品归属 + 分类归属 |
| 商家 | `delete` | 删除 | 校验归属（逻辑删除） |
| 商家 | `updateStatus` | 上架/下架 | 校验归属 |
| 顾客 | `customerMenu` | 菜单 | 只返回上架菜品，按启用分类分组 |

> 注意 5 个商家端方法里，**每个都要做归属校验**。这是 `dish` 模块最容易漏的地方——漏一个，商家 A 就能改商家 B 的菜价。

### `DishApi` 对外 5 个方法

| 方法 | 谁用 | 用途 |
| --- | --- | --- |
| `getSimpleById` | cart | 加购时查状态/库存 |
| `listSimpleByIds` | cart、order | 批量查（避免 N+1） |
| `deductStock` | order | **原子扣减**，返回 0 表示库存不足 |
| `restoreStock` | order | 取消/拒单/超时归还 |
| `incrSales` | order | 订单完成时累加 |

---

## ⑤ 关键代码拆解：为什么「下架」和「删除」效果不同

| 操作 | 数据库变化 | 顾客能看到吗 | 库存还在吗 |
| --- | --- | --- | --- |
| 下架（`status = 0`） | 改 `status` 字段 | ❌ 看不到（菜单过滤 `status=1`） | ✅ 还在 |
| 删除 | 逻辑删除 `del_flag = 1` | ❌ 看不到 | ✅ 还在（行没删） |

`customerMenu` 里过滤的是 `status`，而 MyBatis-Plus 会自动加上 `del_flag = 0`。两者叠加，顾客端只会看到「未被删除且已上架」的菜品。

**为什么下架不直接删除**：商家可能只是暂时停售（比如今天的牛肉用完了），下架是可逆的；而且已下单的历史订单里存的是快照，不依赖菜品本身是否存在。

---

## ⑥ dish 特有的坑

**坑 1：库存和销量的写入，只能走那 3 条 `@Update`**

不要在 Service 里写「先 `getSimpleById` 查库存、再 `updateById` 改库存」——那正是 `04-order` 篇坑 1 的同类问题（TOCTOU）。`DishMapper` 的三个方法就是为了避免这种写法而存在的，**任何新增的库存变动都必须走它们**。

**坑 2：菜品归属校验要先「反查自己的 merchantId」**

菜品表里只有 `merchant_id`，而登录用户是 `userId`。所以每个商家端方法的第一步都是：

```java
Long myMerchantId = merchantApi.getSimpleByUserId(UserContextHolder.getUserId()).getMerchantId();
// 然后比对 dish.getMerchantId() 是否等于 myMerchantId
```

这个反查在 `dish` / `category` / `order` 里反复出现，是本项目最高频的「前置动作」。

**坑 3：`dish` 模块不知道「什么时候该加销量」**

`incrSales` 由 order 模块在「订单完成」时调用，dish 模块本身没有任何触发逻辑。**这个知识必须从 order 侧看**——如果只读 dish 模块，你永远不知道销量是什么时候加的。

> 💡 这体现了模块化的一个代价：**单个模块内的代码是「不自解释」的，业务时序要跨模块才能看清**。这也是为什么 `00 总览` 里那张「一次请求的完整旅程」值得先读。

**坑 4：顾客菜单里「分类禁用」和「菜品下架」的处理级别不同**

- 分类禁用 → **整组隐藏**（连分类标题都不出现）
- 菜品下架 → 只是这一个菜不显示，分类还在

因为一个分类下只要还有一个在售菜品，这个分类就有存在意义；而分类整体禁用是商家的明确意图（比如「今天不做凉菜了」）。

---

## ⑦ 自检问题（不给答案）

**问题 1**：`DishMapper` 里三个方法为什么都用 `@Update` 手写 SQL，而不用 MyBatis-Plus 的 `updateById`？如果用 `updateById` 写「扣库存」，会有并发问题吗？为什么？

**问题 2**：顾客菜单里，「分类被禁用」和「菜品被下架」的处理分别是什么？为什么一个是整组隐藏、一个只是单个不显示？

**问题 3**：商家改了菜品价格。此时有一笔「已下单但还没支付」的订单 —— 这笔订单的金额会变吗？如果不变，是靠什么机制保证的？（提示：快照存在哪张表）

---

**上一篇** → `05-merchant-店铺.md` ｜ **下一篇** → `07-cart-购物车.md`