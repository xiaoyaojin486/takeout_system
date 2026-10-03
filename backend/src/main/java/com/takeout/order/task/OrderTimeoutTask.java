package com.takeout.order.task;

import com.takeout.common.properties.OrderProperties;
import com.takeout.order.entity.Order;
import com.takeout.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 超时未支付订单自动取消
 *
 * <p><b>为什么拆成两个类</b>：定时任务里循环调用 {@code orderService.closeTimeoutOrder()}，
 * 而 closeTimeoutOrder 需要独立事务（单笔失败不影响其他笔）。
 * 如果循环和事务方法写在同一个类里，Spring 的代理机制会让 @Transactional 失效 ——
 * 这是自调用（self-invocation）的经典坑。</p>
 *
 * <p><b>单线程 + 逐笔事务</b>：不使用线程池并发处理，避免同一批订单被重复扫到；
 * 逐笔事务保证一笔失败（例如库存归还时菜品已被物理删除）不会连累其他订单。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderService orderService;
    private final OrderProperties orderProperties;

    /** 启动 30 秒后开始，之后每分钟执行一次 */
    @Scheduled(initialDelay = 30_000, fixedDelay = 60_000)
    public void closeTimeoutOrders() {
        List<Order> orders = orderService.listTimeoutOrders(orderProperties.getPayTimeoutMinutes());
        if (orders.isEmpty()) {
            return;
        }
        log.info("发现 {} 笔超时未支付订单，开始自动取消", orders.size());

        int success = 0;
        for (Order order : orders) {
            try {
                orderService.closeTimeoutOrder(order);
                success++;
            } catch (Exception e) {
                // 吞掉异常继续处理剩余订单，失败的那笔下一轮还会被扫到
                log.error("关闭超时订单失败 orderNo={}", order.getOrderNo(), e);
            }
        }
        log.info("超时订单处理完成：成功 {}/{}", success, orders.size());
    }
}