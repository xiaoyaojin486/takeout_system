package com.takeout.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takeout.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账号 Mapper：只做数据库访问，不写业务逻辑
 *
 * <p>本模块的查询都走 MyBatis-Plus 条件构造器，字段显式指定，不使用 SELECT *。</p>
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}