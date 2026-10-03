-- =====================================================================
-- 外卖系统 演示数据（幂等：可反复执行，不会覆盖你手动改过的数据）
--
-- 演示账号密码统一为 123456，password 列存的是 BCrypt 密文
--   $2a$10$kN9zta3d4fzCXlJ4OhFXUuZWYu7SWvNYQ7zCwy2KBnv.6x6DBuafK
-- （strength=10，已用 BCryptPasswordEncoder.matches("123456", hash) 验证通过）
--
--   账号        密码      身份            对应店铺
--   admin       123456    管理员(3)       —
--   merchant1   123456    商家(2)         merchant.id=1 川味小厨
--   merchant2   123456    商家(2)         merchant.id=2 轻食沙拉铺
--   merchant3   123456    商家(2)         merchant.id=3 待审核烧烤摊（用于验证审核流程）
--   customer1   123456    顾客(1)         —
--
-- id 固定写死，配合 ON DUPLICATE KEY UPDATE 保证幂等；
-- 注意 UPDATE 部分必须写成 id=id（空更新），否则会把你改过的店铺名/价格覆盖回初始值。
-- =====================================================================

-- ---------------------- 账号 ----------------------
INSERT INTO sys_user (id, username, password, phone, nickname, avatar, user_type, status, create_time, update_time, del_flag)
VALUES (1, 'admin', '$2a$10$kN9zta3d4fzCXlJ4OhFXUuZWYu7SWvNYQ7zCwy2KBnv.6x6DBuafK', '13700000001', '平台管理员', NULL, 3, 1, NOW(), NOW(), 0),
       (2, 'merchant1', '$2a$10$kN9zta3d4fzCXlJ4OhFXUuZWYu7SWvNYQ7zCwy2KBnv.6x6DBuafK', '13700000002', '川味小厨老板', NULL, 2, 1, NOW(), NOW(), 0),
       (3, 'merchant2', '$2a$10$kN9zta3d4fzCXlJ4OhFXUuZWYu7SWvNYQ7zCwy2KBnv.6x6DBuafK', '13700000003', '沙拉铺老板', NULL, 2, 1, NOW(), NOW(), 0),
       (4, 'merchant3', '$2a$10$kN9zta3d4fzCXlJ4OhFXUuZWYu7SWvNYQ7zCwy2KBnv.6x6DBuafK', '13700000004', '烧烤摊老板', NULL, 2, 1, NOW(), NOW(), 0),
       (5, 'customer1', '$2a$10$kN9zta3d4fzCXlJ4OhFXUuZWYu7SWvNYQ7zCwy2KBnv.6x6DBuafK', '13900000001', '小明', NULL, 1, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE id = id;

-- ---------------------- 店铺 ----------------------
INSERT INTO merchant (id, user_id, name, logo, description, phone, address, min_amount, delivery_fee,
                      business_status, audit_status, audit_remark, rating, sales, create_time, update_time, del_flag)
VALUES (1, 2, '川味小厨', NULL, '地道川菜，麻辣鲜香，现炒现做', '13800000001', '成都市武侯区天府大道 1 号', 20.00, 3.00,
        1, 1, '资料齐全，审核通过', 4.80, 0, NOW(), NOW(), 0),
       (2, 3, '轻食沙拉铺', NULL, '低卡轻食，健康每一天', '13800000002', '成都市高新区天府三街 88 号', 15.00, 2.00,
        1, 1, '资料齐全，审核通过', 4.50, 0, NOW(), NOW(), 0),
       (3, 4, '待审核烧烤摊', NULL, '深夜烧烤，等待平台审核', '13800000003', '成都市锦江区春熙路 5 号', 30.00, 5.00,
        0, 0, NULL, 5.00, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE id = id;

-- ---------------------- 菜品分类 ----------------------
INSERT INTO category (id, merchant_id, name, sort, status, create_time, update_time, del_flag)
VALUES (1, 1, '招牌热菜', 1, 1, NOW(), NOW(), 0),
       (2, 1, '凉菜小吃', 2, 1, NOW(), NOW(), 0),
       (3, 2, '沙拉主餐', 1, 1, NOW(), NOW(), 0),
       (4, 2, '鲜榨饮品', 2, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE id = id;

-- ---------------------- 菜品 ----------------------
-- 说明：dish.id=7 红油抄手 故意设为下架状态，用于验证「下架菜品不可下单」
INSERT INTO dish (id, merchant_id, category_id, name, image, description, price, stock, sales, status,
                  create_time, update_time, del_flag)
VALUES (1, 1, 1, '水煮牛肉', NULL, '嫩牛肉配秘制红油，麻辣过瘾', 38.00, 100, 0, 1, NOW(), NOW(), 0),
       (2, 1, 1, '宫保鸡丁', NULL, '花生鸡丁，酸甜微辣', 26.00, 100, 0, 1, NOW(), NOW(), 0),
       (3, 1, 1, '麻婆豆腐', NULL, '麻辣鲜香，下饭首选', 18.00, 100, 0, 1, NOW(), NOW(), 0),
       (4, 1, 1, '回锅肉', NULL, '二刀肉配蒜苗，肥而不腻', 32.00, 100, 0, 1, NOW(), NOW(), 0),
       (5, 1, 2, '夫妻肺片', NULL, '经典川式凉菜，麻辣爽口', 28.00, 50, 0, 1, NOW(), NOW(), 0),
       (6, 1, 2, '凉拌黄瓜', NULL, '清爽解腻', 12.00, 80, 0, 1, NOW(), NOW(), 0),
       (7, 1, 2, '红油抄手', NULL, '手工抄手，红油飘香（已下架，用于验证下架拦截）', 16.00, 60, 0, 0, NOW(), NOW(), 0),
       (8, 2, 3, '鸡胸肉沙拉', NULL, '高蛋白低脂肪，健身首选', 29.00, 60, 0, 1, NOW(), NOW(), 0),
       (9, 2, 3, '牛油果蔬菜沙拉', NULL, '牛油果配时蔬，口感清爽', 35.00, 40, 0, 1, NOW(), NOW(), 0),
       (10, 2, 3, '三文鱼藜麦碗', NULL, '三文鱼配藜麦，营养均衡', 45.00, 30, 0, 1, NOW(), NOW(), 0),
       (11, 2, 4, '鲜榨橙汁', NULL, '当日现榨，无添加', 15.00, 50, 0, 1, NOW(), NOW(), 0),
       (12, 2, 4, '柠檬气泡水', NULL, '气泡柠檬，清爽解腻', 12.00, 50, 0, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE id = id;