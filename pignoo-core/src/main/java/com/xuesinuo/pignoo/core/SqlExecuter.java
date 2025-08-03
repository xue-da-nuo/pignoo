package com.xuesinuo.pignoo.core;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Pignoo的SQL执行器
 * <p>
 * SQL Executer in Pignoo
 *
 * @author xuesinuo
 * @since 0.1.0
 * @version 0.1.0
 */
public interface SqlExecuter {

    /**
     * 查询一个实体
     *
     * @param connGetter 获取连接的函数
     * @param connCloser 注销连接的函数
     * @param sql        要执行的SQL
     * @param params     SQL参数
     * @param c          执行结果的映射对象类型
     * @param <E>        执行结果的映射对象类型
     * @return 查询结果
     */
    <E> E selectOne(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<E> c);

    /**
     * 查询实体List
     *
     * @param connGetter 获取连接的函数
     * @param connCloser 注销连接的函数
     * @param sql        要执行的SQL
     * @param params     SQL参数
     * @param c          执行结果的映射对象类型
     * @param <E>        执行结果的映射对象类型
     * @return 查询结果
     */
    <E> List<E> selectList(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<E> c);

    /**
     * 查询Map结果
     *
     * @param connGetter 获取连接的函数
     * @param connCloser 注销连接的函数
     * @param sql        要执行的SQL
     * @param params     SQL参数
     * @return 查询结果
     */
    List<LinkedHashMap<String, String>> selectLinkedHashMap(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params);

    /**
     * 查询一个列
     *
     * @param connGetter 获取连接的函数
     * @param connCloser 注销连接的函数
     * @param sql        要执行的SQL
     * @param params     SQL参数
     * @param c          执行结果的映射数据类型（单个值）
     * @param <R>        执行结果的映射数据类型（单个值）
     * @return 查询结果
     */
    <R> R selectColumn(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<R> c);

    /**
     * 插入一个数据，返回主键
     *
     * @param connGetter 获取连接的函数
     * @param connCloser 注销连接的函数
     * @param sql        要执行的SQL
     * @param params     SQL参数
     * @param c          主键的映射数据类型（单个值）
     * @param <R>        主键的映射数据类型（单个值）
     * @return 主键
     */
    <R> Object insert(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<R> c);

    /**
     * 执行一个非查询，返回受影响行数
     *
     * @param connGetter 获取连接的函数
     * @param connCloser 注销连接的函数
     * @param sql        要执行的SQL
     * @param params     SQL参数
     * @return 受影响行数
     */
    long update(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params);
}
