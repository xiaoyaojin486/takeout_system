package com.takeout.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.takeout.common.context.UserContextHolder;
import com.takeout.common.exception.BusinessException;
import com.takeout.common.result.PageResult;
import com.takeout.common.result.ResultCode;
import com.takeout.merchant.convert.MerchantConvert;
import com.takeout.merchant.dto.AuditDTO;
import com.takeout.merchant.dto.MerchantQuery;
import com.takeout.merchant.dto.ShopDTO;
import com.takeout.merchant.entity.Merchant;
import com.takeout.merchant.mapper.MerchantMapper;
import com.takeout.merchant.service.MerchantService;
import com.takeout.merchant.vo.MerchantVO;
import com.takeout.user.api.UserApi;
import com.takeout.user.api.vo.UserSimpleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 店铺服务实现
 *
 * <p>
 * 三条必须守住的规则：
 * </p>
 * <ol>
 * <li>商家端一切操作都以 {@code UserContextHolder.getUserId()} 反查自己的店铺，不信任前端传入的
 * merchantId；</li>
 * <li>顾客端只暴露 {@code auditStatus=1} 的店铺，待审核/已驳回对外一律视为不存在；</li>
 * <li>运行状态与审核状态解耦：审核通过只是「允许营业」，真正营业由商家（或管理员）另行开启。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private static final Integer AUDIT_PASSED = 1;
    private static final Integer AUDIT_REJECTED = 2;
    private static final Integer BUSINESS_OPEN = 1;

    private final MerchantMapper merchantMapper;
    private final MerchantConvert merchantConvert;
    private final UserApi userApi;

    @Override
    public MerchantVO getMyShop() {
        return toVOWithDesc(requireMyShop());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyShop(ShopDTO dto) {
        Long userId = UserContextHolder.getUserId();
        if (selectByUserId(userId) != null) {
            throw new BusinessException(ResultCode.MERCHANT_EXISTS);
        }

        Merchant entity = new Merchant();
        entity.setUserId(userId);
        entity.setName(dto.getName().trim());
        entity.setLogo(dto.getLogo());
        entity.setDescription(dto.getDescription());
        entity.setPhone(dto.getPhone());
        entity.setAddress(dto.getAddress());
        entity.setMinAmount(dto.getMinAmount());
        entity.setDeliveryFee(dto.getDeliveryFee());
        // 入驻即进入待审核，且默认休息中：审核通过前店铺不应出现在顾客端
        entity.setAuditStatus(0);
        entity.setBusinessStatus(0);
        entity.setRating(new BigDecimal("5.00"));
        entity.setSales(0);

        merchantMapper.insert(entity);

        log.info("商家提交入驻申请 userId={} merchantId={}", userId, entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMyShop(ShopDTO dto) {
        Merchant merchant = requireMyShop();

        Merchant update = new Merchant();
        update.setId(merchant.getId());
        update.setName(dto.getName().trim());
        update.setLogo(dto.getLogo());
        update.setDescription(dto.getDescription());
        update.setPhone(dto.getPhone());
        update.setAddress(dto.getAddress());
        update.setMinAmount(dto.getMinAmount());
        update.setDeliveryFee(dto.getDeliveryFee());
        merchantMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBusinessStatus(Integer businessStatus) {
        Merchant merchant = requireMyShop();
        // 未通过审核的店铺没有营业资格，直接拒绝，避免绕过审核开始接单
        if (!AUDIT_PASSED.equals(merchant.getAuditStatus())) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_AUDITED);
        }

        Merchant update = new Merchant();
        update.setId(merchant.getId());
        update.setBusinessStatus(businessStatus);
        merchantMapper.updateById(update);

        log.info("商家切换营业状态 merchantId={} businessStatus={}", merchant.getId(), businessStatus);
    }

    @Override
    public PageResult<MerchantVO> customerPage(MerchantQuery query) {
        LambdaQueryWrapper<Merchant> wrapper = selectColumns()
                // 顾客端硬编码只查审核通过的店铺，不接受前端传 auditStatus
                .eq(Merchant::getAuditStatus, AUDIT_PASSED)
                .eq(query.getBusinessStatus() != null, Merchant::getBusinessStatus, query.getBusinessStatus());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Merchant::getName, query.getKeyword().trim());
        }
        // 销量倒序：让热门店铺自然排在前面
        wrapper.orderByDesc(Merchant::getSales);

        Page<Merchant> page = merchantMapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page, fillOwnerUsername(page.getRecords()));
    }

    @Override
    public MerchantVO customerDetail(Long merchantId) {
        Merchant merchant = selectById(merchantId);
        // 未通过审核的店铺对顾客视为不存在，不泄露「待审核/已驳回」的内部状态
        if (merchant == null || !AUDIT_PASSED.equals(merchant.getAuditStatus())) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_FOUND);
        }
        return toVOWithDesc(merchant);
    }

    @Override
    public PageResult<MerchantVO> adminPage(MerchantQuery query) {
        LambdaQueryWrapper<Merchant> wrapper = selectColumns()
                .eq(query.getAuditStatus() != null, Merchant::getAuditStatus, query.getAuditStatus())
                .eq(query.getBusinessStatus() != null, Merchant::getBusinessStatus, query.getBusinessStatus());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Merchant::getName, query.getKeyword().trim());
        }
        wrapper.orderByDesc(Merchant::getCreateTime);

        Page<Merchant> page = merchantMapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page, fillOwnerUsername(page.getRecords()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long merchantId, AuditDTO dto) {
        Integer auditStatus = dto.getAuditStatus();
        // 只接受「通过 / 驳回」两种终态；待审核(0)是商家提交时的初始值，不允许管理员回设
        if (!AUDIT_PASSED.equals(auditStatus) && !AUDIT_REJECTED.equals(auditStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        Merchant merchant = selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_FOUND);
        }

        Merchant update = new Merchant();
        update.setId(merchantId);
        update.setAuditStatus(auditStatus);
        update.setAuditRemark(dto.getAuditRemark());
        // 审核通过时营业状态归零：是否开门由商家自己决定，避免「审核通过即自动接单」
        if (AUDIT_PASSED.equals(auditStatus)) {
            update.setBusinessStatus(0);
        }
        merchantMapper.updateById(update);

        log.info("管理员审核店铺 merchantId={} auditStatus={}", merchantId, auditStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBusinessStatusByAdmin(Long merchantId, Integer businessStatus) {
        Merchant merchant = selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_FOUND);
        }

        Merchant update = new Merchant();
        update.setId(merchantId);
        update.setBusinessStatus(businessStatus);
        merchantMapper.updateById(update);

        log.info("管理员强制修改营业状态 merchantId={} businessStatus={}", merchantId, businessStatus);
    }

    // ==================== 内部方法 ====================

    /** 显式列出需要的列，避免 SELECT * 把整行无谓地捞进内存 */
    private LambdaQueryWrapper<Merchant> selectColumns() {
        return Wrappers.<Merchant>lambdaQuery()
                .select(Merchant::getId, Merchant::getUserId, Merchant::getName, Merchant::getLogo,
                        Merchant::getDescription, Merchant::getPhone, Merchant::getAddress,
                        Merchant::getMinAmount, Merchant::getDeliveryFee, Merchant::getBusinessStatus,
                        Merchant::getAuditStatus, Merchant::getAuditRemark, Merchant::getRating,
                        Merchant::getSales, Merchant::getCreateTime);
    }

    private Merchant selectByUserId(Long userId) {
        return merchantMapper.selectOne(selectColumns()
                .eq(Merchant::getUserId, userId)
                .last("limit 1"));
    }

    private Merchant selectById(Long merchantId) {
        if (merchantId == null) {
            return null;
        }
        return merchantMapper.selectOne(selectColumns().eq(Merchant::getId, merchantId));
    }

    /** 取当前登录商家的店铺；没有店铺说明没入驻，按「店铺不存在」处理 */
    private Merchant requireMyShop() {
        Merchant merchant = selectByUserId(UserContextHolder.getUserId());
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_FOUND);
        }
        return merchant;
    }

    /** 批量补全店主账号名；一次性 in 查询，避免逐条查询的 N+1 */
    private List<MerchantVO> fillOwnerUsername(List<Merchant> merchants) {
        List<MerchantVO> voList = merchantConvert.toVOList(merchants);
        if (voList.isEmpty()) {
            return voList;
        }
        Set<Long> userIds = merchants.stream()
                .map(Merchant::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> usernameMap = userApi.listSimpleByIds(userIds).stream()
                .collect(Collectors.toMap(UserSimpleVO::getUserId, UserSimpleVO::getUsername, (a, b) -> a));
        for (MerchantVO vo : voList) {
            vo.setOwnerUsername(usernameMap.get(vo.getOwnerUserId()));
            fillDesc(vo);
        }
        return voList;
    }

    /** Entity → VO，并补全状态描述 */
    private MerchantVO toVOWithDesc(Merchant entity) {
        MerchantVO vo = merchantConvert.toVO(entity);
        fillDesc(vo);
        return vo;
    }

    private void fillDesc(MerchantVO vo) {
        if (vo == null) {
            return;
        }
        vo.setBusinessStatusDesc(businessStatusDesc(vo.getBusinessStatus()));
        vo.setAuditStatusDesc(auditStatusDesc(vo.getAuditStatus()));
    }

    private String businessStatusDesc(Integer status) {
        if (status == null) {
            return null;
        }
        return BUSINESS_OPEN.equals(status) ? "营业中" : "休息中";
    }

    private String auditStatusDesc(Integer status) {
        if (status == null) {
            return null;
        }
        if (AUDIT_PASSED.equals(status)) {
            return "已通过";
        }
        return AUDIT_REJECTED.equals(status) ? "已驳回" : "待审核";
    }
}