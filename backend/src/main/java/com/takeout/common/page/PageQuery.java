package com.takeout.common.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * 分页查询基类：所有 XxxQuery 继承它，统一分页参数与边界保护
 */
@Data
public class PageQuery {

    /** 默认 1；非法值统一矫正，避免 pageNum=0 造成 OFFSET 负数 */
    private Integer pageNum = 1;

    /** 默认 10，上限 100：防止前端传 pageSize=100000 拖垮数据库 */
    private Integer pageSize = 10;

    /** 最大每页条数 */
    private static final int MAX_PAGE_SIZE = 100;

    public Integer getPageNum() {
        return (pageNum == null || pageNum < 1) ? 1 : pageNum;
    }

    public Integer getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /** 转 MyBatis-Plus 分页对象 */
    public <T> Page<T> toPage() {
        return new Page<>(getPageNum(), getPageSize());
    }
}