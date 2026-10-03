package com.takeout.category.service;

import com.takeout.category.dto.CategoryDTO;
import com.takeout.category.vo.CategoryVO;

import java.util.List;

/**
 * 菜品分类服务
 */
public interface CategoryService {

    /** 商家端：我的分类列表（按 sort 升序） */
    List<CategoryVO> listMine();

    /** 商家端：新增分类 */
    void create(CategoryDTO dto);

    /** 商家端：编辑分类（校验归属） */
    void update(Long id, CategoryDTO dto);

    /** 商家端：删除分类（校验归属） */
    void delete(Long id);

    /** 顾客端：按店铺查启用中的分类 */
    List<CategoryVO> customerListByMerchant(Long merchantId);
}