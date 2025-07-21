package com.xuesinuo.pignoo.core.implement;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooList;
import com.xuesinuo.pignoo.core.config.AnnotationMode;
import com.xuesinuo.pignoo.core.config.AnnotationMode.AnnotationMixMode;
import com.xuesinuo.pignoo.core.config.DatabaseEngine;
import com.xuesinuo.pignoo.core.config.PrimaryKeyNamingConvention;

/**
 * 基础的Pignoo实现
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
public class TransactionPignoo implements Pignoo {

    private final DatabaseEngine engine;// 数据库引擎

    private final DataSource dataSource;// 数据源

    private Connection conn;// 数据库连接

    private boolean connAutoCommit;// 原本的conn是否自动提交

    private boolean hasRollbacked = false;// 是否已经回滚

    private boolean hasClosed = false;// 是否已经关闭

    private final AnnotationMode annotationMode;// 使用注解的方式

    private final AnnotationMixMode annotationMixMode;// 混用注解的方式

    private final PrimaryKeyNamingConvention primaryKeyNamingConvention;// 主键命名规则

    private final Boolean autoPrimaryKey;// 是否自动生成主键

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
        AnnotationMode annotationMode = null;
        AnnotationMixMode annotationMixMode = null;
        DatabaseEngine engine = null;
        PrimaryKeyNamingConvention primaryKeyNamingConvention = null;
        Boolean autoPrimaryKey = null;
        if (pignooConfig != null) {
            annotationMode = pignooConfig.getAnnotationMode();
            annotationMixMode = pignooConfig.getAnnotationMixMode();
            primaryKeyNamingConvention = pignooConfig.getPrimaryKeyNamingConvention();
            autoPrimaryKey = pignooConfig.getAutoPrimaryKey();
            engine = pignooConfig.getEngine();
        }
        if (dataSource == null) {
            throw new RuntimeException("Unknow dataSource");
        }
        this.dataSource = dataSource;
        if (engine == null) {
            try {
                this.getConnection();
                engine = DatabaseEngine.getDatabaseEngineByConnection(this.conn);
            } catch (SQLException e) {
                this.close();
                throw new RuntimeException(e);
            }
        }
        if (engine == null) {
            throw new RuntimeException("Unknow database engine");
        }
        this.engine = engine;
        this.annotationMode = annotationMode;
        this.annotationMixMode = annotationMixMode;
        this.primaryKeyNamingConvention = primaryKeyNamingConvention;
        this.autoPrimaryKey = autoPrimaryKey;
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
    public <E> PignooList<E> getList(Class<E> c) {
        switch (engine) {
        case MySQL:
            return new MySqlPignooList<E>(this, connGetter, connCloser, true, c, annotationMode, annotationMixMode, primaryKeyNamingConvention, autoPrimaryKey);
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
