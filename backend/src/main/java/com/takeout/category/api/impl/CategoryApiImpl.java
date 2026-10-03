package com.takeout.category.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.takeout.category.api.CategoryApi;
import com.takeout.category.api.vo.CategorySimpleVO;
import com.takeout.category.convert.CategoryConvert;
import com.takeout.category.entity.Category;
import com.takeout.category.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 分类模块对外契约实现
 *
 * <p>只做「查询 + 转换」，不写业务规则：跨模块调用方拿到的是干净的 VO。</p>
 */
@Service
@RequiredArgsConstructor
public class CategoryApiImpl implements CategoryApi {

    private final CategoryMapper categoryMapper;
    private final CategoryConvert categoryConvert;

    @Override
    public CategorySimpleVO getSimpleById(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryMapper.selectOne(selectSimpleColumns().eq(Category::getId, categoryId));
        return category == null ? null : categoryConvert.toSimpleVO(category);
    }

    @Override
    public List<CategorySimpleVO> listByMerchantId(Long merchantId) {
        if (merchantId == null) {
            return Collections.emptyList();
        }
        List<Category> categories = categoryMapper.selectList(selectSimpleColumns()
                .eq(Category::getMerchantId, merchantId));
        return categoryConvert.toSimpleVOList(categories);
    }

    @Override
    public List<CategorySimpleVO> listSimpleByIds(Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Category> categories = categoryMapper.selectList(selectSimpleColumns()
                .in(Category::getId, categoryIds));
        return categoryConvert.toSimpleVOList(categories);
    }

    /** 契约 VO 只需要的列，显式列出避免 SELECT * */
    private LambdaQueryWrapper<Category> selectSimpleColumns() {
        return Wrappers.<Category>lambdaQuery()
                .select(Category::getId, Category::getMerchantId, Category::getName,
                        Category::getSort, Category::getStatus);
    }
}