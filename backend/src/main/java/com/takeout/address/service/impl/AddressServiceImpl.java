package com.takeout.address.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.takeout.address.convert.AddressConvert;
import com.takeout.address.dto.AddressDTO;
import com.takeout.address.entity.Address;
import com.takeout.address.mapper.AddressMapper;
import com.takeout.address.service.AddressService;
import com.takeout.address.vo.AddressVO;
import com.takeout.common.context.UserContextHolder;
import com.takeout.common.exception.BusinessException;
import com.takeout.common.exception.ForbiddenException;
import com.takeout.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 收货地址服务实现
 *
 * <p>
 * <b>数据权限</b>：所有方法都以 {@code UserContextHolder.getUserId()} 作为过滤条件，
 * 绝不接受前端传入的 userId，否则 A 用户可以读改 B 用户的地址。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    /** 单用户地址上限，防止刷接口无限插入 */
    private static final int MAX_ADDRESS_COUNT = 20;

    private final AddressMapper addressMapper;
    private final AddressConvert addressConvert;

    @Override
    public List<AddressVO> list() {
        Long userId = UserContextHolder.getUserId();
        List<Address> list = addressMapper.selectList(Wrappers.<Address>lambdaQuery()
                .select(Address::getId, Address::getReceiver, Address::getPhone,
                        Address::getProvince, Address::getCity, Address::getDistrict,
                        Address::getDetail, Address::getLabel, Address::getIsDefault)
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getUpdateTime));
        return list.stream().map(this::toVOWithFullAddress).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(AddressDTO dto) {
        Long userId = UserContextHolder.getUserId();

        Long count = addressMapper.selectCount(Wrappers.<Address>lambdaQuery()
                .eq(Address::getUserId, userId));
        if (count != null && count >= MAX_ADDRESS_COUNT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "收货地址最多 " + MAX_ADDRESS_COUNT + " 条");
        }

        Address entity = addressConvert.toEntity(dto);
        entity.setUserId(userId);
        entity.setIsDefault(normalizeDefault(dto.getIsDefault()));
        // 第一条地址强制设为默认：否则用户可能一条默认地址都没有，下单时无从选择
        if (count == null || count == 0) {
            entity.setIsDefault(1);
        }
        if (Integer.valueOf(1).equals(entity.getIsDefault())) {
            addressMapper.clearDefault(userId);
        }
        addressMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AddressDTO dto) {
        Address exist = requireOwned(id);
        Long userId = exist.getUserId();

        if (Integer.valueOf(1).equals(normalizeDefault(dto.getIsDefault()))) {
            addressMapper.clearDefault(userId);
        }

        Address update = addressConvert.toEntity(dto);
        update.setId(id);
        update.setIsDefault(normalizeDefault(dto.getIsDefault()));
        addressMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Address exist = requireOwned(id);
        addressMapper.deleteById(id);

        // 删掉的是默认地址，则把最近更新的另一条提为默认，避免出现「无默认地址」状态
        if (Integer.valueOf(1).equals(exist.getIsDefault())) {
            Address next = addressMapper.selectOne(Wrappers.<Address>lambdaQuery()
                    .select(Address::getId)
                    .eq(Address::getUserId, exist.getUserId())
                    .orderByDesc(Address::getUpdateTime)
                    .last("limit 1"));
            if (next != null) {
                Address promote = new Address();
                promote.setId(next.getId());
                promote.setIsDefault(1);
                addressMapper.updateById(promote);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        Address exist = requireOwned(id);

        addressMapper.clearDefault(exist.getUserId());

        Address update = new Address();
        update.setId(id);
        update.setIsDefault(1);
        addressMapper.updateById(update);
    }

    // ==================== 内部方法 ====================

    /** 取地址并校验归属，越权直接 403 */
    private Address requireOwned(Long id) {
        Address address = addressMapper.selectOne(Wrappers.<Address>lambdaQuery()
                .select(Address::getId, Address::getUserId, Address::getIsDefault)
                .eq(Address::getId, id));
        if (address == null) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }
        Long currentUserId = UserContextHolder.getUserId();
        if (!currentUserId.equals(address.getUserId())) {
            log.warn("越权访问地址 userId={} addressId={} owner={}", currentUserId, id, address.getUserId());
            throw new ForbiddenException(ResultCode.ADDRESS_NOT_OWNER);
        }
        return address;
    }

    private Integer normalizeDefault(Integer isDefault) {
        return Integer.valueOf(1).equals(isDefault) ? 1 : 0;
    }

    private AddressVO toVOWithFullAddress(Address entity) {
        AddressVO vo = addressConvert.toVO(entity);
        vo.setFullAddress(joinFullAddress(entity));
        return vo;
    }

    /** 拼接完整地址：空段自动跳过，避免出现「四川省 成都市 null 天府大道」 */
    public static String joinFullAddress(Address entity) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, entity.getProvince());
        appendIfPresent(sb, entity.getCity());
        appendIfPresent(sb, entity.getDistrict());
        appendIfPresent(sb, entity.getDetail());
        return sb.toString();
    }

    private static void appendIfPresent(StringBuilder sb, String part) {
        if (StringUtils.hasText(part)) {
            sb.append(part.trim());
        }
    }
}