package com.xuesinuo.pignoo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import lombok.AllArgsConstructor;
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
    @AllArgsConstructor
    @Getter
    public static enum FMode {
        /**
         * 等于
         * <p>
         * Equals
         */
        EQ("==", 1, 1),
        /**
         * 不等于
         * <p>
         * Not equals
         */
        NE("!=", 1, 1),
        /**
         * 大于
         * <p>
         * Greater than
         */
        GT(">", 1, 1),
        /**
         * 小于
         * <p>
         * Less than
         */
        LT("<", 1, 1),
        /**
         * 大于等于
         * <p>
         * Greater than or equal to
         */
        GE(">=", 1, 1),
        /**
         * 小于等于
         * <p>
         * Less than or equal to
         */
        LE("<=", 1, 1),
        /**
         * 模糊查询
         * <p>
         * Fuzzy query
         */
        LIKE("like", 1, 1),
        /**
         * 模糊查询，反查询
         * <p>
         * Fuzzy query, reverse query
         */
        NOT_LIKE("not like", 1, 1),
        /**
         * 包含
         * <p>
         * Contains
         */
        IN("in", 1, Integer.MAX_VALUE),
        /**
         * 不包含
         * <p>
         * Not contains
         */
        NOT_IN("not in", 1, Integer.MAX_VALUE),
        /**
         * 为空
         * <p>
         * Is null
         */
        NULL("is null", 0, 0),
        /**
         * 不为空
         * <p>
         * Is not null
         */
        NOT_NULL("is not null", 0, 0);

        /**
         * 筛选条件名称，可以用于代替枚举值，不区分大小写
         * <p>
         * Filter condition name, can be used to replace enum values, case-insensitive
         */
        private String name;

        /**
         * 筛选条件允许的参数最少个数
         * <p>
         * The minimum number of parameters allowed by the filter condition
         */
        private int minCount;

        /**
         * 筛选条件允许的参数最多个数
         * <p>
         * The maximum number of parameters allowed by the filter condition
         */
        private int maxCount;

        /**
         * 名称映射成筛选条件枚举
         * <p>
         * Name mapping to filter condition enum
         * 
         * @param name 筛选条件名称
         * @return 筛选条件枚举
         */
        public static FMode getFMode(String name) {
            if (name == null) {
                throw new RuntimeException("Invalid FMode: null");
            }
            name = name.trim().toLowerCase();
            for (FMode fMode : values()) {
                if (fMode.getName().equals(name)) {
                    return fMode;
                }
            }
            throw new RuntimeException("Invalid FMode: " + name);
        }
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
    private List<PignooFilter<E>> otherPignooFilterList = new ArrayList<>();

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
            pignooFilter.otherPignooFilterList.addAll(filter.getOtherPignooFilterList().stream().map(PignooFilter::copy).toList());
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
            addInCollection(pignooFilter.values, value);
        }
        pignooFilter.xor = XOR.AND;
        return pignooFilter;
    }

    /**
     * 请参考{@link #build(Function, FMode, Object...)}
     * <p>
     * Please refer to {@link #build(Function, FMode, Object...)}
     */
    public static <E> PignooFilter<E> build(Function<E, ?> field, String mode, Object... values) {
        return build(field, FMode.getFMode(mode), values);
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
        PignooFilter<E> filter = new PignooFilter<>();
        filter.field = field;
        filter.mode = mode;
        filter.values = new ArrayList<>();
        for (Object value : values) {
            addInCollection(filter.values, value);
        }
        filter.xor = XOR.AND;
        return this.and(filter);
    }

    /**
     * 请参考{@link #and(Function, FMode, Object...)}
     * <p>
     * Please refer to {@link #and(Function, FMode, Object...)}
     */
    public PignooFilter<E> and(Function<E, ?> field, String mode, Object... values) {
        return and(field, FMode.getFMode(mode), values);
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
        PignooFilter<E> filter = new PignooFilter<>();
        filter.field = field;
        filter.mode = mode;
        filter.values = new ArrayList<>();
        for (Object value : values) {
            addInCollection(filter.values, value);
        }
        filter.xor = XOR.AND;
        return this.or(filter);
    }

    /**
     * 请参考{@link #or(Function, FMode, Object...)}
     * <p>
     * Please refer to {@link #or(Function, FMode, Object...)}
     */
    public PignooFilter<E> or(Function<E, ?> field, String mode, Object... values) {
        return or(field, FMode.getFMode(mode), values);
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
        PignooFilter<E> orFilter = new PignooFilter<>();
        orFilter.xor = XOR.AND;
        orFilter.otherPignooFilterList.add(this);
        orFilter.otherPignooFilterList.add(filter);
        return orFilter;
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
        PignooFilter<E> orFilter = new PignooFilter<>();
        orFilter.xor = XOR.OR;
        orFilter.otherPignooFilterList.add(this);
        orFilter.otherPignooFilterList.add(filter);
        return orFilter;
    }

    /**
     * 添加一个值到集合中，如果值是集合，则将值中的所有元素添加到集合中
     * <p>
     * Add a value to the collection, if the value is a collection, add all elements in the value to the collection
     * 
     * @param collection 集合
     * @param value      添加到集合中的值
     */
    private static void addInCollection(Collection<Object> collection, Object value) {
        if (value instanceof Iterable) {
            for (var item : (Iterable<?>) value) {
                collection.add(item);
            }
        } else if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            for (var item : array) {
                collection.add(item);
            }
        } else if (value instanceof byte[]) {
            byte[] array = (byte[]) value;
            for (var item : array) {
                collection.add(item);
            }
        } else if (value instanceof short[]) {
            short[] array = (short[]) value;
            for (var item : array) {
                collection.add(item);
            }
        } else if (value instanceof int[]) {
            int[] array = (int[]) value;
            for (var item : array) {
                collection.add(item);
            }
        } else if (value instanceof long[]) {
            long[] array = (long[]) value;
            for (var item : array) {
                collection.add(item);
            }
        } else if (value instanceof float[]) {
            float[] array = (float[]) value;
            for (var item : array) {
                collection.add(item);
            }
        } else if (value instanceof double[]) {
            double[] array = (double[]) value;
            for (var item : array) {
                collection.add(item);
            }
        } else if (value instanceof char[]) {
            char[] array = (char[]) value;
            for (var item : array) {
                collection.add(item);
            }
        } else if (value instanceof boolean[]) {
            boolean[] array = (boolean[]) value;
            for (var item : array) {
                collection.add(item);
            }
        } else {
            collection.add(value);
        }
    }
}
