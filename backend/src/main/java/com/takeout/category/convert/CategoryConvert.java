package com.takeout.category.convert;

import com.takeout.category.api.vo.CategorySimpleVO;
import com.takeout.category.entity.Category;
import com.takeout.category.vo.CategoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 菜品分类对象转换：Entity ↔ VO 与 Entity ↔ 跨模块契约 VO
 */
@Mapper
public interface CategoryConvert {

    /** Entity → 前端 VO；dishCount 需要跨模块计数，这里不映射，保持为 null */
    @Mapping(target = "dishCount", ignore = true)
    CategoryVO toVO(Category entity);

    List<CategoryVO> toVOList(List<Category> entities);

    /** Entity → 跨模块简要 VO（实体主键是 id，契约 VO 用 categoryId 命名） */
    @Mapping(target = "categoryId", source = "id")
    CategorySimpleVO toSimpleVO(Category entity);

    /** 批量转换：MapStruct 会复用上面的单元素映射方法 */
    List<CategorySimpleVO> toSimpleVOList(List<Category> entities);
}