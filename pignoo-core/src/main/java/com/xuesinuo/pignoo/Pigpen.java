package com.xuesinuo.pignoo;

import java.util.function.Function;

import javax.sql.DataSource;

import com.xuesinuo.pignoo.Pignoo.DatabaseEngine;

public class Pigpen {
    private final DatabaseEngine engine;
    private final DataSource dataSource;

    public Pigpen(DatabaseEngine engine, DataSource dataSource) {
        this.engine = engine;
        this.dataSource = dataSource;
    }

    public <R> R run(Function<Pignoo, R> function) {
        try (Pignoo pignoo = new Pignoo(this.engine, this.dataSource, false)) {
            return function.apply(pignoo);
        }
    }

    public <R> R runTransaction(Function<Pignoo, R> function) {
        try (Pignoo pignoo = new Pignoo(this.engine, this.dataSource, true)) {
            try {
                return function.apply(pignoo);
            } catch (Exception e) {
                pignoo.rollback();
                throw e;
            }
        }
    }
}
