package com.xuesinuo.pignoo;

import java.util.function.Function;

import lombok.Getter;

@Getter
public class PignooSorter<E> {
    @Getter
    public static enum SMode {
        MIN_FIRST("ASC"), MAX_FIRST("DESC");

        private String sql;

        SMode(String sql) {
            this.sql = sql;
        }
    }

    private Function<E, ?> field;
    private SMode mode;
    private PignooSorter<E> otherPignooSorter;

    private PignooSorter() {}

    public static <E> PignooSorter<E> build(Function<E, ?> field, SMode mode) {
        PignooSorter<E> pignooSorter = new PignooSorter<>();
        pignooSorter.field = field;
        pignooSorter.mode = mode;
        return pignooSorter;
    }

    public PignooSorter<E> then(Function<E, ?> field, SMode mode) {
        PignooSorter<E> pignooSorter = new PignooSorter<>();
        pignooSorter.field = field;
        pignooSorter.mode = mode;
        pignooSorter.otherPignooSorter = this;
        return pignooSorter;
    }

    public PignooSorter<E> then(PignooSorter<E> pignooSorter) {
        pignooSorter.otherPignooSorter = this;
        return pignooSorter;
    }
}
