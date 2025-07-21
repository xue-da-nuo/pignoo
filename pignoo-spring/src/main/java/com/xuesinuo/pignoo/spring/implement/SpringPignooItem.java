package com.xuesinuo.pignoo.spring.implement;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooList;
import com.xuesinuo.pignoo.core.implement.MySqlPignooList;

import lombok.extern.slf4j.Slf4j;

import com.xuesinuo.pignoo.core.config.DatabaseEngine;

/**
 * Spring事务Pignoo实现
 * <p>
 * Spring transaction Pignoo implementation
 * 
 * @author xuesinuo
 * @since 0.2.1
 */
@Slf4j
public class SpringPignooItem implements Pignoo {

    private final PignooConfig config;// Pignoo配置

    private final DataSource dataSource;// 数据源

    private final boolean inTransaction;// 是否在事务中

    private boolean hasClosed = false;// 是否已经关闭

    /**
     *
     * @param dataSource   数据源
     *                     <p>
     *                     Data source
     * @param pignooConfig 配置
     *                     <p>
     *                     Configuration
     */
    protected SpringPignooItem(DataSource dataSource, PignooConfig pignooConfig, boolean inTransaction) {
        if (dataSource == null) {
            throw new RuntimeException("Unknow dataSource");
        }
        this.dataSource = dataSource;
        if (pignooConfig == null) {
            this.config = new PignooConfig();
        } else {
            this.config = pignooConfig.copy();
        }
        this.inTransaction = inTransaction;
        if (this.config.getEngine() == null) {
            Connection conn = null;
            try {
                conn = this.getConnection();
                this.config.setEngine(DatabaseEngine.getDatabaseEngineByConnection(conn));
            } catch (SQLException e) {
                this.close();
                throw new RuntimeException(e);
            } finally {
                if (conn != null) {
                    DataSourceUtils.releaseConnection(conn, this.getDataSource());
                }
            }
        }
        if (this.config.getEngine() == null) {
            throw new RuntimeException("Unknow database engine");
        }
    }

    private DataSource getDataSource() {
        return this.dataSource;
    }

    private synchronized Connection getConnection() {
        return DataSourceUtils.getConnection(this.dataSource);
    }

    private Supplier<Connection> connGetter = () -> this.getConnection();

    private Consumer<Connection> connCloser = (conn) -> {
        DataSourceUtils.releaseConnection(conn, this.getDataSource());
    };

    @Override
    public <E> PignooList<E> getList(Class<E> c) {
        switch (this.config.getEngine()) {
        case MySQL:
            return new MySqlPignooList<E>(this, connGetter, connCloser, this.inTransaction, c, this.config);
        }
        throw new RuntimeException("Unknow database engine");
    }

    @Override
    public synchronized void close() {
        hasClosed = true;
    }

    @Override
    public boolean closed() {
        return hasClosed;
    }
}
