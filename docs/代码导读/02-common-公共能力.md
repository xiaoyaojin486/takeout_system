# 02 · common 公共能力

> 试点模块篇。**先读 `00-总览-先读这篇.md` 和 `01-注解与语法专题.md`**，否则这篇里的注解会让你卡住。
>
> 本篇结构固定 7 段：① 是什么 → ② 在全局的位置 → ③ 只需精读这 2 个文件 → ④ 方法与子包一览 → ⑤ 关键代码拆解 → ⑥ 特有的坑 → ⑦ 自检问题

---

## ① common 是什么

`common` = **公共的**。

它**不是业务模块**，是地基。打个比方：12 个业务模块是 12 家店铺，`common` 是水电煤和物业管理。

**它解决什么问题**：12 个模块都需要做同样四件事——统一返回格式、统一处理异常、知道「当前请求是谁发的」、读写缓存。如果每个模块各写一遍，就会出现 12 种返回格式、12 种异常处理，前端没法对接。

> 判断一个类该不该放在 `common`：**它跟具体业务无关吗？** `Result`（统一返回体）跟外卖业务无关 → 放 common。`OrderStatusEnum`（订单状态）虽然放在 common，但严格说它是订单域的——这是本项目的一处小妥协，见第 ⑥ 段。

---

## ② 在全局的位置

```
  12 个业务模块
        ↓  都可以用 common
   ┌─────────────┐
   │   common    │  ← 它不依赖任何一个业务模块
   └─────────────┘
        ↓
   （没有任何下游）
```

**事实核对**：全项目 **66 个文件、176 处** `import com.takeout.common.*`。它是被引用最多的地方，跳过它就等于每读一段代码都要卡一次。

### 这里有一个值得学的设计

`common` 需要在鉴权后知道「当前登录用户是谁」（名字、身份）。但这个信息在 `sys_user` 表里，属于 `user` 模块——而 `common` **不允许**依赖 `user` 模块（否则依赖方向就破了）。

怎么解决？看 `common/context/LoginUserLoader.java`：

```java
public interface LoginUserLoader {
    LoginUser load(Long userId);
}
```

**`common` 只定义接口，不写实现**。真正的实现放在 `auth` 模块（`LoginUserLoaderImpl`），启动时由 Spring 注入进来。

```
common 说：「我需要一个能按 userId 查用户的东西」   → 定义 LoginUserLoader 接口
auth  答：「我来提供，我去查 user 模块」            → LoginUserLoaderImpl
```

这种写法叫 **SPI（Service Provider Interface）**。它让 `common` 保持「零业务依赖」，同时又能拿到需要的数据。

> **为什么值得记**：这是解「循环依赖」的标准手法。你会经常遇到「A 需要 B 的数据，但 B 又需要 A」的僵局，答案就是把接口和实现分开放。

---

## ③ 只需精读这 2 个文件

`common` 有 49 个文件，**不要全读**。这两个读完，common 的价值就拿到了 80%。

### 精读 1：`common/filter/AuthenticationFilter.java`

**为什么是它**：每个 HTTP 请求（白名单除外）都必须经过它。读完你就彻底明白「系统是怎么知道你是谁」。

**读的时候盯住这 6 步的顺序**——这 6 步的顺序本身就是设计：

| 步  | 做什么                                  | 为什么必须在这个位置                                                     |
| --- | --------------------------------------- | ------------------------------------------------------------------------ |
| 1   | 判断白名单                              | 必须最先。否则登录接口自己就被拦了                                       |
| 2   | 从 `Authorization` 头解析 token         | —                                                                        |
| 3   | 校验 `type == access`                   | 在查黑名单之前。refresh 令牌不走黑名单机制，若先查黑名单等于放行 refresh |
| 4   | 查 `jti` 是否在黑名单                   | 只有 access 需要查                                                       |
| 5   | 通过 `LoginUserLoader` 加载用户         | 放最后，避免为无效令牌做无谓的查库                                       |
| 6   | 存进 `UserContextHolder`（ThreadLocal） | 业务层才能用 `UserContextHolder.getUserId()`                             |

### 精读 2：`common/util/JwtUtil.java`

**为什么是它**：双 token 是本项目最核心的安全机制，`JwtUtil` 是它的实现。全文 121 行，读完你能回答：

- access token 和 refresh token 有什么区别？（`type` 这个 claim）
- 令牌过期时，为什么有时返回 `40101`、有时返回 `40102`？这两个码决定了前端**自动续期**还是**跳登录页**
- 为什么密钥必须 ≥ 32 字符？（不够长直接启动失败，这是故意的）

> `GlobalExceptionHandler`（异常统一转 JSON）也应该看一眼，但它只是 12 个 `@ExceptionHandler` 方法，读起来没有难度，不用当精读对象。
>
> 双 token 的**完整生命周期**（登录 → 刷新旋转 → 登出拉黑）在 `auth` 模块，会写在下一篇 `03-auth` 里。common 这里只管「生成令牌」和「解析令牌」。

---

## ④ 子包与方法一览

### 先认识子包名字

你反馈「模块的英文单词不懂」——`common` 内部也有 16 个子包，含义如下：

| 子包          | 中文     | 放什么                         |
| ------------- | -------- | ------------------------------ |
| `result`      | 结果     | 统一返回体、错误码、分页返回体 |
| `page`        | 分页     | 分页查询参数                   |
| `exception`   | 异常     | 自定义异常 + 全局异常处理器    |
| `context`     | 上下文   | 当前登录用户（ThreadLocal）    |
| `filter`      | 过滤器   | 鉴权第一道闸                   |
| `interceptor` | 拦截器   | 端级授权第二道闸               |
| `cache`       | 缓存     | 缓存抽象 + 两种实现            |
| `util`        | 工具     | JWT、JSON、IP、单号生成        |
| `annotation`  | 注解     | 三个自定义注解                 |
| `aspect`      | 切面     | 上面注解的实现                 |
| `config`      | 配置     | Spring 装配                    |
| `properties`  | 配置属性 | 绑定 yml/.env 的配置类         |
| `constant`    | 常量     | 常量集合                       |
| `enums`       | 枚举     | 字典（用户类型、订单状态…）    |
| `entity`      | 实体     | 数据库表基类                   |
| `validation`  | 校验     | 校验分组（⚠️ 见下）            |

### 关键类与方法

| 子包        | 类                                         | 关键方法                         | 作用                                   | 被谁调用                         |
| ----------- | ------------------------------------------ | -------------------------------- | -------------------------------------- | -------------------------------- |
| result      | `Result`                                   | `ok()` / `fail()`                | 所有接口的统一返回格式                 | 全部 Controller                  |
| result      | `PageResult`                               | `of(page, mapper)`               | 分页返回体（第三个参数是转换函数）     | 各 Service                       |
| result      | `ResultCode`                               | 枚举                             | 全部错误码（`14002` 订单状态不允许…）  | 全局                             |
| page        | `PageQuery`                                | `toPage()`                       | 分页参数，并强制 pageSize ≤ 100        | 所有 XxxQuery                    |
| exception   | `BusinessException`                        | 构造器                           | 可预期的业务失败                       | 全部 Service                     |
| exception   | `GlobalExceptionHandler`                   | 12 个 `@ExceptionHandler`        | 任意异常 → 统一 JSON                   | Spring 自动调用                  |
| context     | `UserContextHolder`                        | `getUserId()` / `requireLogin()` | 取当前登录用户                         | 全部 Service                     |
| context     | `LoginUserLoader`                          | `load(userId)`                   | SPI 接口（**只有接口，实现不在这里**） | AuthenticationFilter             |
| filter      | `AuthenticationFilter`                     | `doFilterInternal()`             | 鉴权第一道闸                           | 每个请求                         |
| interceptor | `UserTypeInterceptor`                      | `preHandle()`                    | 按路径前缀卡身份                       | `/customer` `/merchant` `/admin` |
| util        | `JwtUtil`                                  | `generateAccessToken` / `parse`  | 双 token 生成与解析                    | AuthServiceImpl、过滤器          |
| util        | `OrderNoGenerator`                         | `nextOrderNo()` / `nextPayNo()`  | 单号生成（日期+序号+随机）             | order、payment                   |
| util        | `JsonUtil`                                 | `toJson()`                       | 过滤器里手写 JSON 响应                 | 过滤器、切面                     |
| util        | `IpUtil`                                   | `getClientIp()`                  | 限流时取客户端 IP                      | RateLimitAspect                  |
| cache       | `CacheClient`                              | `set/get/delete/increment`       | 缓存抽象（接口）                       | auth、order、三个切面            |
| cache       | `LocalCacheClient`                         | 同上 + `cleanExpired()`          | 内存实现（默认）                       | 默认启用                         |
| cache       | `RedisCacheClient`                         | 同上                             | Redis 实现                             | `CACHE_TYPE=redis` 时            |
| annotation  | `@RateLimit` `@RepeatSubmit` `@Idempotent` | —                                | 三个自定义注解                         | Controller 方法上                |
| aspect      | `RateLimitAspect` 等 3 个                  | —                                | 上面注解的实现                         | Spring AOP                       |
| config      | `WebConfig`                                | 注册过滤器/拦截器/CORS/静态资源  | Web 装配                               | 启动                             |
| config      | `AuditMetaObjectHandler`                   | `insertFill()` / `updateFill()`  | 自动填 `create_time` 等审计字段        | 每次 insert/update               |
| config      | `PasswordConfig`                           | `passwordEncoder()`              | BCrypt 加密器 Bean                     | user、auth                       |
| config      | `MybatisPlusConfig`                        | 分页插件 + 全表更新拦截          | 防误操作                               | 启动                             |
| properties  | 5 个 `XxxProperties`                       | —                                | 绑定 `takeout.*` 配置                  | 各处                             |
| constant    | `SecurityConstants` / `CacheKeys`          | —                                | 常量（请求头名、缓存 key 前缀）        | 各处                             |
| enums       | `UserTypeEnum` / `OrderStatusEnum` 等 5 个 | `of()` / `canTransfer()`         | 字典与状态机流转表                     | 各处                             |
| entity      | `BaseEntity`                               | —                                | 实体基类（id + 审计字段 + delFlag）    | 所有实体继承                     |

### ⚠️ 三个「零引用」的类（别浪费时间找它们在哪用）

我核查过全项目引用情况：

| 类                         | 状态         | 说明                                                                            |
| -------------------------- | ------------ | ------------------------------------------------------------------------------- |
| `constant/CommonConstants` | **未被使用** | 预留的常量（管理员 id、状态值等）。当前代码里都是直接写 `1` / `0`。你可以忽略它 |
| `validation/AddGroup`      | **未被使用** | 校验分组预留。项目里「新增」和「修改」共用同一个 DTO，还没遇到必须分组的场景    |
| `validation/UpdateGroup`   | **未被使用** | 同上                                                                            |

> 为什么要专门说这个？因为读代码时看到一个类**没有任何引用**，很容易陷入「是不是我漏了什么」的自我怀疑。直接告诉你：这三个就是没用到，跳过即可。

---

## ⑤ 关键代码拆解

### 拆解 1：`AuthenticationFilter.doFilterInternal()` 的异常分类

```java
} catch (UnauthorizedException e) {
    writeUnauthorized(response, e.getResultCode());   // ← 不同的 ResultCode → 不同的 code
} finally {
    UserContextHolder.clear();                        // ← 必须清理！
}
```

`JwtUtil.parse()` 会根据**令牌类型**抛出不同的错误码：

```java
} catch (ExpiredJwtException e) {
    Object type = e.getClaims().get(SecurityConstants.CLAIM_TYPE);
    throw new UnauthorizedException(SecurityConstants.TOKEN_TYPE_REFRESH.equals(type)
            ? ResultCode.REFRESH_TOKEN_INVALID    // 40102：refresh 过期 → 前端跳登录页
            : ResultCode.ACCESS_TOKEN_EXPIRED);   // 40101：access 过期 → 前端静默续期
}
```

**为什么必须区分这两个码**：它们对应前端完全不同的动作。

- `40101` → 前端拦截器**静默调 `/auth/refresh` 换新令牌，然后重放原请求**。用户无感知。
- `40102` → refresh 也废了，只能**清空登录态、跳登录页**。

如果不区分（都返回 `401`），前端就只能一刀切跳登录页——那 access 30 分钟的短有效期就变成了「每 30 分钟被踢一次」，双 token 机制等于白做。

> **注意**：这两个码是通过 **HTTP 401 + 响应体里的 `code`** 返回的，业务码在响应体里、不在 HTTP 状态码上。前端如果在 axios 的 error 分支里「一见 401 就跳登录」就错了。这个坑开发时真实踩过，详见 `docs/验收文档.md` 第八节。

### 拆解 2：`UserContextHolder` 为什么用 ThreadLocal

```java
private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

public static void set(LoginUser loginUser) { HOLDER.set(loginUser); }
public static void clear() { HOLDER.remove(); }
```

**ThreadLocal 的作用**：让「当前登录用户」在同一个线程内随处可取，而不用一层层往下传参数。

```
AuthenticationFilter 里 set() ──→ Service 里 getUserId() 拿到的就是同一个用户
```

**为什么一定要 `clear()`**：Tomcat 处理请求用的是**线程池**，线程会被复用。

```
请求A（张三）用完线程 1 → 没清理
请求B（李四）复用线程 1 → 读到残留的张三角色 → 越权！
```

所以 `finally { UserContextHolder.clear(); }` **不是可选的优化，是安全底线**。

---

## ⑥ common 特有的坑

**坑 1：逻辑删除的开关在配置文件里，不在注解上**

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: delFlag
```

只要实体有 `delFlag` 字段，MyBatis-Plus 就自动把 `delete` 变成 `update del_flag = 1`。

**后果**：实体**有没有** `delFlag` 字段，决定了删除是「打个标记」还是「真删」。

这不是理论问题——`cart_item` 表就是因此出过 bug：唯一索引 `(user_id, dish_id)` + 逻辑删除 → 「移除菜品后再加同一菜品」插入新行时撞唯一键 → 接口报 500。最后把购物车实体改成**不继承 `BaseEntity`**（所以没有 `delFlag` → 真删）才解决。详见 `docs/验收文档.md` 第八节第 2 条。

**坑 2：`@Mapper` 有两个来源**

```java
import org.apache.ibatis.annotations.Mapper;   // MyBatis：管数据库
import org.mapstruct.Mapper;                   // MapStruct：管对象转换
```

同名不同库。看到 `@Mapper` 先看 import。

**坑 3：`AuthenticationFilter` 是手动注册的，不是 `@Component`**

```java
// WebConfig.java
@Bean
public FilterRegistrationBean<AuthenticationFilter> authenticationFilterRegistration() {
    registration.setFilter(new AuthenticationFilter(...));   // 手动 new
}
```

**为什么不直接标 `@Component`**：Spring Boot 会把 `Filter` 类型的 Bean 自动注册到所有请求上，如果同时又在 `WebConfig` 里注册一次，过滤器就**执行两次**（令牌被解析两遍、上下文被清两遍）。手动 `new` + 显式注册，可以精确控制执行顺序和路径。

**坑 4：`LocalCacheClient` 只能单机用**

它把数据存在 `ConcurrentHashMap` 里。开发期很方便（不用装 Redis），但**多实例部署时会出问题**：

```
用户在实例A登出 → 黑名单记在实例A的内存里
下一个请求被负载均衡打到实例B → 实例B不知道这个令牌已被拉黑 → 放行！
```

所以生产多副本必须 `CACHE_TYPE=redis`。这是配置项存在的意义。

**坑 5：`OrderStatusEnum` 放在 common 里，但它其实是订单域的**

严格按「common 不含业务」的规则，订单状态应该放在 `order` 模块。放在 common 是为了让 `Frontend` 之外的模块（如 payment）也能用它的状态判断，避免 payment 反向依赖 order。**这是一处有意的妥协**，知道就好。

---

## ⑦ 自检问题（不给答案）

读完这篇，试着回答下面三个问题。**答不出来说明还没读透**，回去看第 ③ 段指定的两个文件。

**问题 1**：一个**没带任何 token** 的请求打到 `GET /api/customer/cart`，会依次经过哪些类？分别在什么条件下返回什么错误码？（提示：白名单 → 过滤器 → 拦截器，注意拦截器其实不会被执行到）

**问题 2**：用户点「退出登录」之后，他刚才那个 accessToken 为什么立刻就不能用了？这个「失效记录」存在哪里、什么时候会被清理掉？如果把 `CACHE_TYPE` 从 `local` 改成 `redis`，这个机制的行为会有什么不同？

**问题 3**：`common` 里定义了 `LoginUserLoader` 接口，但**故意不写实现类**。为什么？如果直接在 `common` 里写个实现去查 `sys_user` 表，会破坏哪一条架构规则？会带来什么后果？

---

**上一篇** → `01-注解与语法专题.md` ｜ **下一篇** → `03-auth-认证.md`（双 token 完整生命周期）

---

## 附：这份试点文档的写作约定

如果你觉得这个风格可以，后续模块篇会沿用同样的 7 段结构：

1. 这个模块是干什么的
2. 它在全局的位置（依赖谁、被谁依赖）
3. **只需精读的 2 个文件** + 为什么是它们
4. 子包与关键方法一览（含「零引用」标注）
5. 1~2 处关键代码逐行拆解
6. 这个模块特有的坑
7. 3 个自检问题

**如果你希望调整**（比如想要更详细的方法级说明、或者更短的篇幅、或者加时序图），告诉我，我会在写 `03-auth` 之前改成你想要的样式。
