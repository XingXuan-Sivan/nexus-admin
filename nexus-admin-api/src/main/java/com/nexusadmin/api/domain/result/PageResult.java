package com.nexusadmin.api.domain.result;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 分页响应结果，扩展 {@link Result} 增加分页数据字段。
 * <p>
 * 用于需要返回分页列表的 API 场景，携带总记录数、当前页码、每页数量及当前页数据。
 * <p>
 * <strong>构造方式：</strong>
 * <ul>
 *   <li>静态工厂方法：适用于常规场景，如 {@code PageResult.of(total, page, size, items)}</li>
 *   <li>Builder 模式：适用于需要细粒度定制的场景</li>
 * </ul>
 *
 * @param <T> 数据项类型
 */
public class PageResult<T> extends Result {

    /** 总记录数。 */
    private final long total;

    /** 当前页码，从 1 开始。 */
    private final int page;

    /** 每页数量。 */
    private final int size;

    /** 当前页数据列表。 */
    private final List<T> items;

    /**
     * 构造分页响应结果。
     *
     * @param code    状态码
     * @param message 消息文本
     * @param total   总记录数
     * @param page    当前页码
     * @param size    每页数量
     * @param items   当前页数据
     */
    private PageResult(int code, String message, long total, int page, int size, List<T> items) {
        super(code, message);
        this.total = total;
        this.page = page;
        this.size = size;
        this.items = items;
    }

    /**
     * 获取总记录数。
     *
     * @return 总记录数
     */
    public long getTotal() {
        return total;
    }

    /**
     * 获取当前页码。
     *
     * @return 当前页码
     */
    public int getPage() {
        return page;
    }

    /**
     * 获取每页数量。
     *
     * @return 每页数量
     */
    public int getSize() {
        return size;
    }

    /**
     * 获取当前页数据列表。
     *
     * @return 当前页数据
     */
    public List<T> getItems() {
        return items;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 构造分页成功响应。
     *
     * @param total 总记录数
     * @param page  当前页码
     * @param size  每页数量
     * @param items 当前页数据
     * @param <T>   数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(long total, int page, int size, List<T> items) {
        return new PageResult<>(
                StatusCodes.SUCCESS.code(),
                StatusCodes.SUCCESS.message(),
                total,
                page,
                size,
                items != null ? items : Collections.emptyList()
        );
    }

    /**
     * 根据状态码构造分页响应。
     *
     * @param statusCode 状态码
     * @param total      总记录数
     * @param page       当前页码
     * @param size       每页数量
     * @param items      当前页数据
     * @param <T>        数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(StatusCode statusCode, long total, int page, int size, List<T> items) {
        Objects.requireNonNull(statusCode, "状态码不能为空");
        return new PageResult<>(
                statusCode.code(),
                statusCode.message(),
                total,
                page,
                size,
                items != null ? items : Collections.emptyList()
        );
    }

    // ==================== Builder ====================

    /**
     * 创建 Builder 实例。
     *
     * @param <T> 数据类型
     * @return Builder
     */
    public static <T> Builder<T> pageBuilder() {
        return new Builder<>();
    }

    /**
     * PageResult 构造器。
     *
     * @param <T> 数据类型
     */
    public static class Builder<T> {

        private int code;
        private String message;
        private long total;
        private int page;
        private int size;
        private List<T> items = Collections.emptyList();

        protected Builder() {
        }

        /**
         * 设置状态码数值。
         *
         * @param code 状态码
         * @return 当前 Builder
         */
        public Builder<T> code(int code) {
            this.code = code;
            return this;
        }

        /**
         * 设置消息文本。
         *
         * @param message 消息文本
         * @return 当前 Builder
         */
        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        /**
         * 从状态码契约填充 code 和 message。
         *
         * @param statusCode 状态码
         * @return 当前 Builder
         */
        public Builder<T> statusCode(StatusCode statusCode) {
            Objects.requireNonNull(statusCode, "状态码不能为空");
            this.code = statusCode.code();
            this.message = statusCode.message();
            return this;
        }

        /**
         * 设置总记录数。
         *
         * @param total 总记录数
         * @return 当前 Builder
         */
        public Builder<T> total(long total) {
            this.total = total;
            return this;
        }

        /**
         * 设置当前页码。
         *
         * @param page 当前页码
         * @return 当前 Builder
         */
        public Builder<T> page(int page) {
            this.page = page;
            return this;
        }

        /**
         * 设置每页数量。
         *
         * @param size 每页数量
         * @return 当前 Builder
         */
        public Builder<T> size(int size) {
            this.size = size;
            return this;
        }

        /**
         * 设置当前页数据列表。
         *
         * @param items 当前页数据
         * @return 当前 Builder
         */
        public Builder<T> items(List<T> items) {
            this.items = items != null ? items : Collections.emptyList();
            return this;
        }

        /**
         * 构造 PageResult 实例。
         *
         * @return 分页响应结果
         */
        public PageResult<T> build() {
            return new PageResult<>(code, message, total, page, size, items);
        }
    }
}
