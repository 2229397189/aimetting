package com.aimeeting.interview.common.convention.result;

import java.io.Serializable;
import lombok.Data;

/**
 * 分页查询基类，各列表接口的查询 DTO 继承本类并追加筛选字段。
 *
 * <p>默认第 1 页、每页 10 条；页码与页大小均做了上界保护，防止恶意大分页拖垮数据库。
 */
@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 最大页大小，防止一次性拉取全表。 */
    public static final long MAX_PAGE_SIZE = 200L;

    /** 页码，1-based。 */
    private long pageNum = 1L;

    /** 每页条数。 */
    private long pageSize = 10L;

    /**
     * 取得修正后的页码（小于 1 时归一到 1）。
     *
     * @return 合法页码
     */
    public long safePageNum() {
        return pageNum < 1L ? 1L : pageNum;
    }

    /**
     * 取得修正后的页大小（小于 1 归为 10，大于 {@link #MAX_PAGE_SIZE} 截断）。
     *
     * @return 合法页大小
     */
    public long safePageSize() {
        if (pageSize < 1L) {
            return 10L;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
