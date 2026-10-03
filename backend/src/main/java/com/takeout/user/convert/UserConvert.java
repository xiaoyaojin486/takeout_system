package com.takeout.user.convert;

import com.takeout.user.api.vo.UserAuthVO;
import com.takeout.user.api.vo.UserSimpleVO;
import com.takeout.user.dto.RegisterDTO;
import com.takeout.user.entity.SysUser;
import com.takeout.user.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 账号对象转换：DTO ↔ Entity、Entity ↔ VO
 *
 * <p>
 * 转换职责集中在这里，Service 里不出现 setter 搬运；
 * 审计字段、状态描述、脱敏等补全逻辑也在这里做。
 * </p>
 */
@Mapper
public interface UserConvert {

    /** Entity → 前端 VO（不含 password） */
    UserVO toVO(SysUser entity);

    List<UserVO> toVOList(List<SysUser> entities);

    /** Entity → 跨模块认证 VO（含 password，仅 auth 使用）；实体主键是 id，契约 VO 用 userId 命名 */
    @Mapping(target = "userId", source = "id")
    UserAuthVO toAuthVO(SysUser entity);

    /** Entity → 跨模块简要 VO */
    @Mapping(target = "userId", source = "id")
    UserSimpleVO toSimpleVO(SysUser entity);

    /** 批量转换：MapStruct 会复用上面的单元素映射方法 */
    List<UserSimpleVO> toSimpleVOList(List<SysUser> entities);

    /**
     * 注册 DTO → Entity
     *
     * <p>
     * <b>password 显式 ignore</b>：DTO 里是明文，若被自动映射进 Entity，
     * 一旦 Service 忘记 encode 就会明文入库。这里直接掐断这条路径，
     * 密码只能由 Service 用 encoder.encode() 显式写入。
     * </p>
     */
    @Mapping(target = "password", ignore = true)
    SysUser toEntity(RegisterDTO dto);
}