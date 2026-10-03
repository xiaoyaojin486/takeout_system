package com.takeout.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.takeout.common.context.UserContextHolder;
import com.takeout.common.enums.UserTypeEnum;
import com.takeout.common.exception.BusinessException;
import com.takeout.common.result.PageResult;
import com.takeout.common.result.ResultCode;
import com.takeout.user.convert.UserConvert;
import com.takeout.user.dto.ChangePasswordDTO;
import com.takeout.user.dto.RegisterDTO;
import com.takeout.user.dto.UpdateProfileDTO;
import com.takeout.user.dto.UserQuery;
import com.takeout.user.entity.SysUser;
import com.takeout.user.mapper.SysUserMapper;
import com.takeout.user.service.UserService;
import com.takeout.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 账号服务实现
 *
 * <p>
 * 密码相关的三条铁律（验收时会逐条核对）：
 * </p>
 * <ol>
 * <li>入库前必须 {@code encoder.encode()}，库里永远只有密文；</li>
 * <li>校验只能用 {@code encoder.matches(raw, encoded)}，不能用字符串比对；</li>
 * <li>返回前端的 VO 里没有 password 字段。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final UserConvert userConvert;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        String username = dto.getUsername().trim();
        if (existsByUsername(username)) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        if (existsByPhone(dto.getPhone())) {
            throw new BusinessException(ResultCode.PHONE_EXISTS);
        }

        SysUser entity = userConvert.toEntity(dto);
        entity.setUsername(username);
        // 明文密码在这里被终结：入库的只有 BCrypt 密文
        entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        entity.setUserType(UserTypeEnum.CUSTOMER.getCode());
        entity.setStatus(1);
        entity.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname().trim() : username);
        sysUserMapper.insert(entity);

        log.info("顾客注册成功 userId={} username={}", entity.getId(), username);
    }

    @Override
    public UserVO getProfile() {
        Long userId = UserContextHolder.getUserId();
        SysUser user = selectColumnsById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return toVOWithDesc(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UpdateProfileDTO dto) {
        Long userId = UserContextHolder.getUserId();

        if (StringUtils.hasText(dto.getPhone())) {
            // 换手机号要排除自己，否则「保持原手机号不变」会被误判成重复
            Long samePhoneUserId = findIdByPhone(dto.getPhone());
            if (samePhoneUserId != null && !samePhoneUserId.equals(userId)) {
                throw new BusinessException(ResultCode.PHONE_EXISTS);
            }
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setNickname(dto.getNickname());
        update.setAvatar(dto.getAvatar());
        update.setPhone(dto.getPhone());
        sysUserMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = UserContextHolder.getUserId();

        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .select(SysUser::getId, SysUser::getPassword)
                .eq(SysUser::getId, userId));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        sysUserMapper.updateById(update);

        log.info("用户修改密码成功 userId={}", userId);
    }

    @Override
    public PageResult<UserVO> page(UserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.<SysUser>lambdaQuery()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getPhone, SysUser::getNickname,
                        SysUser::getAvatar, SysUser::getUserType, SysUser::getStatus,
                        SysUser::getLastLoginTime, SysUser::getCreateTime)
                .eq(query.getUserType() != null, SysUser::getUserType, query.getUserType())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus());

        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            // 括号包裹：不加括号会变成 (type=?) and (username like ?) or (phone like ?)，OR 会击穿前面的过滤条件
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> page = sysUserMapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page, this::toVOWithDesc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, Integer status) {
        SysUser user = selectColumnsById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 防呆：把管理员全停用会让平台无人能管，直接拒绝
        if (UserTypeEnum.ADMIN.getCode().equals(user.getUserType())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不允许修改管理员账号状态");
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setStatus(status);
        sysUserMapper.updateById(update);

        log.info("管理员修改账号状态 userId={} status={}", userId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = selectColumnsById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(update);

        log.info("管理员重置密码 userId={}", userId);
    }

    // ==================== 内部方法 ====================

    private SysUser selectColumnsById(Long userId) {
        // 显式列出需要的列：避免 SELECT * 把 password 密文无谓地捞进内存
        return sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getPhone, SysUser::getNickname,
                        SysUser::getAvatar, SysUser::getUserType, SysUser::getStatus,
                        SysUser::getLastLoginTime, SysUser::getCreateTime)
                .eq(SysUser::getId, userId));
    }

    private boolean existsByUsername(String username) {
        return sysUserMapper.exists(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
    }

    private boolean existsByPhone(String phone) {
        return sysUserMapper.exists(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPhone, phone));
    }

    private Long findIdByPhone(String phone) {
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .select(SysUser::getId)
                .eq(SysUser::getPhone, phone)
                .last("limit 1"));
        return user == null ? null : user.getId();
    }

    /** Entity → VO，并补全状态描述 */
    private UserVO toVOWithDesc(SysUser entity) {
        UserVO vo = userConvert.toVO(entity);
        if (vo != null) {
            UserTypeEnum type = UserTypeEnum.of(entity.getUserType());
            vo.setUserTypeDesc(type == null ? null : type.getDesc());
        }
        return vo;
    }
}