package com.xuesinuo.pignoo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.xuesinuo.pignoo.implement.MySqlPignooList;

public class Pignoo implements AutoCloseable {
    private final DatabaseEngine engine;

    private final Connection conn;

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
        try {
            this.conn = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <E> PignooList<E> getPignooList(Class<E> c) {
        switch (engine) {
        case MySQL:
            return new MySqlPignooList<>(conn, c);
        }
        throw new RuntimeException("Unknow database engine");
    }

    @Override
    public void close() throws IOException {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
