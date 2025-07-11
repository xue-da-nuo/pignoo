package com.xuesinuo.pignoo;

import javax.sql.DataSource;

import com.xuesinuo.pignoo.implement.MySqlPignooList;

public class Pignoo {
    private final DatabaseEngine engine;

    private final DataSource dataSource;

    public static enum DatabaseEngine {
        MySQL
    }

    public Pignoo(DatabaseEngine engine, DataSource dataSource) {
        if (engine == null) {
            throw new RuntimeException("Unknow database engine");
        }
        if (dataSource == null) {
            throw new RuntimeException("Unknow dataSource");
        }
        this.engine = engine;
        this.dataSource = dataSource;
    }

    public <E> PignooList<E> getPignooList(Class<E> c) {
        switch (engine) {
        case MySQL:
            return new MySqlPignooList<>(dataSource, c);
        }
        throw new RuntimeException("Unknow database engine");
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }
}
