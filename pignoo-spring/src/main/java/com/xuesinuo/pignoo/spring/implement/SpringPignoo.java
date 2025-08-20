package com.xuesinuo.pignoo.spring.implement;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooWriter;
import com.xuesinuo.pignoo.core.PignooReader;

import lombok.extern.slf4j.Slf4j;

import com.xuesinuo.pignoo.core.config.DatabaseEngine;
import com.xuesinuo.pignoo.core.exception.DataSourceException;
import com.xuesinuo.pignoo.core.exception.PignooRuntimeException;
import com.xuesinuo.pignoo.spring.config.PignooTransactionSynchronizationAdapter;

/**
 * 用于SpringBean的Pignoo实现，本质是一个线程化的PignooBean代理。
 * <p>
 * Pignoo implementation for SpringBean, essentially a threadized PignooBean proxy.
 * <p>
 * 融入Spirng时，只需正确构造SpringPignoo，并注入Spring容器中即可。
 * <p>
 * When integrated into Spirng, just construct SpringPignoo correctly and inject it into the Spring container.
 * <p>
 *
 * <pre>
 * // Example:
 * &#64;Configuration
 * public class SpringPignooConfig {
 *     &#64;Bean
 *     Pignoo pignoo(&#64;Autowired DataSource dataSource) {
 *         return new SpringPignoo(dataSource);
 *     }
 * }
 * </pre>
 *
 * @author xuesinuo
 * @since 0.2.1
 * @version 0.2.1
 */
@Slf4j
public class SpringPignoo implements Pignoo {

    private DataSource dataSource;// 数据源

    private final PignooConfig config;// 配置

    private SpringPignooItem basePignoo;// 基础Pignoo，用于非事务操作

    private final ThreadLocal<SpringPignooItem> transactionPignooThreadLocal = new ThreadLocal<>();// 事务Pignoo，每个线程分配一个（支持JTA）

    private boolean hasClosed = false;// 是否已经关闭

    /**
     * 构造器
     * <p>
     * Constructor
     *
     * @param dataSource 数据源
     *                   <p>
     *                   Data source
     */
    public SpringPignoo(DataSource dataSource) {
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
    public SpringPignoo(DataSource dataSource, PignooConfig pignooConfig) {
        DatabaseEngine engine = null;
        if (dataSource == null) {
            throw new DataSourceException("Unknow dataSource");
        }
        this.dataSource = dataSource;
        if (engine == null) {
            try (Connection conn = DataSourceUtils.getConnection(dataSource)) {
                engine = DatabaseEngine.getDatabaseEngineByConnection(conn);
            } catch (SQLException e) {
                throw new DataSourceException("Search database engine error", e);
            }
        }
        if (engine == null) {
            throw new DataSourceException("Unknow database engine");
        }
        if (pignooConfig == null) {
            pignooConfig = new PignooConfig();
        } else {
            pignooConfig = pignooConfig.copy();
        }
        if (pignooConfig.getEngine() == null) {
            pignooConfig.setEngine(engine);
        } else if (engine != pignooConfig.getEngine()) {
            throw new DataSourceException("Database engine mismatch");
        }
        this.config = pignooConfig;
        this.basePignoo = new SpringPignooItem(dataSource, pignooConfig, false);
    }

    @Override
    public <E> PignooWriter<E> writer(Class<E> c) {
        return this.getPignoo().writer(c);
    }

    @Override
    public <E> PignooReader<E> reader(Class<E> c) {
        return this.getPignoo().reader(c);
    }

    private SpringPignooItem getPignoo() {
        if (this.hasClosed) {
            throw new PignooRuntimeException("Pignoo-Spring has closed!");
        }
        boolean inTransaction = TransactionSynchronizationManager.isActualTransactionActive();
        SpringPignooItem pignoo = null;
        if (inTransaction) {
            SpringPignooItem transactionPignoo = transactionPignooThreadLocal.get();
            if (transactionPignoo == null) {
                TransactionSynchronizationManager.registerSynchronization(new PignooTransactionSynchronizationAdapter(this));
                transactionPignoo = new SpringPignooItem(this.dataSource, this.config, true);
                transactionPignooThreadLocal.set(transactionPignoo);
            }
            pignoo = transactionPignoo;
        } else {
            if (this.basePignoo.closed()) {
                this.basePignoo = new SpringPignooItem(dataSource, config, false);
            }
            pignoo = this.basePignoo;
        }
        return pignoo;
    }

    /**
     * 关闭Spring事务时的关闭资源规则
     * <p>
     * Close resource rules when closing Spring transactions
     */
    public void closeSpringTransaction() {
        SpringPignooItem pignoo = transactionPignooThreadLocal.get();
        if (pignoo != null) {
            pignoo.close();
        }
        transactionPignooThreadLocal.remove();
        log.debug("一次Spring-Pignoo事务完成了，并正确得回收了资源！");
        log.debug("Once Spring-Pignoo transaction finished!");
    }

    @Override
    public void close() {
        this.basePignoo.close();
        this.hasClosed = true;
        this.dataSource = null;
        log.warn("Spring-Pignoo 被关闭了！请确认SpringIoC容器也正在关闭，否则这是一次错误的Spring-Pignoo关闭！");
        log.warn("Spring-Pignoo closed! Please confirm that the SpringIoC container is also closed, otherwise this is an abnormal shutdown!");
    }

    @Override
    public boolean closed() {
        return this.hasClosed;
    }
}
