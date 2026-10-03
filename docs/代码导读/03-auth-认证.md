# 03 · auth 认证

> 系列第 3 篇。前置：`00 总览`、`01 注解与语法专题`、`02 common`（尤其 02 的「精读 2」讲了 `JwtUtil` 怎么生成/解析令牌，本篇讲令牌的**生命周期**）。
>
> 结构同 02 篇的 7 段。

---

## ① auth 是什么

`auth` = **authentication，认证**。注意不是 `author`（作者），这是两个完全不同的词。

**它解决什么问题**：三端登录、签发令牌、续期、作废。一句话——**它回答「怎么证明你是你，以及这个证明能用多久」**。

### 和 `user` 模块的分工（很容易混）

|        | `user` 模块                                  | `auth` 模块                            |
| ------ | -------------------------------------------- | -------------------------------------- |
| 管什么 | **账号数据**：注册、资料、改密、管理员管账号 | **身份凭证**：登录、发令牌、刷新、登出 |
| 回答   | 「这个账号存不存在？密码对不对？」           | 「怎么证明你是你？证明有效期多长？」   |
| 类比   | 人事档案                                     | 门禁卡办理处                           |

看到 `AuthServiceImpl` 里调 `userApi.findAuthByUsername()` 就明白这个分界了：**auth 不自己查账号表，它问 user 模块要。**

---

## ② 在全局的位置

```
   payment ──┐
   stats  ───┼──> order ──> dish / merchant / address / cart ──> common
             │
   user  ────┴──> common

   auth ─────────> user-api, common          ← auth 谁也不依赖它
```

**事实核对**：全项目**没有任何一个文件 `import com.takeout.auth.*`**（除了 auth 自己的 9 个文件）。

**auth 是一个「叶子模块」**——没有任何模块依赖它。为什么能这样？

- 鉴权发生在 `common` 的 `AuthenticationFilter` 里，它不依赖 auth；
- 它需要「按 userId 查用户」这个能力，通过 `common` 定义的 **SPI 接口**（`LoginUserLoader`）拿到；
- **auth 恰好是那个接口的实现方**（`LoginUserLoaderImpl`）。

```
common 定义：  interface LoginUserLoader { LoginUser load(Long userId); }
auth  实现：  class LoginUserLoaderImpl implements LoginUserLoader { ... }
运行时：      AuthenticationFilter 通过 ObjectProvider 拿到 auth 的实现
```

> 💡 **这就是 02 篇自检问题 3 的答案**。`common` 不能依赖 `auth`（依赖方向反了），但运行时要用到 auth 的代码——SPI 是解这类僵局的标准手法。
>
> **为什么 auth 必须依赖 user 而不能反过来**：登录要读 `sys_user` 表验证密码，所以在数据上 auth → user 是天然方向。如果反过来（user 依赖 auth），就会出现 `user → auth → user` 的循环。

---

## ③ 只需精读这 2 个文件

`auth` 只有 9 个文件，是**最适合拿来练手的最小模块**——建议全看，其中这两个是核心。

### 精读 1：`auth/service/impl/AuthServiceImpl.java`（211 行）

**为什么是它**：双 token 的**完整生命周期**都在这里——三个方法 `login` / `refresh` / `logout`，共约 90 行有效代码。这是全项目安全机制的心脏。

读的时候带着一个问题：**「为什么每次刷新都要把旧的 refresh 作废？」**（答案在第 ⑤ 段）

### 精读 2：`auth/service/impl/LoginUserLoaderImpl.java`（38 行）

**为什么是它**：它是 ② 段那个 SPI 的落地实现，只有 20 行有效代码，但能让你彻底看懂「common 定义接口、业务模块提供实现」是怎么装配起来的。**看懂了这个模式，你就看懂了整个项目的解耦思路。**

> `AuthController`（72 行）也值得扫一眼，重点看两个 `@RateLimit`：登录按 IP 每分钟 10 次、刷新每分钟 30 次。这是防暴力破解和防刷新风暴的第一道防线。

---

## ④ 子包与方法一览

`auth` 模块 9 个文件（按行数排序）：

| 子包           | 文件                  | 行数 | 作用                                                 |
| -------------- | --------------------- | ---- | ---------------------------------------------------- |
| `service/impl` | `AuthServiceImpl`     | 211  | **核心**：登录 / 刷新 / 登出 / 当前用户信息          |
| `controller`   | `AuthController`      | 72   | 4 个接口：`/login` `/refresh` `/logout` `/me`        |
| `service`      | `AuthService`         | 25   | 上面 4 个方法的接口定义                              |
| `service/impl` | `LoginUserLoaderImpl` | 38   | SPI 实现：按 userId 加载登录用户                     |
| `vo`           | `UserInfoVO`          | 30   | `/auth/me` 的返回体                                  |
| `dto`          | `LoginDTO`            | 29   | 登录入参（账号 / 密码 / **clientType 端标识**）      |
| `vo`           | `TokenVO`             | 28   | 令牌返回体（accessToken / refreshToken / expiresIn） |
| `dto`          | `LogoutDTO`           | 18   | 登出入参（可选带 refreshToken）                      |
| `dto`          | `RefreshTokenDTO`     | 17   | 刷新入参                                             |

### `AuthServiceImpl` 的 4 个公开方法

| 方法                        | 做什么   | 关键动作                                                                 |
| --------------------------- | -------- | ------------------------------------------------------------------------ |
| `login(LoginDTO)`           | 登录     | 失败计数检查 → 校验密码 → 校验账号状态 → **校验端身份匹配** → 签发令牌对 |
| `refresh(RefreshTokenDTO)`  | 刷新令牌 | 校验类型 → 校验缓存中的 jti → 回查账号状态 → **旋转**（删旧签新）        |
| `logout(String, LogoutDTO)` | 登出     | access 的 jti **进黑名单**；refresh 的 jti **从缓存删除**                |
| `userInfo()`                | 当前用户 | 从 `UserContextHolder` 读（不查库）                                      |

### 两个私有方法（值得看）

| 方法                                      | 作用                                    | 为什么单独抽出来                                    |
| ----------------------------------------- | --------------------------------------- | --------------------------------------------------- |
| `issueTokens(userId, username, userType)` | 签发令牌对 + 把 refresh 的 jti 写进缓存 | login 和 refresh 都要用，抽出来保证两处行为完全一致 |
| `parseClientType(String)`                 | `clientType` 字符串 → 枚举              | 非法值必须**拒绝**，不能静默兜底（见第 ⑥ 段坑 3）   |

---

## ⑤ 关键代码拆解

### 拆解 1：一张时间线看懂双 token 的一生

这是本篇最值得带走的东西。假设 access 有效期 30 分钟、refresh 7 天：

| 时刻        | 发生了什么                                          | 缓存里的状态                                |
| ----------- | --------------------------------------------------- | ------------------------------------------- |
| T+0         | 用户登录 → 签发 access（30m）+ refresh（7d）        | `auth:refresh:{jti_A} = userId`             |
| T+0 ~ T+30m | 用户正常操作，每次请求带 access                     | 不变                                        |
| T+30m       | access 过期 → 前端自动调 `/auth/refresh`            | 删除 `jti_A`，写入 `jti_B`                  |
| T+30m       | 拿到新的 access + 新的 refresh                      | `auth:refresh:{jti_B} = userId`             |
| **T+31m**   | **如果攻击者拿着偷来的旧 refresh（`jti_A`）来刷新** | 缓存里**查不到** `jti_A` → 返回 `40102` ❌  |
| T+2h        | 用户点登出                                          | access 的 jti 写入 **黑名单**；删除 `jti_B` |
| T+2h 之后   | 攻击者拿这个 access 访问业务接口                    | 过滤器查到黑名单 → `401` ❌                 |
| T+2.5h      | 黑名单那条记录**自动过期**（TTL = 令牌剩余有效期）  | 黑名单自动瘦身                              |

**这一张表解释了双 token 的全部设计意图**：

- **access 短**（30 分钟）→ 泄露了也只有 30 分钟窗口，且能靠黑名单立刻掐断
- **refresh 长**（7 天）→ 用户不用天天登录
- **旋转**（每次刷新换新的）→ 把 refresh 泄露的窗口从「7 天」压缩到「一次使用」：攻击者一旦用过，真正的用户下次刷新就会失败，**异常立刻暴露**

### 拆解 2：旋转的 3 行代码

`AuthServiceImpl.refresh()` 的核心就这三步：

```java
// ① 先校验：缓存里必须存在这个 jti，且对应的 userId 与令牌里的 subject 一致
String cachedUserId = cacheClient.get(CacheKeys.AUTH_REFRESH + jti);
if (cachedUserId == null || !cachedUserId.equals(claims.getSubject())) {
    log.warn("刷新令牌无效或已被旋转 jti={}", jti);
    throw new UnauthorizedException(ResultCode.REFRESH_TOKEN_INVALID);   // 40102
}

// ② 回查账号状态：被停用/删除的账号不能靠刷新续命
UserAuthVO user = userApi.findAuthById(userId);
if (user == null || user.getStatus() == null || user.getStatus() != 1) {
    cacheClient.delete(CacheKeys.AUTH_REFRESH + jti);   // 顺手清理脏数据
    throw new UnauthorizedException(ResultCode.REFRESH_TOKEN_INVALID);
}

// ③ 旋转：删掉旧的，签发新的一对
cacheClient.delete(CacheKeys.AUTH_REFRESH + jti);
return issueTokens(userId, user.getUsername(), user.getUserType());
```

**为什么第 ① 步要对比 `userId`**：只判断「缓存里有这个 jti」不够。如果缓存 key 因某种原因被串改（或实现有 bug），对比一次 `subject` 能兜住。这是**纵深防御**的写法——安全代码不信任单一校验。

### 拆解 3：登出的黑名单 TTL 为什么这么算

```java
Claims claims = jwtUtil.parse(accessToken);
cacheClient.set(CacheKeys.AUTH_BLACKLIST + claims.getId(), "1", jwtUtil.remainingSeconds(claims));
//                                                                   ↑ TTL = 令牌剩余有效期
```

**如果 TTL 写死成 7 天会怎样**：每条登出记录都要占内存 7 天。1 万个用户每天登出一次，黑名单里就躺着 7 万条**永远不可能被命中的垃圾**（令牌 30 分钟后就过期了，黑名单里留着它毫无意义）。

**用「剩余有效期」做 TTL 的好处**：令牌一过期，黑名单记录**同时**过期，缓存自动瘦身、永不多占。

> 这个技巧值得记住：**做去重/黑名单时，TTL 应该等于「这个记录还有意义的时长」，而不是随便取个值。**

---

## ⑥ auth 特有的坑

**坑 1：refresh 为什么不走黑名单？**

直觉上会想「登出了，refresh 也该拉黑」。但 refresh **不需要黑名单**——因为它本身就是**有状态的**（jti 存在缓存里）。让它失效只需要 `delete` 那一条缓存记录，比「再维护一套黑名单」简单得多。

这也是为什么 `AuthenticationFilter` 里要先校验 `type == access` 再查黑名单（见 02 篇精读 1 的第 3 步）：refresh 的 jti 从来没进过黑名单，拿它去查永远是「查不到」，那就会**放行 refresh 去访问业务接口**。

**坑 2：登录时「先看失败计数，再查库」的顺序是有意的**

```java
// ① 先看失败计数：已锁定就不再查库、不再比对密码
Long failCount = toLong(cacheClient.get(failKey));
if (failCount != null && failCount >= MAX_LOGIN_FAIL) { throw ... }

// ② 再查账号 + 校验密码
UserAuthVO user = userApi.findAuthByUsername(username);
```

如果顺序反过来（先查库比对密码、再判断是否锁定），那么**锁定状态下每次请求仍会执行一次 BCrypt 比对**（每次约 50~100ms）。攻击者可以靠响应时间差异判断「这个账号存在且密码错」，等于把锁定机制变成了**账号探测器**。

**坑 3：`clientType` 非法值必须拒绝，不能兜底**

```java
} catch (IllegalArgumentException e) {
    throw new BusinessException(ResultCode.CLIENT_TYPE_NOT_ALLOWED);   // 正确：拒绝
}
```

想象一下写成「解析失败就当顾客」：那么商家账号只要传个乱码 `clientType`，就能从顾客端登录（如果后续校验也依赖这个值的话）——**一个静默兜底就等于开了一道后门**。安全相关的解析，宁可报错也不能兜底。

**坑 4：多端登录的边界在哪**

refresh 按 `jti` 独立存储，所以同一账号在 H5、商家端、后台同时登录**互不影响**，任意一端登出也不影响其他端。

但要注意：**access 是无状态的**。「在手机上登出」并不会让电脑上的 access 失效——除非你在手机上登出时把**电脑的**那个 access 的 jti 也拉黑（本项目没这么做，这是有意的：更符合用户预期）。

**坑 5：`CACHE_TYPE=local` 重启会踢人**

refresh 记录存在 `LocalCacheClient` 的内存里。**重启后端 = 所有用户的 refresh 记录消失 = 所有人被踢回登录页**。

开发期可以接受（重启本来就是开发常态），生产必须 `CACHE_TYPE=redis`。这也是「为什么要有 `CacheClient` 这个抽象」的现实理由之一。

**坑 6：前端必须读 HTTP 401 响应体里的 `code`**（真实踩过）

`accessToken` 过期时，后端返回的是 **HTTP 401 + 响应体 `code = 40101`**。业务码在**响应体**里，不在 HTTP 状态码上。

前端 axios 如果在错误分支里「一见 401 就清登录态跳登录页」，那么**自动续期永远不会触发**——用户每 30 分钟被踢一次，双 token 机制等于没做。

正确顺序：`error.response.data.code === 40101` → 静默刷新重放；其余 401 → 跳登录页。详见 `docs/验收文档.md` 第八节第 4 条。

---

## ⑦ 自检问题（不给答案）

**问题 1**：用户在 T+30 分钟时 access 过期，前端发起刷新。如果此时前端因为一个 bug，**同时发出了 8 个刷新请求**（都带着同一个 refreshToken），会发生什么？为什么说「有 7 个会失败」这件事**恰好说明旋转机制在正常工作**？

**问题 2**：`refresh` 过期时返回的是 `40102`，而 access 过期返回 `40101`。如果偷懒让 refresh 过期也返回 `40101`，前端会怎么做？会导致什么后果？

**问题 3**：假设现在要加一个需求——「用户修改密码后，所有设备都必须重新登录」。你需要动 `auth` 模块的哪些地方？现有设计里有没有已经能用的机制？（提示：想想 refresh 记录是怎么存的、能不能按 userId 一次性清掉它）

---

**上一篇** → `02-common-公共能力.md` ｜ **下一篇** → `04-order-订单.md`
