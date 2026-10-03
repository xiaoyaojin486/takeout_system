package com.takeout.dish.service;

import com.takeout.common.result.PageResult;
import com.takeout.dish.dto.DishDTO;
import com.takeout.dish.dto.DishQuery;
import com.takeout.dish.dto.DishStatusDTO;
import com.takeout.dish.vo.DishGroupVO;
import com.takeout.dish.vo.DishVO;

import java.util.List;

/**
 * 菜品服务
 */
public interface DishService {

    /** 商家端：我的菜品分页（仅自己的菜品） */
    PageResult<DishVO> merchantPage(DishQuery query);

    /** 商家端：新增菜品（校验分类属于自己） */
    void create(DishDTO dto);

    /** 商家端：编辑菜品（校验菜品与分类归属） */
    void update(Long id, DishDTO dto);

    /** 商家端：删除菜品（校验归属） */
    void delete(Long id);

    /** 商家端：上下架（校验归属） */
    void updateStatus(Long id, DishStatusDTO dto);

    /** 顾客端：按分类分组的菜单（仅上架菜品，按启用分类分组） */
    List<DishGroupVO> customerMenu(Long merchantId);
}