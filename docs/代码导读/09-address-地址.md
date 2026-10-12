# 09 · address — 收货地址

> 阅读级别：**略读**（约 800 字）。本模块的核心是**默认地址的单例约束**和**归属校验下沉到契约层**。

---

## 1. 是什么

`address` 模块管理顾客的收货地址。一个顾客有多条地址，其中最多一条是「默认地址」。

地址字段：`receiver / phone / province / city / district / detail / label / isDefault`。

**关键点**：下单时地址会被**快照**进订单表，之后顾客改地址、删地址都不影响历史订单。

## 2. 在全局的位置

```
user ──< address
           │
           └─ AddressApi.findByIdAndUserId(addressId, userId)
                └─ order 模块下单时调用，取地址快照
```

- **上游**：`user`（地址归属于顾客）
- **下游**：`order`（下单时取地址快照）

## 3. 精读 2 个文件

| 文件 | 作用 |
|------|------|
| [AddressServiceImpl.java](file:///c:/Users/huhaohua/Desktop/myproject/外卖系统/backend/src/main/java/com/takeout/address/service/impl/AddressServiceImpl.java) | 地址 CRUD + 默认地址维护 |
| [AddressApiImpl.java](file:///c:/Users/huhaohua/Desktop/myproject/外卖系统/backend/src/main/java/com/takeout/address/api/impl/AddressApiImpl.java) | 跨模块契约，强制带 userId |

## 4. 方法一览

| 方法 | 说明 |
|------|------|
| `list()` | 列出自己的地址，默认地址排最前 |
| `create(dto)` | 新增；第一条强制设默认；设默认时先清掉旧默认 |
| `update(id, dto)` | 修改；设默认时先清旧默认 |
| `delete(id)` | 删除；删的是默认地址则把最近更新的另一条提为默认 |
| `setDefault(id)` | 设为默认（先清旧默认再设新默认） |

## 5. 关键代码拆解

### 5.1 默认地址的单例约束：`clearDefault(userId)`

每次设默认地址前，先把该用户所有地址的 `isDefault` 清为 0：

```java
if (Integer.valueOf(1).equals(entity.getIsDefault())) {
    addressMapper.clearDefault(userId);   // UPDATE address SET is_default=0 WHERE user_id=?
}
addressMapper.insert(entity);            // 再插入新默认地址
```

这是「先清后设」模式，保证任何时刻一个用户只有一条默认地址。**这两步在同一个事务里**（`@Transactional`），不会出现「旧的清了但新的没插入」导致没有默认地址。

### 5.2 删除默认地址后的兜底

```java
if (Integer.valueOf(1).equals(exist.getIsDefault())) {
    Address next = addressMapper.selectOne(...orderByDesc(UpdateTime).last("limit 1"));
    if (next != null) {
        Address promote = new Address();
        promote.setId(next.getId());
        promote.setIsDefault(1);
        addressMapper.updateById(promote);
    }
}
```

删掉默认地址后，把「最近更新的另一条」提为默认。**避免出现「用户一条默认地址都没有」的状态**——否则下单页会让用户无从选择。

### 5.3 归属校验下沉到契约层

`AddressApi.findByIdAndUserId(addressId, userId)` 强制带 `userId` 参数：

```java
Address address = addressMapper.selectOne(...
        .eq(Address::getId, addressId)
        .eq(Address::getUserId, userId));   // ← 查询条件里硬编码 userId
```

订单模块下单时只能传「自己的 userId」，想越权查别人的地址，SQL 层就查不到。这比「先查出来再比对 userId」更安全——**数据根本不会返回给调用方**。

## 6. 特有坑

### 坑 1：单用户地址上限

```java
private static final int MAX_ADDRESS_COUNT = 20;
```

新增前先 `selectCount`，超过 20 条抛异常。防止恶意刷接口无限插入。

### 坑 2：完整地址拼接

`joinFullAddress()` 把省/市/区/详细地址拼接，**空段自动跳过**：

```java
appendIfPresent(sb, entity.getProvince());   // null 就跳过
```

避免出现「四川省 成都市 null 天府大道」。

## 7. 自检问题

1. 用户有 3 条地址，都不是默认的。再新增一条时，新地址会是默认地址吗？哪段代码决定的？
2. 用户删除了唯一的默认地址，剩下两条地址会发生什么？
3. 订单模块下单时调 `AddressApi.findByIdAndUserId(addressId, userId)`，如果传别人的 `userId` 会怎样？为什么说这种设计比「查出来再比对」更安全？
