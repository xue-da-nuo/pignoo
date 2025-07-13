package com.xuesinuo.pignoo.core.implement;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooList;

/**
 * 基础的Pignoo实现
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
public class BasePignoo implements Pignoo {

    private DatabaseEngine engine;

    private Connection conn;

    private boolean useJdbcTransaction;

    private boolean connAutoCommit;

    private boolean hasRollbacked = false;

    private boolean hasClosed = false;

    /**
     *
     * @param engine             数据库引擎
     *                           <p>
     *                           Database engine
     * @param dataSource         数据源
     *                           <p>
     *                           Data source
     * @param useJdbcTransaction 是否使用JDBC事务
     *                           <p>
     *                           Whether to use JDBC transaction
     */
    public BasePignoo(DatabaseEngine engine, DataSource dataSource, boolean useJdbcTransaction) {
        if (engine == null) {
            throw new RuntimeException("Unknow database engine");
        }
        if (dataSource == null) {
            throw new RuntimeException("Unknow dataSource");
        }
        this.engine = engine;
        try {
            this.conn = dataSource.getConnection();
            this.connAutoCommit = conn.getAutoCommit();
            if (useJdbcTransaction) {
                conn.setAutoCommit(false);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        this.useJdbcTransaction = useJdbcTransaction;
    }

    @Override
    public <E> PignooList<E> getPignooList(Class<E> c) {
        switch (engine) {
        case MySQL:
            return new MySqlPignooList<E>(this, conn, useJdbcTransaction, c);
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
        if (hasClosed) {
            return;
        }
        hasClosed = true;
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
            if (connAutoCommit != conn.getAutoCommit()) {
                conn.setAutoCommit(connAutoCommit);
            }
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasClosed() {
        return hasClosed;
    }
}
