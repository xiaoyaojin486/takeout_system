# 05 · merchant 店铺

> 系列第 5 篇（简版）。前置：`00 总览`、`01 注解与语法专题`、`02 common`、`04 order`。
>
> 本篇回答：**一个账号怎么变成一家能被下单的店**。

---

## ① merchant 是什么

`merchant` = **商家 / 店铺**。

**它解决什么问题**：把「一个想开店的账号」变成「一家可被顾客下单的店铺」。包含四件事：入驻申请、平台审核、营业开关、以及决定下单门槛的两个金额（起送价、配送费）。

**和 `user` 的关系**：`merchant.user_id` 指向 `sys_user.id`。一个商家账号对应一个店铺。

---

## ② 在全局的位置

```
        user（账号）
          ↑
      merchant（店铺）  ← 商家域的根
       ↑    ↑    ↑
 category  dish  order/cart/stats
 （都挂在店铺下）
```

- **依赖**：`user-api`（需要店主的账号信息）
- **被依赖**：`category` / `dish` / `cart` / `order` / `stats` 都要拿店铺信息

**关键位置**：它是「商家域」的根。`category` 和 `dish` 都必须属于某个 `merchant`，所以任何涉及菜品归属的校验，最终都要回到「这个店铺是不是我的」。

---

## ③ 只需精读这 2 个地方

### 精读 1：`MerchantServiceImpl.customerPage()` 和 `customerDetail()`

**为什么是它们**：这两个方法示范了一条重要原则——**对外接口的过滤条件不能由前端决定**。代码里有一行注释直接点了出来：

```java
LambdaQueryWrapper<Merchant> wrapper = selectColumns()
        // 顾客端硬编码只查审核通过的店铺，不接受前端传 auditStatus
        .eq(Merchant::getAuditStatus, AUDIT_PASSED)
        .eq(query.getBusinessStatus() != null, Merchant::getBusinessStatus, query.getBusinessStatus());
```

### 精读 2：`api/impl/MerchantApiImpl.java`（82 行）

**为什么是它**：借它理解「跨模块契约只给必要字段」。`MerchantSimpleVO` 里那十几个字段（起送价、配送费、营业状态、审核状态）**正好是 order / cart 下单校验需要的**——不多不少。契约给多了会造成耦合，给少了对方就得再查一次。

---

## ④ 子包与方法一览

`merchant` 模块 16 个文件（`MerchantServiceImpl` 293 行最长）。

### 按端分的 8 个方法

| 端 | 方法 | 作用 | 关键约束 |
| --- | --- | --- | --- |
| 顾客 | `customerPage` | 店铺列表 | **硬编码只查审核通过**，销量倒序 |
| 顾客 | `customerDetail` | 店铺详情 | 未通过审核 → 视为不存在（404） |
| 商家 | `getMyShop` | 我的店铺 | 按当前 userId 反查 |
| 商家 | `applyShop` | 入驻申请 | 已有店铺则拒绝；初始状态待审核 |
| 商家 | `updateMyShop` | 改资料 | 只改基本信息，改不了审核状态 |
| 商家 | `updateBusinessStatus` | 营业开关 | **必须已通过审核**才能营业 |
| 管理 | `adminPage` | 全部店铺 | 可按审核/营业状态筛选 |
| 管理 | `audit` | 审核通过/驳回 | 只接受 1 或 2；通过时把营业状态置 0 |

---

## ⑤ 关键代码拆解：三个「不该由前端决定」的地方

```java
// ① 列表：硬编码过滤条件，不接受前端传 auditStatus
.eq(Merchant::getAuditStatus, AUDIT_PASSED)

// ② 详情：未通过审核的店铺对顾客视为不存在，不泄露「待审核/已驳回」的内部状态
if (merchant == null || !AUDIT_PASSED.equals(merchant.getAuditStatus())) {
    throw new BusinessException(ResultCode.MERCHANT_NOT_FOUND);
}

// ③ 审核通过时把营业状态置 0（由商家自己决定何时开门）
//    避免「审核通过的一瞬间自动开始接单」——商家还没准备好备菜
```

**为什么这三处要放在服务端**：

如果第 ① 处写成 `.eq(query.getAuditStatus() != null, ...)`，那么前端只要传 `auditStatus=0` 就能把**所有待审核、甚至被驳回的店铺**列出来。这不是「功能」，是**信息泄露**——平台还没审的店、被驳回的店，顾客都不该知道存在。

---

## ⑥ merchant 特有的坑

**坑 1：「审核通过」和「营业中」是两件独立的事**

| 字段 | 决定什么 | 谁改 |
| --- | --- | --- |
| `audit_status` | 顾客**能不能看到**这家店 | 平台审核 |
| `business_status` | 顾客**能不能下单** | 商家自己 |

所以顾客端列表只筛 `audit_status=1`，但**仍然返回** `business_status` —— 前端拿到后把休息中的店置灰显示。这样用户能看到「哦那家店今天没开」，而不是觉得店消失了。

**坑 2：客服端的 404 是故意的，不是 bug**

`customerDetail` 对未通过审核的店铺返回 `11001 店铺不存在`，**不返回**「该店铺未通过审核」。后者会泄露平台的审核进度。

**坑 3：`getMyShop` 与 `currentMerchantId()` 的关系**

商家端的登录身份是 `userId`，但店铺表里存的是 `merchantId`。中间这一步反查（`merchantApi.getSimpleByUserId(userId)`）在 `dish` / `category` / `order` 模块里都会重复出现，**这是本项目最高频的一个「前置动作」**。

---

## ⑦ 自检问题（不给答案）

**问题 1**：顾客端店铺列表故意不接受前端传的 `auditStatus`。如果放开让它传，攻击者能拿到什么信息？这类「过滤条件必须服务端硬编码」的场景，你还在项目里见过哪些？

**问题 2**：「已通过审核 + 休息中」的店铺，顾客端**能不能看到**？**能不能下单**？这两个判断分别写在哪一行代码里？（提示：一个在 merchant 模块，一个在 order 模块）

**问题 3**：平台把某店铺**驳回**（`auditStatus = 2`）后，顾客端和商家端分别会发生什么？商家还能登录、还能看到自己的店吗？

---

**上一篇** → `04-order-订单.md` ｜ **下一篇** → `06-dish-菜品.md`