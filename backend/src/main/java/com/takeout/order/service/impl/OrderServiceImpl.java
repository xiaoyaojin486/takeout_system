package com.takeout.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.takeout.address.api.AddressApi;
import com.takeout.address.api.vo.AddressSimpleVO;
import com.takeout.cart.api.CartApi;
import com.takeout.common.context.UserContextHolder;
import com.takeout.common.enums.OrderOperatorEnum;
import com.takeout.common.enums.OrderStatusEnum;
import com.takeout.common.exception.BusinessException;
import com.takeout.common.exception.ForbiddenException;
import com.takeout.common.properties.OrderProperties;
import com.takeout.common.result.PageResult;
import com.takeout.common.result.ResultCode;
import com.takeout.common.util.OrderNoGenerator;
import com.takeout.dish.api.DishApi;
import com.takeout.dish.api.vo.DishSimpleVO;
import com.takeout.merchant.api.MerchantApi;
import com.takeout.merchant.api.vo.MerchantSimpleVO;
import com.takeout.order.convert.OrderConvert;
import com.takeout.order.dto.CreateOrderDTO;
import com.takeout.order.dto.OrderItemDTO;
import com.takeout.order.dto.OrderQuery;
import com.takeout.order.entity.Order;
import com.takeout.order.entity.OrderItem;
import com.takeout.order.entity.OrderStatusLog;
import com.takeout.order.mapper.OrderItemMapper;
import com.takeout.order.mapper.OrderMapper;
import com.takeout.order.mapper.OrderStatusLogMapper;
import com.takeout.order.service.OrderService;
import com.takeout.order.service.OrderStatusMachine;
import com.takeout.order.vo.OrderCreateVO;
import com.takeout.order.vo.OrderDetailVO;
import com.takeout.order.vo.OrderItemVO;
import com.takeout.order.vo.OrderStatusLogVO;
import com.takeout.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 *
 * <p><b>金额口径</b>：前端只传 dishId + quantity，总额、配送费、实付全部由后端重算，
 * 前端传的任何价格字段都不认 —— 这是防改价的第一道也是唯一一道防线。</p>
 *
 * <p><b>库存口径</b>：下单即扣（原子 UPDATE），取消/拒单/超时归还；
 * 销量只在订单「已完成」时累加，因此取消链路不需要回退销量。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final OrderConvert orderConvert;
    private final OrderStatusMachine statusMachine;
    private final OrderNoGenerator orderNoGenerator;
    private final OrderProperties orderProperties;
    private final AddressApi addressApi;
    private final DishApi dishApi;
    private final MerchantApi merchantApi;
    private final CartApi cartApi;

    // ==================== 顾客端 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO createOrder(CreateOrderDTO dto) {
        Long userId = UserContextHolder.getUserId();

        // ① 地址：只能用自己的地址（归属校验下沉在 AddressApi 内部）
        AddressSimpleVO address = addressApi.findByIdAndUserId(dto.getAddressId(), userId);
        if (address == null) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }

        // ② 菜品去重合并：前端可能传两条相同 dishId，合并后再校验库存，否则会重复扣减
        Map<Long, Integer> quantityMap = new LinkedHashMap<>();
        for (OrderItemDTO item : dto.getItems()) {
            quantityMap.merge(item.getDishId(), item.getQuantity(), Integer::sum);
        }
        Map<Long, DishSimpleVO> dishMap = dishApi.listSimpleByIds(quantityMap.keySet()).stream()
                .collect(Collectors.toMap(DishSimpleVO::getDishId, d -> d, (a, b) -> a));
        if (dishMap.size() != quantityMap.size()) {
            // 有菜品查不到：被删除或已下架到逻辑删除
            throw new BusinessException(ResultCode.DISH_NOT_FOUND);
        }

        // ③ 单店铺约束：一个订单只能属于一个商家
        Set<Long> merchantIds = dishMap.values().stream()
                .map(DishSimpleVO::getMerchantId).collect(Collectors.toSet());
        if (merchantIds.size() > 1) {
            throw new BusinessException(ResultCode.CART_MULTI_MERCHANT, "一个订单只能包含同一家店铺的菜品");
        }
        Long merchantId = merchantIds.iterator().next();

        // ④ 店铺必须已通过审核且在营业中
        MerchantSimpleVO merchant = merchantApi.getSimpleById(merchantId);
        if (merchant == null || !Integer.valueOf(1).equals(merchant.getAuditStatus())) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_AUDITED);
        }
        if (!Integer.valueOf(1).equals(merchant.getBusinessStatus())) {
            throw new BusinessException(ResultCode.MERCHANT_CLOSED);
        }

        // ⑤ 逐项校验 + 重算金额 + 组装快照
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> itemEntities = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantityMap.entrySet()) {
            DishSimpleVO dish = dishMap.get(entry.getKey());
            int quantity = entry.getValue();

            if (!Integer.valueOf(1).equals(dish.getStatus())) {
                throw new BusinessException(ResultCode.DISH_OFF_SHELF, "「" + dish.getName() + "」已下架");
            }
            if (dish.getStock() == null || dish.getStock() < quantity) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH,
                        "「" + dish.getName() + "」库存不足，当前仅剩 " + (dish.getStock() == null ? 0 : dish.getStock()) + " 份");
            }

            BigDecimal amount = dish.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(amount);

            OrderItem item = new OrderItem();
            item.setDishId(dish.getDishId());
            item.setDishName(dish.getName());
            item.setDishImage(dish.getImage());
            item.setPrice(dish.getPrice());
            item.setQuantity(quantity);
            item.setAmount(amount);
            itemEntities.add(item);
        }

        // ⑥ 起送价校验
        BigDecimal minAmount = merchant.getMinAmount() == null ? BigDecimal.ZERO : merchant.getMinAmount();
        if (totalAmount.compareTo(minAmount) < 0) {
            throw new BusinessException(ResultCode.BELOW_MIN_AMOUNT,
                    String.format("「%s」起送价 %.2f 元，当前菜品金额 %.2f 元", merchant.getName(), minAmount, totalAmount));
        }

        BigDecimal deliveryFee = merchant.getDeliveryFee() == null ? BigDecimal.ZERO : merchant.getDeliveryFee();
        BigDecimal payAmount = totalAmount.add(deliveryFee);

        // ⑦ 落订单主表（地址快照在这里定格）
        Order order = new Order();
        order.setOrderNo(orderNoGenerator.nextOrderNo());
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setReceiver(address.getReceiver());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverAddress(address.getFullAddress());
        order.setTotalAmount(totalAmount);
        order.setDeliveryFee(deliveryFee);
        order.setPayAmount(payAmount);
        order.setStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
        order.setRemark(dto.getRemark());
        orderMapper.insert(order);

        // ⑧ 原子扣库存 + 落明细：任一菜品扣减失败则整单回滚，不会出现"扣了一半"
        for (OrderItem item : itemEntities) {
            int affected = dishApi.deductStock(item.getDishId(), item.getQuantity());
            if (affected == 0) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH, "「" + item.getDishName() + "」库存不足");
            }
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // ⑨ 状态日志（0 → 10）
        statusMachine.logCreated(order.getId(), userId, "顾客下单");

        // ⑩ 清掉已下单的购物车行（保留该店铺中未下单的其他菜品）
        cartApi.removeByUserIdAndDishIds(userId, quantityMap.keySet());

        log.info("下单成功 orderNo={} userId={} merchantId={} total={} pay={}",
                order.getOrderNo(), userId, merchantId, totalAmount, payAmount);

        OrderCreateVO vo = new OrderCreateVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPayAmount(payAmount);
        return vo;
    }

    @Override
    public PageResult<OrderVO> myOrders(OrderQuery query) {
        Long userId = UserContextHolder.getUserId();
        LambdaQueryWrapper<Order> wrapper = baseWrapper(query).eq(Order::getUserId, userId);
        Page<Order> page = orderMapper.selectPage(query.toPage(), wrapper);
        return assemblePage(page);
    }

    @Override
    public OrderDetailVO myOrderDetail(Long orderId) {
        return buildDetail(requireCustomerOrder(orderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByCustomer(Long orderId, String reason) {
        Order order = requireCustomerOrder(orderId);
        // 商家已开始备餐后顾客不能再单方面取消，需联系商家（商家侧可取消）
        if (!OrderStatusEnum.PENDING_PAYMENT.getCode().equals(order.getStatus())
                && !OrderStatusEnum.PENDING_ACCEPT.getCode().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR,
                    "订单已进入「" + OrderStatusEnum.of(order.getStatus()).getDesc() + "」，无法取消，请联系商家");
        }

        restoreStock(order.getId());
        statusMachine.transfer(order, OrderStatusEnum.CANCELED, OrderOperatorEnum.CUSTOMER,
                order.getUserId(), StringUtils.hasText(reason) ? reason : "顾客取消订单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmByCustomer(Long orderId) {
        Order order = requireCustomerOrder(orderId);
        statusMachine.transfer(order, OrderStatusEnum.COMPLETED, OrderOperatorEnum.CUSTOMER,
                order.getUserId(), "顾客确认收货");
        accumulateSales(order);
    }

    // ==================== 商家端 ====================

    @Override
    public PageResult<OrderVO> merchantOrders(OrderQuery query) {
        Long merchantId = currentMerchantId();
        LambdaQueryWrapper<Order> wrapper = baseWrapper(query).eq(Order::getMerchantId, merchantId);
        Page<Order> page = orderMapper.selectPage(query.toPage(), wrapper);
        return assemblePage(page);
    }

    @Override
    public OrderDetailVO merchantOrderDetail(Long orderId) {
        return buildDetail(requireMerchantOrder(orderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptByMerchant(Long orderId) {
        Order order = requireMerchantOrder(orderId);
        statusMachine.transfer(order, OrderStatusEnum.PREPARING, OrderOperatorEnum.MERCHANT,
                UserContextHolder.getUserId(), "商家已接单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectByMerchant(Long orderId, String reason) {
        Order order = requireMerchantOrder(orderId);
        if (!OrderStatusEnum.PENDING_ACCEPT.getCode().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "只有待接单的订单可以拒单");
        }

        restoreStock(order.getId());
        statusMachine.transfer(order, OrderStatusEnum.CANCELED, OrderOperatorEnum.MERCHANT,
                UserContextHolder.getUserId(), "商家拒单：" + reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByMerchant(Long orderId, String reason) {
        Order order = requireMerchantOrder(orderId);
        if (!OrderStatusEnum.PENDING_ACCEPT.getCode().equals(order.getStatus())
                && !OrderStatusEnum.PREPARING.getCode().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "只有待接单/备餐中的订单可以取消");
        }

        restoreStock(order.getId());
        statusMachine.transfer(order, OrderStatusEnum.CANCELED, OrderOperatorEnum.MERCHANT,
                UserContextHolder.getUserId(), "商家取消：" + reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverByMerchant(Long orderId) {
        Order order = requireMerchantOrder(orderId);
        statusMachine.transfer(order, OrderStatusEnum.DELIVERING, OrderOperatorEnum.MERCHANT,
                UserContextHolder.getUserId(), "商家已出餐");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishByMerchant(Long orderId) {
        Order order = requireMerchantOrder(orderId);
        statusMachine.transfer(order, OrderStatusEnum.COMPLETED, OrderOperatorEnum.MERCHANT,
                UserContextHolder.getUserId(), "商家已送达");
        accumulateSales(order);
    }

    // ==================== 管理后台 ====================

    @Override
    public PageResult<OrderVO> adminOrders(OrderQuery query) {
        LambdaQueryWrapper<Order> wrapper = baseWrapper(query)
                .eq(query.getMerchantId() != null, Order::getMerchantId, query.getMerchantId());
        Page<Order> page = orderMapper.selectPage(query.toPage(), wrapper);
        return assemblePage(page);
    }

    @Override
    public OrderDetailVO adminOrderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return buildDetail(order);
    }

    // ==================== 系统 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        statusMachine.transfer(order, OrderStatusEnum.PENDING_ACCEPT, OrderOperatorEnum.SYSTEM,
                null, "支付成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeTimeoutOrder(Order order) {
        // 重新查一次再判断：定时任务的候选集是 1 分钟前查出来的，
        // 这一分钟里用户可能刚好支付成功，不复核就会把已支付订单取消掉
        Order latest = orderMapper.selectById(order.getId());
        if (latest == null || !OrderStatusEnum.PENDING_PAYMENT.getCode().equals(latest.getStatus())) {
            return;
        }

        restoreStock(latest.getId());
        statusMachine.transfer(latest, OrderStatusEnum.CANCELED, OrderOperatorEnum.SYSTEM, null,
                "超过 " + orderProperties.getPayTimeoutMinutes() + " 分钟未支付，系统自动取消");
        log.info("超时未支付订单已自动取消 orderNo={}", latest.getOrderNo());
    }

    @Override
    public List<Order> listTimeoutOrders(int timeoutMinutes) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .select(Order::getId, Order::getOrderNo, Order::getStatus, Order::getUserId,
                        Order::getMerchantId, Order::getCreateTime)
                .eq(Order::getStatus, OrderStatusEnum.PENDING_PAYMENT.getCode())
                .lt(Order::getCreateTime, deadline)
                // 单轮最多处理 200 笔：避免积压时一次扫出几万条把事务撑爆
                .last("limit 200"));
    }

    // ==================== 内部方法 ====================

    /** 列表查询公共条件：显式列出字段，避免 SELECT * */
    private LambdaQueryWrapper<Order> baseWrapper(OrderQuery query) {
        LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery()
                .select(Order::getId, Order::getOrderNo, Order::getUserId, Order::getMerchantId,
                        Order::getReceiver, Order::getReceiverPhone, Order::getReceiverAddress,
                        Order::getTotalAmount, Order::getDeliveryFee, Order::getPayAmount,
                        Order::getStatus, Order::getRemark, Order::getCancelReason,
                        Order::getPayTime, Order::getAcceptTime, Order::getFinishTime,
                        Order::getCancelTime, Order::getCreateTime)
                .eq(query.getStatus() != null, Order::getStatus, query.getStatus());

        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            // 括号包裹，防止 OR 击穿前面的状态过滤
            wrapper.and(w -> w.like(Order::getOrderNo, keyword)
                    .or().like(Order::getReceiver, keyword));
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return wrapper;
    }

    /** 当前登录商家自己的店铺 id */
    private Long currentMerchantId() {
        MerchantSimpleVO merchant = merchantApi.getSimpleByUserId(UserContextHolder.getUserId());
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_FOUND, "当前账号还没有店铺，请先完成入驻");
        }
        return merchant.getMerchantId();
    }

    private Order requireCustomerOrder(Long orderId) {
        return requireOrder(orderId, OrderOperatorEnum.CUSTOMER);
    }

    private Order requireMerchantOrder(Long orderId) {
        return requireOrder(orderId, OrderOperatorEnum.MERCHANT);
    }

    /** 载入订单并校验归属：不存在返回 404，存在但不属于本人返回 403 */
    private Order requireOrder(Long orderId, OrderOperatorEnum viewer) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        Long currentUserId = UserContextHolder.getUserId();
        if (viewer == OrderOperatorEnum.CUSTOMER) {
            if (!currentUserId.equals(order.getUserId())) {
                log.warn("越权访问订单 userId={} orderId={} owner={}", currentUserId, orderId, order.getUserId());
                throw new ForbiddenException(ResultCode.ORDER_NOT_OWNER);
            }
        } else if (viewer == OrderOperatorEnum.MERCHANT) {
            if (!currentMerchantId().equals(order.getMerchantId())) {
                log.warn("商家越权访问订单 merchantUserId={} orderId={} merchantId={}",
                        currentUserId, orderId, order.getMerchantId());
                throw new ForbiddenException(ResultCode.ORDER_NOT_OWNER);
            }
        }
        return order;
    }

    /** 归还订单全部菜品库存（取消/拒单/超时时调用） */
    private void restoreStock(Long orderId) {
        List<OrderItem> items = listItems(orderId);
        for (OrderItem item : items) {
            dishApi.restoreStock(item.getDishId(), item.getQuantity());
        }
    }

    /** 订单完成时累加销量：菜品销量 + 店铺销量（店铺销量记 1 笔订单） */
    private void accumulateSales(Order order) {
        List<OrderItem> items = listItems(order.getId());
        for (OrderItem item : items) {
            dishApi.incrSales(item.getDishId(), item.getQuantity());
        }
        merchantApi.incrSales(order.getMerchantId(), 1);
    }

    private List<OrderItem> listItems(Long orderId) {
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .select(OrderItem::getId, OrderItem::getOrderId, OrderItem::getDishId, OrderItem::getDishName,
                        OrderItem::getDishImage, OrderItem::getPrice, OrderItem::getQuantity, OrderItem::getAmount)
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
    }

    private OrderDetailVO buildDetail(Order order) {
        OrderDetailVO detail = new OrderDetailVO();
        detail.setOrder(assembleVO(order, listItems(order.getId()), merchantApi.getSimpleById(order.getMerchantId())));

        List<OrderStatusLog> logs = orderStatusLogMapper.selectList(Wrappers.<OrderStatusLog>lambdaQuery()
                .select(OrderStatusLog::getId, OrderStatusLog::getOrderId, OrderStatusLog::getFromStatus,
                        OrderStatusLog::getToStatus, OrderStatusLog::getOperatorType,
                        OrderStatusLog::getOperatorId, OrderStatusLog::getRemark, OrderStatusLog::getCreateTime)
                .eq(OrderStatusLog::getOrderId, order.getId())
                .orderByAsc(OrderStatusLog::getId));

        List<OrderStatusLogVO> logVOs = new ArrayList<>();
        for (OrderStatusLog logEntity : logs) {
            OrderStatusLogVO vo = orderConvert.toLogVO(logEntity);
            OrderStatusEnum from = OrderStatusEnum.of(logEntity.getFromStatus());
            OrderStatusEnum to = OrderStatusEnum.of(logEntity.getToStatus());
            OrderOperatorEnum operator = OrderOperatorEnum.of(logEntity.getOperatorType());
            vo.setFromStatusDesc(from == null ? null : from.getDesc());
            vo.setToStatusDesc(to == null ? null : to.getDesc());
            vo.setOperatorTypeDesc(operator == null ? null : operator.getDesc());
            logVOs.add(vo);
        }
        detail.setStatusLogs(logVOs);
        return detail;
    }

    /** 分页结果装配：批量补齐明细与店铺名，避免逐条查询造成 N+1 */
    private PageResult<OrderVO> assemblePage(Page<Order> page) {
        List<Order> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty((int) page.getCurrent(), (int) page.getSize());
        }

        List<Long> orderIds = records.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemMap = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .select(OrderItem::getId, OrderItem::getOrderId, OrderItem::getDishId, OrderItem::getDishName,
                                OrderItem::getDishImage, OrderItem::getPrice, OrderItem::getQuantity, OrderItem::getAmount)
                        .in(OrderItem::getOrderId, orderIds)
                        .orderByAsc(OrderItem::getId))
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));

        Set<Long> merchantIds = records.stream().map(Order::getMerchantId).collect(Collectors.toSet());
        Map<Long, MerchantSimpleVO> merchantMap = merchantApi.listSimpleByIds(merchantIds).stream()
                .collect(Collectors.toMap(MerchantSimpleVO::getMerchantId, m -> m, (a, b) -> a));

        List<OrderVO> vos = records.stream()
                .map(order -> assembleVO(order, itemMap.getOrDefault(order.getId(), Collections.emptyList()),
                        merchantMap.get(order.getMerchantId())))
                .toList();
        return PageResult.of(page, vos);
    }

    private OrderVO assembleVO(Order order, List<OrderItem> items, MerchantSimpleVO merchant) {
        OrderVO vo = orderConvert.toVO(order);
        OrderStatusEnum status = OrderStatusEnum.of(order.getStatus());
        vo.setStatusDesc(status == null ? null : status.getDesc());
        vo.setMerchantName(merchant == null ? null : merchant.getName());

        List<OrderItemVO> itemVOs = orderConvert.toItemVOList(items);
        vo.setItems(itemVOs);
        vo.setItemCount(items.stream().mapToInt(OrderItem::getQuantity).sum());
        return vo;
    }
}