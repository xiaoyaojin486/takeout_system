# 07 · cart 购物车

> 系列第 7 篇（简版）。前置：`00 总览`、`01 注解与语法专题`、`02 common`、`04 order`。
>
> 这个模块只有 11 个文件、最简单的业务，却是**全项目唯一一个真实踩过并修复了 bug 的模块**——那个 bug 的教训值得完整读一遍。

---

## ① cart 是什么

`cart` = **购物车**。

**它解决什么问题**：暂存「想买什么」。

它有两个鲜明特点：

1. **它是全项目唯一不存金额的持久化模块**——只存 `userId + dishId + quantity`，价格一律实时读菜品表算
2. **它是全项目唯一不用逻辑删除的模块**（见第 ⑥ 段坑 1）

---

## ② 在全局的位置

```
   dish ──┐
          ├──> cart ──> order（下单后清购物车）
merchant ─┘
```

- **依赖**：`dish-api`（查价格、库存、上架状态）、`merchant-api`（查店铺名、起送价、配送费）
- **被依赖**：`order`（下单成功后调 `CartApi.removeByUserIdAndDishIds` 清掉已下单的菜）

**没有其他模块读购物车数据**——下单时顾客是「前端把购物车内容整理成 items 传过来」，而不是 order 去读购物车表。这样 order 不依赖「购物车必须先加购」这个前提，直接下单也能用。

---

## ③ 只需精读这 2 个地方

### 精读 1：`CartItem.java`（实体）⭐

**为什么是它**：这个实体的注释解释了**为什么它故意不继承 `BaseEntity`**——这是一条真实的踩坑记录。

```java
/**
 * <p><b>刻意不继承 BaseEntity</b>，因为 BaseEntity 带 delFlag 字段，
 * 而 MyBatis-Plus 会把「实体有 delFlag」的删除操作改写成逻辑删除（UPDATE del_flag = 1）。
 * 购物车行有唯一索引 uk_user_dish(user_id, dish_id)，逻辑删除后行仍在表里，
 * 于是「移除菜品后再加回同一菜品」会插入一条新行并撞上唯一键，
 * 直接抛 DuplicateKeyException（表现为 500）。</p>
 */
```

### 精读 2：`CartServiceImpl.list()` —— 分组 + 实时算钱 + 批量查询

**为什么是它**：一个方法里同时演示了「按店铺分组」「实时算钱」「避免 N+1」三件事。

```java
// ① 批量取菜品与店铺：避免在循环里逐个查询（N+1）
Map<Long, DishSimpleVO> dishMap = dishApi.listSimpleByIds(dishIds).stream()
        .collect(Collectors.toMap(DishSimpleVO::getDishId, d -> d, (a, b) -> a));

// ② 按店铺分组，保持加入顺序
Map<Long, List<CartItemVO>> grouped = new LinkedHashMap<>();
for (CartItem item : items) {
    DishSimpleVO dish = dishMap.get(item.getDishId());
    if (dish == null) { continue; }   // 菜品已被删除：跳过展示
    grouped.computeIfAbsent(item.getMerchantId(), k -> new ArrayList<>()).add(toItemVO(item, dish));
}

// ③ 营业中的店铺排前面，方便用户先结算可下单的
result.sort(Comparator.comparing(g -> Integer.valueOf(1).equals(g.getBusinessStatus()) ? 0 : 1));
```

> **① 处的 `(a, b) -> a` 是 `Collectors.toMap` 的第三个参数**（key 重复时保留先出现的那个）。不加它会抛 `IllegalStateException: Duplicate key`——语法细节见 `01 注解与语法专题` 第 2 节。

---

## ④ 子包与方法一览

`cart` 模块 11 个文件（`CartServiceImpl` 243 行），是全项目**最小的业务模块**。

| 方法 | 作用 | 关键点 |
| --- | --- | --- |
| `list()` | 购物车列表 | 按店铺分组；价格实时算；营业中店铺排前 |
| `add()` | 加购 | 校验菜品上架 + 店铺已审核；同菜品累加数量；**累加后校验库存** |
| `updateQuantity()` | 改数量 | 传 0 等价于移除；校验库存 |
| `remove()` | 移除单品 | 归属校验 |
| `clear()` | 清空 | 清当前用户全部 |
| `count()` | 件数 | H5 角标用 |

`CartApi`（对外契约）只有 **1 个方法**：`removeByUserIdAndDishIds` —— 全项目最细的契约接口。

---

## ⑤ 关键代码拆解：为什么购物车不存价格

`CartItem` 实体只有 4 个业务字段：

```java
private Long userId;
private Long merchantId;
private Long dishId;
private Integer quantity;   // ← 没有 price，没有 amount
```

**加购时不存价格**，每次查列表时现算：

```java
vo.setPrice(dish.getPrice());
vo.setAmount(dish.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
```

**为什么不存**：购物车是「意向」不是「凭证」。

如果加购时存了价格快照，就会出现：顾客 3 天前加了购物车（当时 26 元），今天商家改成 30 元，顾客结算时看到购物车显示 26 元、支付时却要付 30 元 —— **金额前后不一致，这是最容易引起投诉的一类问题**。

**存快照的正确位置是 `order_item`**（下单那一刻）。购物车不需要，也不应该有历史价值。

---

## ⑥ cart 特有的坑

### 坑 1：⭐ 唯一索引 + 逻辑删除 = 500（真实 bug）

这是全项目**唯一一个真实踩到并修复的 bug**，完整记录在 `docs/验收文档.md` 第八节第 2 条。

**现象**：购物车里移除某个菜品，再重新加购同一菜品 → 接口返回 **500 系统异常**。

**根因**：
```sql
-- cart_item 表有唯一索引
UNIQUE KEY uk_user_dish (user_id, dish_id)
```
而 MyBatis-Plus 的**逻辑删除**只是把 `del_flag` 置为 1，**行还留在表里**。于是：

```
移除菜品  → del_flag = 1（行还在，唯一索引仍然占着 (5, 1) 这个位置）
重新加购  → 代码查不到（查询带 del_flag = 0）→ 认为不存在 → 执行 INSERT
          → 撞上唯一索引 (5, 1) → DuplicateKeyException → 500
```

**修复**：让 `CartItem` **不继承 `BaseEntity`**（于是实体没有 `delFlag` 字段），MyBatis-Plus 就不再做逻辑删除，删除变成**物理删除**。

**为什么这样修而不是改索引**：购物车是临时数据，**不需要审计追溯**。逻辑删除的意义是「保留历史」，而购物车没有历史价值——留着反而是垃圾数据。**用错机制，比不用机制更糟。**

> 💡 **这条规律值得记住**：`逻辑删除 + 唯一索引` 是一对天然冲突的组合。
> 以后给任何表加唯一键之前，先问一句：**这张表会不会用逻辑删除？** 会的话，就得在「物理删除」和「改索引设计」之间做选择。
>
> 同样的坑 `sys_user.username` 也存在（账号唯一 + 逻辑删除），只是当前没有「注销账号」功能所以没暴露。**将来加注销功能时必定踩到。**

### 坑 2：跨店购物车是允许的，但一次只能下单一家店

购物车**允许**加不同店铺的菜品（按店铺分组展示）。但**下单时 order 模块会拦**：

```
一个订单只能属于一个商家 → 违反则抛 13005
```

**为什么不在加购时就拦住**：用户想「先都加上、再决定先买哪家」是合理诉求；而强制单店加购会让用户反复清空。**把限制放在真正必要的那一步（下单），而不是提前限制用户。** 这个拦截写在第 ① 步之前（`createOrder` 第 ③ 步），不在 cart 模块。

### 坑 3：已删除的菜品会「静默消失」

`list()` 里 `if (dish == null) { continue; }` —— 菜品被删除后，对应的购物车行会被跳过展示，但**行还留在表里**（不会被自动清理）。

用户看到的现象是「购物车里的菜变少了」，但数据库里还留着孤儿行。当前没有清理任务，属于可接受的简化。

---

## ⑦ 自检问题（不给答案）

**问题 1**：`cart_item` 表有唯一索引 `(user_id, dish_id)`，为什么购物车**不能**用逻辑删除？如果因为某些原因必须保留逻辑删除，你能想出几种解法？（提示：从索引设计、SQL 写法、数据清理三个方向想）

**问题 2**：顾客在购物车页面停留了 10 分钟，期间商家把某菜品从 26 元改成 30 元。他点「去结算」时，页面上的金额和最终要付的金额分别是多少？为什么？

**问题 3**：顾客往购物车里加了 3 家店的菜，能一次下单吗？在哪一层被拦下来？**为什么不在加购时就拦住他？**

---

**上一篇** → `06-dish-菜品.md` ｜ **下一篇**（待写）→ `08-category`、`09-address`、`10-payment`、`11-stats`、`12-file`

---

## 附：本篇的一句话总结

> **cart 模块的全部设计，都围绕一个判断：它是「意向」而不是「凭证」。**
> 因为是意向 —— 所以不存价格快照、所以允许跨店、所以不需要逻辑删除保留历史。
> 三个看似无关的设计决定，背后是同一个定位。