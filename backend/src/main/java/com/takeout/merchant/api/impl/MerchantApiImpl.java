package com.takeout.merchant.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.takeout.merchant.api.MerchantApi;
import com.takeout.merchant.api.vo.MerchantSimpleVO;
import com.takeout.merchant.convert.MerchantConvert;
import com.takeout.merchant.entity.Merchant;
import com.takeout.merchant.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 店铺模块对外契约实现
 *
 * <p>
 * 只做「查询 + 转换」，不写业务规则：跨模块调用方拿到的是干净的 VO，
 * 不会接触到 merchant 模块的 Entity 与 Mapper。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class MerchantApiImpl implements MerchantApi {

    private final MerchantMapper merchantMapper;
    private final MerchantConvert merchantConvert;

    @Override
    public MerchantSimpleVO getSimpleById(Long merchantId) {
        if (merchantId == null) {
            return null;
        }
        Merchant merchant = merchantMapper.selectOne(selectSimpleColumns().eq(Merchant::getId, merchantId));
        return merchant == null ? null : merchantConvert.toSimpleVO(merchant);
    }

    @Override
    public MerchantSimpleVO getSimpleByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        Merchant merchant = merchantMapper.selectOne(selectSimpleColumns()
                .eq(Merchant::getUserId, userId)
                .last("limit 1"));
        return merchant == null ? null : merchantConvert.toSimpleVO(merchant);
    }

    @Override
    public List<MerchantSimpleVO> listSimpleByIds(Collection<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Merchant> merchants = merchantMapper.selectList(selectSimpleColumns()
                .in(Merchant::getId, merchantIds));
        return merchantConvert.toSimpleVOList(merchants);
    }

    @Override
    public long countByAuditStatus(Integer auditStatus) {
        Long count = merchantMapper.selectCount(Wrappers.<Merchant>lambdaQuery()
                .eq(auditStatus != null, Merchant::getAuditStatus, auditStatus));
        return count == null ? 0L : count;
    }

    @Override
    public void incrSales(Long merchantId, int delta) {
        merchantMapper.incrSales(merchantId, delta);
    }

    /** 契约 VO 只需要的列，显式列出避免 SELECT * */
    private LambdaQueryWrapper<Merchant> selectSimpleColumns() {
        return Wrappers.<Merchant>lambdaQuery()
                .select(Merchant::getId, Merchant::getUserId, Merchant::getName, Merchant::getLogo,
                        Merchant::getAddress, Merchant::getPhone, Merchant::getMinAmount,
                        Merchant::getDeliveryFee, Merchant::getBusinessStatus, Merchant::getAuditStatus,
                        Merchant::getRating, Merchant::getSales);
    }
}