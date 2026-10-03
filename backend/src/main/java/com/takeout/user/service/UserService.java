package com.takeout.user.service;

import com.takeout.common.result.PageResult;
import com.takeout.user.dto.ChangePasswordDTO;
import com.takeout.user.dto.RegisterDTO;
import com.takeout.user.dto.UpdateProfileDTO;
import com.takeout.user.dto.UserQuery;
import com.takeout.user.vo.UserVO;

/**
 * 账号服务
 */
public interface UserService {

    /** 顾客注册：账号唯一校验 + BCrypt 加密后落库 */
    void register(RegisterDTO dto);

    /** 当前登录用户的资料 */
    UserVO getProfile();

    /** 修改当前登录用户的资料（账号名与身份不可改） */
    void updateProfile(UpdateProfileDTO dto);

    /** 修改当前登录用户的密码（必须校验原密码） */
    void changePassword(ChangePasswordDTO dto);

    /** 管理后台：账号分页 */
    PageResult<UserVO> page(UserQuery query);

    /** 管理后台：启用/停用账号（不允许停用管理员） */
    void updateStatus(Long userId, Integer status);

    /** 管理后台：重置他人密码 */
    void resetPassword(Long userId, String newPassword);
}