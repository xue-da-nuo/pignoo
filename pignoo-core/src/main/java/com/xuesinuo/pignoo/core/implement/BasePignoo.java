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
            try (Connection conn = dataSource.getConnection()) {
                this.config.setEngine(DatabaseEngine.getDatabaseEngineByConnection(conn));
            } catch (SQLException e) {
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
        try {
            return this.dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Supplier<Connection> connGetter = () -> this.getConnection();

    private Consumer<Connection> connCloser = (conn) -> {
        Exception e = null;
        try {
            if (conn.getAutoCommit() == false) {
                conn.commit();
            }
        } catch (Exception ex) {
            log.error("Connection commit error", ex);
            e = ex;
        } finally {
            try {
                conn.close();
            } catch (Exception ex) {
                log.error("Connection close error", ex);
                e = ex;
            }
        }
        if (e != null) {
            throw new RuntimeException(e);
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
    }

    @Override
    public boolean closed() {
        return this.hasClosed;
    }
}
