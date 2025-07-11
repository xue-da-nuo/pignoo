package com.xuesinuo.pignoo;

import javax.sql.DataSource;

import com.xuesinuo.pignoo.Pignoo.DatabaseEngine;

public class Pigpen {
    private final DatabaseEngine engine;
    private final DataSource dataSource;

    public Pigpen(DatabaseEngine engine, DataSource dataSource) {
        this.engine = engine;
        this.dataSource = dataSource;
    }

    public Pignoo build()  {
        Pignoo pignoo = new Pignoo(this.engine, this.dataSource);
        return pignoo;
    }
}
