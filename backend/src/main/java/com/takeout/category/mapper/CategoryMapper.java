package com.takeout.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takeout.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜品分类 Mapper：只做数据库访问，不写业务逻辑
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}