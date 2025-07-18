package com.xuesinuo.pignoo.core;

import java.util.List;
import java.util.function.Function;

/**
 * PignooList是Pignoo提供的List
 * <p>
 * PignooList is a List provided by Pignoo
 * <p>
 * 特点1 - 基于SQL操作：对List的操作，会被转为SQL操作，从而实现对数据库的操作
 * <p>
 * Features 1 - Based on SQL operation: The operations on the List will be converted to SQL operations, so that the database can be operated
 * <p>
 * 特点2 - 延迟查询：在使用过滤、排序后，不会立刻查询数据库，而是将查询条件缓存下来，在调用get等终结方法时查询
 * <p>
 * Features 2 - Lazy query: After filtering and sorting, the query will not be executed immediately, but the query conditions will be cached, and the query will be executed when the get method is
 * called
 * <p>
 * 特点3 - 筛选：筛选逻辑与{@link java.util.List}一致，可以把PignooList当成普通List用
 * <p>
 * Features 3 - Filter: The filtering logic is consistent with {@link java.util.List}, and you can treat PignooList as a normal List
 * <p>
 * 特点4 - JavaBean代理：查询出的JavaBean会被代理，操作从List取出的JavaBean时，数据会保存到数据库
 * <p>
 * Features 4 - JavaBean proxy: The JavaBean queried will be proxied, and the data will be saved to the database when the JavaBean is operated from the List
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
public interface PignooList<E> {

    /**
     * 复制一个List（保持当前的查询条件）
     * <p>
     * Copy a List (keep the current query conditions)
     * 
     * @return 复制后的List
     *         <p>
     *         The copied List
     */
    PignooList<E> copy();

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
    PignooList<E> sort(Function<E, ?> field, PignooSorter.SMode mode);

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
    PignooList<E> sort(PignooSorter<E> sorter);

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
    PignooList<E> filter(Function<E, ?> field, PignooFilter.FMode mode, Object... values);

    /**
     * 请参考{@link #filter(Function, com.xuesinuo.pignoo.core.PignooFilter.FMode, Object...)}
     * <p>
     * Please refer to {@link #filter(Function, com.xuesinuo.pignoo.core.PignooFilter.FMode, Object...)}
     */
    PignooList<E> filter(Function<E, ?> field, String mode, Object... values);

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
    PignooList<E> filter(PignooFilter<E> filter);

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
    PignooList<E> filter(Function<PignooFilter<E>, PignooFilter<E>> filterBuilder);

    /**
     * 新增一条数据
     * <p>
     * Add a data
     * 
     * @param e 数据
     *          <p>
     *          Data
     * @return 新增后的数据（从List反查出来的）
     *         <p>
     *         Data after adding (retrieved from the List)
     */
    E add(E e);

    /**
     * 根据主键修改数据：混入不为NULL的属性
     * <p>
     * Modify data by Primary-Key: mix in properties that are not NULL
     * 
     * @param e 数据
     *          <p>
     *          Data
     * @return 受影响条数
     *         <p>
     *         Number of affected entries
     */
    long mixById(E e);

    /**
     * 根据主键修改数据：完全替换
     * <p>
     * Modify data by Primary-Key: completely replace
     * 
     * @param e 数据
     *          <p>
     *          Data
     * @return 受影响条数
     *         <p>
     *         Number of affected entries
     */
    long replaceById(E e);

    /**
     * 修改数据：混入不为NULL的属性
     * <p>
     * Modify data: mix in properties that are not NULL
     * 
     * @param e 数据
     *          <p>
     *          Data
     * @return 受影响条数
     *         <p>
     *         Number of affected entries
     */
    long mixAll(E e);

    /**
     * 修改数据：完全替换
     * <p>
     * Modify data: completely replace
     * 
     * @param e 数据
     *          <p>
     *          Data
     * @return 受影响条数
     *         <p>
     *         Number of affected entries
     */
    long replaceAll(E e);

    /**
     * 根据主键删除数据
     * <p>
     * Delete data by Primary-Key
     * 
     * @param e 含主键的实体
     *          <p>
     *          data with Primary-Key
     * @return 受影响条数
     *         <p>
     *         Number of affected entries
     */
    long removeById(E e);

    /**
     * 删除数据
     * <p>
     * Delete data
     * 
     * @return 受影响条数
     *         <p>
     *         Number of affected entries
     */
    long removeAll();

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
