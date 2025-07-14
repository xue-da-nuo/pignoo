package com.xuesinuo.pignoo.core.implement;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooList;

/**
 * 基础的Pignoo实现
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
public class BasePignoo implements Pignoo {

    private final DatabaseEngine engine;// 数据库引擎

    private final Connection conn;// 数据库连接

    private final boolean useTransaction;// 是否使用JDBC事务

    private final boolean connAutoCommit;// 原本的conn是否自动提交

    private boolean hasRollbacked = false;// 是否已经回滚

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
        DatabaseEngine engine = null;
        boolean useTransaction = false;
        if (pignooConfig != null) {
            engine = pignooConfig.getEngine();
            if (pignooConfig.getUseTransaction() != null) {
                useTransaction = pignooConfig.getUseTransaction();
            }
        }
        if (dataSource == null) {
            throw new RuntimeException("Unknow dataSource");
        }
        try {
            this.conn = dataSource.getConnection();
            if (engine == null) {
                engine = DatabaseEngine.getDatabaseEngineByConnection(this.conn);
            }
            if (engine == null) {
                throw new RuntimeException("Unknow database engine");
            }
            this.engine = engine;
            this.connAutoCommit = conn.getAutoCommit();
            if (useTransaction) {
                conn.setAutoCommit(false);
            }
        } catch (SQLException e) {
            this.close();
            throw new RuntimeException(e);
        }
        this.useTransaction = useTransaction;
    }

    @Override
    public <E> PignooList<E> getPignooList(Class<E> c) {
        switch (engine) {
        case MySQL:
            return new MySqlPignooList<E>(this, conn, useTransaction, c);
        }
        throw new RuntimeException("Unknow database engine");
    }

    public void rollback() {
        if (!useTransaction) {
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
        if (useTransaction) {
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
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasClosed() {
        return hasClosed;
    }
}
