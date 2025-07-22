package com.xuesinuo.pignoo.core.implement;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooWriter;
import com.xuesinuo.pignoo.core.PignooReader;
import com.xuesinuo.pignoo.core.config.DatabaseEngine;

/**
 * 基础的Pignoo实现
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
public class TransactionPignoo implements Pignoo {

    private final PignooConfig config;// Pignoo配置

    private DataSource dataSource;// 数据源

    private Connection conn;// 数据库连接

    private boolean connAutoCommit;// 原本的conn是否自动提交

    private boolean hasRollbacked = false;// 是否已经回滚

    private boolean hasClosed = false;// 是否已经关闭

    /**
     * 
     * @param dataSource 数据源
     *                   <p>
     *                   Data source
     */
    public TransactionPignoo(DataSource dataSource) {
        this(dataSource, null);
    }

    /**
     *
     * @param dataSource   数据源
     *                     <p>
     *                     Data source
     * @param pignooConfig 配置
     *                     <p>
     *                     Configuration
     */
    public TransactionPignoo(DataSource dataSource, PignooConfig pignooConfig) {
        if (dataSource == null) {
            throw new RuntimeException("Unknow dataSource");
        }
        this.dataSource = dataSource;
        if (pignooConfig == null) {
            this.config = new PignooConfig();
        } else {
            this.config = pignooConfig.copy();
        }
        if (this.config.getEngine() == null) {
            try {
                this.config.setEngine(DatabaseEngine.getDatabaseEngineByConnection(this.getConnection()));
            } catch (SQLException e) {
                this.close();
                throw new RuntimeException(e);
            }
        }
        if (this.config.getEngine() == null) {
            throw new RuntimeException("Unknow database engine");
        }
    }

    private synchronized Connection getConnection() {
        if (this.hasClosed) {
            throw new RuntimeException("Pignoo has closed, can not get connection");
        }
        if (this.conn != null) {
            return this.conn;
        }
        try {
            this.conn = dataSource.getConnection();
            this.connAutoCommit = this.conn.getAutoCommit();
            this.conn.setAutoCommit(false);
        } catch (SQLException e) {
            this.close();
            throw new RuntimeException(e);
        }
        return this.conn;
    }

    private Supplier<Connection> connGetter = () -> this.getConnection();

    private Consumer<Connection> connCloser = (conn) -> {};

    @Override
    public <E> PignooWriter<E> writer(Class<E> c) {
        switch (this.config.getEngine()) {
        case MySQL:
            return new MySqlPignooWriter<E>(this, connGetter, connCloser, true, c, this.config);
        }
        throw new RuntimeException("Unknow database engine");
    }

    @Override
    public <E> PignooReader<E> reader(Class<E> c) {
        switch (this.config.getEngine()) {
        case MySQL:
            return new MySqlPignooReadOnlyList<E>(this, connGetter, connCloser, true, c, this.config);
        }
        throw new RuntimeException("Unknow database engine");
    }

    public void rollback() {
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
    public synchronized void close() {
        if (hasClosed) {
            return;
        }
        hasClosed = true;
        conn = null;
        dataSource = null;
        if (!hasRollbacked) {
            try {
                conn.commit();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            if (connAutoCommit != conn.getAutoCommit()) {
                conn.setAutoCommit(connAutoCommit);
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean closed() {
        return hasClosed;
    }
}
