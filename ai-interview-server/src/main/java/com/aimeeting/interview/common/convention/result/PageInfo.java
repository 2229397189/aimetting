package com.aimeeting.interview.common.convention.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.Data;

/**
 * 统一分页返回体。
 *
 * <p>由 MyBatis-Plus 的 {@link IPage} 转换而来，避免把分页实现细节暴露给前端。
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageInfo<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据。 */
    private List<T> list = new ArrayList<>();

    /** 总记录数。 */
    private long total = 0L;

    /** 当前页码（1-based）。 */
    private long pageNum = 1L;

    /** 每页条数。 */
    private long pageSize = 10L;

    /**
     * 直接由 {@link IPage} 转换（列表元素原样输出）。
     *
     * @param page MyBatis-Plus 分页对象
     * @param <T>  元素类型
     * @return 分页返回体
     */
    public static <T> PageInfo<T> of(IPage<T> page) {
        return of(page, Function.identity());
    }

    /**
     * 由 {@link IPage} 转换并逐条映射为目标类型（DO -&gt; Resp）。
     *
     * @param page   MyBatis-Plus 分页对象
     * @param mapper 元素映射函数
     * @param <T>    源元素类型
     * @param <R>    目标元素类型
     * @return 分页返回体
     */
    public static <T, R> PageInfo<R> of(IPage<T> page, Function<T, R> mapper) {
        PageInfo<R> pageInfo = new PageInfo<>();
        if (page == null) {
            return pageInfo;
        }
        List<R> records = new ArrayList<>();
        if (page.getRecords() != null) {
            for (T record : page.getRecords()) {
                records.add(mapper.apply(record));
            }
        }
        pageInfo.setList(records);
        pageInfo.setTotal(page.getTotal());
        pageInfo.setPageNum(page.getCurrent());
        pageInfo.setPageSize(page.getSize());
        return pageInfo;
    }
}
