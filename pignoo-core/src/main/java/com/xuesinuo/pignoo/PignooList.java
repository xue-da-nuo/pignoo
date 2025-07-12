package com.xuesinuo.pignoo;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * PignooList like {@link java.util.List}, but the operation is based on the SQL database.
 * 
 * A PignooList is a table in the database.
 * 
 * Entity(Table) must have a primary key.
 */
public interface PignooList<E> {

    /** 复制一个List（保持当前的查询条件） */
    PignooList<E> copy();

    /** 获取表中的第一条数据 */
    E getOne();

    /** 获取列表 */
    List<E> getAll();

    /** 获取部分信息 */
    List<E> get(long limit, long offset);

    /** 获取总数 */
    long size();

    /** 排序 */
    PignooList<E> sort(Function<E, ?> field, PignooSorter.SMode mode);

    /** 排序 */
    PignooList<E> sort(PignooSorter<E> sorter);

    /** 过滤 */
    PignooList<E> filter(Function<E, ?> field, PignooFilter.FMode mode, Object... values);

    /** 过滤 */
    PignooList<E> filter(Function<E, ?> field, PignooFilter.FMode mode, Collection<Object> values);

    /** 过滤 */
    PignooList<E> filter(PignooFilter<E> filter);

    /** 新增一条数据 */
    E add(E e);

    /** 修改数据：混入不为NULL的属性 */
    long mixByPk(E e);

    /** 修改数据：完全替换 */
    long replaceByPk(E e);

    /** 修改数据：混入不为NULL的属性 */
    long mix(E e);

    /** 修改数据：完全替换 */
    long replace(E e);

    /** 删除数据 */
    long removeByPk(E e);

    /** 删除数据 */
    long remove();

    /** 求和 */
    <R> R sum(Function<E, R> field, Class<R> c);

    /** 求平均 */
    <R> R avg(Function<E, R> field, Class<R> c);
}
