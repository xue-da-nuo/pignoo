package com.xuesinuo.pignoo.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.sql.DataSource;

import com.xuesinuo.pignoo.core.Pignoo.DatabaseEngine;
import com.xuesinuo.pignoo.core.implement.BasePignoo;

/**
 * 格鲁（Gru） - 小黄人的主人
 * <p>
 * Gru - The owner of the Minions
 * <p>
 * Gru是一个Pignoo提供的原生DataSource的包装器，项目中每存在一个DataSource，就可以对应构建一个Gru实例
 * <p>
 * Gru is a wrapper of the native DataSource provided by Pignoo. Each DataSource in the project corresponds to a Gru instance
 * <p>
 * Gru可以帮你合理利用DataSource，避免忘记提交、回滚、关闭或返还连接
 * <p>
 * Gru can help you use DataSource in a more reasonable way, avoiding forgetting to commit, rollback, close or return the connection
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
public class Gru {
    private final DatabaseEngine engine;// 数据库引擎
    private final DataSource dataSource;// 数据源

    /**
     * 
     * @param dataSource 数据源
     *                   <p>
     *                   Data source
     * @param config     配置
     *                   <p>
     *                   Configuration
     */
    public Gru(DataSource dataSource, PignooConfig config) {
        DatabaseEngine engine = config.getEngine();
        try (Connection conn = dataSource.getConnection()) {
            if (conn == null) {
                throw new RuntimeException("DataSource Error: get connection failed");
            }
            if (engine == null) {
                engine = DatabaseEngine.getDatabaseEngineByConnection(conn);
            }
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Read database engine failed", e);
        }
        if (engine == null) {
            throw new RuntimeException("Unknow database engine");
        }
        this.engine = engine;
        this.dataSource = dataSource;
    }

    /**
     * 在非事务环境执行Pignoo
     * <p>
     * Execute Pignoo in a non-transactional environment
     * 
     * @param <R>      自定义返回值类型
     *                 <p>
     *                 Custom return value type
     * @param function 执行一段Pignoo方法
     *                 <p>
     *                 Execute a piece of Pignoo method
     * @return 自定义返回值
     *         <p>
     *         Custom return value
     */
    public <R> R run(Function<Pignoo, R> function) {
        PignooConfig config = new PignooConfig();
        config.setEngine(this.engine);
        config.setUseTransaction(false);
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, config)) {
            return function.apply(pignoo);
        }
    }

    /**
     * 在事务环境执行Pignoo
     * <p>
     * Execute Pignoo in a transactional environment
     * 
     * @param <R>      自定义返回值类型
     *                 <p>
     *                 Custom return value type
     * @param function 执行一段Pignoo方法
     *                 <p>
     *                 Execute a piece of Pignoo method
     * @return 自定义返回值
     *         <p>
     *         Custom return value
     */
    public void runTransaction(Consumer<Pignoo> function) {
        PignooConfig config = new PignooConfig();
        config.setEngine(this.engine);
        config.setUseTransaction(true);
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, config)) {
            try {
                function.accept(pignoo);
            } catch (Exception e) {
                pignoo.rollback();
                throw e;
            }
        }
    }
}
