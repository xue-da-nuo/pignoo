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

import lombok.extern.slf4j.Slf4j;

import com.xuesinuo.pignoo.core.config.DatabaseEngine;
import com.xuesinuo.pignoo.core.config.PrimaryKeyNamingConvention;

/**
 * 基础的Pignoo实现
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
@Slf4j
public class BasePignoo implements Pignoo {

    private final DatabaseEngine engine;// 数据库引擎

    private final DataSource dataSource;// 数据源

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
            try (Connection conn = dataSource.getConnection()) {
                engine = DatabaseEngine.getDatabaseEngineByConnection(conn);
            } catch (SQLException e) {
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
        switch (engine) {
        case MySQL:
            return new MySqlPignooList<E>(this, connGetter, connCloser, false, c, annotationMode, annotationMixMode, primaryKeyNamingConvention, autoPrimaryKey);
        }
        throw new RuntimeException("Unknow database engine");
    }

    @Override
    public void close() {
        this.hasClosed = true;
    }

    @Override
    public boolean closed() {
        return this.hasClosed;
    }
}
