package com.takeout.category.api;

import com.takeout.category.api.vo.CategorySimpleVO;

import java.util.Collection;
import java.util.List;

/**
 * 分类模块对外契约
 */
public interface CategoryApi {

    CategorySimpleVO getSimpleById(Long categoryId);

    List<CategorySimpleVO> listByMerchantId(Long merchantId);

    List<CategorySimpleVO> listSimpleByIds(Collection<Long> categoryIds);
}