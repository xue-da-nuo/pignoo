package com.xuesinuo.pignoo.spring.implement;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooList;

import lombok.extern.slf4j.Slf4j;

import com.xuesinuo.pignoo.core.config.DatabaseEngine;
import com.xuesinuo.pignoo.spring.config.PignooTransactionSynchronizationAdapter;

/**
 * 用于SpringBean的Pignoo实现，本质是一个线程化的PignooBean代理。
 * <p>
 * Pignoo implementation for SpringBean, essentially a threadized PignooBean proxy.
 * 
 * @author xuesinuo
 * @since 0.2.1
 */
@Slf4j
public class SpringPignoo implements Pignoo {

    private DataSource dataSource;// 数据源

    private final PignooConfig config;// 配置

    private SpringPignooItem basePignoo;// 基础Pignoo，用于非事务操作

    private final ThreadLocal<SpringPignooItem> transactionPignooThreadLocal = new ThreadLocal<>();// 事务Pignoo，每个线程分配一个

    private final ThreadLocal<Boolean> transactionPignooThreadLocalFlag = new ThreadLocal<>();// 事务已开启标志，开启时，会注册一个事务生命周期钩子

    private boolean hasClosed = false;// 是否已经关闭

    /**
     * 
     * @param dataSource 数据源
     *                   <p>
     *                   Data source
     */
    public SpringPignoo(DataSource dataSource) {
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
    public SpringPignoo(DataSource dataSource, PignooConfig pignooConfig) {
        DatabaseEngine engine = null;
        if (dataSource == null) {
            throw new RuntimeException("Unknow dataSource");
        }
        this.dataSource = dataSource;
        if (engine == null) {
            try (Connection conn = DataSourceUtils.getConnection(dataSource)) {
                engine = DatabaseEngine.getDatabaseEngineByConnection(conn);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        if (engine == null) {
            throw new RuntimeException("Unknow database engine");
        }
        if (pignooConfig == null) {
            pignooConfig = new PignooConfig();
        } else {
            pignooConfig = pignooConfig.copy();
        }
        if (pignooConfig.getEngine() == null) {
            pignooConfig.setEngine(engine);
        } else if (engine != pignooConfig.getEngine()) {
            throw new RuntimeException("Database engine mismatch");
        }
        this.config = pignooConfig;
        this.basePignoo = new SpringPignooItem(dataSource, pignooConfig, false);
    }

    @Override
    public <E> PignooList<E> getList(Class<E> c) {
        if (this.hasClosed) {
            throw new RuntimeException("Pignoo-Spring has closed!");
        }
        boolean inTransaction = TransactionSynchronizationManager.isActualTransactionActive();
        Pignoo pignoo = null;
        if (inTransaction) {
            synchronized (transactionPignooThreadLocal) {
                Boolean flag = transactionPignooThreadLocalFlag.get();
                if (flag == null || !flag) {
                    TransactionSynchronizationManager.registerSynchronization(new PignooTransactionSynchronizationAdapter(this));
                    transactionPignooThreadLocalFlag.set(true);
                }
                SpringPignooItem transactionPignoo = transactionPignooThreadLocal.get();
                if (transactionPignoo == null) {
                    transactionPignoo = new SpringPignooItem(this.dataSource, this.config, true);
                    transactionPignooThreadLocal.set(transactionPignoo);
                }
                pignoo = transactionPignoo;
            }
        } else {
            if (this.basePignoo.closed()) {
                this.basePignoo = new SpringPignooItem(dataSource, config, false);
            }
            pignoo = this.basePignoo;
        }
        return pignoo.getList(c);
    }

    public void closeSpringTransaction() {
        SpringPignooItem pignoo = transactionPignooThreadLocal.get();
        if (pignoo != null) {
            pignoo.close();
        }
        transactionPignooThreadLocal.remove();
        transactionPignooThreadLocalFlag.remove();
        log.debug("Once Pignoo-Spring transaction finished!");
    }

    @Override
    public void close() {
        this.basePignoo.close();
        this.hasClosed = true;
        this.dataSource = null;
        log.warn("Pignoo-Spring closed! Please confirm that the SpringIoC container is also closed, otherwise this is an abnormal shutdown!");
    }

    @Override
    public boolean closed() {
        return this.hasClosed;
    }
}
