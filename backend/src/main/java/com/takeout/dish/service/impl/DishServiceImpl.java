package com.takeout.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.takeout.category.api.CategoryApi;
import com.takeout.category.api.vo.CategorySimpleVO;
import com.takeout.common.context.UserContextHolder;
import com.takeout.common.exception.BusinessException;
import com.takeout.common.result.PageResult;
import com.takeout.common.result.ResultCode;
import com.takeout.dish.convert.DishConvert;
import com.takeout.dish.dto.DishDTO;
import com.takeout.dish.dto.DishQuery;
import com.takeout.dish.dto.DishStatusDTO;
import com.takeout.dish.entity.Dish;
import com.takeout.dish.mapper.DishMapper;
import com.takeout.dish.service.DishService;
import com.takeout.dish.vo.DishGroupVO;
import com.takeout.dish.vo.DishVO;
import com.takeout.merchant.api.MerchantApi;
import com.takeout.merchant.api.vo.MerchantSimpleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜品服务实现
 *
 * <p>菜品归属店铺、分类归属店铺：商家端写操作先反查自己的 merchantId，
 * 菜品与分类都必须落在自己名下，前端传入的 id 一律不可信。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private static final Integer STATUS_ON_SHELF = 1;
    private static final Integer CATEGORY_ENABLED = 1;

    private final DishMapper dishMapper;
    private final DishConvert dishConvert;
    private final CategoryApi categoryApi;
    private final MerchantApi merchantApi;

    @Override
    public PageResult<DishVO> merchantPage(DishQuery query) {
        Long merchantId = myMerchantId();
        LambdaQueryWrapper<Dish> wrapper = selectColumns()
                // 商家端只能看到自己的菜品，这里用反查出的 merchantId 硬过滤
                .eq(Dish::getMerchantId, merchantId)
                .eq(query.getCategoryId() != null, Dish::getCategoryId, query.getCategoryId())
                .eq(query.getStatus() != null, Dish::getStatus, query.getStatus());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Dish::getName, query.getKeyword().trim());
        }
        wrapper.orderByDesc(Dish::getCreateTime);

        Page<Dish> page = dishMapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page, fillCategoryName(page.getRecords()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(DishDTO dto) {
        Long merchantId = myMerchantId();
        requireOwnCategory(dto.getCategoryId(), merchantId);

        Dish entity = new Dish();
        entity.setMerchantId(merchantId);
        entity.setCategoryId(dto.getCategoryId());
        entity.setName(dto.getName().trim());
        entity.setImage(dto.getImage());
        entity.setDescription(dto.getDescription());
        entity.setPrice(dto.getPrice());
        entity.setStock(dto.getStock());
        entity.setSales(0);
        entity.setStatus(dto.getStatus() == null ? STATUS_ON_SHELF : dto.getStatus());
        dishMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DishDTO dto) {
        Dish dish = requireOwn(id);
        // 换分类时新分类也必须属于自己，否则会把菜品挂到别人店下
        requireOwnCategory(dto.getCategoryId(), dish.getMerchantId());

        Dish update = new Dish();
        update.setId(dish.getId());
        update.setCategoryId(dto.getCategoryId());
        update.setName(dto.getName().trim());
        update.setImage(dto.getImage());
        update.setDescription(dto.getDescription());
        update.setPrice(dto.getPrice());
        update.setStock(dto.getStock());
        update.setStatus(dto.getStatus());
        dishMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Dish dish = requireOwn(id);
        dishMapper.deleteById(dish.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, DishStatusDTO dto) {
        Dish dish = requireOwn(id);

        Dish update = new Dish();
        update.setId(dish.getId());
        update.setStatus(dto.getStatus());
        dishMapper.updateById(update);

        log.info("商家修改菜品状态 dishId={} status={}", dish.getId(), dto.getStatus());
    }

    @Override
    public List<DishGroupVO> customerMenu(Long merchantId) {
        if (merchantId == null) {
            return Collections.emptyList();
        }

        // 分类被禁用即整组隐藏，因此先按启用状态筛出有效分类，并保持 sort 顺序作为分组顺序
        List<CategorySimpleVO> categories = categoryApi.listByMerchantId(merchantId).stream()
                .filter(category -> CATEGORY_ENABLED.equals(category.getStatus()))
                .sorted(Comparator
                        .comparing(CategorySimpleVO::getSort,
                                Comparator.comparingInt(sort -> sort == null ? Integer.MAX_VALUE : sort))
                        .thenComparing(CategorySimpleVO::getCategoryId))
                .collect(Collectors.toList());
        if (categories.isEmpty()) {
            return Collections.emptyList();
        }

        List<Dish> dishes = dishMapper.selectList(selectColumns()
                .eq(Dish::getMerchantId, merchantId)
                .eq(Dish::getStatus, STATUS_ON_SHELF));
        Map<Long, List<DishVO>> dishGroupMap = dishes.stream()
                .map(dish -> {
                    DishVO vo = dishConvert.toVO(dish);
                    vo.setStatusDesc(statusDesc(vo.getStatus()));
                    return vo;
                })
                .collect(Collectors.groupingBy(DishVO::getCategoryId));

        List<DishGroupVO> groups = new ArrayList<>();
        for (CategorySimpleVO category : categories) {
            List<DishVO> groupDishes = dishGroupMap.get(category.getCategoryId());
            // 分类下没有上架菜品时跳过该分组，避免前端出现空锚点
            if (groupDishes == null || groupDishes.isEmpty()) {
                continue;
            }
            groupDishes.forEach(vo -> vo.setCategoryName(category.getName()));

            DishGroupVO group = new DishGroupVO();
            group.setCategoryId(category.getCategoryId());
            group.setCategoryName(category.getName());
            group.setDishes(groupDishes);
            groups.add(group);
        }
        return groups;
    }

    // ==================== 内部方法 ====================

    /** 显式列出需要的列，避免 SELECT * */
    private LambdaQueryWrapper<Dish> selectColumns() {
        return Wrappers.<Dish>lambdaQuery()
                .select(Dish::getId, Dish::getMerchantId, Dish::getCategoryId, Dish::getName,
                        Dish::getImage, Dish::getDescription, Dish::getPrice, Dish::getStock,
                        Dish::getSales, Dish::getStatus, Dish::getCreateTime);
    }

    /** 取当前登录商家的店铺 id（路径前缀卡不到「哪条数据」，归属校验的唯一可信来源是 userId） */
    private Long myMerchantId() {
        MerchantSimpleVO merchant = merchantApi.getSimpleByUserId(UserContextHolder.getUserId());
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_FOUND);
        }
        return merchant.getMerchantId();
    }

    /** 校验菜品存在且属于当前登录商家 */
    private Dish requireOwn(Long id) {
        Dish dish = dishMapper.selectOne(selectColumns().eq(Dish::getId, id));
        if (dish == null) {
            throw new BusinessException(ResultCode.DISH_NOT_FOUND);
        }
        if (!myMerchantId().equals(dish.getMerchantId())) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_OWNER);
        }
        return dish;
    }

    /** 校验分类存在且属于指定店铺；路径前缀只能卡到端，分类是否属于自己必须显式比对 */
    private void requireOwnCategory(Long categoryId, Long merchantId) {
        CategorySimpleVO category = categoryApi.getSimpleById(categoryId);
        if (category == null || !merchantId.equals(category.getMerchantId())) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
    }

    /** 批量补全分类名称；一次性 in 查询，避免逐条查询的 N+1 */
    private List<DishVO> fillCategoryName(List<Dish> dishes) {
        List<DishVO> voList = dishConvert.toVOList(dishes);
        if (voList.isEmpty()) {
            return voList;
        }
        Set<Long> categoryIds = dishes.stream()
                .map(Dish::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = categoryApi.listSimpleByIds(categoryIds).stream()
                .collect(Collectors.toMap(CategorySimpleVO::getCategoryId, CategorySimpleVO::getName, (a, b) -> a));
        for (DishVO vo : voList) {
            vo.setCategoryName(categoryNameMap.get(vo.getCategoryId()));
            vo.setStatusDesc(statusDesc(vo.getStatus()));
        }
        return voList;
    }

    private String statusDesc(Integer status) {
        if (status == null) {
            return null;
        }
        return STATUS_ON_SHELF.equals(status) ? "上架" : "下架";
    }
}