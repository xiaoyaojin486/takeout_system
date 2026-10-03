package com.takeout.dish.convert;

import com.takeout.dish.api.vo.DishSimpleVO;
import com.takeout.dish.entity.Dish;
import com.takeout.dish.vo.DishVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 菜品对象转换：Entity ↔ VO 与 Entity ↔ 跨模块契约 VO
 *
 * <p>分类名、状态描述需要额外查询或计算，由 Service 补全，这里只搬字段。</p>
 */
@Mapper
public interface DishConvert {

    /** Entity → 前端 VO：categoryName 需跨模块查分类，statusDesc 需按状态计算，均在此忽略 */
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "statusDesc", ignore = true)
    DishVO toVO(Dish entity);

    List<DishVO> toVOList(List<Dish> entities);

    /** Entity → 跨模块简要 VO（实体主键是 id，契约 VO 用 dishId 命名） */
    @Mapping(target = "dishId", source = "id")
    DishSimpleVO toSimpleVO(Dish entity);

    /** 批量转换：MapStruct 会复用上面的单元素映射方法 */
    List<DishSimpleVO> toSimpleVOList(List<Dish> entities);
}