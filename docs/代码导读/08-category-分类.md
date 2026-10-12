# 08 · category — 菜品分类

> 阅读级别：**略读**（约 800 字）。本模块是全项目最简单的模块之一，核心只有一件事：**分类归属于店铺，商家只能改自己的**。

---

## 1. 是什么

`category` 模块管理「店铺内的菜品分类」，比如「热销」「主食」「饮品」。一个分类只属于一个店铺，一个店铺有多个分类。

它不负责「分类下有哪些菜」——那是 `dish` 模块的事。分类表只有 `merchant_id / name / sort / status` 四个业务字段。

## 2. 在全局的位置

```
merchant ──< category ──< dish
              │
              └─ 对外暴露 CategoryApi，供 dish 模块反查分类名
```

- **上游**：`merchant`（决定分类归属哪个店铺）
- **下游**：`dish`（菜品挂在分类下）
- **对外契约**：`CategoryApi`（3 个只读方法）

顾客端和商家端看到的分类不一样：顾客端只看「启用中」的，商家端看全部（含禁用的，方便重新启用）。

## 3. 精读 2 个文件

| 文件 | 作用 |
|------|------|
| [CategoryServiceImpl.java](file:///c:/Users/huhaohua/Desktop/myproject/外卖系统/backend/src/main/java/com/takeout/category/service/impl/CategoryServiceImpl.java) | 全部业务逻辑，4 个写操作 + 2 个读操作 |
| [CategoryApi.java](file:///c:/Users/huhaohua/Desktop/myproject/外卖系统/backend/src/main/java/com/takeout/category/api/CategoryApi.java) | 跨模块契约 |

## 4. 方法一览

| 方法 | 端 | 说明 |
|------|----|------|
| `listMine()` | 商家 | 按 `sort` 升序列出自己店铺全部分类 |
| `create(dto)` | 商家 | 新增分类，`merchantId` 由后端注入 |
| `update(id, dto)` | 商家 | 编辑分类，先校验归属 |
| `delete(id)` | 商家 | 删除分类（物理删，无外键约束） |
| `customerListByMerchant(merchantId)` | 顾客 | 只返回 `status=1` 的分类 |

## 5. 关键代码拆解

### 5.1 归属校验：`requireOwn(id)`

```java
private Category requireOwn(Long id) {
    Category category = categoryMapper.selectOne(...eq(Category::getId, id));
    if (category == null) throw new BusinessException(CATEGORY_NOT_FOUND);
    if (!myMerchantId().equals(category.getMerchantId()))
        throw new BusinessException(MERCHANT_NOT_OWNER);
    return category;
}
```

`myMerchantId()` 用当前登录 `userId` 反查自己的 `merchantId`。**前端传的 `id` 不可信**——路径前缀只能卡到「商家端」，卡不到「哪条分类」，所以必须按 `userId → merchantId → category.merchantId` 逐级比对。

### 5.2 显式列：`selectColumns()`

```java
private LambdaQueryWrapper<Category> selectColumns() {
    return Wrappers.<Category>lambdaQuery()
            .select(Category::getId, Category::getMerchantId, Category::getName,
                    Category::getSort, Category::getStatus);
}
```

不写 `SELECT *`，只取需要的列。这是全项目的统一风格，见 [02-common](02-common-公共能力.md)。

## 6. 特有坑：删分类不检查菜品

`delete()` 里没有「分类下还有菜品就禁止删除」的校验。原因有二：

1. **模块隔离**：`category` 不允许直接查 `dish` 表，跨模块契约 `DishApi` 也没提供计数方法。
2. **无外键**：数据库不建外键约束（项目约定），所以数据库层也不拦。

后果：删除分类后，挂在它下面的菜品变成「孤儿菜品」。但顾客点菜时菜品列表仍然会展示（`category_id` 只是个数字，不影响展示），商家端编辑菜品时分类下拉框里没有这一项——属于可接受的边界情况。

> 想做得更严谨：给 `DishApi` 加一个 `countByCategoryId(categoryId)` 方法，在 `delete` 前调一下，大于 0 就抛异常。

## 7. 自检问题

1. 商家 A 拿到商家 B 的分类 id，直接调 `PUT /merchant/category/{id}`，会发生什么？哪段代码拦的？
2. 顾客端调 `GET /customer/category/list?merchantId=X`，如果店铺有「禁用」分类，返回里会包含吗？
3. 如果要加「删除分类前检查是否有菜品」，应该改哪个文件、调用哪个模块的哪个 Api？
