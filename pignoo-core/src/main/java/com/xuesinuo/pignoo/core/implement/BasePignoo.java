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

import lombok.extern.slf4j.Slf4j;

import com.xuesinuo.pignoo.core.config.DatabaseEngine;
import com.xuesinuo.pignoo.core.exception.DataSourceException;
import com.xuesinuo.pignoo.core.exception.PignooRuntimeException;

/**
 * 基础的Pignoo实现
 * <p>
 * Basic Pignoo implementation
 * <p>
 * 继续单线程下，单一数据库连接，允许多次使用连接，退出则关闭连接（返还连接池）
 * <p>
 * Continue to single thread, single database connection, allow multiple use of connections, exit to close the connection (return to the connection pool)
 *
 * @author xuesinuo
 * @since 0.1.0
 * @version 0.2.3
 */
@Slf4j
public class BasePignoo implements Pignoo {

    private final PignooConfig config;// Pignoo配置

    private DataSource dataSource;// 数据源

    private Connection conn;// 数据库连接

    private boolean hasClosed = false;// 是否已经关闭

    /**
     * 构造器，使用默认配置
     * <p>
     * Constructor， use default configuration
     *
     * @param dataSource 数据源
     *                   <p>
     *                   Data source
     */
    public BasePignoo(DataSource dataSource) {
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
    public BasePignoo(DataSource dataSource, PignooConfig pignooConfig) {
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
                if (this.conn != null) {
                    try {
                        this.conn.close();
                    } catch (SQLException e1) {
                        log.error("Open connection error, and then close connection error", e1);
                    }
                }
                throw new DataSourceException("Search database engine error", e);
            }
        }
        if (this.config.getEngine() == null) {
            throw new DataSourceException("Unknow database engine");
        }
    }

    private synchronized Connection getConnection() {
        if (hasClosed) {
            throw new PignooRuntimeException("Pignoo has closed, can not get connection");
        }
        if (this.conn == null) {
            try {
                this.conn = this.dataSource.getConnection();
            } catch (SQLException e) {
                throw new DataSourceException("Get connection error", e);
            }
        }
        return this.conn;
    }

    private Supplier<Connection> connGetter = () -> this.getConnection();

    private Consumer<Connection> connCloser = (conn) -> {
        try {
            if (conn.getAutoCommit() == false) {
                conn.commit();
            }
        } catch (Exception ex) {
            throw new DataSourceException("Connection commit error", ex);
        }
    };

    @Override
    public <E> PignooWriter<E> writer(Class<E> c) {
        switch (this.config.getEngine()) {
        case MySQL:
            return new PignooWriter4Mysql<E>(this, connGetter, connCloser, false, c, this.config);
        }
        throw new DataSourceException("Unknow database engine");
    }

    @Override
    public <E> PignooReader<E> reader(Class<E> c) {
        switch (this.config.getEngine()) {
        case MySQL:
            return new PignooReader4Mysql<E>(this, connGetter, connCloser, false, c, this.config);
        }
        throw new DataSourceException("Unknow database engine");
    }

    @Override
    public void close() {
        this.hasClosed = true;
        this.dataSource = null;
        if (this.conn != null) {
            try {
                this.conn.close();
            } catch (SQLException e) {
                throw new DataSourceException("Close connection error", e);
            } finally {
                this.conn = null;
            }
        }
    }

    @Override
    public boolean closed() {
        return this.hasClosed;
    }

}
