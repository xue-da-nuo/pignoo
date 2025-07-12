package com.xuesinuo.pignoo;

import java.util.function.Consumer;
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

    public Pignoo build() {
        return new Pignoo(this.engine, this.dataSource, false);
    }

    public <R> R run(Function<Pignoo, R> function) {
        try (Pignoo pignoo = new Pignoo(this.engine, this.dataSource, false)) {
            return function.apply(pignoo);
        }
    }

    public void runTransaction(Consumer<Pignoo> function) {
        try (Pignoo pignoo = new Pignoo(this.engine, this.dataSource, true)) {
            try {
                function.accept(pignoo);
            } catch (Exception e) {
                pignoo.rollback();
                throw e;
            }
        }
    }
}
