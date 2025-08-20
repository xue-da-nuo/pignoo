package com.xuesinuo.pignoo.spring.implement;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooWriter;
import com.xuesinuo.pignoo.core.PignooReader;
import com.xuesinuo.pignoo.core.implement.PignooWriter4Mysql;
import com.xuesinuo.pignoo.core.implement.PignooReader4Mysql;

import lombok.extern.slf4j.Slf4j;

import com.xuesinuo.pignoo.core.config.DatabaseEngine;
import com.xuesinuo.pignoo.core.exception.DataSourceException;

/**
 * Spring事务Pignoo实现
 * <p>
 * Spring transaction Pignoo implementation
 *
 * @author xuesinuo
 * @since 0.2.1
 * @version 0.2.1
 */
@Slf4j
public class SpringPignooItem implements Pignoo {

    private final PignooConfig config;// Pignoo配置

    private DataSource dataSource;// 数据源

    private final boolean inTransaction;// 是否在事务中

    private boolean hasClosed = false;// 是否已经关闭

    /**
     * 构造器
     * <p>
     * Constructor
     *
     * @param dataSource    数据源
     *                      <p>
     *                      Data source
     * @param pignooConfig  配置
     *                      <p>
     *                      Configuration
     * @param inTransaction a boolean
     */
    protected SpringPignooItem(DataSource dataSource, PignooConfig pignooConfig, boolean inTransaction) {
        if (dataSource == null) {
            throw new DataSourceException("Unknow dataSource");
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
                throw new DataSourceException("Search database engine error", e);
            } finally {
                if (conn != null) {
                    DataSourceUtils.releaseConnection(conn, this.getDataSource());
                }
            }
        }
        if (this.config.getEngine() == null) {
            throw new DataSourceException("Unknow database engine");
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
    public <E> PignooWriter<E> writer(Class<E> c) {
        switch (this.config.getEngine()) {
        case MySQL:
            return new PignooWriter4Mysql<E>(this, connGetter, connCloser, this.inTransaction, c, this.config);
        }
        throw new DataSourceException("Unknow database engine");
    }

    @Override
    public <E> PignooReader<E> reader(Class<E> c) {
        switch (this.config.getEngine()) {
        case MySQL:
            return new PignooReader4Mysql<E>(this, connGetter, connCloser, this.inTransaction, c, this.config);
        }
        throw new DataSourceException("Unknow database engine");
    }

    @Override
    public void close() {
        this.hasClosed = true;
        this.dataSource = null;
    }

    @Override
    public boolean closed() {
        return hasClosed;
    }

}
