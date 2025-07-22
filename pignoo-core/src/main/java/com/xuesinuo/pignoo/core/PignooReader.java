package com.xuesinuo.pignoo.core;

import java.util.List;
import java.util.function.Function;

/**
 * PignooReader是Pignoo提供的读取用List，直接实现PignooReader一般视为只读的List
 * <p>
 * PignooReader is a read-only List provided by Pignoo. Directly implementing PignooReader is generally regarded as a read-only List
 * <p>
 * 只读List只提供读取功能
 * <p>
 * Read-only List only provides read-only functions
 * <p>
 * 只读List读取出的对象做修改操作，不会映射到数据库
 * <p>
 * The objects read from the read-only List are modified, and the modifications will not be mapped to the database
 * <p>
 * 在事务中的只读查询不会形写锁
 * <p>
 * Read-only queries in transactions will not form write locks
 * 
 * @author xuesinuo
 * @since 0.2.3
 */
public interface PignooReader<E> {

    /**
     * 复制一个读写List（保持当前的查询条件）
     * <p>
     * Copy a read-write List (keep the current query conditions)
     * 
     * @return 复制后的List
     *         <p>
     *         The copied List
     */
    PignooWriter<E> copyWriter();

    /**
     * 复制一个只读List（保持当前的查询条件）
     * <p>
     * Copy a read-only List (keep the current query conditions)
     * 
     * @return 复制后的List
     *         <p>
     *         The copied List
     */
    PignooReader<E> copyReader();

    /**
     * 当前PignooWriter是否只读
     * <p>
     * Whether the current PignooWriter is read-only
     * 
     * @return 是否只读
     *         <p>
     *         Whether it is read-only
     */
    boolean isReadOnly();

    /**
     * 获取表中的第一条数据
     * <p>
     * Get the first data in the table
     * 
     * @return 第一条数据，可能为null
     *         <p>
     *         The first data, may be null
     */
    E getOne();

    /**
     * 获取列表
     * <p>
     * Get the list
     * 
     * @return 列表
     *         <p>
     *         The list
     */
    List<E> getAll();

    /**
     * 获取部分List
     * <p>
     * Get a part of the list
     * 
     * @param offset 跳过条数
     *               <p>
     *               Number of entries to skip
     * @param limit  获取条数
     *               <p>
     *               Number of entries to get
     * @return 部分List
     *         <p>
     *         Part of the list
     */
    List<E> get(long offset, long count);

    /**
     * 获取List大小
     * <p>
     * Get the size of the list
     * 
     * @return 大小
     *         <p>
     *         The size
     */
    long size();

    /**
     * 排序
     * <p>
     * Sort
     * 
     * @param field 排序字段
     *              <p>
     *              Sorting field
     * @param mode  排序方式
     *              <p>
     *              Sorting mode
     * @return 排序后的List
     *         <p>
     *         The sorted list
     */
    PignooReader<E> sort(Function<E, ?> field, PignooSorter.SMode mode);

    /**
     * 排序
     * <p>
     * Sort
     * 
     * @param sorter 排序器
     *               <p>
     *               Sorter
     * @return 排序后的List
     *         <p>
     *         The sorted list
     */
    PignooReader<E> sort(PignooSorter<E> sorter);

    /**
     * 过滤
     * <p>
     * Filter
     * 
     * @param field  过滤字段
     *               <p>
     *               Filtering field
     * @param mode   过滤方式
     *               <p>
     *               Filtering mode
     * @param values 过滤值
     *               <p>
     *               Filtering value
     * @return 过滤后的List
     *         <p>
     *         The filtered list
     */
    PignooReader<E> filter(Function<E, ?> field, PignooFilter.FMode mode, Object... values);

    /**
     * 请参考{@link #filter(Function, com.xuesinuo.pignoo.core.PignooFilter.FMode, Object...)}
     * <p>
     * Please refer to {@link #filter(Function, com.xuesinuo.pignoo.core.PignooFilter.FMode, Object...)}
     */
    PignooReader<E> filter(Function<E, ?> field, String mode, Object... values);

    /**
     * 过滤
     * <p>
     * Filter
     * 
     * @param filter 过滤器
     *               <p>
     *               Filter
     * @return 过滤后的List
     *         <p>
     *         The filtered list
     */
    PignooReader<E> filter(PignooFilter<E> filter);

    /**
     * 过滤
     * <p>
     * Filter
     * 
     * @param filterBuilder 过滤器
     *                      <p>
     *                      Filter
     * @return 过滤后的List
     *         <p>
     *         The filtered list
     */
    PignooReader<E> filter(Function<PignooFilter<E>, PignooFilter<E>> filterBuilder);

    /**
     * 求和
     * <p>
     * Sum
     * 
     * @param field 求和的字段
     *              <p>
     *              Field
     * @param c     结果类型
     *              <p>
     *              Class of result
     * @return 求和结果
     *         <p>
     *         result
     */
    <R> R sum(Function<E, R> field, Class<R> c);

    /**
     * 求平均
     * <p>
     * Avg
     * 
     * @param field 求平均的字段
     *              <p>
     *              Field
     * @param c     结果类型
     *              <p>
     *              Class of result
     * @return 求平均结果
     *         <p>
     *         result of avg
     */
    <R> R avg(Function<E, R> field, Class<R> c);
}
