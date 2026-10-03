package com.takeout.merchant.service;

import com.takeout.common.result.PageResult;
import com.takeout.merchant.dto.AuditDTO;
import com.takeout.merchant.dto.MerchantQuery;
import com.takeout.merchant.dto.ShopDTO;
import com.takeout.merchant.vo.MerchantVO;

/**
 * 店铺服务
 */
public interface MerchantService {

    /** 商家端：当前登录商家自己的店铺 */
    MerchantVO getMyShop();

    /** 商家端：入驻申请（同一账号只能开通一个店铺） */
    void applyShop(ShopDTO dto);

    /** 商家端：编辑自己的店铺信息 */
    void updateMyShop(ShopDTO dto);

    /** 商家端：切换营业状态（审核通过后才允许营业） */
    void updateBusinessStatus(Integer businessStatus);

    /** 顾客端：店铺列表（仅审核通过的店铺，按销量倒序） */
    PageResult<MerchantVO> customerPage(MerchantQuery query);

    /** 顾客端：店铺详情（仅审核通过的店铺） */
    MerchantVO customerDetail(Long merchantId);

    /** 管理后台：店铺分页（全部状态） */
    PageResult<MerchantVO> adminPage(MerchantQuery query);

    /** 管理后台：审核店铺（通过时营业状态归零，由商家自行开启） */
    void audit(Long merchantId, AuditDTO dto);

    /** 管理后台：强制修改营业状态 */
    void updateBusinessStatusByAdmin(Long merchantId, Integer businessStatus);
}