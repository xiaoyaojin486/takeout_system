package com.takeout.merchant.convert;

import com.takeout.merchant.api.vo.MerchantSimpleVO;
import com.takeout.merchant.entity.Merchant;
import com.takeout.merchant.vo.MerchantVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 店铺对象转换：Entity ↔ VO 与 Entity ↔ 跨模块契约 VO
 *
 * <p>状态描述、店主账号名等需要额外查询或计算的字段不在这里做，交给 Service 补全。</p>
 */
@Mapper
public interface MerchantConvert {

    /** Entity → 前端 VO；实体主键是 id，VO 里的店主账号名 ownerUsername 由 Service 补齐 */
    @Mapping(target = "ownerUserId", source = "userId")
    @Mapping(target = "businessStatusDesc", ignore = true)
    @Mapping(target = "auditStatusDesc", ignore = true)
    @Mapping(target = "ownerUsername", ignore = true)
    MerchantVO toVO(Merchant entity);

    List<MerchantVO> toVOList(List<Merchant> entities);

    /** Entity → 跨模块简要 VO（实体主键是 id，契约 VO 用 merchantId 命名） */
    @Mapping(target = "merchantId", source = "id")
    MerchantSimpleVO toSimpleVO(Merchant entity);

    /** 批量转换：MapStruct 会复用上面的单元素映射方法 */
    List<MerchantSimpleVO> toSimpleVOList(List<Merchant> entities);
}