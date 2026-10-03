package com.takeout.user.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.takeout.user.api.UserApi;
import com.takeout.user.api.vo.UserAuthVO;
import com.takeout.user.api.vo.UserSimpleVO;
import com.takeout.user.convert.UserConvert;
import com.takeout.user.entity.SysUser;
import com.takeout.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 账号模块对外契约实现
 *
 * <p>
 * 只做「查询 + 转换」，不写业务规则：跨模块调用方拿到的是干净的 VO，
 * 不会接触到 user 模块的 Entity 与 Mapper。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class UserApiImpl implements UserApi {

    private final SysUserMapper sysUserMapper;
    private final UserConvert userConvert;

    @Override
    public UserAuthVO findAuthByUsername(String username) {
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getPassword, SysUser::getNickname,
                        SysUser::getAvatar, SysUser::getPhone, SysUser::getUserType, SysUser::getStatus)
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        if (user == null) {
            return null;
        }
        return userConvert.toAuthVO(user);
    }

    @Override
    public UserAuthVO findAuthById(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getPassword, SysUser::getNickname,
                        SysUser::getAvatar, SysUser::getPhone, SysUser::getUserType, SysUser::getStatus)
                .eq(SysUser::getId, userId));
        if (user == null) {
            return null;
        }
        return userConvert.toAuthVO(user);
    }

    @Override
    public UserSimpleVO getSimpleById(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = selectSimpleById(userId);
        return user == null ? null : userConvert.toSimpleVO(user);
    }

    @Override
    public List<UserSimpleVO> listSimpleByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysUser> users = sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getNickname, SysUser::getPhone)
                .in(SysUser::getId, userIds));
        return userConvert.toSimpleVOList(users);
    }

    @Override
    public long countByType(Integer userType) {
        Long count = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(userType != null, SysUser::getUserType, userType));
        return count == null ? 0L : count;
    }

    @Override
    public void updateLastLoginTime(Long userId) {
        SysUser update = new SysUser();
        update.setId(userId);
        update.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(update);
    }

    private SysUser selectSimpleById(Long userId) {
        return sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getNickname, SysUser::getPhone)
                .eq(SysUser::getId, userId));
    }
}