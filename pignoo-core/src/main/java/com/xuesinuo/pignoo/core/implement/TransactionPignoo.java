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
import com.xuesinuo.pignoo.core.exception.DataSourceException;
import com.xuesinuo.pignoo.core.exception.PignooRuntimeException;
import com.xuesinuo.pignoo.core.exception.SqlExecuteException;

/**
 * 基于JDBC事务的Pignoo实现
 * <p>
 * Pignoo implementation based on JDBC transaction
 *
 * @author xuesinuo
 * @since 0.1.0
 * @version 0.1.0
 */
public class TransactionPignoo implements Pignoo {

    private final PignooConfig config;// Pignoo配置

    private DataSource dataSource;// 数据源

    private Connection conn;// 数据库连接

    private boolean connAutoCommit;// 原本的conn是否自动提交

    private boolean hasRollbacked = false;// 是否已经回滚

    private boolean hasClosed = false;// 是否已经关闭

    /**
     * 构造器，使用默认配置
     * <p>
     * Constructor, use default configuration
     *
     * @param dataSource 数据源
     *                   <p>
     *                   Data source
     */
    public TransactionPignoo(DataSource dataSource) {
        this(dataSource, null);
    }

    /**
     * 构造器
     * <p>
     * Constructor
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
            throw new DataSourceException("Unknow dataSource");
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
                throw new DataSourceException("Search database engine error", e);
            }
        }
        if (this.config.getEngine() == null) {
            throw new DataSourceException("Unknow database engine");
        }
    }

    private synchronized Connection getConnection() {
        if (this.hasClosed) {
            throw new PignooRuntimeException("Pignoo has closed, can not get connection");
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
            throw new SqlExecuteException(e);
        }
        return this.conn;
    }

    private Supplier<Connection> connGetter = () -> this.getConnection();

    private Consumer<Connection> connCloser = (conn) -> {};

    @Override
    public <E> PignooWriter<E> writer(Class<E> c) {
        switch (this.config.getEngine()) {
        case MySQL:
            return new PignooWriter4Mysql<E>(this, connGetter, connCloser, true, c, this.config);
        }
        throw new DataSourceException("Unknow database engine");
    }

    @Override
    public <E> PignooReader<E> reader(Class<E> c) {
        switch (this.config.getEngine()) {
        case MySQL:
            return new PignooReader4Mysql<E>(this, connGetter, connCloser, true, c, this.config);
        }
        throw new DataSourceException("Unknow database engine");
    }

    /**
     * 事务回滚
     * <p>
     * Transaction rollback
     */
    public void rollback() {
        if (hasRollbacked) {
            return;
        }
        try {
            conn.rollback();
            hasRollbacked = true;
        } catch (SQLException e) {
            throw new SqlExecuteException(e);
        }
    }

    @Override
    public synchronized void close() {
        if (hasClosed) {
            return;
        }
        hasClosed = true;
        dataSource = null;
        if (!hasRollbacked) {
            try {
                conn.commit();
            } catch (SQLException e) {
                throw new SqlExecuteException(e);
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
            throw new SqlExecuteException(e);
        } finally {
            conn = null;
        }
    }

    @Override
    public boolean closed() {
        return hasClosed;
    }
}
