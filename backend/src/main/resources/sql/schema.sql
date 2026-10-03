-- =====================================================================
-- 外卖系统 建表脚本（MySQL 8，utf8mb4）
--
-- 约定：
--   1. 全部使用 CREATE TABLE IF NOT EXISTS，脚本可反复执行；
--   2. 索引一律写在 CREATE TABLE 内（MySQL 不支持 CREATE INDEX IF NOT EXISTS）；
--   3. 审计字段 create_by/create_time/update_by/update_time + 逻辑删除 del_flag 由
--      AuditMetaObjectHandler 自动填充，业务代码不手动赋值；
--   4. 金额统一 DECIMAL(10,2)，禁止 FLOAT/DOUBLE（浮点会丢精度）；
--   5. 不建外键约束：关联完整性由 Service 层保证，避免分库/归档时被外键锁死。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 统一账号表：三端（顾客/商家/管理员）共用一张表，用 user_type 区分
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user
(
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username        VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password        VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文（60 字符，留版本升级余量）',
    phone           VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    nickname        VARCHAR(50)           DEFAULT NULL COMMENT '昵称',
    avatar          VARCHAR(255)          DEFAULT NULL COMMENT '头像 URL',
    user_type       TINYINT      NOT NULL COMMENT '1 顾客 / 2 商家 / 3 管理员',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 / 0 停用',
    last_login_time DATETIME              DEFAULT NULL COMMENT '最后登录时间',
    create_by       BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_by       BIGINT                DEFAULT NULL COMMENT '更新人',
    update_time     DATETIME              DEFAULT NULL COMMENT '更新时间',
    del_flag        TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_phone (phone),
    KEY idx_user_type (user_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='统一账号表（三端共用）';

-- ---------------------------------------------------------------------
-- 2. 店铺表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS merchant
(
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT         NOT NULL COMMENT '店主账号 id（sys_user.id）',
    name            VARCHAR(50)    NOT NULL COMMENT '店铺名称',
    logo            VARCHAR(255)            DEFAULT NULL COMMENT '店铺 logo URL',
    description     VARCHAR(255)            DEFAULT NULL COMMENT '店铺简介',
    phone           VARCHAR(20)             DEFAULT NULL COMMENT '联系电话',
    address         VARCHAR(255)            DEFAULT NULL COMMENT '店铺地址',
    min_amount      DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '起送价',
    delivery_fee    DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '配送费',
    business_status TINYINT        NOT NULL DEFAULT 0 COMMENT '1 营业中 / 0 休息中',
    audit_status    TINYINT        NOT NULL DEFAULT 0 COMMENT '0 待审核 / 1 已通过 / 2 已驳回',
    audit_remark    VARCHAR(255)            DEFAULT NULL COMMENT '审核意见',
    rating          DECIMAL(3, 2)  NOT NULL DEFAULT 5.00 COMMENT '平均评分',
    sales           INT            NOT NULL DEFAULT 0 COMMENT '累计销量（完成的订单数）',
    create_by       BIGINT                  DEFAULT NULL,
    create_time     DATETIME                DEFAULT NULL,
    update_by       BIGINT                  DEFAULT NULL,
    update_time     DATETIME                DEFAULT NULL,
    del_flag        TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_audit_status (audit_status),
    KEY idx_business_status (business_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='店铺表';

-- ---------------------------------------------------------------------
-- 3. 菜品分类表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS category
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    merchant_id BIGINT       NOT NULL COMMENT '所属店铺',
    name        VARCHAR(30)  NOT NULL COMMENT '分类名称',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 禁用',
    create_by   BIGINT                DEFAULT NULL,
    create_time DATETIME              DEFAULT NULL,
    update_by   BIGINT                DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    del_flag    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_merchant_id (merchant_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='菜品分类表';

-- ---------------------------------------------------------------------
-- 4. 菜品表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dish
(
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    merchant_id BIGINT         NOT NULL COMMENT '所属店铺',
    category_id BIGINT         NOT NULL COMMENT '所属分类',
    name        VARCHAR(50)    NOT NULL COMMENT '菜品名称',
    image       VARCHAR(255)            DEFAULT NULL COMMENT '菜品图片 URL',
    description VARCHAR(255)            DEFAULT NULL COMMENT '菜品描述',
    price       DECIMAL(10, 2) NOT NULL COMMENT '单价',
    stock       INT            NOT NULL DEFAULT 0 COMMENT '库存（下单原子扣减，取消归还）',
    sales       INT            NOT NULL DEFAULT 0 COMMENT '销量（订单完成时累加）',
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '1 上架 / 0 下架',
    create_by   BIGINT                  DEFAULT NULL,
    create_time DATETIME                DEFAULT NULL,
    update_by   BIGINT                  DEFAULT NULL,
    update_time DATETIME                DEFAULT NULL,
    del_flag    TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_merchant_id (merchant_id),
    KEY idx_category_id (category_id),
    KEY idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='菜品表';

-- ---------------------------------------------------------------------
-- 5. 购物车表
--   注意 1：不存价格快照，结算时实时读取 dish 表重算金额（避免菜品改价后购物车金额不一致）
--   注意 2：本表刻意【不设 del_flag】。购物车行有唯一索引 uk_user_dish，
--          若用逻辑删除，「移除菜品→重新加入同一菜品」会插入新行并撞唯一键报 500。
--          购物车是临时数据、无需审计追溯，因此直接物理删除。
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_item
(
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT   NOT NULL COMMENT '顾客 id',
    merchant_id BIGINT   NOT NULL COMMENT '店铺 id',
    dish_id     BIGINT   NOT NULL COMMENT '菜品 id',
    quantity    INT      NOT NULL DEFAULT 1 COMMENT '数量',
    create_by   BIGINT            DEFAULT NULL,
    create_time DATETIME          DEFAULT NULL,
    update_by   BIGINT            DEFAULT NULL,
    update_time DATETIME          DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_dish (user_id, dish_id),
    KEY idx_user_id (user_id),
    KEY idx_user_merchant (user_id, merchant_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='购物车表';

-- ---------------------------------------------------------------------
-- 6. 收货地址表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS address
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '所属顾客',
    receiver    VARCHAR(50)  NOT NULL COMMENT '收货人',
    phone       VARCHAR(20)  NOT NULL COMMENT '收货电话',
    province    VARCHAR(30)           DEFAULT NULL COMMENT '省',
    city        VARCHAR(30)           DEFAULT NULL COMMENT '市',
    district    VARCHAR(30)           DEFAULT NULL COMMENT '区/县',
    detail      VARCHAR(255) NOT NULL COMMENT '详细地址',
    label       VARCHAR(20)           DEFAULT NULL COMMENT '标签：家/公司/学校',
    is_default  TINYINT      NOT NULL DEFAULT 0 COMMENT '1 默认地址 / 0 普通',
    create_by   BIGINT                DEFAULT NULL,
    create_time DATETIME              DEFAULT NULL,
    update_by   BIGINT                DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    del_flag    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='收货地址表';

-- ---------------------------------------------------------------------
-- 7. 订单主表
--   地址字段为下单时的快照：之后顾客改地址、删地址都不影响历史订单
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders
(
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no         VARCHAR(32)    NOT NULL COMMENT '订单号（唯一）',
    user_id          BIGINT         NOT NULL COMMENT '下单顾客',
    merchant_id      BIGINT         NOT NULL COMMENT '店铺 id',
    receiver         VARCHAR(50)    NOT NULL COMMENT '收货人（快照）',
    receiver_phone   VARCHAR(20)    NOT NULL COMMENT '收货电话（快照）',
    receiver_address VARCHAR(500)   NOT NULL COMMENT '收货地址全文（快照）',
    total_amount     DECIMAL(10, 2) NOT NULL COMMENT '菜品总额',
    delivery_fee     DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '配送费',
    pay_amount       DECIMAL(10, 2) NOT NULL COMMENT '实付金额 = 菜品总额 + 配送费',
    status           TINYINT        NOT NULL DEFAULT 10 COMMENT '10 待支付/20 待接单/30 备餐中/40 配送中/50 已完成/60 已取消/70 已退款',
    remark           VARCHAR(255)            DEFAULT NULL COMMENT '顾客备注',
    cancel_reason    VARCHAR(255)            DEFAULT NULL COMMENT '取消原因',
    pay_time         DATETIME                DEFAULT NULL COMMENT '支付时间',
    accept_time      DATETIME                DEFAULT NULL COMMENT '商家接单时间',
    finish_time      DATETIME                DEFAULT NULL COMMENT '完成时间',
    cancel_time      DATETIME                DEFAULT NULL COMMENT '取消时间',
    create_by        BIGINT                  DEFAULT NULL,
    create_time      DATETIME                DEFAULT NULL COMMENT '下单时间',
    update_by        BIGINT                  DEFAULT NULL,
    update_time      DATETIME                DEFAULT NULL,
    del_flag         TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_status (user_id, status),
    KEY idx_merchant_status (merchant_id, status),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单主表';

-- ---------------------------------------------------------------------
-- 8. 订单明细表
--   菜名/图片/单价全部快照落库：商家后续改名、改价、删图都不影响历史订单展示
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_item
(
    id         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id   BIGINT         NOT NULL COMMENT '订单 id',
    dish_id    BIGINT         NOT NULL COMMENT '菜品 id',
    dish_name  VARCHAR(50)    NOT NULL COMMENT '菜品名称（快照）',
    dish_image VARCHAR(255)            DEFAULT NULL COMMENT '菜品图片（快照）',
    price      DECIMAL(10, 2) NOT NULL COMMENT '下单时单价（快照）',
    quantity   INT            NOT NULL COMMENT '数量',
    amount     DECIMAL(10, 2) NOT NULL COMMENT '小计 = 单价 × 数量',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单明细表';

-- ---------------------------------------------------------------------
-- 9. 支付单表
--    order_id 用【唯一索引】而非普通索引：一个订单只能有一条支付单。
--    原因：PaymentServiceImpl.createIfAbsent 是「先按 orderId 查、没有就插入」，
--    这中间存在并发窗口，两个并发支付请求可能各插一条。
--    应用层靠订单状态的乐观锁拦住，这里再用数据库唯一约束兜底一层。
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment
(
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    pay_no      VARCHAR(32)    NOT NULL COMMENT '支付单号（唯一）',
    order_id    BIGINT         NOT NULL COMMENT '订单 id',
    user_id     BIGINT         NOT NULL COMMENT '付款顾客',
    amount      DECIMAL(10, 2) NOT NULL COMMENT '支付金额',
    pay_type    TINYINT        NOT NULL DEFAULT 1 COMMENT '1 模拟 / 2 微信 / 3 支付宝',
    status      TINYINT        NOT NULL DEFAULT 0 COMMENT '0 待支付 / 1 已支付 / 2 已退款',
    trade_no    VARCHAR(64)             DEFAULT NULL COMMENT '第三方流水号（模拟单为 MOCK 前缀）',
    pay_time    DATETIME                DEFAULT NULL COMMENT '支付时间',
    create_by   BIGINT                  DEFAULT NULL,
    create_time DATETIME                DEFAULT NULL,
    update_by   BIGINT                  DEFAULT NULL,
    update_time DATETIME                DEFAULT NULL,
    del_flag    TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_no (pay_no),
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='支付单表';

-- ---------------------------------------------------------------------
-- 10. 订单状态流转日志
--     状态机全程留痕：排查「订单为什么变成已取消」「谁拒的单」时唯一可信来源
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_status_log
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id      BIGINT       NOT NULL COMMENT '订单 id',
    from_status   TINYINT      NOT NULL COMMENT '原状态',
    to_status     TINYINT      NOT NULL COMMENT '新状态',
    operator_type TINYINT      NOT NULL COMMENT '1 顾客 / 2 商家 / 3 系统 / 4 管理员',
    operator_id   BIGINT                DEFAULT NULL COMMENT '操作人 id（系统操作可为空）',
    remark        VARCHAR(255)          DEFAULT NULL COMMENT '备注（取消原因 / 拒单原因）',
    create_time   DATETIME              DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单状态流转日志';