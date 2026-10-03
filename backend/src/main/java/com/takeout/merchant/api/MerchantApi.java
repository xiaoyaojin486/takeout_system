package com.takeout.merchant.api;

import com.takeout.merchant.api.vo.MerchantSimpleVO;

import java.util.Collection;
import java.util.List;

/**
 * 店铺模块对外契约
 *
 * <p>
 * order / dish / cart / stats 等模块只能依赖本接口，禁止直接查 merchant 表。
 * </p>
 */
public interface MerchantApi {

    /** 按店铺 id 查简要信息 */
    MerchantSimpleVO getSimpleById(Long merchantId);

    /** 按店主账号 id 查店铺（商家端「我的店铺」用） */
    MerchantSimpleVO getSimpleByUserId(Long userId);

    /** 批量查简要信息 */
    List<MerchantSimpleVO> listSimpleByIds(Collection<Long> merchantIds);

    /**
     * 按审核状态统计店铺数
     *
     * @param auditStatus null 表示统计全部
     */
    long countByAuditStatus(Integer auditStatus);

    /**
     * 累加店铺销量
     *
     * <p>
     * 调用时机：订单完成时。取消链路不会调用它，因此无需回退逻辑。
     * </p>
     */
    void incrSales(Long merchantId, int delta);
}