package com.xuesinuo.pignoo.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.xuesinuo.pignoo.core.PignooSorter.SMode;

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
 * @param <E> JavaBean Type
 * @author xuesinuo
 * @since 0.2.3
 * @version 1.1.0
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
     * 获取第一条数据
     * <p>
     * Get the first data
     *
     * @return 第一条数据，可能为null
     *         <p>
     *         The first data, may be null
     */
    E getFirst();

    /**
     * 获取满足条件的一条数据
     * <p>
     * Get one data that meets the condition
     * 
     * @return 满足条件的一条数据，可能为null
     *         <p>
     *         One data that meets the condition, may be null
     */
    E getAny();

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
     * @param count  获取条数
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
     * 在满足条件后，单一条件过滤
     * <p>
     * Single condition filter after satisfying the condition
     * 
     * @param condition 条件：仅为True时使用此条件
     *                  <p>
     *                  Condition: use this condition only when it is True
     * @param field     过滤字段
     *                  <p>
     *                  Filtering field
     * @param mode      过滤方式
     *                  <p>
     *                  Filtering mode
     * @param values    过滤值
     *                  <p>
     *                  Filtering value
     * @return 过滤后的结果
     *         <p>
     *         The filtered result
     */
    PignooReader<E> filter(Boolean condition, Function<E, ?> field, PignooFilter.FMode mode, Object... values);

    /**
     * 请参考{@link #filter(Boolean, Function, com.xuesinuo.pignoo.core.PignooFilter.FMode, Object...)}
     * <p>
     * Please refer to {@link #filter(Boolean, Function, com.xuesinuo.pignoo.core.PignooFilter.FMode, Object...)}
     * 
     * @param condition 条件：仅为True时使用此条件
     *                  <p>
     *                  Condition: use this condition only when it is True
     * @param field     过滤字段
     *                  <p>
     *                  Filtering field
     * @param mode      过滤方式
     *                  <p>
     *                  Filtering mode
     * @param values    过滤值
     *                  <p>
     *                  Filtering value
     * @return 过滤后的结果
     *         <p>
     *         The filtered result
     */
    PignooReader<E> filter(Boolean condition, Function<E, ?> field, String mode, Object... values);

    /**
     * 单一条件过滤
     * <p>
     * Single condition filter
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
     * @return 过滤后的结果
     *         <p>
     *         The filtered result
     */
    PignooReader<E> filter(Function<E, ?> field, PignooFilter.FMode mode, Object... values);

    /**
     * 请参考{@link #filter(Function, com.xuesinuo.pignoo.core.PignooFilter.FMode, Object...)}
     * <p>
     * Please refer to {@link #filter(Function, com.xuesinuo.pignoo.core.PignooFilter.FMode, Object...)}
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
     * @return 过滤后的结果
     *         <p>
     *         The filtered result
     */
    PignooReader<E> filter(Function<E, ?> field, String mode, Object... values);

    /**
     * 最通用的过滤条件，使用PignooFilter嵌套，构建一个复杂的Filter后，应用它
     * <p>
     * The most general filter condition, use PignooFilter nesting, build a complex Filter, and apply it
     *
     * @param filter 过滤器
     *               <p>
     *               Filter
     * @return 过滤后的结果
     *         <p>
     *         The filtered result
     */
    PignooReader<E> filter(PignooFilter<E> filter);

    /**
     * 使用函数方式创建构造器，适合复杂度适中的过滤条件
     * <p>
     * Use the function method to create a constructor, suitable for moderate complexity filter conditions
     *
     * @param filterBuilder 过滤器构造函数
     *                      <p>
     *                      Filter
     * @return 过滤后的结果
     *         <p>
     *         The filtered result
     */
    PignooReader<E> filter(Function<PignooFilter<E>, PignooFilter<E>> filterBuilder);

    /**
     * 求最大值
     * <p>
     * Max
     *
     * @param field 求最大值的字段
     *              <p>
     *              Field
     * @param c     结果类型
     *              <p>
     *              Class of result
     * @param <R>   结果类型
     *              <p>
     *              Class of result
     * @return 求最大值结果
     *         <p>
     *         result
     */
    <R> R max(Function<E, R> field, Class<R> c);

    /**
     * 求最大值
     * <p>
     * Max
     *
     * @param field  求最大值的字段
     *               <p>
     *               Field
     * @param c      结果类型
     *               <p>
     *               Class of result
     * @param nullAs 将null视为此值计算
     *               <p>
     *               View null as this value
     * @param <R>    结果类型
     *               <p>
     *               Class of result
     * @return 求最大值结果
     *         <p>
     *         result
     */
    <R> R maxNullAs(Function<E, R> field, Class<R> c, R nullAs);

    /**
     * 求最小值
     * <p>
     * Min
     *
     * @param field 求最小值的字段
     *              <p>
     *              Field
     * @param c     结果类型
     *              <p>
     *              Class of result
     * @param <R>   结果类型
     *              <p>
     *              Class of result
     * @return 求最小值结果
     *         <p>
     *         result
     */
    <R> R min(Function<E, R> field, Class<R> c);

    /**
     * 求最小值
     * <p>
     * Min
     *
     * @param field  求最小值的字段
     *               <p>
     *               Field
     * @param c      结果类型
     *               <p>
     *               Class of result
     * @param nullAs 将null视为此值计算
     *               <p>
     *               View null as this value
     * @param <R>    结果类型
     *               <p>
     *               Class of result
     * @return 求最小值结果
     *         <p>
     *         result
     */
    <R> R minNullAs(Function<E, R> field, Class<R> c, R nullAs);

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
     * @param <R>   结果类型
     *              <p>
     *              Class of result
     * @return 求和结果
     *         <p>
     *         result
     */
    <R> R sum(Function<E, R> field, Class<R> c);

    /**
     * 求和
     * <p>
     * Sum
     *
     * @param field  求和的字段
     *               <p>
     *               Field
     * @param c      结果类型
     *               <p>
     *               Class of result
     * @param nullAs 将null视为此值计算
     *               <p>
     *               View null as this value
     * @param <R>    结果类型
     *               <p>
     *               Class of result
     * @return 求和结果
     *         <p>
     *         result
     */
    <R> R sumNullAs(Function<E, R> field, Class<R> c, R nullAs);

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
     * @param <R>   结果类型
     *              <p>
     *              Class of result
     * @return 求平均结果
     *         <p>
     *         result of avg
     */
    <R> R avg(Function<E, R> field, Class<R> c);

    /**
     * 求平均
     * <p>
     * Avg
     *
     * @param field  求平均的字段
     *               <p>
     *               Field
     * @param c      结果类型
     *               <p>
     *               Class of result
     * @param nullAs 将null视为此值计算
     *               <p>
     *               View null as this value
     * @param <R>    结果类型
     *               <p>
     *               Class of result
     * @return 求平均结果
     *         <p>
     *         result of avg
     */
    <R> R avgNullAs(Function<E, R> field, Class<R> c, R nullAs);

    /**
     * 求不重复的总条数
     * <p>
     * Count distinct
     *
     * @param field 求总条数的字段
     *              <p>
     *              Field
     * @param <R>   字段类型
     *              <p>
     *              Class of result
     * @return 求总条数结果
     *         <p>
     *         result of avg
     */
    <R> long countDistinct(Function<E, R> field);

    /**
     * 求不重复的总条数
     * <p>
     * Count distinct
     *
     * @param field  求总条数的字段
     *               <p>
     *               Field
     * @param nullAs 将null视为此值计算
     *               <p>
     *               View null as this value
     * @param <R>    字段类型
     *               <p>
     *               Class of result
     * @return 求总条数结果
     *         <p>
     *         result of avg
     */
    <R> long countDistinctNullAs(Function<E, R> field, R nullAs);

    /**
     * 是否包含指定ID
     * <p>
     * Is contains id
     * 
     * @param e 含ID的实体
     * @return 是否包含指定ID
     */
    boolean containsId(E e);

    /**
     * 是否全包含指定ID
     * <p>
     * Is contains all ids
     * 
     * @param collection 含ID的实体集合
     * @return 是否全包含指定ID
     */
    boolean containsIds(Collection<E> collection);

    /**
     * 默认迭代器：按ID从前到后遍历全部数据
     * <p>
     * Default iterator: Traverse all data from front to back by ID
     * <p>
     * reader获取的遍历器是只读的，writer获取的遍历器是可写的
     * <p>
     * The iterator obtained by 'reader' is read-only, and the iterator obtained by 'writer' is writable
     * 
     * @return iterator
     */
    Iterator<E> iterator();

    /**
     * 步长迭代器
     * <p>
     * Step iterator
     * 
     * @param step 步长：每次从数据源查询这么多条并缓存，遍历完后再查询
     *             <p>
     *             Step: How many to query each time, and then query again when the data is traversed
     * @return iterator
     */
    Iterator<E> iterator(int step);

    /**
     * 含排序方式的步长迭代器
     * <p>
     * Step iterator with sort mode
     * 
     * @param step       步长：每次从数据源查询这么多条并缓存，遍历完后再查询
     *                   <p>
     *                   Step: How many to query each time, and then query again when the data is traversed
     * @param idSortMode ID排序方式
     *                   <p>
     *                   ID sort mode
     * @return iterator
     */
    Iterator<E> iterator(int step, SMode idSortMode);

    /**
     * 含偏移量、总条数的步长迭代器
     * <p>
     * Step iterator with offset and limit
     * 
     * @param step       步长：每次从数据源查询这么多条并缓存，遍历完后再查询
     *                   <p>
     *                   Step: How many to query each time, and then query again when the data is traversed
     * @param idSortMode ID排序方式
     *                   <p>
     *                   ID sort mode
     * @param offset     偏移量：跳过offset条数据后开始遍历
     *                   <p>
     *                   Skip 'offset' data and start traversing
     * @param limit      总条数：最多遍历limit条数据
     *                   <p>
     *                   Maximum number of data to traverse
     * @return iterator
     */
    Iterator<E> iterator(int step, SMode idSortMode, long offset, long limit);
}
