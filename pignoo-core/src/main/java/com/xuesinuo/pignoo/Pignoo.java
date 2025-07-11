package com.xuesinuo.pignoo;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.xuesinuo.pignoo.implement.MySqlPignooList;

public class Pignoo implements AutoCloseable {

    private DatabaseEngine engine;

    private Connection conn;

    private boolean useJdbcTransaction;

    private boolean connIsAutoCommit = false;

    private boolean hasRollbacked = false;

    public static enum DatabaseEngine {
        MySQL
    }

    public Pignoo(DatabaseEngine engine, DataSource dataSource, boolean useJdbcTransaction) {
        if (engine == null) {
            throw new RuntimeException("Unknow database engine");
        }
        if (dataSource == null) {
            throw new RuntimeException("Unknow dataSource");
        }
        this.engine = engine;
        try {
            this.conn = dataSource.getConnection();
            if (useJdbcTransaction) {
                if (conn.getAutoCommit()) {
                    connIsAutoCommit = true;
                    conn.setAutoCommit(false);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        this.useJdbcTransaction = useJdbcTransaction;
    }

    public <E> PignooList<E> getPignooList(Class<E> c) {
        switch (engine) {
        case MySQL:
            return new MySqlPignooList<>(conn, c);
        }
        throw new RuntimeException("Unknow database engine");
    }

    public void rollback() {
        if (!useJdbcTransaction) {
            throw new RuntimeException("Can not rollback, because JDBC-Transaction is not used");
        }
        if (hasRollbacked) {
            return;
        }
        try {
            conn.rollback();
            hasRollbacked = true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        if (useJdbcTransaction) {
            if (!hasRollbacked) {
                try {
                    conn.commit();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        try {
            if (useJdbcTransaction) {
                if (connIsAutoCommit) {
                    conn.setAutoCommit(true);
                }
            }
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
