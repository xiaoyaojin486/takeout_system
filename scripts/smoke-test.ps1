# =====================================================================
#  外卖系统 · 接口冒烟测试脚本
#
#  用途：一条命令跑通「登录 → 下单 → 支付 → 商家接单出餐送达 → 统计」全链路，
#        以及双 token 旋转/黑名单、跨端拦截、越权拦截等安全校验。
#
#  前置条件：
#    1. 后端已启动（mvn spring-boot:run），端口 8080
#    2. .env 中的 MYSQL_PASSWORD 已填写
#    3. 数据库处于演示初始状态（脚本会真实下单，重复执行会累积测试订单）
#       想重置数据：DROP DATABASE takeout; 再重启后端
#
#  执行：powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
# =====================================================================

$ErrorActionPreference = 'Stop'
# 控制台按 UTF-8 输出，否则中文提示在 Windows 默认代码页下会变成乱码
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Base = 'http://localhost:8080/api'

$script:Pass = 0
$script:Fail = 0

function Check($name, $condition, $detail) {
    if ($condition) {
        $script:Pass++
        Write-Host "  [PASS] $name" -ForegroundColor Green
    } else {
        $script:Fail++
        Write-Host "  [FAIL] $name  -> $detail" -ForegroundColor Red
    }
}

function Section($title) {
    Write-Host ""
    Write-Host "=== $title ===" -ForegroundColor Cyan
}

# 以 UTF-8 读取响应流
# 不能直接用 $resp.Content：PowerShell 5.1 在响应头没有 charset 时会按 Latin-1 解码，
# 中文会变成「èåä¸è½ä¸ºç©º」这种乱码，后续按中文匹配的断言全部会失败。
function ReadUtf8($stream) {
    if ($null -eq $stream) { return $null }
    if ($stream.CanSeek) { $stream.Position = 0 }
    $ms = New-Object System.IO.MemoryStream
    $stream.CopyTo($ms)
    $bytes = $ms.ToArray()
    if ($bytes.Length -eq 0) { return $null }
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        return [System.Text.Encoding]::UTF8.GetString($bytes, 3, $bytes.Length - 3)
    }
    return [System.Text.Encoding]::UTF8.GetString($bytes)
}

# 统一请求封装：返回 @{ status = HTTP状态码; body = 解析后的 JSON }
function Api($method, $path, $body, $token, $extraHeaders) {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    if ($extraHeaders) { $extraHeaders.GetEnumerator() | ForEach-Object { $headers[$_.Key] = $_.Value } }

    $params = @{
        Method      = $method
        Uri         = "$Base$path"
        Headers     = $headers
        ContentType = 'application/json; charset=utf-8'
    }
    if ($null -ne $body) {
        $params['Body'] = ([System.Text.Encoding]::UTF8.GetBytes((($body | ConvertTo-Json -Depth 8 -Compress))))
    }

    try {
        $resp = Invoke-WebRequest @params -UseBasicParsing
        $text = ReadUtf8 $resp.RawContentStream
        return @{ status = [int]$resp.StatusCode; body = ($text | ConvertFrom-Json) }
    } catch {
        $status = 0
        $text = $null
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            $text = ReadUtf8 $_.Exception.Response.GetResponseStream()
        }
        $json = $null
        if ($text) { try { $json = $text | ConvertFrom-Json } catch { } }
        return @{ status = $status; body = $json; raw = $text }
    }
}

function Login($username, $password, $clientType) {
    return (Api 'POST' '/auth/login' @{ username = $username; password = $password; clientType = $clientType } $null)
}

Write-Host "外卖系统冒烟测试开始（$Base）" -ForegroundColor Yellow

# ---------------------------------------------------------------- A 认证
Section 'A. 认证与双 token'

$r = Login 'customer1' '123456' 'CUSTOMER'
Check 'A1 顾客登录成功' ($r.status -eq 200 -and $r.body.code -eq 200) "HTTP=$($r.status) code=$($r.body.code) msg=$($r.body.msg)"
if ($r.body.code -eq 15003) {
    Write-Host "  提示：命中登录限流（同 IP 10 次/分钟）。上一轮脚本刚跑完时请等 1 分钟再执行。" -ForegroundColor Yellow
}
$custAccess = $r.body.data.accessToken
$custRefresh = $r.body.data.refreshToken
Check 'A2 返回双 token' ($custAccess -and $custRefresh) 'accessToken/refreshToken 缺失'

$r = Api 'GET' '/auth/me' $null $custAccess
Check 'A3 /auth/me 返回顾客身份' ($r.body.data.userType -eq 1) "userType=$($r.body.data.userType)"

$r = Api 'GET' '/customer/cart' $null $null
Check 'A4 无 token 访问受保护接口 -> 401' ($r.status -eq 401 -and $r.body.code -eq 401) "HTTP=$($r.status) code=$($r.body.code)"

$r = Api 'GET' '/merchant/order' $null $custAccess
Check 'A5 顾客 token 访问商家端 -> 403 跨端拦截' ($r.body.code -eq 403) "code=$($r.body.code) msg=$($r.body.msg)"

$r = Api 'POST' '/auth/refresh' @{ refreshToken = $custAccess } $null
Check 'A6 用 accessToken 冒充 refresh -> 被拒' ($r.body.code -eq 401 -or $r.body.code -eq 40102) "code=$($r.body.code)"

$r = Api 'POST' '/auth/refresh' @{ refreshToken = $custRefresh } $null
Check 'A7 刷新成功（旋转）' ($r.body.code -eq 200 -and $r.body.data.accessToken) "code=$($r.body.code)"
$custAccess = $r.body.data.accessToken
$newRefresh = $r.body.data.refreshToken

$r = Api 'POST' '/auth/refresh' @{ refreshToken = $custRefresh } $null
Check 'A8 旧 refreshToken 复用 -> 40102 已作废' ($r.body.code -eq 40102) "code=$($r.body.code)"
$custRefresh = $newRefresh

$r = Login 'customer1' 'wrong-password' 'CUSTOMER'
Check 'A9 密码错误 -> 10001' ($r.body.code -eq 10001) "code=$($r.body.code)"

$r = Login 'customer1' '123456' 'MERCHANT'
Check 'A10 端身份不匹配 -> 10001' ($r.body.code -eq 10001) "code=$($r.body.code)"

# ---------------------------------------------------------------- B 下单
Section 'B. 顾客下单主链路'

$r = Api 'GET' '/customer/merchant/list?pageNum=1&pageSize=10' $null $custAccess
$merchantCount = $r.body.data.list.Count
Check 'B1 店铺列表可查' ($merchantCount -ge 2) "count=$merchantCount"
$shop = $r.body.data.list | Where-Object { $_.id -eq 1 }
Check 'B2 演示店铺「川味小厨」存在且在营业' ($shop -and $shop.businessStatus -eq 1) "shop=$($shop.name)"

$r = Api 'GET' '/customer/dish/menu?merchantId=1' $null $custAccess
$groups = $r.body.data
Check 'B3 菜单按分类分组返回' ($groups.Count -ge 2) "groups=$($groups.Count)"
$allDishes = $groups | ForEach-Object { $_.dishes }
# mainDish：宫保鸡丁 26 元，用于下单主流程
# cheapDish：麻婆豆腐 18 元，低于该店 20 元起送价，用于验证起送价拦截
$mainDish = $allDishes | Where-Object { $_.name -eq '宫保鸡丁' }
$cheapDish = $allDishes | Where-Object { $_.name -eq '麻婆豆腐' }
Check 'B3b 定位到演示菜品' ($mainDish -and $cheapDish) "main=$($mainDish.name) cheap=$($cheapDish.name)"
$stockBefore = $mainDish.stock

# 下架菜品拦截（dish.id=7 红油抄手 已在演示数据中设为下架）
$offShelf = ($groups | ForEach-Object { $_.dishes } | Where-Object { $_.id -eq 7 })
$r = Api 'GET' '/customer/dish/menu?merchantId=1' $null $custAccess
$allDishIds = ($r.body.data | ForEach-Object { $_.dishes } | ForEach-Object { $_.id })
Check 'B4 下架菜品不出现在顾客菜单' (-not ($allDishIds -contains 7)) "id=7 出现在菜单中"

# 地址：没有就建一条
$r = Api 'GET' '/customer/address' $null $custAccess
if ($r.body.data.Count -eq 0) {
    $null = Api 'POST' '/customer/address' @{
        receiver = '冒烟测试'; phone = '13900000009'; province = '四川省'; city = '成都市'
        district = '武侯区'; detail = '测试路 1 号'; label = '家'; isDefault = 1
    } $custAccess
    $r = Api 'GET' '/customer/address' $null $custAccess
}
$addressId = $r.body.data[0].id
Check 'B5 收货地址可用' ($addressId -gt 0) "addressId=$addressId"

# 加购物车
# 前置清空：B6 断言的是「购物车里只有这一个菜品」，如果上一次运行或手工操作留下过
# 同款菜品的购物车行，数量会被累加导致金额对不上。先清空让断言与初始状态无关。
$null = Api 'DELETE' '/customer/cart' $null $custAccess

$addResp = Api 'POST' '/customer/cart' @{ dishId = $mainDish.id; quantity = 1 } $custAccess
if ($addResp.body.code -ne 200) {
    Write-Host "  加购失败: code=$($addResp.body.code) msg=$($addResp.body.msg)" -ForegroundColor Yellow
}
$r = Api 'GET' '/customer/cart' $null $custAccess
if ($r.body.code -ne 200) {
    Write-Host "  购物车查询失败: code=$($r.body.code) msg=$($r.body.msg)" -ForegroundColor Yellow
}
$cartGroup = $r.body.data | Where-Object { $_.merchantId -eq 1 }
Check 'B6 购物车按店铺分组且金额=单价×数量' ($cartGroup -and $cartGroup.totalAmount -eq $mainDish.price) "total=$($cartGroup.totalAmount) price=$($mainDish.price) addCode=$($addResp.body.code) listCode=$($r.body.code)"

# 清掉购物车，避免影响后续断言
$null = Api 'DELETE' '/customer/cart' $null $custAccess

# 起送价拦截：只点一个低于起送价的菜（下单金额由后端重算，与购物车无关）
$r = Api 'POST' '/customer/order' @{
    addressId = $addressId; remark = '冒烟测试订单'
    items = @(@{ dishId = $cheapDish.id; quantity = 1 })
} $custAccess
Check 'B7 未达起送价 -> 14003 拦截' ($r.body.code -eq 14003) "code=$($r.body.code) msg=$($r.body.msg) cheapPrice=$($cheapDish.price) minAmount=$($shop.minAmount)"

# 跨店下单拦截
$r2 = Api 'GET' '/customer/dish/menu?merchantId=2' $null $custAccess
$otherDish = ($r2.body.data | Select-Object -First 1).dishes | Select-Object -First 1
$r = Api 'POST' '/customer/order' @{
    addressId = $addressId
    items = @(@{ dishId = $mainDish.id; quantity = 1 }, @{ dishId = $otherDish.id; quantity = 1 })
} $custAccess
Check 'B8 跨店铺下单 -> 13005 拦截' ($r.body.code -eq 13005) "code=$($r.body.code) msg=$($r.body.msg)"

# 先加入购物车：下单成功后应当被自动清除，B13 据此验证
$null = Api 'POST' '/customer/cart' @{ dishId = $mainDish.id; quantity = 2 } $custAccess
$null = Api 'POST' '/customer/cart' @{ dishId = $cheapDish.id; quantity = 1 } $custAccess

# 正常下单
$idem = [guid]::NewGuid().ToString()
$r = Api 'POST' '/customer/order' @{
    addressId = $addressId; remark = '冒烟测试订单'
    items = @(@{ dishId = $mainDish.id; quantity = 2 }, @{ dishId = $cheapDish.id; quantity = 1 })
} $custAccess @{ 'X-Idempotent-Token' = $idem }
Check 'B9 下单成功' ($r.body.code -eq 200 -and $r.body.data.orderNo) "code=$($r.body.code) msg=$($r.body.msg)"
$orderId = $r.body.data.orderId
$orderNo = $r.body.data.orderNo
$payAmount = $r.body.data.payAmount
$expectAmount = $mainDish.price * 2 + $cheapDish.price + $shop.deliveryFee
Check 'B10 金额由后端重算正确（菜品+配送费）' ($payAmount -eq $expectAmount) "实际=$payAmount 期望=$expectAmount"

$r = Api 'POST' '/customer/order' @{
    addressId = $addressId; remark = '冒烟测试订单'
    items = @(@{ dishId = $mainDish.id; quantity = 2 }, @{ dishId = $cheapDish.id; quantity = 1 })
} $custAccess @{ 'X-Idempotent-Token' = $idem }
Check 'B11 同一幂等令牌重复下单 -> 15002 拦截' ($r.body.code -eq 15002) "code=$($r.body.code)"

$r = Api 'GET' "/customer/dish/menu?merchantId=1" $null $custAccess
$stockAfter = (($r.body.data | ForEach-Object { $_.dishes } | Where-Object { $_.id -eq $mainDish.id })).stock
Check 'B12 下单后库存已扣减' ($stockAfter -eq ($stockBefore - 2)) "before=$stockBefore after=$stockAfter"

$r = Api 'GET' '/customer/cart' $null $custAccess
$cartLeft = ($r.body.data | ForEach-Object { $_.items } | Where-Object { $_.dishId -eq $mainDish.id -or $_.dishId -eq $cheapDish.id })
Check 'B13 已下单菜品从购物车移除（购物车清空）' (-not $cartLeft -and $r.body.data.Count -eq 0) "left=$($cartLeft.dishName) groups=$($r.body.data.Count)"

# 支付
$r = Api 'POST' "/customer/payment/$orderNo/pay" @{ payType = 1 } $custAccess
Check 'B14 模拟支付成功' ($r.body.code -eq 200 -and $r.body.data.status -eq 1) "code=$($r.body.code) msg=$($r.body.msg)"

$r = Api 'GET' "/customer/order/$orderId" $null $custAccess
Check 'B15 支付后订单状态 -> 20 待接单' ($r.body.data.order.status -eq 20) "status=$($r.body.data.order.status)"
Check 'B16 状态时间轴已记录流转' ($r.body.data.statusLogs.Count -ge 2) "logs=$($r.body.data.statusLogs.Count)"

$r = Api 'POST' "/customer/payment/$orderNo/pay" @{ payType = 1 } $custAccess
Check 'B17 已支付订单重复支付 -> 被拦截（15001 防连点先命中，或 14002 状态非法）' ($r.body.code -eq 15001 -or $r.body.code -eq 14002) "code=$($r.body.code) msg=$($r.body.msg)"

# ---------------------------------------------------------------- C 商家
Section 'C. 商家接单履约链路'

$r = Login 'merchant1' '123456' 'MERCHANT'
Check 'C1 商家登录成功' ($r.body.code -eq 200) "code=$($r.body.code)"
$merAccess = $r.body.data.accessToken

$r = Api 'GET' "/merchant/order/$orderId" $null $merAccess
Check 'C2 商家可查看自己店铺的订单' ($r.body.code -eq 200 -and $r.body.data.order.id -eq $orderId) "code=$($r.body.code)"

$r = Api 'PUT' "/merchant/order/$orderId/accept" $null $merAccess
Check 'C3 接单 -> 30 备餐中' ($r.body.code -eq 200) "code=$($r.body.code) msg=$($r.body.msg)"

$r = Api 'PUT' "/merchant/order/$orderId/deliver" $null $merAccess
Check 'C4 出餐 -> 40 配送中' ($r.body.code -eq 200) "code=$($r.body.code) msg=$($r.body.msg)"

$r = Api 'PUT' "/merchant/order/$orderId/finish" $null $merAccess
Check 'C5 送达 -> 50 已完成' ($r.body.code -eq 200) "code=$($r.body.code) msg=$($r.body.msg)"

$r = Api 'PUT' "/merchant/order/$orderId/accept" $null $merAccess
Check 'C6 已完成订单再次接单 -> 14002 非法流转拦截' ($r.body.code -eq 14002) "code=$($r.body.code)"

# 用 payType=2 让请求指纹与 B14 不同，从而绕过 3 秒防连点窗口，真正打到状态机校验
$r = Api 'POST' "/customer/payment/$orderNo/pay" @{ payType = 2 } $custAccess
Check 'C6b 已完成订单再次支付 -> 14002 状态机拦截' ($r.body.code -eq 14002) "code=$($r.body.code) msg=$($r.body.msg)"

$r = Api 'GET' '/merchant/stats/overview' $null $merAccess
Check 'C7 商家概览今日订单数 >= 1' ($r.body.data.todayOrderCount -ge 1) "today=$($r.body.data.todayOrderCount)"

$r = Api 'GET' '/merchant/stats/trend?days=7' $null $merAccess
Check 'C8 趋势图返回 7 个点且已补零' ($r.body.data.Count -eq 7) "points=$($r.body.data.Count)"

$r = Api 'GET' '/merchant/stats/hot-dish?days=30&limit=5' $null $merAccess
Check 'C9 热销菜品有数据' ($r.body.data.Count -ge 1) "count=$($r.body.data.Count)"

# 商家越权：merchant2 操作 merchant1 的订单
$r = Login 'merchant2' '123456' 'MERCHANT'
$mer2Access = $r.body.data.accessToken
$r = Api 'PUT' "/merchant/order/$orderId/accept" $null $mer2Access
Check 'C10 商家B 操作商家A 的订单 -> 14004 越权拦截' ($r.body.code -eq 14004) "code=$($r.body.code)"

# ---------------------------------------------------------------- D 管理端
Section 'D. 平台管理后台'

$r = Login 'admin' '123456' 'ADMIN'
Check 'D1 管理员登录成功' ($r.body.code -eq 200) "code=$($r.body.code)"
$adminAccess = $r.body.data.accessToken

$r = Api 'GET' '/admin/stats/overview' $null $adminAccess
Check 'D2 平台看板数据可用' ($r.body.data.merchantCount -ge 2 -and $r.body.data.customerCount -ge 1) "merchant=$($r.body.data.merchantCount) customer=$($r.body.data.customerCount)"

$r = Api 'GET' '/admin/merchant?auditStatus=0&pageNum=1&pageSize=10' $null $adminAccess
Check 'D3 可按「待审核」筛选店铺列表' ($r.body.code -eq 200 -and $null -ne $r.body.data.total) "code=$($r.body.code) total=$($r.body.data.total)"

# 断言写成与初始状态无关：无论店铺 3 之前是否已通过，审核后都必须出现在「已通过」列表
$r = Api 'PUT' '/admin/merchant/3/audit' @{ auditStatus = 1; auditRemark = '冒烟测试审核通过' } $adminAccess
Check 'D4 审核通过店铺' ($r.body.code -eq 200) "code=$($r.body.code) msg=$($r.body.msg)"

$r = Api 'GET' '/admin/merchant?auditStatus=1&pageNum=1&pageSize=10' $null $adminAccess
$audited = $r.body.data.list | Where-Object { $_.id -eq 3 }
Check 'D5 审核后店铺出现在「已通过」列表' ($audited -and $audited.auditStatus -eq 1) "auditStatus=$($audited.auditStatus)"

# 顾客端店铺列表的过滤条件必须只有一个：auditStatus = 1
# （营业中与否不影响是否展示，休息中的店也要能看到并显示「休息中」，只是不能下单）
$r = Api 'PUT' '/admin/merchant/3/audit' @{ auditStatus = 2; auditRemark = '冒烟测试驳回' } $adminAccess
Check 'D5b 驳回店铺成功' ($r.body.code -eq 200) "code=$($r.body.code)"

$r = Api 'GET' '/customer/merchant/list?pageNum=1&pageSize=20' $null $custAccess
$rejected = $r.body.data.list | Where-Object { $_.id -eq 3 }
Check 'D5c 已驳回店铺不出现在顾客端列表' (-not $rejected) "仍出现: $($rejected.name)"

# 恢复为已通过，避免影响后续手工验收的演示数据
$null = Api 'PUT' '/admin/merchant/3/audit' @{ auditStatus = 1; auditRemark = '冒烟测试审核通过' } $adminAccess

$r = Api 'GET' '/admin/order?pageNum=1&pageSize=10' $null $adminAccess
Check 'D6 平台订单总览可查' ($r.body.data.total -ge 1) "total=$($r.body.data.total)"

$r = Api 'PUT' '/admin/user/1/status' @{ status = 0 } $adminAccess
Check 'D7 停用管理员账号 -> 被拒（防锁死平台）' ($r.body.code -eq 403) "code=$($r.body.code)"

$r = Api 'GET' '/customer/cart' $null $adminAccess
Check 'D8 管理员 token 访问顾客端 -> 403 跨端拦截' ($r.body.code -eq 403) "code=$($r.body.code)"

# ---------------------------------------------------------------- E 数据权限
Section 'E. 数据权限（横向越权）'

$newUser = "smoke$((Get-Random -Maximum 99999))"
$r = Api 'POST' '/customer/user/register' @{
    username = $newUser; password = '123456'; phone = "139$((Get-Random -Minimum 10000000 -Maximum 99999999))"; nickname = '冒烟顾客'
} $null
Check 'E1 顾客注册成功（BCrypt 加密落库）' ($r.body.code -eq 200) "code=$($r.body.code) msg=$($r.body.msg)"

$r = Login $newUser '123456' 'CUSTOMER'
$otherAccess = $r.body.data.accessToken

$r = Api 'GET' "/customer/order/$orderId" $null $otherAccess
Check 'E2 顾客B 查看顾客A 的订单 -> 14004 越权拦截' ($r.body.code -eq 14004) "code=$($r.body.code)"

$r = Api 'GET' '/customer/order?pageNum=1&pageSize=10' $null $otherAccess
Check 'E3 顾客B 只能看到自己的订单（列表为空）' ($r.body.data.total -eq 0) "total=$($r.body.data.total)"

# ---------------------------------------------------------------- F 登出黑名单
Section 'F. 登出与黑名单'

# 复用 A 段拿到的令牌对，不再多打一次登录接口
# （登录接口有同 IP 10 次/分钟限流，省一次就能明显降低连续跑脚本时被限流的概率）
$tAccess = $custAccess
$tRefresh = $custRefresh

$r = Api 'POST' '/auth/logout' @{ refreshToken = $tRefresh } $tAccess
Check 'F1 登出成功' ($r.body.code -eq 200) "code=$($r.body.code)"

$r = Api 'GET' '/auth/me' $null $tAccess
Check 'F2 登出后原 accessToken 被拉黑 -> 401' ($r.status -eq 401 -and $r.body.code -eq 401) "HTTP=$($r.status) code=$($r.body.code)"

$r = Api 'POST' '/auth/refresh' @{ refreshToken = $tRefresh } $null
Check 'F3 登出后原 refreshToken 失效 -> 40102' ($r.body.code -eq 40102) "code=$($r.body.code)"

# ---------------------------------------------------------------- 汇总
Write-Host ""
Write-Host "======================================" -ForegroundColor Yellow
Write-Host "  通过 $script:Pass 项 / 失败 $script:Fail 项" -ForegroundColor $(if ($script:Fail -eq 0) { 'Green' } else { 'Red' })
Write-Host "======================================" -ForegroundColor Yellow
if ($script:Fail -gt 0) { exit 1 }