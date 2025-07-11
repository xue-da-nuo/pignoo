package com.xuesinuo.pignoo;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import lombok.Getter;

@Getter
public class PignooFilter<E> {
    @Getter
    public static enum FMode {
        EQ("="),
        NOT_EQ("!="),
        GT(">"),
        LT("<"),
        GE(">="),
        LE("<="),
        LIKE("LIKE"),
        NOT_LIKE("NOT LIKE"),
        IN("IN"),
        NOT_IN("NOT IN"),
        IS_NULL("IS NULL"),
        IS_NOT_NULL("IS NOT NULL");

        private String sql;

        FMode(String sql) {
            this.sql = sql;
        }
    }

    @Getter
    public enum XOR {
        AND("AND"), OR("OR");

        private String sql;

        XOR(String sql) {
            this.sql = sql;
        }
    }

    private Function<E, ?> field;
    private FMode mode;
    private Collection<Object> values;
    private XOR xor;
    private List<PignooFilter<E>> otherPignooFilterList;

    private PignooFilter() {}

    public static <E> PignooFilter<E> build(Function<E, ?> field, FMode mode, Object... values) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.field = field;
        pignooFilter.mode = mode;
        pignooFilter.values = Arrays.asList(values);
        pignooFilter.xor = XOR.AND;
        return pignooFilter;
    }

    public static <E> PignooFilter<E> build(Function<E, ?> field, FMode mode, Collection<Object> values) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.field = field;
        pignooFilter.mode = mode;
        pignooFilter.values = values;
        pignooFilter.xor = XOR.AND;
        return pignooFilter;
    }

    public static <E> PignooFilter<E> build(Class<E> c) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        return pignooFilter;
    }

    public static <E> PignooFilter<E> build(E e) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        // TODO 各属性相等的条件
        return pignooFilter;
    }

    public PignooFilter<E> and(Function<E, ?> field, FMode mode, Collection<Object> values) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.field = field;
        pignooFilter.mode = mode;
        pignooFilter.values = values;
        pignooFilter.xor = XOR.AND;
        pignooFilter.otherPignooFilterList = Arrays.asList(this);
        return pignooFilter;
    }

    public PignooFilter<E> or(Function<E, ?> field, FMode mode, Collection<Object> values) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.field = field;
        pignooFilter.mode = mode;
        pignooFilter.values = values;
        pignooFilter.xor = XOR.OR;
        pignooFilter.otherPignooFilterList = Arrays.asList(this);
        return pignooFilter;
    }

    public PignooFilter<E> and(Function<E, ?> field, FMode mode, Object... values) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.field = field;
        pignooFilter.mode = mode;
        pignooFilter.values = Arrays.asList(values);
        pignooFilter.xor = XOR.AND;
        pignooFilter.otherPignooFilterList = Arrays.asList(this);
        return pignooFilter;
    }

    public PignooFilter<E> or(Function<E, ?> field, FMode mode, Object... values) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.field = field;
        pignooFilter.mode = mode;
        pignooFilter.values = Arrays.asList(values);
        pignooFilter.xor = XOR.OR;
        pignooFilter.otherPignooFilterList = Arrays.asList(this);
        return pignooFilter;
    }

    public PignooFilter<E> and(PignooFilter<E> filter) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.field = filter.field;
        pignooFilter.mode = filter.mode;
        pignooFilter.values = filter.values;
        pignooFilter.xor = XOR.AND;
        pignooFilter.otherPignooFilterList = Arrays.asList(this);
        return pignooFilter;
    }

    public PignooFilter<E> or(PignooFilter<E> filter) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.field = filter.field;
        pignooFilter.mode = filter.mode;
        pignooFilter.values = filter.values;
        pignooFilter.xor = XOR.OR;
        pignooFilter.otherPignooFilterList = Arrays.asList(this);
        return pignooFilter;
    }

    public static <E> PignooFilter<E> and(List<PignooFilter<E>> filters) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.xor = XOR.AND;
        pignooFilter.otherPignooFilterList = filters;
        return pignooFilter;
    }

    public static <E> PignooFilter<E> or(List<PignooFilter<E>> filters) {
        PignooFilter<E> pignooFilter = new PignooFilter<>();
        pignooFilter.xor = XOR.OR;
        pignooFilter.otherPignooFilterList = filters;
        return pignooFilter;
    }
}
