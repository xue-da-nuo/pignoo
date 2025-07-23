package com.xuesinuo.pignoo.core;

import java.sql.Connection;
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
 * @version 0.1.0
 */
public interface SqlExecuter {

    /** 查询一个实体 */
    <E> E selectOne(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<E> c);

    /** 查询实体List */
    <E> List<E> selectList(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<E> c);

    /** 查询一个列 */
    <R> R selectColumn(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<R> c);

    /** 插入一个数据，返回主键 */
    <R> Object insert(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<R> c);

    /** 执行一个非查询，返回受影响行数 */
    long update(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params);
}
