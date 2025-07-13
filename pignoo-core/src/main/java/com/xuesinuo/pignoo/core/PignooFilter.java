package com.xuesinuo.pignoo.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import lombok.Getter;

/**
 * PignooList的条件筛选器
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
@Getter
public class PignooFilter<E> {
    /**
     * 筛选条件
     * <p>
     * Filter conditions
     */
    public static enum FMode {
        /**
         * 等于
         * <p>
         * Equals
         */
        EQ,
        /**
         * 不等于
         * <p>
         * Not equals
         */
        NOT_EQ,
        /**
         * 大于
         * <p>
         * Greater than
         */
        GT,
        /**
         * 小于
         * <p>
         * Less than
         */
        LT,
        /**
         * 大于等于
         * <p>
         * Greater than or equal to
         */
        GE,
        /**
         * 小于等于
         * <p>
         * Less than or equal to
         */
        LE,
        /**
         * 模糊查询
         * <p>
         * Fuzzy query
         */
        LIKE,
        /**
         * 模糊查询，反查询
         * <p>
         * Fuzzy query, reverse query
         */
        NOT_LIKE,
        /**
         * 包含
         * <p>
         * Contains
         */
        IN,
        /**
         * 不包含
         * <p>
         * Not contains
         */
        NOT_IN,
        /**
         * 为空
         * <p>
         * Is null
         */
        IS_NULL,
        /**
         * 不为空
         * <p>
         * Is not null
         */
        IS_NOT_NULL;
    }

    /**
     * 逻辑运算
     * <p>
     * Logical operation
     */
    public enum XOR {
        AND, OR;
    }

    private Function<E, ?> field;
    private FMode mode;
    private Collection<Object> values;
    private XOR xor;
    private List<PignooFilter<E>> otherPignooFilterList;

    private PignooFilter() {}

    /**
     * 复制一个PignooFilter实例
     * <p>
     * Copy a PignooFilter instance
     *
     * @param filter 要复制的PignooFilter实例
     *               <p>
     *               The PignooFilter instance to be copied
     * @return 复制后的PignooFilter实例
     *         <p>
     *         The copied PignooFilter instance
     */
    public static <E> PignooFilter<E> copy(PignooFilter<E> filter) {
        if (filter == null) {
            return null;
        }
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.field = filter.getField();
        pignooFilter.mode = filter.getMode();
        pignooFilter.values = List.copyOf(filter.getValues());
        pignooFilter.xor = filter.getXor();
        if (filter.getOtherPignooFilterList() != null) {
            pignooFilter.otherPignooFilterList = filter.getOtherPignooFilterList().stream().map(PignooFilter::copy).toList();
        }
        return pignooFilter;
    }

    /**
     * 构建一个PignooFilter实例
     * <p>
     * Build a PignooFilter instance
     *
     * @param field  字段（用Getter方法指代）
     *               <p>
     *               Field (use Getter method to refer)
     * @param mode   筛选条件
     *               <p>
     *               Filter conditions
     * @param values 值
     *               <p>
     *               Value
     * @return PignooFilter实例
     *         <p>
     *         PignooFilter instance
     */
    public static <E> PignooFilter<E> build(Function<E, ?> field, FMode mode, Object... values) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.field = field;
        pignooFilter.mode = mode;
        pignooFilter.values = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof Collection) {
                pignooFilter.values.addAll((Collection<?>) value);
            } else {
                pignooFilter.values.add(value);
            }
        }
        pignooFilter.xor = XOR.AND;
        return pignooFilter;
    }

    /**
     * 构建一个空的PignooFilter实例
     * <p>
     * Build an empty PignooFilter instance
     *
     * @param c 实体类型
     *          <p>
     *          Entity type
     * @return PignooFilter实例
     *         <p>
     *         PignooFilter instance
     */
    public static <E> PignooFilter<E> build(Class<E> c) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        return pignooFilter;
    }

    /**
     * 构建一个PignooFilter实例，各属性相等的条件
     * <p>
     * Build a PignooFilter instance, equal conditions for all attributes
     *
     * @param e 实体
     *          <p>
     *          Entity
     * @return PignooFilter实例
     *         <p>
     *         PignooFilter instance
     */
    public static <E> PignooFilter<E> build(E e) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        // TODO 各属性相等的条件
        return pignooFilter;
    }

    /**
     * 现有条件上AND拼接下一个条件
     * <p>
     * AND concatenate the next condition on the existing condition
     *
     * @param field  字段（用Getter方法指代）
     *               <p>
     *               Field (use Getter method to refer)
     * @param mode   筛选条件
     *               <p>
     *               Filter conditions
     * @param values 值
     *               <p>
     *               Value
     * @return PignooFilter实例
     *         <p>
     *         PignooFilter instance
     */
    public PignooFilter<E> and(Function<E, ?> field, FMode mode, Object... values) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.field = field;
        pignooFilter.mode = mode;
        pignooFilter.values = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof Collection) {
                pignooFilter.values.addAll((Collection<?>) value);
            } else {
                pignooFilter.values.add(value);
            }
        }
        pignooFilter.xor = XOR.AND;
        pignooFilter.otherPignooFilterList = Arrays.asList(this);
        return pignooFilter;
    }

    /**
     * 现有条件上OR拼接下一个条件
     * <p>
     * OR concatenate the next condition on the existing condition
     *
     * @param field  字段（用Getter方法指代）
     *               <p>
     *               Field (use Getter method to refer)
     * @param mode   筛选条件
     *               <p>
     *               Filter conditions
     * @param values 值
     *               <p>
     *               Value
     * @return PignooFilter实例
     *         <p>
     *         PignooFilter instance
     */
    public PignooFilter<E> or(Function<E, ?> field, FMode mode, Object... values) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.field = field;
        pignooFilter.mode = mode;
        pignooFilter.values = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof Collection) {
                pignooFilter.values.addAll((Collection<?>) value);
            } else {
                pignooFilter.values.add(value);
            }
        }
        pignooFilter.xor = XOR.OR;
        pignooFilter.otherPignooFilterList = Arrays.asList(this);
        return pignooFilter;
    }

    /**
     * 现有条件上AND拼接下一个条件
     * <p>
     * AND concatenate the next condition on the existing condition
     *
     * @param filter 下一个条件
     *               <p>
     *               Next condition
     * @return PignooFilter实例
     *         <p>
     *         PignooFilter instance
     */
    public PignooFilter<E> and(PignooFilter<E> filter) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.field = filter.field;
        pignooFilter.mode = filter.mode;
        pignooFilter.values = filter.values;
        pignooFilter.xor = XOR.AND;
        pignooFilter.otherPignooFilterList = Arrays.asList(this);
        return pignooFilter;
    }

    /**
     * 现有条件上OR拼接下一个条件
     * <p>
     * OR concatenate the next condition on the existing condition
     *
     * @param filter 下一个条件
     *               <p>
     *               Next condition
     * @return PignooFilter实例
     *         <p>
     *         PignooFilter instance
     */
    public PignooFilter<E> or(PignooFilter<E> filter) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.field = filter.field;
        pignooFilter.mode = filter.mode;
        pignooFilter.values = filter.values;
        pignooFilter.xor = XOR.OR;
        pignooFilter.otherPignooFilterList = Arrays.asList(this);
        return pignooFilter;
    }

    /**
     * 现有条件上AND拼接多个条件
     * <p>
     * AND concatenate multiple conditions on the existing condition
     *
     * @param filters 多个条件
     *                <p>
     *                Next conditions
     * @return PignooFilter实例
     *         <p>
     *         PignooFilter instance
     */
    public static <E> PignooFilter<E> and(List<PignooFilter<E>> filters) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.xor = XOR.AND;
        pignooFilter.otherPignooFilterList = filters;
        return pignooFilter;
    }

    /**
     * 现有条件上OR拼接多个条件
     * <p>
     * OR concatenate multiple conditions on the existing condition
     *
     * @param filters 多个条件
     *                <p>
     *                Next conditions
     * @return PignooFilter实例
     *         <p>
     *         PignooFilter instance
     */
    public static <E> PignooFilter<E> or(List<PignooFilter<E>> filters) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.xor = XOR.OR;
        pignooFilter.otherPignooFilterList = filters;
        return pignooFilter;
    }
}
