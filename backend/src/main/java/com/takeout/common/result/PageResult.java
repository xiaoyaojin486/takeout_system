package com.takeout.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一分页返回体
 */
@Data
public class PageResult<T> implements Serializable {

    /** 当前页数据 */
    private List<T> list;

    /** 总条数 */
    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    /** Entity/PO 分页直接转 VO 分页（转换在 Convert 里做，这里只负责搬运） */
    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> mapper) {
        PageResult<T> result = new PageResult<>();
        result.setList(page.getRecords() == null ? Collections.emptyList()
                : page.getRecords().stream().map(mapper).collect(Collectors.toList()));
        result.setTotal(page.getTotal());
        result.setPageNum((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        return result;
    }

    /** 已经转好 VO 的情况 */
    public static <T> PageResult<T> of(IPage<?> page, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setList(list == null ? Collections.emptyList() : list);
        result.setTotal(page.getTotal());
        result.setPageNum((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        return result;
    }

    /** 空结果 */
    public static <T> PageResult<T> empty(Integer pageNum, Integer pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(Collections.emptyList());
        result.setTotal(0L);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }
}