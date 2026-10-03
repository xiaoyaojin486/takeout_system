package com.takeout.user.api;

import com.takeout.user.api.vo.UserAuthVO;
import com.takeout.user.api.vo.UserSimpleVO;

import java.util.Collection;
import java.util.List;

/**
 * 账号模块对外契约
 *
 * <p>
 * 其他模块（auth / order / merchant-admin 等）只能依赖本接口，
 * 禁止注入 user 模块的 Service、Mapper、Entity。
 * </p>
 */
public interface UserApi {

    /** 按登录账号查认证信息（含密码密文），仅 auth 登录使用 */
    UserAuthVO findAuthByUsername(String username);

    /** 按 id 查认证信息，用于刷新令牌时校验账号状态 */
    UserAuthVO findAuthById(Long userId);

    /** 按 id 查简要信息 */
    UserSimpleVO getSimpleById(Long userId);

    /** 批量查简要信息 */
    List<UserSimpleVO> listSimpleByIds(Collection<Long> userIds);

    /**
     * 按身份统计账号数（管理后台看板用）
     *
     * @param userType null 表示统计全部
     */
    long countByType(Integer userType);

    /** 更新最后登录时间（登录成功后调用） */
    void updateLastLoginTime(Long userId);
}