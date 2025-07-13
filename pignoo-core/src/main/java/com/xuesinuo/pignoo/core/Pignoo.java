package com.xuesinuo.pignoo.core;

/**
 * Pignoo - 小黄人语的“无聊”。《卑鄙的我3》中小黄人们高呼“Pignoo”抗议格活过于无聊
 * <p>
 * Pignoo - The "boring" in the Minions's Language. The Minions call "Pignoo" to protest the boring life
 * <p>
 * Pignoo也是本框架的名称，Pignoo类是本框架的核心类
 * <p>
 * Pignoo is also the name of this framework, and the Pignoo class is the core class of this framework
 * <p>
 * 一个Pignoo实例，就是一个从DataSource中获取的Connection封装，在此基础上，Pignoo有着自己独特的数据操作方式
 * <p>
 * A Pignoo instance is a Connection encapsulated from a DataSource, on top of which, Pignoo has its own unique data operation way
 * <p>
 * 如果使用Pignoo来管理事务的提交、回滚、关闭或返还连接，请使用{@link Gru}构建Pignoo实例
 * <p>
 * If you use Pignoo to manage the commit, rollback, close or return the connection, please use {@link Gru} to build Pignoo instances
 * <p>
 * 也可以使用外部框架管理事务，这时构建Pignoo需传入一个有完善事务管理机制的DataSource实例
 * <p>
 * You can also use an external framework to manage transactions, at this time, you need to pass in a DataSource instance with a complete transaction management mechanism
 * <p>
 * Pignoo是一个需要关闭的({@link AutoCloseable})，在关闭Pignoo时执行的是JDBC事务的提交或回滚
 * <p>
 * Pignoo is a need to close ({@link AutoCloseable}), and when closing Pignoo, the execution is the commit or rollback of the JDBC transaction
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
public interface Pignoo extends AutoCloseable {

    /**
     * 数据库引擎
     * <p>
     * Database engine
     */
    public static enum DatabaseEngine {
        MySQL
    }

    /**
     * 获取一个PignooList实例，这是Pignoo的最核心用法
     * <p>
     * Get a PignooList instance, which is the most core usage of Pignoo
     * 
     * @param <E> 实体类型
     *            <p>
     *            Entity type
     * @param c   实体类型
     *            <p>
     *            Entity type
     * @return PignooList
     */
    public <E> PignooList<E> getPignooList(Class<E> c);

    /**
     * 连接是否已经关闭
     * <p>
     * Whether the connection has been closed
     */
    public boolean hasClosed();
}
