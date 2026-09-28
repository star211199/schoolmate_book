package com.schoolmate.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 统一分页结果。
 *
 * @author Albot
 */
@Data
public class PageResult<T> {

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Long pageNum;

    /** 每页条数 */
    private Long pageSize;

    /** 总页数 */
    private Long totalPages;

    /** 当前页数据 */
    private List<T> records;

    public PageResult() {
    }

    public PageResult(Long total, Long pageNum, Long pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalPages = pageSize == null || pageSize == 0 ? 0L : (total + pageSize - 1) / pageSize;
        this.records = records == null ? Collections.emptyList() : records;
    }

    /**
     * 将 MyBatis-Plus 的 IPage&lt;实体&gt; 转换为 IPage&lt;VO&gt; 后再包装，避免 entity 直接出参。
     */
    public static <E, V> PageResult<V> of(IPage<E> page, Function<E, V> converter) {
        List<V> records = page.getRecords().stream().map(converter).toList();
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    public static <V> PageResult<V> of(IPage<V> page) {
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}
