# 11 · stats — 统计看板

> 阅读级别：**略读**（约 800 字）。本模块本身**不查数据库**，它是个「聚合层」——把 `order`、`merchant`、`user` 三个模块的数据拼起来，给商家端和管理后台的看板用。

---

## 1. 是什么

`stats` 模块提供经营数据看板：

- **商家端**：今日/本月订单数与营业额、待接单/待支付数、近 N 日营业额趋势、热销菜品 TOP N
- **管理后台**：平台概览（顾客数、店铺数、待审核数、今日与累计成交）、商家成交排行 TOP N

核心设计原则：**stats 不直接查表**，所有数据通过各模块的 `XxxApi` 拿，自己只做「拼装 + 补零 + 范围收敛」。

## 2. 在全局的位置

```
order ─┐
merchant ─┼─> stats ─> 商家看板 / 管理后台看板
user ──┘
```

- **依赖**：`order`（订单聚合）、`merchant`（店铺信息）、`user`（用户计数）
- **不被任何模块依赖**：是纯下游模块

## 3. 精读 2 个文件

| 文件 | 作用 |
|------|------|
| [StatsServiceImpl.java](file:///c:/Users/huhaohua/Desktop/myproject/外卖系统/backend/src/main/java/com/takeout/stats/service/impl/StatsServiceImpl.java) | 全部统计逻辑，纯拼装无 SQL |
| [AdminStatsController.java](file:///c:/Users/huhaohua/Desktop/myproject/外卖系统/backend/src/main/java/com/takeout/stats/controller/AdminStatsController.java) | 管理后台看板接口 |

## 4. 方法一览

| 方法 | 端 | 说明 |
|------|----|------|
| `merchantOverview()` | 商家 | 今日/本月订单数与营业额、待办数 |
| `merchantTrend(days)` | 商家 | 近 N 日营业额趋势（自动补零） |
| `merchantHotDish(days, limit)` | 商家 | 热销菜品 TOP N |
| `platformOverview()` | 管理 | 平台累计数据 |
| `merchantRank(days, limit)` | 管理 | 商家成交排行 TOP N（补店铺名） |

## 5. 关键代码拆解

### 5.1 补零：`fillMissingDays()`

数据库按天聚合 `GROUP BY DATE(create_time)` 只会返回「有订单的日期」。直接丢给前端画折线图，没订单的日子就断了。

```java
private List<DailyStatVO> fillMissingDays(List<DailyStatVO> raw, LocalDate from, LocalDate to) {
    Map<LocalDate, DailyStatVO> byDate = raw.stream()
            .collect(Collectors.toMap(DailyStatVO::getStatDate, Function.identity(), (a, b) -> a));

    List<DailyStatVO> result = new ArrayList<>();
    for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
        DailyStatVO exist = byDate.get(date);
        if (exist != null) {
            result.add(exist);
        } else {
            DailyStatVO empty = new DailyStatVO();
            empty.setStatDate(date);
            empty.setOrderCount(0);
            empty.setAmount(BigDecimal.ZERO);   // ← 缺失日期补 0
            result.add(empty);
        }
    }
    return result;
}
```

### 5.2 范围收敛：防止恶意传参

```java
private static final int MAX_DAYS = 90;
private static final int MAX_LIMIT = 20;

private int normalizeDays(int days) {
    if (days < 1) return 7;
    return Math.min(days, MAX_DAYS);   // 最多 90 天
}
```

前端传 `days=99999` 也只会查 90 天，防止一次查太多数据把数据库拖垮。

### 5.3 商家排行补店铺名

`orderApi.statMerchantRank()` 的聚合 SQL 只拿得到 `merchant_id` 和成交金额，没有店铺名。stats 模块拿到结果后，批量调 `merchantApi.listSimpleByIds()` 补名字：

```java
Map<Long, MerchantSimpleVO> merchantMap = merchantApi
        .listSimpleByIds(ranks.stream().map(MerchantRankVO::getMerchantId).collect(Collectors.toSet()))
        .stream().collect(Collectors.toMap(MerchantSimpleVO::getMerchantId, Function.identity(), (a, b) -> a));
ranks.forEach(rank -> {
    MerchantSimpleVO merchant = merchantMap.get(rank.getMerchantId());
    rank.setMerchantName(merchant == null ? null : merchant.getName());
});
```

这是典型的**批量查询避免 N+1**——先收集所有 id，一次查回 Map，再循环赋值。

## 6. 特有坑

### 坑 1：累计数据的起点

平台累计数据从 `2020-01-01` 开始查：

```java
private static final LocalDate PLATFORM_START = LocalDate.of(2020, 1, 1);
```

写死一个「早于系统存在的时间」即可，不用动态算「最早的订单日期」。

### 坑 2：stats 不做权限校验

`platformOverview()` 和 `merchantRank()` 只在 `/admin/**` 路径下暴露，端级权限由 AOP 拦截（见 [02-common](02-common-公共能力.md) 的端级权限控制）。stats Service 内部不再重复校验。

## 7. 自检问题

1. 商家查「近 7 日营业额趋势」，但其中 3 天没订单，返回的列表有几条？为什么？
2. `merchantRank()` 返回的排行里，店铺名是怎么来的？为什么不直接在 SQL 里 JOIN 店铺表？
3. 前端传 `days=1000&limit=100`，后端实际会查多少天、多少条？哪段代码收敛的？
