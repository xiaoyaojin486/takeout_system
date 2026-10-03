package com.takeout.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.takeout.category.convert.CategoryConvert;
import com.takeout.category.dto.CategoryDTO;
import com.takeout.category.entity.Category;
import com.takeout.category.mapper.CategoryMapper;
import com.takeout.category.service.CategoryService;
import com.takeout.category.vo.CategoryVO;
import com.takeout.common.context.UserContextHolder;
import com.takeout.common.exception.BusinessException;
import com.takeout.common.result.ResultCode;
import com.takeout.merchant.api.MerchantApi;
import com.takeout.merchant.api.vo.MerchantSimpleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 菜品分类服务实现
 *
 * <p>分类归属店铺：商家端每个写操作都先反查自己的 merchantId，再比对分类的 merchantId，
 * 前端传入的分类 id 本身不可信。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final Integer STATUS_ENABLED = 1;

    private final CategoryMapper categoryMapper;
    private final CategoryConvert categoryConvert;
    private final MerchantApi merchantApi;

    @Override
    public List<CategoryVO> listMine() {
        Long merchantId = myMerchantId();
        List<Category> list = categoryMapper.selectList(selectColumns()
                .eq(Category::getMerchantId, merchantId)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
        return categoryConvert.toVOList(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CategoryDTO dto) {
        Category entity = new Category();
        entity.setMerchantId(myMerchantId());
        entity.setName(dto.getName().trim());
        entity.setSort(dto.getSort());
        entity.setStatus(dto.getStatus() == null ? STATUS_ENABLED : dto.getStatus());
        categoryMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CategoryDTO dto) {
        Category category = requireOwn(id);

        Category update = new Category();
        update.setId(category.getId());
        update.setName(dto.getName().trim());
        update.setSort(dto.getSort());
        update.setStatus(dto.getStatus());
        categoryMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Category category = requireOwn(id);
        // 分类下是否存在菜品的拦截不在这里做：category 模块不允许直接查 dish 表，
        // 跨模块契约 DishApi 也未提供计数方法。数据库不建外键，孤儿菜品由菜品列表查询兜底展示。
        categoryMapper.deleteById(category.getId());

        log.info("商家删除分类 categoryId={} merchantId={}", category.getId(), category.getMerchantId());
    }

    @Override
    public List<CategoryVO> customerListByMerchant(Long merchantId) {
        if (merchantId == null) {
            return Collections.emptyList();
        }
        List<Category> list = categoryMapper.selectList(selectColumns()
                .eq(Category::getMerchantId, merchantId)
                .eq(Category::getStatus, STATUS_ENABLED)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
        return categoryConvert.toVOList(list);
    }

    // ==================== 内部方法 ====================

    /** 显式列出需要的列，避免 SELECT * */
    private LambdaQueryWrapper<Category> selectColumns() {
        return Wrappers.<Category>lambdaQuery()
                .select(Category::getId, Category::getMerchantId, Category::getName,
                        Category::getSort, Category::getStatus);
    }

    /** 取当前登录商家的店铺 id；路径前缀只能卡到「商家端」，卡不到「哪条分类」，故必须按 userId 反查归属 */
    private Long myMerchantId() {
        MerchantSimpleVO merchant = merchantApi.getSimpleByUserId(UserContextHolder.getUserId());
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_FOUND);
        }
        return merchant.getMerchantId();
    }

    /** 校验分类存在且属于当前登录商家 */
    private Category requireOwn(Long id) {
        Category category = categoryMapper.selectOne(selectColumns().eq(Category::getId, id));
        if (category == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        if (!myMerchantId().equals(category.getMerchantId())) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_OWNER);
        }
        return category;
    }
}