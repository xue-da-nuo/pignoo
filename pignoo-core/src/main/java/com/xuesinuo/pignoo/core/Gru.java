package com.xuesinuo.pignoo.core;

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
    private final DatabaseEngine engine;
    private final DataSource dataSource;

    /**
     * 
     * @param engine     数据库引擎
     *                   <p>
     *                   Database engine
     * @param dataSource 数据源
     *                   <p>
     *                   Data source
     */
    public Gru(DatabaseEngine engine, DataSource dataSource) {
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
        try (BasePignoo pignoo = new BasePignoo(this.engine, this.dataSource, false)) {
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
    public <R> R runTransaction(Function<Pignoo, R> function) {
        try (BasePignoo pignoo = new BasePignoo(this.engine, this.dataSource, true)) {
            try {
                return function.apply(pignoo);
            } catch (Exception e) {
                pignoo.rollback();
                throw e;
            }
        }
    }
}
