package com.xuesinuo.pignoo.core;

import java.util.function.Function;

import lombok.Getter;

/**
 * Pignoo的排序器
 * <p>
 * Pignoo's sorter
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
@Getter
public class PignooSorter<E> {
    /** 排序方式 */
    public static enum SMode {
        /** 小的在前 */
        MIN_FIRST,
        /** 大的在前 */
        MAX_FIRST;
    }

    private Function<E, ?> field;
    private SMode mode;
    private PignooSorter<E> otherPignooSorter;

    private PignooSorter() {}

    /**
     * 复制一个PignooSorter
     * <p>
     * Copy a PignooSorter
     *
     * @param sorter 要复制的PignooSorter
     *               <p>
     *               The PignooSorter to be copied
     * @return 复制后的PignooSorter
     *         <p>
     *         The copied PignooSorter
     */
    public static <E> PignooSorter<E> copy(PignooSorter<E> sorter) {
        if (sorter == null) {
            return null;
        }
        PignooSorter<E> pignooSorter = new PignooSorter<>();
        pignooSorter.field = sorter.getField();
        pignooSorter.mode = sorter.getMode();
        if (sorter.getOtherPignooSorter() != null) {
            pignooSorter.otherPignooSorter = PignooSorter.copy(sorter.getOtherPignooSorter());
        }
        return pignooSorter;
    }

    /**
     * 构建一个PignooSorter
     * <p>
     * Build a PignooSorter
     *
     * @param field 排序字段
     *              <p>
     *              Sorting field
     * @param mode  排序方式
     *              <p>
     *              Sorting mode
     * @return 构建后的PignooSorter
     *         <p>
     *         The built PignooSorter
     */
    public static <E> PignooSorter<E> build(Function<E, ?> field, SMode mode) {
        PignooSorter<E> pignooSorter = new PignooSorter<>();
        pignooSorter.field = field;
        pignooSorter.mode = mode;
        return pignooSorter;
    }

    /**
     * 下一步排序
     * <p>
     * Next sorting
     *
     * @param field 排序字段
     *              <p>
     *              Sorting field
     * @param mode  排序方式
     *              <p>
     *              Sorting mode
     * @return 构建后的PignooSorter
     *         <p>
     *         The built PignooSorter
     */
    public PignooSorter<E> then(Function<E, ?> field, SMode mode) {
        PignooSorter<E> pignooSorter = new PignooSorter<>();
        pignooSorter.field = field;
        pignooSorter.mode = mode;
        pignooSorter.otherPignooSorter = this;
        return pignooSorter;
    }

    /**
     * 下一步排序
     * <p>
     * Next sorting
     *
     * @param pignooSorter 下一步排序器
     *                     <p>
     *                     Next sorting sorter
     * @return 构建后的PignooSorter
     *         <p>
     *         The built PignooSorter
     */
    public PignooSorter<E> then(PignooSorter<E> pignooSorter) {
        pignooSorter.otherPignooSorter = this;
        return pignooSorter;
    }
}
