package com.xuesinuo.pignoo.core.implement;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooList;

import lombok.extern.slf4j.Slf4j;

import com.xuesinuo.pignoo.core.config.DatabaseEngine;

/**
 * 基础的Pignoo实现
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
@Slf4j
public class BasePignoo implements Pignoo {

    private final PignooConfig config;// Pignoo配置

    private DataSource dataSource;// 数据源

    private Connection conn;// 数据库连接

    private boolean hasClosed = false;// 是否已经关闭

    /**
     * 
     * @param dataSource 数据源
     *                   <p>
     *                   Data source
     */
    public BasePignoo(DataSource dataSource) {
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
    public BasePignoo(DataSource dataSource, PignooConfig pignooConfig) {
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
                if (this.conn != null) {
                    try {
                        this.conn.close();
                    } catch (SQLException e1) {
                        log.error("Open connection error, and then close connection error", e1);
                    }
                }
                throw new RuntimeException(e);
            }
        }
        if (this.config.getEngine() == null) {
            throw new RuntimeException("Unknow database engine");
        }
    }

    private synchronized Connection getConnection() {
        if (hasClosed) {
            throw new RuntimeException("Pignoo has closed, can not get connection");
        }
        if (this.conn == null) {
            try {
                this.conn = this.dataSource.getConnection();
            } catch (SQLException e) {
                throw new RuntimeException(e);
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
            log.error("Connection commit error", ex);
            throw new RuntimeException(ex);
        }
    };

    @Override
    public <E> PignooList<E> getList(Class<E> c) {
        switch (this.config.getEngine()) {
        case MySQL:
            return new MySqlPignooList<E>(this, connGetter, connCloser, false, c, this.config);
        }
        throw new RuntimeException("Unknow database engine");
    }

    @Override
    public void close() {
        this.hasClosed = true;
        this.dataSource = null;
        if (this.conn != null) {
            try {
                this.conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
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
